package com.example.poprogknowledgebaseback.domain.search.port

import com.example.poprogknowledgebaseback.domain.search.SearchItem

interface SearchIndexPort {
    fun replaceAll(items: List<SearchItem>)
    fun search(query: String, limit: Int): List<SearchItem>
}
