package com.example.poprogknowledgebaseback.adapters.inbound.web.account

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AccountProfileResponse(
    val subject: String,
    val name: String,
    val email: String,
    val roles: Set<String>
)

data class UpdateAccountProfileRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,
    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    val email: String
)

data class RegisterAccountRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,
    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    val email: String,
    @field:NotBlank
    @field:Size(min = 12, max = 128)
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
        message = "must contain lowercase, uppercase, digit and special character"
    )
    val password: String
)
