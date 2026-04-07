package com.example.poprogknowledgebaseback.domain.search.port

import com.example.poprogknowledgebaseback.domain.search.SearchChunk
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType

interface SearchChunkIndexPort {
    fun replaceAll(chunks: List<SearchChunk>)
    fun index(chunks: List<SearchChunk>)
    fun deleteBySource(sourceType: SearchSourceType, sourceId: Long)
    fun findBySource(sourceType: SearchSourceType, sourceId: Long, limit: Int): List<SearchChunk>
    fun search(
        query: String,
        limit: Int,
        sourceType: SearchSourceType? = null,
        sourceId: Long? = null
    ): List<SearchChunk>
}
