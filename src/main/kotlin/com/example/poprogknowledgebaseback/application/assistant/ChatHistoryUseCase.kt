package com.example.poprogknowledgebaseback.application.assistant

import java.util.UUID

interface ChatHistoryUseCase {
    fun getHistory(chatId: UUID): ChatHistoryResult
}
