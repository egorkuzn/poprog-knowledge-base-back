package com.example.poprogknowledgebaseback.application.search

data class SearchResult(
    val id: String,
    val type: String,
    val sourceId: Long,
    val groupTitle: String,
    val groupHash: String?,
    val authors: String,
    val theme: String,
    val published: String,
    val link: String?
)
