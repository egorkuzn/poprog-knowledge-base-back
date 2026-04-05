package com.example.poprogknowledgebaseback.application.projectmenu

data class ProjectMenuResponseModel(
    val title: String,
    val sections: List<ProjectMenuSectionResult>
)

data class ProjectMenuSectionResult(
    val id: Long,
    val hash: String,
    val title: String,
    val description: String,
    val ctaTitle: String,
    val ctaUrl: String,
    val sortOrder: Int,
    val items: List<ProjectMenuItemResult>,
    val promos: List<ProjectMenuPromoResult>
)

data class ProjectMenuItemResult(
    val id: Long,
    val sectionId: Long,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String?,
    val highlighted: Boolean,
    val sortOrder: Int
)

data class ProjectMenuPromoResult(
    val id: Long,
    val sectionId: Long,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val sortOrder: Int
)

data class UpsertProjectMenuSectionCommand(
    val hash: String,
    val title: String,
    val description: String,
    val ctaTitle: String,
    val ctaUrl: String,
    val sortOrder: Int
)

data class UpsertProjectMenuItemCommand(
    val sectionId: Long,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String?,
    val highlighted: Boolean,
    val sortOrder: Int
)

data class UpsertProjectMenuPromoCommand(
    val sectionId: Long,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val sortOrder: Int
)

data class ProjectMenuResourceUploadResult(
    val resourceUrl: String
)
