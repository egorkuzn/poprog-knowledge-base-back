package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.domain.assistant.ChatConversationNotFoundException
import com.example.poprogknowledgebaseback.domain.assistant.port.ChatConversationPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ChatHistoryService(
    private val chatConversationPersistencePort: ChatConversationPersistencePort
) : ChatHistoryUseCase {

    @Transactional(readOnly = true)
    override fun getHistory(chatId: UUID): ChatHistoryResult {
        chatConversationPersistencePort.findConversationById(chatId)
            ?: throw ChatConversationNotFoundException(chatId)

        val messages = chatConversationPersistencePort.findMessagesByChatIdOrderByCreatedAtAscIdAsc(chatId)

        return ChatHistoryResult(
            chatId = chatId,
            messages = messages.map { message ->
                ChatHistoryMessageResult(
                    id = message.id ?: error("Stored chat message id was not generated"),
                    role = message.role,
                    content = message.content,
                    createdAt = message.createdAt
                )
            }
        )
    }
}
