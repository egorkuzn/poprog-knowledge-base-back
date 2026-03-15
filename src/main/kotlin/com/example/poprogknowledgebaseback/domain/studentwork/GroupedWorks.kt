package com.example.poprogknowledgebaseback.domain.studentwork

data class WorkModel(
    val authors: String,
    val theme: String,
    val published: String
)

data class WorksByProjectType(
    val title: String,
    val hash: String,
    val works: List<WorkModel>
)
