package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole
import com.example.poprogknowledgebaseback.domain.assistant.ChatConversation
import com.example.poprogknowledgebaseback.domain.assistant.ChatConversationNotFoundException
import com.example.poprogknowledgebaseback.domain.assistant.StoredChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.port.ChatConversationPersistencePort
import java.time.Clock
import java.util.UUID
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
@ConditionalOnProperty(name = ["app.gigachat.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpAiAssistantService(
    private val chatConversationPersistencePort: ChatConversationPersistencePort,
    private val clock: Clock,
    private val environment: Environment
) : AiAssistantUseCase {

    @Transactional
    override fun chat(command: AssistantChatCommand): AssistantChatResult {
        require(command.messages.isNotEmpty()) { "At least one chat message is required" }

        if (!isLocalLikeProfile()) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "GigaChat integration is disabled"
            )
        }

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

        val lastUserMessage = command.messages.lastOrNull { it.role == AiChatMessageRole.USER }?.content?.trim().orEmpty()
        val fallbackResponse = if (lastUserMessage.isBlank()) {
            "Локальный режим: интеграция GigaChat отключена. История чата сохранена, но ответ модели сейчас недоступен."
        } else {
            "Локальный режим: интеграция GigaChat отключена. История чата сохранена, но полноценный ответ модели сейчас недоступен. Последний запрос: \"$lastUserMessage\"."
        }

        persistConversationMessages(conversation.id, command.messages, fallbackResponse)

        return AssistantChatResult(
            chatId = conversation.id,
            content = fallbackResponse,
            model = "local-fallback",
            finishReason = "stop",
            promptTokens = null,
            completionTokens = null,
            totalTokens = null,
            documentHints = emptyList()
        )
    }

    private fun isLocalLikeProfile(): Boolean {
        val activeProfiles = environment.activeProfiles.map { it.lowercase() }.toSet()
        return activeProfiles.any { it == "local" || it == "dev" }
    }

    private fun persistConversationMessages(
        chatId: UUID,
        requestMessages: List<com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage>,
        assistantResponse: String
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
            content = assistantResponse,
            createdAt = now.plusMillis(requestMessages.size.toLong())
        )

        chatConversationPersistencePort.saveMessages(messages)
    }
}
