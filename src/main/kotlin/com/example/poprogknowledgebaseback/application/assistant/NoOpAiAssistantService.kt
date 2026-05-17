package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole
import com.example.poprogknowledgebaseback.domain.assistant.ChatConversation
import com.example.poprogknowledgebaseback.domain.assistant.ChatConversationNotFoundException
import com.example.poprogknowledgebaseback.domain.assistant.StoredChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.port.ChatConversationPersistencePort
import java.time.Clock
import java.util.UUID
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
@ConditionalOnProperty(name = ["app.gigachat.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpAiAssistantService(
    private val chatConversationPersistencePort: ChatConversationPersistencePort,
    private val assistantWidgetRouter: AssistantWidgetRouter,
    private val clock: Clock,
    private val objectMapper: ObjectMapper
) : AiAssistantUseCase {

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
            persistConversationMessages(conversation.id, command.messages, widgetContent, widgetResponse)
            return AssistantChatResult(
                chatId = conversation.id,
                content = widgetContent,
                model = "widget-router",
                finishReason = "stop",
                promptTokens = null,
                completionTokens = null,
                totalTokens = null,
                documentHints = emptyList(),
                mode = AssistantResponseMode.WIDGET,
                widget = widgetResponse
            )
        }

        val lastUserMessage = command.messages.lastOrNull { it.role == AiChatMessageRole.USER }?.content?.trim().orEmpty()
        val fallbackResponse = if (lastUserMessage.isBlank()) {
            "Режим базового ассистента: внешний ИИ-сервис временно недоступен. История чата сохранена."
        } else {
            "Режим базового ассистента: внешний ИИ-сервис временно недоступен. История чата сохранена. Ваш последний запрос: \"$lastUserMessage\"."
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
            documentHints = emptyList(),
            mode = AssistantResponseMode.TEXT
        )
    }

    private fun persistConversationMessages(
        chatId: UUID,
        requestMessages: List<com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage>,
        assistantResponse: String,
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
            content = assistantResponse,
            widgetPayload = widgetResponse?.let { objectMapper.writeValueAsString(it) },
            createdAt = now.plusMillis(requestMessages.size.toLong())
        )

        chatConversationPersistencePort.saveMessages(messages)
    }
}
