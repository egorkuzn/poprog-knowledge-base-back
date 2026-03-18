package com.example.poprogknowledgebaseback.adapters.outbound.search.elasticsearch

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface SearchDocumentRepository : ElasticsearchRepository<SearchDocument, String>
