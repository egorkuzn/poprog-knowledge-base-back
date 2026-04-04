package com.example.poprogknowledgebaseback.adapters.outbound.persistence.assistant

import com.example.poprogknowledgebaseback.domain.assistant.ChatConversation
import com.example.poprogknowledgebaseback.domain.assistant.StoredChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.port.ChatConversationPersistencePort
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ChatConversationPersistenceAdapter(
    private val chatConversationRepository: SpringDataChatConversationRepository,
    private val chatMessageRepository: SpringDataChatMessageRepository
) : ChatConversationPersistencePort {

    override fun findConversationById(chatId: UUID): ChatConversation? =
        chatConversationRepository.findById(chatId).orElse(null)?.toDomain()

    override fun saveConversation(conversation: ChatConversation): ChatConversation =
        chatConversationRepository.save(conversation.toEntity()).toDomain()

    override fun saveMessages(messages: List<StoredChatMessage>): List<StoredChatMessage> =
        chatMessageRepository.saveAll(messages.map { it.toEntity() }).map { it.toDomain() }

    override fun findMessagesByChatIdOrderByCreatedAtAscIdAsc(chatId: UUID): List<StoredChatMessage> =
        chatMessageRepository.findAllByChatIdOrderByCreatedAtAscIdAsc(chatId).map { it.toDomain() }

    private fun ChatConversationJpaEntity.toDomain() = ChatConversation(
        id = id,
        createdAt = createdAt
    )

    private fun ChatConversation.toEntity() = ChatConversationJpaEntity(
        id = id,
        createdAt = createdAt
    )

    private fun ChatMessageJpaEntity.toDomain() = StoredChatMessage(
        id = id,
        chatId = chatId,
        role = role,
        content = content,
        createdAt = createdAt
    )

    private fun StoredChatMessage.toEntity() = ChatMessageJpaEntity(
        id = id,
        chatId = chatId,
        role = role,
        content = content,
        createdAt = createdAt
    )
}
