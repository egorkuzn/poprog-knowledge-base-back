package com.example.poprogknowledgebaseback.adapters.inbound.web.projectmenu

import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuItemNotFoundException
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuPromoNotFoundException
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuSectionHashAlreadyExistsException
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuSectionNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ProjectMenuExceptionHandler {

    @ExceptionHandler(ProjectMenuSectionNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleSectionNotFound(ex: ProjectMenuSectionNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Project menu section not found")

    @ExceptionHandler(ProjectMenuItemNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleItemNotFound(ex: ProjectMenuItemNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Project menu item not found")

    @ExceptionHandler(ProjectMenuPromoNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlePromoNotFound(ex: ProjectMenuPromoNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Project menu promo not found")

    @ExceptionHandler(ProjectMenuSectionHashAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleHashConflict(ex: ProjectMenuSectionHashAlreadyExistsException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message ?: "Project menu section hash already exists")
}
