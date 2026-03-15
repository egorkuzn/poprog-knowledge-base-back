package com.example.poprogknowledgebaseback.adapters.inbound.web.publication

import com.example.poprogknowledgebaseback.domain.publication.PublicationNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class PublicationExceptionHandler {

    @ExceptionHandler(PublicationNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlePublicationNotFound(ex: PublicationNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Publication not found")
}
