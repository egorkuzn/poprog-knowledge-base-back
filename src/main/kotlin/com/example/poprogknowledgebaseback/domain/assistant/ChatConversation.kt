package com.example.poprogknowledgebaseback.domain.assistant

import java.time.Instant
import java.util.UUID

data class ChatConversation(
    val id: UUID,
    val createdAt: Instant
)
