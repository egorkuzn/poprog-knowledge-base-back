package com.example.poprogknowledgebaseback.adapters.outbound.search.elasticsearch

import com.example.poprogknowledgebaseback.domain.search.SearchChunk
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType
import com.example.poprogknowledgebaseback.domain.search.port.SearchChunkIndexPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.search.enabled"], havingValue = "false")
class NoOpSearchChunkIndexAdapter : SearchChunkIndexPort {
    override fun replaceAll(chunks: List<SearchChunk>) = Unit
    override fun index(chunks: List<SearchChunk>) = Unit
    override fun deleteBySource(sourceType: SearchSourceType, sourceId: Long) = Unit
    override fun findBySource(sourceType: SearchSourceType, sourceId: Long, limit: Int): List<SearchChunk> = emptyList()

    override fun search(
        query: String,
        limit: Int,
        sourceType: SearchSourceType?,
        sourceId: Long?
    ): List<SearchChunk> = emptyList()
}
