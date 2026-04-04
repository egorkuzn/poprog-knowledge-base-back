package com.example.poprogknowledgebaseback.application.assistant

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["app.gigachat.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpAiAssistantService : AiAssistantUseCase {

    override fun chat(command: AssistantChatCommand): AssistantChatResult {
        error("GigaChat integration is disabled")
    }
}
