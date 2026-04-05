package com.example.poprogknowledgebaseback.domain.projectmenu

data class ProjectMenu(
    val title: String,
    val sections: List<ProjectMenuSection>
)

data class ProjectMenuSection(
    val id: Long? = null,
    val hash: String,
    val title: String,
    val description: String,
    val ctaTitle: String,
    val ctaUrl: String,
    val sortOrder: Int,
    val items: List<ProjectMenuItem> = emptyList(),
    val promos: List<ProjectMenuPromo> = emptyList()
)

data class ProjectMenuItem(
    val id: Long? = null,
    val sectionId: Long,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String? = null,
    val highlighted: Boolean,
    val sortOrder: Int
)

data class ProjectMenuPromo(
    val id: Long? = null,
    val sectionId: Long,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val sortOrder: Int
)
