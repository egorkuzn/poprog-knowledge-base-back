package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole
import java.time.Instant
import java.util.UUID

data class AssistantChatCommand(
    val chatId: UUID?,
    val messages: List<AiChatMessage>
)

data class AssistantChatResult(
    val chatId: UUID,
    val content: String,
    val model: String,
    val finishReason: String?,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)

data class ChatHistoryResult(
    val chatId: UUID,
    val messages: List<ChatHistoryMessageResult>
)

data class ChatHistoryMessageResult(
    val id: Long,
    val role: AiChatMessageRole,
    val content: String,
    val createdAt: Instant
)
