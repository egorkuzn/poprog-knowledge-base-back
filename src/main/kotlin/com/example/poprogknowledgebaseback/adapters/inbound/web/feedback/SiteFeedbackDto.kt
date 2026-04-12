package com.example.poprogknowledgebaseback.adapters.inbound.web.feedback

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SiteFeedbackUsefulnessRequest(
    val helpful: Boolean,
    @field:NotBlank
    @field:Size(max = 255)
    val userName: String,
    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    val userEmail: String,
    @field:Size(max = 255)
    val source: String? = null,
    @field:Size(max = 2000)
    val comment: String? = null
)

data class SiteFeedbackUsefulnessResponse(
    val id: Long
)
