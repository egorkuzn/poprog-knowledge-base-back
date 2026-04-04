package com.example.poprogknowledgebaseback.adapters.inbound.web.assistant

import com.example.poprogknowledgebaseback.domain.assistant.ChatConversationNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AiAssistantExceptionHandler {

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleAssistantUnavailable(ex: IllegalStateException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            ex.message ?: "AI assistant integration is unavailable"
        )

    @ExceptionHandler(ChatConversationNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleConversationNotFound(ex: ChatConversationNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.message ?: "Chat conversation was not found"
        )
}
