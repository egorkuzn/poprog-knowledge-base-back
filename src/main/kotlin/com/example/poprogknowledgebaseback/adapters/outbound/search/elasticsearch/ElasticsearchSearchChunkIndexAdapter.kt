package com.example.poprogknowledgebaseback.adapters.outbound.search.elasticsearch

import co.elastic.clients.elasticsearch._types.query_dsl.Operator
import com.example.poprogknowledgebaseback.domain.search.SearchChunk
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType
import com.example.poprogknowledgebaseback.domain.search.port.SearchChunkIndexPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.search.enabled"], havingValue = "true", matchIfMissing = true)
class ElasticsearchSearchChunkIndexAdapter(
    private val repository: SearchChunkDocumentRepository,
    private val operations: ElasticsearchOperations
) : SearchChunkIndexPort {

    companion object {
        private const val MIN_PARTIAL_QUERY_LENGTH = 3
    }

    override fun replaceAll(chunks: List<SearchChunk>) {
        val indexCoordinates = IndexCoordinates.of("knowledge_chunks")

        if (operations.indexOps(indexCoordinates).exists()) {
            operations.indexOps(indexCoordinates).delete()
        }

        operations.indexOps(SearchChunkDocument::class.java).create()
        operations.indexOps(SearchChunkDocument::class.java).putMapping()

        repository.saveAll(chunks.map { it.toDocument() })
    }

    override fun index(chunks: List<SearchChunk>) {
        if (chunks.isEmpty()) {
            return
        }

        ensureIndex()
        repository.saveAll(chunks.map { it.toDocument() })
    }

    override fun deleteBySource(sourceType: SearchSourceType, sourceId: Long) {
        val indexCoordinates = IndexCoordinates.of("knowledge_chunks")
        if (!operations.indexOps(indexCoordinates).exists()) {
            return
        }

        val query = NativeQuery.builder()
            .withQuery { q ->
                q.bool { bool ->
                    bool.filter { filter ->
                        filter.term { term -> term.field("sourceType").value(sourceType.name) }
                    }.filter { filter ->
                        filter.term { term -> term.field("sourceId").value(sourceId) }
                    }
                }
            }
            .build()

        operations.delete(query, SearchChunkDocument::class.java, indexCoordinates)
    }

    override fun search(
        query: String,
        limit: Int,
        sourceType: SearchSourceType?,
        sourceId: Long?
    ): List<SearchChunk> {
        if (query.length < MIN_PARTIAL_QUERY_LENGTH) {
            return emptyList()
        }

        val nativeQuery = NativeQuery.builder()
            .withQuery { q ->
                q.bool { bool ->
                    if (sourceType != null) {
                        bool.filter { filter ->
                            filter.term { term -> term.field("sourceType").value(sourceType.name) }
                        }
                    }
                    if (sourceId != null) {
                        bool.filter { filter ->
                            filter.term { term -> term.field("sourceId").value(sourceId) }
                        }
                    }

                    bool
                        .should { should ->
                            should.multiMatch { multiMatch ->
                                multiMatch
                                    .query(query)
                                    .operator(Operator.Or)
                                    .fields(
                                        "content^4",
                                        "theme^2",
                                        "authors^2",
                                        "groupTitle",
                                        "published",
                                        "groupHash"
                                    )
                            }
                        }
                        .should { should ->
                            should.multiMatch { multiMatch ->
                                multiMatch
                                    .query(query)
                                    .operator(Operator.Or)
                                    .fields(
                                        "content.partial^4",
                                        "theme.partial^2",
                                        "authors.partial^2",
                                        "groupTitle.partial",
                                        "published.partial",
                                        "groupHash.partial"
                                    )
                            }
                        }
                        .minimumShouldMatch("1")
                }
            }
            .withPageable(PageRequest.of(0, limit))
            .build()

        return operations.search(nativeQuery, SearchChunkDocument::class.java)
            .searchHits
            .map { it.content.toDomain() }
    }

    private fun ensureIndex() {
        val indexOps = operations.indexOps(IndexCoordinates.of("knowledge_chunks"))
        if (!indexOps.exists()) {
            operations.indexOps(SearchChunkDocument::class.java).create()
            operations.indexOps(SearchChunkDocument::class.java).putMapping()
        }
    }

    private fun SearchChunk.toDocument() = SearchChunkDocument(
        id = id,
        sourceType = sourceType.name,
        sourceId = sourceId,
        chunkIndex = chunkIndex,
        content = content,
        groupTitle = groupTitle,
        groupHash = groupHash,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )

    private fun SearchChunkDocument.toDomain() = SearchChunk(
        id = id,
        sourceType = SearchSourceType.valueOf(sourceType),
        sourceId = sourceId,
        chunkIndex = chunkIndex,
        content = content,
        groupTitle = groupTitle,
        groupHash = groupHash,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )
}
