package com.example.poprogknowledgebaseback.adapters.inbound.web.studentwork

import jakarta.validation.constraints.NotBlank

data class WorkModelDto(
    val authors: String,
    val theme: String,
    val published: String
)

data class WorksByProjectTypeDto(
    val title: String,
    val hash: String,
    val works: List<WorkModelDto>
)

data class StudentWorkCreateUpdateRequest(
    @field:NotBlank
    val projectTypeHash: String,
    @field:NotBlank
    val authors: String,
    @field:NotBlank
    val theme: String,
    @field:NotBlank
    val published: String
)

data class StudentWorkUploadRequest(
    @field:NotBlank
    val projectTypeHash: String,
    @field:NotBlank
    val authors: String,
    @field:NotBlank
    val theme: String,
    @field:NotBlank
    val published: String
)

data class StudentWorkResponse(
    val id: Long,
    val title: String,
    val hash: String,
    val authors: String,
    val theme: String,
    val published: String,
    val documentLink: String?
)
