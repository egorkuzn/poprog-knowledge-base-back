package com.example.poprogknowledgebaseback.application.search

import com.example.poprogknowledgebaseback.domain.search.SearchChunk
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType

interface SearchChunkUseCase {
    fun findBySource(sourceType: SearchSourceType, sourceId: Long, limit: Int = 5): List<SearchChunk>

    fun search(
        query: String,
        limit: Int = 5,
        sourceType: SearchSourceType? = null,
        sourceId: Long? = null
    ): List<SearchChunk>
}
