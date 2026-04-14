package com.example.poprogknowledgebaseback.adapters.inbound.web.metrics

import com.example.poprogknowledgebaseback.application.metrics.DauWauReportResult
import com.example.poprogknowledgebaseback.application.metrics.ProductMetricEventCommand
import com.example.poprogknowledgebaseback.application.metrics.ProductMetricsService
import com.example.poprogknowledgebaseback.application.metrics.SearchSuccessReportResult
import com.example.poprogknowledgebaseback.application.metrics.SourceTypeCtrReportResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class ProductMetricEventRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val eventType: String,
    @field:NotBlank
    @field:Size(max = 255)
    val route: String,
    @field:Size(max = 2048)
    val referrer: String? = null,
    @field:NotBlank
    @field:Size(max = 128)
    val sessionId: String,
    @field:Size(max = 128)
    val userKey: String? = null,
    val timestampClient: OffsetDateTime,
    val payload: Map<String, Any?> = emptyMap()
)

data class ProductMetricAcceptedResponse(
    val id: Long,
    val status: String = "accepted"
)

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Продуктовые метрики", description = "Прием событий и продуктовые отчеты")
class ProductMetricsController(
    private val productMetricsService: ProductMetricsService
) {

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Принять событие продуктовой аналитики")
    fun saveEvent(@Valid @RequestBody request: ProductMetricEventRequest): ProductMetricAcceptedResponse {
        val id = productMetricsService.saveEvent(
            ProductMetricEventCommand(
                eventType = request.eventType,
                route = request.route,
                referrer = request.referrer,
                sessionId = request.sessionId,
                userKey = request.userKey,
                timestampClient = request.timestampClient,
                payload = request.payload
            )
        )
        return ProductMetricAcceptedResponse(id = id)
    }

    @GetMapping("/reports/dau-wau")
    @Operation(summary = "Получить отчет по ежедневной и недельной активности")
    fun getDauWauReport(
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?
    ): DauWauReportResult = productMetricsService.getDauWauReport(from, to)

    @GetMapping("/reports/search-success")
    @Operation(summary = "Получить отчет по успешности поисковых сессий")
    fun getSearchSuccessReport(
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?
    ): SearchSuccessReportResult = productMetricsService.getSearchSuccessReport(from, to)

    @GetMapping("/reports/ctr")
    @Operation(summary = "Получить отчет по кликам и показам по типам источников")
    fun getCtrReport(
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?
    ): SourceTypeCtrReportResult = productMetricsService.getCtrReport(from, to)
}
