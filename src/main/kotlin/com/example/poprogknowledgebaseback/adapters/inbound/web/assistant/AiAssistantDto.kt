package com.example.poprogknowledgebaseback.adapters.inbound.web.assistant

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import java.time.Instant
import java.util.UUID

data class AiAssistantChatRequest(
    val chatId: UUID? = null,
    @field:Valid
    @field:NotEmpty
    val messages: List<AiAssistantChatMessageRequest>
)

data class AiAssistantChatMessageRequest(
    @field:Pattern(regexp = "system|user|assistant")
    val role: String,
    @field:NotBlank
    val content: String
)

data class AiAssistantChatResponse(
    val chatId: UUID,
    val content: String,
    val model: String,
    val finishReason: String?,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)

data class ChatHistoryResponse(
    val chatId: UUID,
    val messages: List<ChatHistoryMessageResponse>
)

data class ChatHistoryMessageResponse(
    val id: Long,
    val role: String,
    val content: String,
    val createdAt: Instant
)
