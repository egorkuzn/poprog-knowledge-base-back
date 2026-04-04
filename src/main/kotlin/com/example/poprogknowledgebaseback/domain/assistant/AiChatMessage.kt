package com.example.poprogknowledgebaseback.domain.assistant

data class AiChatMessage(
    val role: AiChatMessageRole,
    val content: String
)

enum class AiChatMessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}
