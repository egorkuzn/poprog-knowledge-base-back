package com.example.poprogknowledgebaseback.adapters.inbound.web.studentwork

import com.example.poprogknowledgebaseback.domain.studentwork.ProjectTypeNotFoundException
import com.example.poprogknowledgebaseback.domain.studentwork.StudentWorkNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class StudentWorkExceptionHandler {

    @ExceptionHandler(StudentWorkNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleStudentWorkNotFound(ex: StudentWorkNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Student work not found")

    @ExceptionHandler(ProjectTypeNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleProjectTypeNotFound(ex: ProjectTypeNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Project type not found")
}
