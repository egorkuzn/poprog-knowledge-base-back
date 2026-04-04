package com.example.poprogknowledgebaseback.domain.assistant.port

import com.example.poprogknowledgebaseback.domain.assistant.AiAssistantResponse
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage

interface AiAssistantPort {
    fun complete(messages: List<AiChatMessage>): AiAssistantResponse
}
