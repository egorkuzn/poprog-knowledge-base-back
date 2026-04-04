package com.example.poprogknowledgebaseback.domain.assistant

import java.util.UUID

class ChatConversationNotFoundException(chatId: UUID) :
    RuntimeException("Chat conversation with id=$chatId was not found")
