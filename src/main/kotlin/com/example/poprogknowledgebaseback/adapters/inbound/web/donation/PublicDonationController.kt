package com.example.poprogknowledgebaseback.adapters.inbound.web.donation

import com.example.poprogknowledgebaseback.application.account.AccountDonationResult
import com.example.poprogknowledgebaseback.application.account.AccountDonationService
import com.example.poprogknowledgebaseback.application.account.CreateDonationCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class PublicDonationRequest(
    @field:DecimalMin(value = "1.00")
    val amount: BigDecimal,
    @field:Pattern(regexp = "^[A-Za-z]{3}$")
    val currency: String,
    @field:Size(max = 255)
    val source: String? = null,
    @field:Size(max = 1000)
    val message: String? = null,
    @field:NotBlank
    @field:Size(max = 2048)
    val returnUrl: String,
    @field:Size(max = 120)
    val userName: String? = null,
    @field:Size(max = 254)
    val userEmail: String? = null
)

data class PublicDonationResponse(
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

@RestController
@RequestMapping("/api/donations")
@Tag(name = "Пожертвования", description = "Публичные пожертвования в проект")
class PublicDonationController(
    private val accountDonationService: AccountDonationService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать пожертвование без авторизации")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Пожертвование создано"),
            ApiResponse(responseCode = "400", description = "Некорректные данные")
        ]
    )
    fun createPublicDonation(@Valid @RequestBody request: PublicDonationRequest): PublicDonationResponse {
        val fullMessage = buildMessage(request.message, request.userName, request.userEmail)

        val donation = accountDonationService.createDonation(
            userSub = null,
            command = CreateDonationCommand(
                amount = request.amount,
                currency = request.currency,
                source = request.source,
                message = fullMessage,
                returnUrl = request.returnUrl
            )
        )

        return donation.toResponse()
    }

    private fun buildMessage(message: String?, userName: String?, userEmail: String?): String? {
        val chunks = mutableListOf<String>()
        userName?.trim()?.takeIf { it.isNotBlank() }?.let { chunks += "name=$it" }
        userEmail?.trim()?.takeIf { it.isNotBlank() }?.let { chunks += "email=$it" }
        message?.trim()?.takeIf { it.isNotBlank() }?.let { chunks += it }

        if (chunks.isEmpty()) {
            return null
        }

        return chunks.joinToString(" | ").take(1000)
    }

    private fun AccountDonationResult.toResponse() = PublicDonationResponse(
        id = id,
        amount = amount,
        currency = currency,
        status = status,
        source = source,
        message = message,
        providerPaymentId = providerPaymentId,
        confirmationUrl = confirmationUrl,
        returnUrl = returnUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        paidAt = paidAt
    )
}
