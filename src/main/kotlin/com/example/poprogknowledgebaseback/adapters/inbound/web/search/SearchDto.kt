package com.example.poprogknowledgebaseback.adapters.inbound.web.search

data class SearchResponse(
    val query: String,
    val total: Int,
    val items: List<SearchItemDto>
)

data class SearchItemDto(
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
