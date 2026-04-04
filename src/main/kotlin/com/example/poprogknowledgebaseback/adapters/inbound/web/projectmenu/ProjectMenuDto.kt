package com.example.poprogknowledgebaseback.adapters.inbound.web.projectmenu

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ProjectMenuResponse(
    val title: String,
    val sections: List<ProjectMenuSectionResponse>
)

data class ProjectMenuSectionResponse(
    val id: Long,
    val hash: String,
    val title: String,
    val description: String,
    val cta: LinkResponse,
    val sortOrder: Int,
    val items: List<ProjectMenuItemResponse>,
    val promos: List<ProjectMenuPromoResponse>
)

data class ProjectMenuItemResponse(
    val id: Long,
    val sectionId: Long,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String?,
    val highlighted: Boolean,
    val sortOrder: Int
)

data class ProjectMenuPromoResponse(
    val id: Long,
    val sectionId: Long,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val sortOrder: Int
)

data class LinkResponse(
    val title: String,
    val url: String
)

data class ProjectMenuSectionRequest(
    @field:NotBlank
    val hash: String,
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val description: String,
    @field:NotBlank
    val ctaTitle: String,
    @field:NotBlank
    val ctaUrl: String,
    @field:NotNull
    val sortOrder: Int
)

data class ProjectMenuItemRequest(
    @field:NotNull
    val sectionId: Long,
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val description: String,
    @field:NotBlank
    val url: String,
    val imageUrl: String? = null,
    @field:NotNull
    val highlighted: Boolean,
    @field:NotNull
    val sortOrder: Int
)

data class ProjectMenuPromoRequest(
    @field:NotNull
    val sectionId: Long,
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val description: String,
    @field:NotBlank
    val url: String,
    @field:NotBlank
    val imageUrl: String,
    @field:NotNull
    val sortOrder: Int
)

data class ProjectMenuResourceUploadResponse(
    val resourceUrl: String
)
