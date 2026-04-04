package com.example.poprogknowledgebaseback.application.assistant

interface AiAssistantUseCase {
    fun chat(command: AssistantChatCommand): AssistantChatResult
}
