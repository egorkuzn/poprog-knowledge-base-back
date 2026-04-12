package com.example.poprogknowledgebaseback.adapters.inbound.web.account

import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUser
import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUserParam
import com.example.poprogknowledgebaseback.application.account.AccountDonationService
import com.example.poprogknowledgebaseback.application.account.CreateDonationCommand
import com.example.poprogknowledgebaseback.application.account.DonationStatusUpdateCommand
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
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class AccountDonationResponse(
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

data class CreateDonationRequest(
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
    val returnUrl: String
)

data class UpdateDonationStatusRequest(
    @field:Pattern(regexp = "^(PENDING|SUCCEEDED|CANCELED)$")
    val status: String,
    @field:Size(max = 128)
    val providerPaymentId: String? = null
)

@RestController
@RequestMapping("/api/account/donations")
@Tag(name = "Личный кабинет", description = "Пожертвования текущего пользователя")
class AccountDonationController(
    private val accountDonationService: AccountDonationService
) {

    @GetMapping
    @Operation(summary = "Получить историю пожертвований")
    fun getDonations(@CurrentUserParam currentUser: CurrentUser): List<AccountDonationResponse> =
        accountDonationService.getDonations(currentUser.subject)
            .map { it.toResponse() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать платёж пожертвования")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Платёж создан"),
            ApiResponse(responseCode = "401", description = "Требуется авторизация")
        ]
    )
    fun createDonation(
        @CurrentUserParam currentUser: CurrentUser,
        @Valid @RequestBody request: CreateDonationRequest
    ): AccountDonationResponse =
        accountDonationService.createDonation(
            currentUser.subject,
            CreateDonationCommand(
                amount = request.amount,
                currency = request.currency,
                source = request.source,
                message = request.message,
                returnUrl = request.returnUrl
            )
        ).toResponse()

    @PostMapping("/{donationId}/status")
    @Operation(
        summary = "Обновить статус пожертвования",
        description = "Техническая ручка для локальной отладки интеграции до подключения webhook от платёжного провайдера."
    )
    fun updateDonationStatus(
        @CurrentUserParam currentUser: CurrentUser,
        @PathVariable donationId: UUID,
        @Valid @RequestBody request: UpdateDonationStatusRequest
    ): AccountDonationResponse =
        accountDonationService.updateDonationStatus(
            currentUser.subject,
            donationId,
            DonationStatusUpdateCommand(
                status = request.status,
                providerPaymentId = request.providerPaymentId
            )
        ).toResponse()

    private fun com.example.poprogknowledgebaseback.application.account.AccountDonationResult.toResponse() =
        AccountDonationResponse(
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
