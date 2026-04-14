package com.example.poprogknowledgebaseback.application.account

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.donation.DonationPaymentJpaEntity
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.donation.SpringDataDonationPaymentRepository
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

data class CreateDonationCommand(
    val amount: BigDecimal,
    val currency: String,
    val source: String?,
    val message: String?,
    val returnUrl: String
)

data class DonationStatusUpdateCommand(
    val status: String,
    val providerPaymentId: String?
)

data class AccountDonationResult(
    val id: String,
    val amount: String,
    val currency: String,
    val status: String,
    val source: String?,
    val message: String?,
    val providerPaymentId: String?,
    val confirmationUrl: String?,
    val returnUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val paidAt: String?
)

@Service
class AccountDonationService(
    private val donationRepository: SpringDataDonationPaymentRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun getDonations(userSub: String): List<AccountDonationResult> =
        donationRepository.findAllByUserSubOrderByCreatedAtDesc(userSub)
            .map { it.toResult() }

    @Transactional(readOnly = true)
    fun exportDonationsCsv(userSub: String, from: OffsetDateTime?, to: OffsetDateTime?): ByteArray {
        val donations = findByFilters(userSub, from, to)
        auditLogger.info(
            "Account donation export requested: format=CSV, userSub={}, from={}, to={}, count={}",
            userSub,
            from,
            to,
            donations.size
        )

        val header = listOf(
            "id",
            "amount",
            "currency",
            "status",
            "source",
            "message",
            "providerPaymentId",
            "returnUrl",
            "createdAt",
            "updatedAt",
            "paidAt"
        ).joinToString(",")
        val rows = donations.joinToString("\n") { donation ->
            listOf(
                donation.id.toString(),
                donation.amount.toMoney(),
                donation.currency,
                donation.status,
                donation.source.orEmpty(),
                donation.message.orEmpty(),
                donation.providerPaymentId.orEmpty(),
                donation.returnUrl,
                donation.createdAt.toString(),
                donation.updatedAt.toString(),
                donation.paidAt?.toString().orEmpty()
            ).joinToString(",") { escapeCsv(it) }
        }
        return "$header\n$rows\n".toByteArray(Charsets.UTF_8)
    }

    @Transactional(readOnly = true)
    fun exportDonationsPdf(userSub: String, from: OffsetDateTime?, to: OffsetDateTime?): ByteArray {
        val donations = findByFilters(userSub, from, to)
        auditLogger.info(
            "Account donation export requested: format=PDF, userSub={}, from={}, to={}, count={}",
            userSub,
            from,
            to,
            donations.size
        )

        val lines = mutableListOf("POPROG Account Donations Report", "")
        donations.forEachIndexed { index, donation ->
            lines += buildString {
                append(index + 1)
                append(". ")
                append(donation.createdAt)
                append(" | ")
                append(donation.amount.toMoney())
                append(" ")
                append(donation.currency)
                append(" | ")
                append(donation.status)
                append(" | ")
                append(donation.source ?: "direct")
            }.take(160)
        }
        return renderPdf(lines)
    }

    @Transactional
    fun createDonation(userSub: String?, command: CreateDonationCommand): AccountDonationResult {
        val now = OffsetDateTime.now(clock)
        val paymentId = UUID.randomUUID()
        val normalizedCurrency = command.currency.trim().uppercase()

        if (normalizedCurrency.length != 3) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency must be a 3-letter ISO code")
        }

        val normalizedAmount = command.amount.setScale(2)
        if (normalizedAmount <= BigDecimal.ZERO) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Donation amount must be positive")
        }

        val confirmationUrl = buildLocalConfirmationUrl(paymentId)

        val entity = DonationPaymentJpaEntity(
            id = paymentId,
            userSub = userSub,
            amount = normalizedAmount,
            currency = normalizedCurrency,
            status = "PENDING",
            source = command.source?.trim()?.takeIf { it.isNotBlank() },
            message = command.message?.trim()?.takeIf { it.isNotBlank() },
            providerPaymentId = null,
            confirmationUrl = confirmationUrl,
            returnUrl = command.returnUrl.trim(),
            createdAt = now,
            updatedAt = now,
            paidAt = null
        )

        return donationRepository.save(entity).toResult()
    }

    @Transactional
    fun updateDonationStatus(userSub: String, donationId: UUID, command: DonationStatusUpdateCommand): AccountDonationResult {
        val entity = donationRepository.findByIdAndUserSub(donationId, userSub)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found") }

        val normalizedStatus = command.status.trim().uppercase()
        if (normalizedStatus !in allowedStatuses) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported donation status")
        }

        val now = OffsetDateTime.now(clock)
        entity.status = normalizedStatus
        entity.updatedAt = now
        entity.providerPaymentId = command.providerPaymentId?.trim()?.takeIf { it.isNotBlank() }
        entity.paidAt = if (normalizedStatus == "SUCCEEDED") now else null

        return donationRepository.save(entity).toResult()
    }

    private fun findByFilters(userSub: String, from: OffsetDateTime?, to: OffsetDateTime?): List<DonationPaymentJpaEntity> {
        validateRange(from, to)
        return when {
            from != null && to != null ->
                donationRepository.findAllByUserSubAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                    userSub,
                    from,
                    to
                )

            from != null ->
                donationRepository.findAllByUserSubAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(userSub, from)

            to != null ->
                donationRepository.findAllByUserSubAndCreatedAtLessThanEqualOrderByCreatedAtDesc(userSub, to)

            else -> donationRepository.findAllByUserSubOrderByCreatedAtDesc(userSub)
        }
    }

    private fun validateRange(from: OffsetDateTime?, to: OffsetDateTime?) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be less than or equal to to")
        }
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

    private fun buildLocalConfirmationUrl(paymentId: UUID): String =
        "https://yookassa.local/checkout/$paymentId"

    private fun DonationPaymentJpaEntity.toResult() = AccountDonationResult(
        id = id.toString(),
        amount = amount.toPlainString(),
        currency = currency,
        status = status,
        source = source,
        message = message,
        providerPaymentId = providerPaymentId,
        confirmationUrl = confirmationUrl,
        returnUrl = returnUrl,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        paidAt = paidAt?.toString()
    )

    companion object {
        private val allowedStatuses = setOf("PENDING", "SUCCEEDED", "CANCELED")
        private val auditLogger = LoggerFactory.getLogger(AccountDonationService::class.java)
    }
}
