package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.domain.search.SearchSourceType

data class DocumentSearchResult(
    val sourceType: SearchSourceType,
    val sourceId: Long,
    val scoreHint: Int,
    val groupTitle: String,
    val groupHash: String?,
    val authors: String,
    val theme: String,
    val published: String,
    val link: String?,
    val snippet: String
)
