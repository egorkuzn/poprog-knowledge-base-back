package com.example.poprogknowledgebaseback.application.account

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.donation.DonationPaymentJpaEntity
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.donation.SpringDataDonationPaymentRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID
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

    @Transactional
    fun createDonation(userSub: String, command: CreateDonationCommand): AccountDonationResult {
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
    }
}
