package com.example.poprogknowledgebaseback.adapters.outbound.search.elasticsearch

import com.example.poprogknowledgebaseback.domain.search.SearchItem
import com.example.poprogknowledgebaseback.domain.search.port.SearchIndexPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.search.enabled"], havingValue = "false")
class NoOpSearchIndexAdapter : SearchIndexPort {
    override fun replaceAll(items: List<SearchItem>) = Unit

    override fun search(query: String, limit: Int): List<SearchItem> = emptyList()
}
