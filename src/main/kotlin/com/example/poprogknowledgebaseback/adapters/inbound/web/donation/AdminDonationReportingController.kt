package com.example.poprogknowledgebaseback.adapters.inbound.web.donation

import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUser
import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUserParam
import com.example.poprogknowledgebaseback.application.account.AdminDonationReportingService
import com.example.poprogknowledgebaseback.application.account.DonationEventPageResult
import com.example.poprogknowledgebaseback.application.account.DonationKpiReportResult
import com.example.poprogknowledgebaseback.application.account.DonationReportingFilters
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.OffsetDateTime
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/donations")
@Tag(name = "Пожертвования", description = "Административная аналитика и экспорт пожертвований")
class AdminDonationReportingController(
    private val reportingService: AdminDonationReportingService
) {

    @GetMapping("/kpi")
    @Operation(summary = "[ADMIN] Получить KPI по пожертвованиям")
    fun getKpi(
        @CurrentUserParam currentUser: CurrentUser,
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?
    ): DonationKpiReportResult {
        requireAdmin(currentUser)
        return reportingService.getKpi(
            DonationReportingFilters(
                from = from,
                to = to
            )
        )
    }

    @GetMapping("/events")
    @Operation(summary = "[ADMIN] Получить события по пожертвованиям")
    fun getEvents(
        @CurrentUserParam currentUser: CurrentUser,
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): DonationEventPageResult {
        requireAdmin(currentUser)
        return reportingService.getEvents(
            filters = DonationReportingFilters(from = from, to = to, status = status),
            page = page,
            size = size
        )
    }

    @GetMapping("/export.csv")
    @Operation(summary = "[ADMIN] Экспортировать события пожертвований в CSV")
    fun exportCsv(
        @CurrentUserParam currentUser: CurrentUser,
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "2000") limit: Int
    ): ResponseEntity<ByteArray> {
        requireAdmin(currentUser)
        val payload = reportingService.exportEventsCsv(
            filters = DonationReportingFilters(from = from, to = to, status = status),
            limit = limit
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType("text/csv; charset=utf-8")
        headers.contentDisposition = ContentDisposition.attachment().filename("donations-events.csv").build()
        return ResponseEntity(payload, headers, HttpStatus.OK)
    }

    @GetMapping("/export.pdf")
    @Operation(summary = "[ADMIN] Экспортировать события пожертвований в PDF")
    fun exportPdf(
        @CurrentUserParam currentUser: CurrentUser,
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "1000") limit: Int
    ): ResponseEntity<ByteArray> {
        requireAdmin(currentUser)
        val payload = reportingService.exportEventsPdf(
            filters = DonationReportingFilters(from = from, to = to, status = status),
            limit = limit
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.contentDisposition = ContentDisposition.attachment().filename("donations-events.pdf").build()
        return ResponseEntity(payload, headers, HttpStatus.OK)
    }

    private fun requireAdmin(currentUser: CurrentUser) {
        if (!currentUser.hasRole("ADMIN")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required")
        }
    }
}
