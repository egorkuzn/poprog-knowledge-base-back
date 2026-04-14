package com.example.poprogknowledgebaseback.application.account

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.donation.DonationPaymentJpaEntity
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.donation.SpringDataDonationPaymentRepository
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

data class DonationReportingFilters(
    val from: OffsetDateTime? = null,
    val to: OffsetDateTime? = null,
    val status: String? = null
)

data class DonationKpiReportResult(
    val totalDonationsCount: Long,
    val succeededDonationsCount: Long,
    val pendingDonationsCount: Long,
    val canceledDonationsCount: Long,
    val anonymousDonationsCount: Long,
    val uniqueDonorsCount: Long,
    val totalAmount: String,
    val succeededAmount: String,
    val averageAmount: String,
    val conversionRatePercent: String,
    val currencies: List<String>
)

data class DonationEventResult(
    val id: String,
    val eventType: String,
    val eventAt: String,
    val status: String,
    val amount: String,
    val currency: String,
    val userSub: String?,
    val source: String?,
    val message: String?,
    val providerPaymentId: String?,
    val createdAt: String,
    val updatedAt: String,
    val paidAt: String?
)

data class DonationEventPageResult(
    val items: List<DonationEventResult>,
    val totalCount: Long,
    val page: Int,
    val size: Int
)

