package com.example.poprogknowledgebaseback.application.search

import com.example.poprogknowledgebaseback.domain.search.SearchChunk
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType
import com.example.poprogknowledgebaseback.domain.search.port.SearchChunkIndexPort
import org.springframework.stereotype.Service

@Service
class SearchChunkService(
    private val searchChunkIndexPort: SearchChunkIndexPort
) : SearchChunkUseCase {

    companion object {
        private const val MIN_QUERY_LENGTH = 3
    }

    override fun search(
        query: String,
        limit: Int,
        sourceType: SearchSourceType?,
        sourceId: Long?
    ): List<SearchChunk> {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            return emptyList()
        }

        return searchChunkIndexPort.search(trimmed, limit, sourceType, sourceId)
    }
}
