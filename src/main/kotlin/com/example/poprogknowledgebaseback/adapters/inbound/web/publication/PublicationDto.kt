package com.example.poprogknowledgebaseback.adapters.inbound.web.publication

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class PublicationModelDto(
    val authors: String,
    val theme: String,
    val published: String,
    val link: String
)

data class PublicationsByDateDto(
    val date: String,
    val publications: List<PublicationModelDto>
)

data class PublicationCreateUpdateRequest(
    @field:Min(1900)
    @field:Max(2100)
    val year: Int,
    @field:NotBlank
    val authors: String,
    @field:NotBlank
    val theme: String,
    @field:NotBlank
    val published: String,
    val link: String = ""
)

data class PublicationUploadRequest(
    @field:Min(1900)
    @field:Max(2100)
    val year: Int,
    @field:NotBlank
    val authors: String,
    @field:NotBlank
    val theme: String,
    @field:NotBlank
    val published: String
)

data class PublicationResponse(
    val id: Long,
    val year: Int,
    val authors: String,
    val theme: String,
    val published: String,
    val link: String
)
