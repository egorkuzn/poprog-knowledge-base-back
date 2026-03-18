package com.example.poprogknowledgebaseback.domain.search

data class SearchItem(
    val id: String,
    val sourceType: SearchSourceType,
    val sourceId: Long,
    val groupTitle: String,
    val groupHash: String?,
    val authors: String,
    val theme: String,
    val published: String,
    val link: String?
)

enum class SearchSourceType {
    PUBLICATION,
    STUDENT_WORK
}
