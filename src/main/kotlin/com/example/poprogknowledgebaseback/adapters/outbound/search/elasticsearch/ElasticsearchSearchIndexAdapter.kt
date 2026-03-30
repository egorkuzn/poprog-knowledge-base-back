package com.example.poprogknowledgebaseback.adapters.outbound.search.elasticsearch

import co.elastic.clients.elasticsearch._types.query_dsl.Operator
import com.example.poprogknowledgebaseback.domain.search.SearchItem
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType
import com.example.poprogknowledgebaseback.domain.search.port.SearchIndexPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.search.enabled"], havingValue = "true", matchIfMissing = true)
class ElasticsearchSearchIndexAdapter(
    private val repository: SearchDocumentRepository,
    private val operations: ElasticsearchOperations
) : SearchIndexPort {

    companion object {
        private const val MIN_PARTIAL_QUERY_LENGTH = 3
    }

    override fun replaceAll(items: List<SearchItem>) {
        val indexCoordinates = IndexCoordinates.of("knowledge_search")

        if (operations.indexOps(indexCoordinates).exists()) {
            operations.indexOps(indexCoordinates).delete()
        }

        operations.indexOps(SearchDocument::class.java).create()
        operations.indexOps(SearchDocument::class.java).putMapping()

        repository.saveAll(items.map { it.toDocument() })
    }

    override fun search(query: String, limit: Int): List<SearchItem> {
        if (query.length < MIN_PARTIAL_QUERY_LENGTH) {
            return emptyList()
        }

        val nativeQuery = NativeQuery.builder()
            .withQuery { q ->
                q.bool { bool ->
                    bool
                        .should { should ->
                            should.multiMatch { multiMatch ->
                                multiMatch
                                    .query(query)
                                    .operator(Operator.Or)
                                    .fields(
                                        "theme^4",
                                        "authors^3",
                                        "groupTitle^2",
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
                                        "theme.partial^4",
                                        "authors.partial^3",
                                        "groupTitle.partial^2",
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

        return operations.search(nativeQuery, SearchDocument::class.java)
            .searchHits
            .map { it.content.toDomain() }
    }

    private fun SearchItem.toDocument() = SearchDocument(
        id = id,
        sourceType = sourceType.name,
        sourceId = sourceId,
        groupTitle = groupTitle,
        groupHash = groupHash,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )

    private fun SearchDocument.toDomain() = SearchItem(
        id = id,
        sourceType = SearchSourceType.valueOf(sourceType),
        sourceId = sourceId,
        groupTitle = groupTitle,
        groupHash = groupHash,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )
}
