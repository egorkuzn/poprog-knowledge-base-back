package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.domain.assistant.AiAssistantResponse
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole
import com.example.poprogknowledgebaseback.domain.assistant.ChatConversation
import com.example.poprogknowledgebaseback.domain.assistant.ChatConversationNotFoundException
import com.example.poprogknowledgebaseback.domain.assistant.StoredChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.port.AiAssistantPort
import com.example.poprogknowledgebaseback.domain.assistant.port.ChatConversationPersistencePort
import java.time.Clock
import java.util.UUID
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ConditionalOnProperty(name = ["app.gigachat.enabled"], havingValue = "true")
class AiAssistantService(
    private val aiAssistantPort: AiAssistantPort,
    private val chatConversationPersistencePort: ChatConversationPersistencePort,
    private val clock: Clock
) : AiAssistantUseCase {

    @Transactional
    override fun chat(command: AssistantChatCommand): AssistantChatResult {
        require(command.messages.isNotEmpty()) { "At least one chat message is required" }

        val chatId = command.chatId ?: UUID.randomUUID()
        val conversation = command.chatId?.let { existingChatId ->
            chatConversationPersistencePort.findConversationById(existingChatId)
                ?: throw ChatConversationNotFoundException(existingChatId)
        } ?: chatConversationPersistencePort.saveConversation(
            ChatConversation(
                id = chatId,
                createdAt = clock.instant()
            )
        )

        val history = chatConversationPersistencePort.findMessagesByChatIdOrderByCreatedAtAscIdAsc(conversation.id)
            .map { AiChatMessage(role = it.role, content = it.content) }

        val assistantResponse = aiAssistantPort.complete(history + command.messages)
        persistConversationMessages(conversation.id, command.messages, assistantResponse)

        return AssistantChatResult(
            chatId = conversation.id,
            content = assistantResponse.content,
            model = assistantResponse.model,
            finishReason = assistantResponse.finishReason,
            promptTokens = assistantResponse.promptTokens,
            completionTokens = assistantResponse.completionTokens,
            totalTokens = assistantResponse.totalTokens
        )
    }

    private fun persistConversationMessages(
        chatId: UUID,
        requestMessages: List<AiChatMessage>,
        assistantResponse: AiAssistantResponse
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
            createdAt = now.plusMillis(requestMessages.size.toLong())
        )

        chatConversationPersistencePort.saveMessages(messages)
    }
}
