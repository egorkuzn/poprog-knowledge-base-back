package com.example.poprogknowledgebaseback.adapters.inbound.web.feedback

import com.example.poprogknowledgebaseback.application.feedback.CreateSiteFeedbackCommand
import com.example.poprogknowledgebaseback.application.feedback.SiteFeedbackService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Обратная связь", description = "Сбор реакции пользователя о полезности сайта")
class SiteFeedbackController(
    private val siteFeedbackService: SiteFeedbackService
) {

    @PostMapping("/usefulness")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Сохранить реакцию пользователя о полезности сайта",
        description = "Публичная ручка для сбора оценки полезности сайта и необязательного комментария."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Реакция успешно сохранена",
                content = [Content(schema = Schema(implementation = SiteFeedbackUsefulnessResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Некорректные входные данные")
        ]
    )
    fun saveUsefulness(
        @Valid @RequestBody request: SiteFeedbackUsefulnessRequest,
        servletRequest: HttpServletRequest
    ): SiteFeedbackUsefulnessResponse {
        val id = siteFeedbackService.create(
            CreateSiteFeedbackCommand(
                helpful = request.helpful,
                userName = request.userName,
                userEmail = request.userEmail,
                userAgent = servletRequest.getHeader("User-Agent"),
                ipAddress = servletRequest.remoteAddr,
                source = request.source,
                comment = request.comment
            )
        )
        return SiteFeedbackUsefulnessResponse(id = id)
    }
}
