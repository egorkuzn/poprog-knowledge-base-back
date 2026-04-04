package com.example.poprogknowledgebaseback.domain.assistant

data class AiAssistantResponse(
    val content: String,
    val model: String,
    val finishReason: String?,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)
