package com.example.poprogknowledgebaseback.domain.assistant

import java.time.Instant
import java.util.UUID

data class StoredChatMessage(
    val id: Long? = null,
    val chatId: UUID,
    val role: AiChatMessageRole,
    val content: String,
    val createdAt: Instant
)
