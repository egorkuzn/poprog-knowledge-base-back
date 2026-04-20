package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.domain.assistant.ChatConversationNotFoundException
import com.example.poprogknowledgebaseback.domain.assistant.port.ChatConversationPersistencePort
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class ChatHistoryService(
    private val chatConversationPersistencePort: ChatConversationPersistencePort,
    private val objectMapper: ObjectMapper
) : ChatHistoryUseCase {

    @Transactional(readOnly = true)
    override fun getHistory(chatId: UUID, requesterSub: String?): ChatHistoryResult {
        val conversation = chatConversationPersistencePort.findConversationById(chatId)
            ?: throw ChatConversationNotFoundException(chatId)
        if (conversation.ownerSub != null && requesterSub != conversation.ownerSub) {
            throw ChatConversationNotFoundException(chatId)
        }

        val messages = chatConversationPersistencePort.findMessagesByChatIdOrderByCreatedAtAscIdAsc(chatId)

        return ChatHistoryResult(
            chatId = chatId,
            messages = messages.map { message ->
                ChatHistoryMessageResult(
                    id = message.id ?: error("Stored chat message id was not generated"),
                    role = message.role,
                    content = message.content,
                    widget = message.widgetPayload?.let { objectMapper.readValue(it, AssistantWidgetResult::class.java) },
                    createdAt = message.createdAt
                )
            }
        )
    }
}
