package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.domain.assistant.AiAssistantResponse
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole
import com.example.poprogknowledgebaseback.domain.assistant.ChatConversation
import com.example.poprogknowledgebaseback.domain.assistant.ChatConversationNotFoundException
import com.example.poprogknowledgebaseback.domain.assistant.StoredChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.port.AiAssistantPort
import com.example.poprogknowledgebaseback.domain.assistant.port.ChatConversationPersistencePort
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType
import java.time.Clock
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
@ConditionalOnProperty(name = ["app.gigachat.enabled"], havingValue = "true")
class AiAssistantService(
    private val aiAssistantPort: AiAssistantPort,
    private val chatConversationPersistencePort: ChatConversationPersistencePort,
    private val documentQuestionResolver: DocumentQuestionResolver,
    private val contextPromptBuilder: AssistantContextPromptBuilder,
    private val assistantWidgetRouter: AssistantWidgetRouter,
    private val clock: Clock,
    private val environment: Environment,
    private val objectMapper: ObjectMapper
) : AiAssistantUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun chat(command: AssistantChatCommand): AssistantChatResult {
        require(command.messages.isNotEmpty()) { "At least one chat message is required" }

        val chatId = command.chatId ?: UUID.randomUUID()
        val conversation = command.chatId?.let { existingChatId ->
            val existingConversation = chatConversationPersistencePort.findConversationById(existingChatId)
                ?: throw ChatConversationNotFoundException(existingChatId)
            if (existingConversation.ownerSub != null && command.requesterSub != existingConversation.ownerSub) {
                throw ChatConversationNotFoundException(existingChatId)
            }
            existingConversation
        } ?: chatConversationPersistencePort.saveConversation(
            ChatConversation(
                id = chatId,
                createdAt = clock.instant(),
                ownerSub = command.requesterSub
            )
        )

        val widgetResponse = assistantWidgetRouter.resolve(command)
        if (widgetResponse != null) {
            val widgetContent = widgetResponse.subtitle ?: widgetResponse.title
            val assistantResponse = AiAssistantResponse(
                content = widgetContent,
                model = "widget-router",
                finishReason = "stop",
                promptTokens = null,
                completionTokens = null,
                totalTokens = null
            )
            persistConversationMessages(conversation.id, command.messages, assistantResponse, widgetResponse)
            return AssistantChatResult(
                chatId = conversation.id,
                content = widgetContent,
                model = assistantResponse.model,
                finishReason = assistantResponse.finishReason,
                promptTokens = assistantResponse.promptTokens,
                completionTokens = assistantResponse.completionTokens,
                totalTokens = assistantResponse.totalTokens,
                documentHints = emptyList(),
                mode = AssistantResponseMode.WIDGET,
                widget = widgetResponse
            )
        }

        val history = chatConversationPersistencePort.findMessagesByChatIdOrderByCreatedAtAscIdAsc(conversation.id)
            .map { AiChatMessage(role = it.role, content = it.content) }

        val documentContext = buildDocumentContext(command)
        val documentHints = documentContext.hints
        val systemPrompt = documentContext.systemPrompt
        val messageList = if (systemPrompt.isBlank()) {
            history + command.messages
        } else {
            listOf(
                AiChatMessage(role = AiChatMessageRole.SYSTEM, content = systemPrompt)
            ) + history + command.messages
        }

        val assistantResponse = runCatching { aiAssistantPort.complete(messageList) }
            .getOrElse { error ->
                if (!isLocalLikeProfile()) {
                    throw error
                }

                log.warn("GigaChat request failed in local/dev profile. Returning local fallback response.", error)
                buildLocalFallbackResponse(command.messages)
            }
        persistConversationMessages(conversation.id, command.messages, assistantResponse)

        return AssistantChatResult(
            chatId = conversation.id,
            content = assistantResponse.content,
            model = assistantResponse.model,
            finishReason = assistantResponse.finishReason,
            promptTokens = assistantResponse.promptTokens,
            completionTokens = assistantResponse.completionTokens,
            totalTokens = assistantResponse.totalTokens,
            documentHints = documentHints.map { it.toHint() },
            mode = AssistantResponseMode.TEXT
        )
    }

    private fun buildDocumentContext(command: AssistantChatCommand): DocumentContext {
        val userMessage = command.messages.lastOrNull { it.role == AiChatMessageRole.USER }?.content?.trim().orEmpty()
        if (userMessage.isBlank()) {
            return DocumentContext(emptyList(), "")
        }

        val ref = command.documentRef
        val preferredType = ref?.sourceType?.let { sourceType ->
            SearchSourceType.values().firstOrNull { it.name.equals(sourceType, ignoreCase = true) }
        }
        val explicitUuid = ref?.sourceUuid
            ?.let { runCatching { UUID.fromString(it.trim()) }.getOrNull() }

        val uuidsFromMessages = if (explicitUuid == null) {
            documentQuestionResolver.extractUuids(command.messages.map { it.content })
        } else {
            emptyList()
        }
        val questionForSearch = sanitizeQuestion(userMessage, listOfNotNull(explicitUuid) + uuidsFromMessages)

        val candidateUuids = listOfNotNull(explicitUuid) + uuidsFromMessages
        if (candidateUuids.isNotEmpty()) {
            val candidates = candidateUuids.flatMap { sourceUuid ->
                documentQuestionResolver.resolveCandidatesByUuid(sourceUuid, preferredType)
            }.distinctBy { Triple(it.sourceType, it.sourceId, it.sourceUuid) }

            if (candidates.isNotEmpty()) {
                val matches = candidates.mapNotNull { candidate ->
                    val chunks = documentQuestionResolver.resolveChunksForDocument(
                        question = questionForSearch,
                        sourceType = candidate.sourceType,
                        sourceId = candidate.sourceId
                    )
                    if (chunks.isEmpty()) {
                        null
                    } else {
                        buildDocumentSearchResult(candidate, chunks)
                    }
                }

                if (matches.isNotEmpty()) {
                    val systemPrompt = contextPromptBuilder.buildSystemPrompt(matches)
                    return DocumentContext(matches, systemPrompt)
                }
            }
        }

        val hints = documentQuestionResolver.resolveBestDocuments(questionForSearch)
        if (hints.isEmpty()) {
            return DocumentContext(emptyList(), "")
        }
        val systemPrompt = contextPromptBuilder.buildSystemPrompt(hints)
        return DocumentContext(hints, systemPrompt)
    }

    private fun DocumentSearchResult.toHint() = AssistantDocumentHint(
        sourceType = sourceType.name.lowercase(),
        sourceUuid = sourceUuid,
        scoreHint = scoreHint,
        groupTitle = groupTitle,
        groupHash = groupHash,
        authors = authors,
        theme = theme,
        published = published,
        link = link,
        snippet = snippet
    )

    private data class DocumentContext(
        val hints: List<DocumentSearchResult>,
        val systemPrompt: String
    )

    private fun buildDocumentSearchResult(
        candidate: DocumentCandidate,
        chunks: List<com.example.poprogknowledgebaseback.domain.search.SearchChunk>
    ): DocumentSearchResult {
        val first = chunks.first()
        val snippet = chunks.joinToString(" ") { it.content }
            .replace(Regex("\\s+"), " ")
            .trim()
            .let { text -> if (text.length > 320) text.take(320) + "…" else text }

        return DocumentSearchResult(
            sourceType = candidate.sourceType,
            sourceId = candidate.sourceId,
            sourceUuid = candidate.sourceUuid,
            scoreHint = chunks.size,
            groupTitle = first.groupTitle,
            groupHash = first.groupHash,
            authors = first.authors,
            theme = first.theme,
            published = first.published,
            link = first.link,
            snippet = snippet
        )
    }

    private fun sanitizeQuestion(message: String, uuids: List<UUID>): String {
        if (uuids.isEmpty()) {
            return message
        }
        var sanitized = message
        uuids.forEach { uuid ->
            sanitized = sanitized.replace(uuid.toString(), " ", ignoreCase = true)
        }
        sanitized = sanitized.replace(Regex("\\s+"), " ").trim()
        return if (sanitized.length >= 3) sanitized else message
    }

    private fun isLocalLikeProfile(): Boolean {
        val activeProfiles = environment.activeProfiles.map { it.lowercase() }.toSet()
        return activeProfiles.any { it == "local" || it == "dev" }
    }

    private fun buildLocalFallbackResponse(requestMessages: List<AiChatMessage>): AiAssistantResponse {
        val lastUserMessage = requestMessages.lastOrNull { it.role == AiChatMessageRole.USER }?.content?.trim().orEmpty()
        val content = if (lastUserMessage.isBlank()) {
            "Режим базового ассистента: внешний ИИ-сервис временно недоступен. История чата сохранена."
        } else {
            "Режим базового ассистента: внешний ИИ-сервис временно недоступен. История чата сохранена. Ваш последний запрос: \"$lastUserMessage\"."
        }

        return AiAssistantResponse(
            content = content,
            model = "local-fallback",
            finishReason = "stop",
            promptTokens = null,
            completionTokens = null,
            totalTokens = null
        )
    }

    private fun persistConversationMessages(
        chatId: UUID,
        requestMessages: List<AiChatMessage>,
        assistantResponse: AiAssistantResponse,
        widgetResponse: AssistantWidgetResult? = null
    ) {
        val now = clock.instant()
        val messages = requestMessages.mapIndexed { index, message ->
            StoredChatMessage(
                chatId = chatId,
                role = message.role,
                content = message.content,
                createdAt = now.plusMillis(index.toLong())
            )
        } + StoredChatMessage(
            chatId = chatId,
            role = AiChatMessageRole.ASSISTANT,
            content = assistantResponse.content,
            widgetPayload = widgetResponse?.let { objectMapper.writeValueAsString(it) },
            createdAt = now.plusMillis(requestMessages.size.toLong())
        )

        chatConversationPersistencePort.saveMessages(messages)
    }
}
