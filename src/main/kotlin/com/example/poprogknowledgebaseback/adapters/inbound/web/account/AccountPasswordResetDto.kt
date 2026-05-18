package com.example.poprogknowledgebaseback.adapters.inbound.web.account

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class AccountPasswordResetRequest(
    @field:NotBlank
    @field:Email
    val email: String
)

data class AccountPasswordResetResponse(
    val status: String,
    val message: String
)