@Service
class AdminDonationReportingService(
    private val donationRepository: SpringDataDonationPaymentRepository
) {

    @Transactional(readOnly = true)
    fun getKpi(filters: DonationReportingFilters): DonationKpiReportResult {
        val donations = findByFilters(filters.copy(status = null))
        val totalCount = donations.size.toLong()
        val succeeded = donations.filter { it.status == "SUCCEEDED" }
        val pending = donations.filter { it.status == "PENDING" }
        val canceled = donations.filter { it.status == "CANCELED" }
        val anonymousCount = donations.count { it.userSub.isNullOrBlank() }.toLong()
        val uniqueDonorsCount = donations.mapNotNull { it.userSub?.takeIf(String::isNotBlank) }.toSet().size.toLong()
        val totalAmount = donations.fold(BigDecimal.ZERO) { acc, donation -> acc + donation.amount }
        val succeededAmount = succeeded.fold(BigDecimal.ZERO) { acc, donation -> acc + donation.amount }
        val averageAmount = if (totalCount > 0) {
            totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
        val conversionRatePercent = if (totalCount > 0) {
            BigDecimal.valueOf(succeeded.size.toLong())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
        val currencies = donations.map { it.currency }.distinct().sorted()

        return DonationKpiReportResult(
            totalDonationsCount = totalCount,
            succeededDonationsCount = succeeded.size.toLong(),
            pendingDonationsCount = pending.size.toLong(),
            canceledDonationsCount = canceled.size.toLong(),
            anonymousDonationsCount = anonymousCount,
            uniqueDonorsCount = uniqueDonorsCount,
            totalAmount = totalAmount.toMoney(),
            succeededAmount = succeededAmount.toMoney(),
            averageAmount = averageAmount.toMoney(),
            conversionRatePercent = conversionRatePercent.toMoney(),
            currencies = currencies
        )
    }

    @Transactional(readOnly = true)
    fun getEvents(filters: DonationReportingFilters, page: Int, size: Int): DonationEventPageResult {
        val sanitizedPage = page.coerceAtLeast(0)
        val sanitizedSize = size.coerceIn(1, 200)
        val events = findEventResults(filters)
        val fromIndex = (sanitizedPage * sanitizedSize).coerceAtMost(events.size)
        val items = events.drop(fromIndex).take(sanitizedSize)

        return DonationEventPageResult(
            items = items,
            totalCount = events.size.toLong(),
            page = sanitizedPage,
            size = sanitizedSize
        )
    }

    @Transactional(readOnly = true)
    fun exportEventsCsv(filters: DonationReportingFilters, limit: Int): ByteArray {
        val events = findEventResults(filters, limit)
        val header = listOf(
            "id",
            "eventType",
            "eventAt",
            "status",
            "amount",
            "currency",
            "userSub",
            "source",
            "message",
            "providerPaymentId",
            "createdAt",
            "updatedAt",
            "paidAt"
        ).joinToString(",")
        val rows = events.joinToString("\n") { event ->
            listOf(
                event.id,
                event.eventType,
                event.eventAt,
                event.status,
                event.amount,
                event.currency,
                event.userSub.orEmpty(),
                event.source.orEmpty(),
                event.message.orEmpty(),
                event.providerPaymentId.orEmpty(),
                event.createdAt,
                event.updatedAt,
                event.paidAt.orEmpty()
            ).joinToString(",") { escapeCsv(it) }
        }
        return "$header\n$rows\n".toByteArray(Charsets.UTF_8)
    }

    @Transactional(readOnly = true)
    fun exportEventsPdf(filters: DonationReportingFilters, limit: Int): ByteArray {
        val events = findEventResults(filters, limit)
        val title = "POPROG Donations Events Report"
        val lines = mutableListOf(title, "")
        events.forEachIndexed { index, event ->
            val line = buildString {
                append(index + 1)
                append(". ")
                append(event.eventAt)
                append(" | ")
                append(event.eventType)
                append(" | ")
                append(event.status)
                append(" | ")
                append(event.amount)
                append(" ")
                append(event.currency)
                append(" | user=")
                append(event.userSub ?: "anonymous")
            }.take(160)
            lines += line
        }
        return renderPdf(lines)
    }

    private fun findEventResults(filters: DonationReportingFilters, limit: Int? = null): List<DonationEventResult> {
        val rawEvents = findByFilters(filters)
            .sortedByDescending { resolveEventAt(it) }
            .map { donation ->
                DonationEventResult(
                    id = donation.id.toString(),
                    eventType = resolveEventType(donation.status),
                    eventAt = resolveEventAt(donation).toString(),
                    status = donation.status,
                    amount = donation.amount.toMoney(),
                    currency = donation.currency,
                    userSub = donation.userSub,
                    source = donation.source,
                    message = donation.message,
                    providerPaymentId = donation.providerPaymentId,
                    createdAt = donation.createdAt.toString(),
                    updatedAt = donation.updatedAt.toString(),
                    paidAt = donation.paidAt?.toString()
                )
            }
        return if (limit == null) {
            rawEvents
        } else {
            rawEvents.take(limit.coerceIn(1, 5000))
        }
    }

    private fun findByFilters(filters: DonationReportingFilters): List<DonationPaymentJpaEntity> {
        validateRange(filters.from, filters.to)
        val normalizedStatus = filters.status?.trim()?.uppercase()
        if (normalizedStatus != null && normalizedStatus !in allowedStatuses) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status filter")
        }

        val base = when {
            filters.from != null && filters.to != null ->
                donationRepository.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                    filters.from,
                    filters.to
                )

            filters.from != null ->
                donationRepository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(filters.from)

            filters.to != null ->
                donationRepository.findAllByCreatedAtLessThanEqualOrderByCreatedAtDesc(filters.to)

            else -> donationRepository.findAllByOrderByCreatedAtDesc()
        }

        return if (normalizedStatus == null) {
            base
        } else {
            base.filter { it.status == normalizedStatus }
        }
    }

    private fun validateRange(from: OffsetDateTime?, to: OffsetDateTime?) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be less than or equal to to")
        }
    }

    private fun resolveEventAt(donation: DonationPaymentJpaEntity): OffsetDateTime =
        when (donation.status) {
            "SUCCEEDED" -> donation.paidAt ?: donation.updatedAt
            else -> donation.updatedAt
        }

    private fun resolveEventType(status: String): String =
        when (status) {
            "SUCCEEDED" -> "DONATION_SUCCEEDED"
            "CANCELED" -> "DONATION_CANCELED"
            else -> "DONATION_PENDING"
        }

    private fun escapeCsv(rawValue: String): String {
        if (!rawValue.contains(',') && !rawValue.contains('"') && !rawValue.contains('\n')) {
            return rawValue
        }
        val escaped = rawValue.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun BigDecimal.toMoney(): String =
        setScale(2, RoundingMode.HALF_UP).toPlainString()

    private fun renderPdf(lines: List<String>): ByteArray {
        val document = PDDocument()
        val output = ByteArrayOutputStream()
        try {
            var page = PDPage(PDRectangle.A4)
            document.addPage(page)
            var content = PDPageContentStream(document, page)
            content.setFont(PDType1Font.COURIER, 10f)
            var y = page.mediaBox.height - 40f

            lines.forEach { line ->
                if (y < 50f) {
                    content.close()
                    page = PDPage(PDRectangle.A4)
                    document.addPage(page)
                    content = PDPageContentStream(document, page)
                    content.setFont(PDType1Font.COURIER, 10f)
                    y = page.mediaBox.height - 40f
                }
                content.beginText()
                content.newLineAtOffset(35f, y)
                content.showText(line)
                content.endText()
                y -= 14f
            }
            content.close()
            document.save(output)
            return output.toByteArray()
        } finally {
            document.close()
            output.close()
        }
    }

    companion object {
        private val allowedStatuses = setOf("PENDING", "SUCCEEDED", "CANCELED")
    }
}
