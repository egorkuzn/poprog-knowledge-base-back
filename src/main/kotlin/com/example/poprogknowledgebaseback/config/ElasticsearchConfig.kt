package com.example.poprogknowledgebaseback.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@ConditionalOnProperty(name = ["app.search.enabled"], havingValue = "true", matchIfMissing = true)
@EnableElasticsearchRepositories(
    basePackages = ["com.example.poprogknowledgebaseback.adapters.outbound.search.elasticsearch"]
)
class ElasticsearchConfig
