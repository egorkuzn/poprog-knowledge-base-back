package com.example.poprogknowledgebaseback.application.account

import com.example.poprogknowledgebaseback.domain.assistant.port.ChatConversationPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AccountChatSummaryResult(
    val chatId: String,
    val createdAt: String,
    val messageCount: Int,
    val lastMessagePreview: String?
)

@Service
class AccountChatService(
    private val chatConversationPersistencePort: ChatConversationPersistencePort
) {

    @Transactional(readOnly = true)
    fun getChats(ownerSub: String, limit: Int = 50): List<AccountChatSummaryResult> {
        return chatConversationPersistencePort.findConversationsByOwnerSubOrderByCreatedAtDesc(ownerSub, limit)
            .map { conversation ->
                val messages = chatConversationPersistencePort.findMessagesByChatIdOrderByCreatedAtAscIdAsc(conversation.id)
                val last = messages.lastOrNull()?.content
                    ?.replace(Regex("\\s+"), " ")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { content -> if (content.length > 180) content.take(180) + "…" else content }

                AccountChatSummaryResult(
                    chatId = conversation.id.toString(),
                    createdAt = conversation.createdAt.toString(),
                    messageCount = messages.size,
                    lastMessagePreview = last
                )
            }
    }
}
