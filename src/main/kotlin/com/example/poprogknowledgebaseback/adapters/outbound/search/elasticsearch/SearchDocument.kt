package com.example.poprogknowledgebaseback.adapters.outbound.search.elasticsearch

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.InnerField
import org.springframework.data.elasticsearch.annotations.MultiField
import org.springframework.data.elasticsearch.annotations.Setting

@Document(indexName = "knowledge_search")
@Setting(settingPath = "elasticsearch/knowledge-search-settings.json")
data class SearchDocument(
    @Id
    val id: String,
    @Field(type = FieldType.Keyword)
    val sourceType: String,
    @Field(type = FieldType.Long)
    val sourceId: Long,
    @MultiField(
        mainField = Field(type = FieldType.Text),
        otherFields = [
            InnerField(
                suffix = "partial",
                type = FieldType.Text,
                analyzer = "partial_index",
                searchAnalyzer = "partial_search"
            )
        ]
    )
    val groupTitle: String,
    @MultiField(
        mainField = Field(type = FieldType.Text),
        otherFields = [
            InnerField(suffix = "keyword", type = FieldType.Keyword),
            InnerField(
                suffix = "partial",
                type = FieldType.Text,
                analyzer = "partial_index",
                searchAnalyzer = "partial_search"
            )
        ]
    )
    val groupHash: String? = null,
    @MultiField(
        mainField = Field(type = FieldType.Text),
        otherFields = [
            InnerField(
                suffix = "partial",
                type = FieldType.Text,
                analyzer = "partial_index",
                searchAnalyzer = "partial_search"
            )
        ]
    )
    val authors: String,
    @MultiField(
        mainField = Field(type = FieldType.Text),
        otherFields = [
            InnerField(
                suffix = "partial",
                type = FieldType.Text,
                analyzer = "partial_index",
                searchAnalyzer = "partial_search"
            )
        ]
    )
    val theme: String,
    @MultiField(
        mainField = Field(type = FieldType.Text),
        otherFields = [
            InnerField(
                suffix = "partial",
                type = FieldType.Text,
                analyzer = "partial_index",
                searchAnalyzer = "partial_search"
            )
        ]
    )
    val published: String,
    @Field(type = FieldType.Keyword)
    val link: String? = null
)
