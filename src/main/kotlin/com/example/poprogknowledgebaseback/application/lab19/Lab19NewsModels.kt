package com.example.poprogknowledgebaseback.application.lab19

data class Lab19NewsItem(
    val id: Long,
    val title: String,
    val sourceUrl: String,
    val sourcePage: String,
    val year: Int?,
    val contentType: String?,
    val materialKind: String,
    val kbPublicationId: Long?,
    val kbStudentWorkId: Long?,
    val status: String
)
