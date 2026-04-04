package com.example.poprogknowledgebaseback.domain.assistant.port

import com.example.poprogknowledgebaseback.domain.assistant.ChatConversation
import com.example.poprogknowledgebaseback.domain.assistant.StoredChatMessage
import java.util.UUID

interface ChatConversationPersistencePort {
    fun findConversationById(chatId: UUID): ChatConversation?
    fun saveConversation(conversation: ChatConversation): ChatConversation
    fun saveMessages(messages: List<StoredChatMessage>): List<StoredChatMessage>
    fun findMessagesByChatIdOrderByCreatedAtAscIdAsc(chatId: UUID): List<StoredChatMessage>
}
