package com.example.poprogknowledgebaseback.adapters.outbound.search.elasticsearch

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "knowledge_search")
data class SearchDocument(
    @Id
    val id: String,
    @Field(type = FieldType.Keyword)
    val sourceType: String,
    @Field(type = FieldType.Long)
    val sourceId: Long,
    @Field(type = FieldType.Text)
    val groupTitle: String,
    @Field(type = FieldType.Keyword)
    val groupHash: String? = null,
    @Field(type = FieldType.Text)
    val authors: String,
    @Field(type = FieldType.Text)
    val theme: String,
    @Field(type = FieldType.Text)
    val published: String,
    @Field(type = FieldType.Keyword)
    val link: String? = null
)
