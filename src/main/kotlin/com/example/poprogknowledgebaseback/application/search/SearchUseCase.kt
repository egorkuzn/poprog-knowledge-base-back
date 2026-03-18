package com.example.poprogknowledgebaseback.application.search

interface SearchUseCase {
    fun search(query: String, limit: Int = 20): List<SearchResult>
    fun reindex()
}
