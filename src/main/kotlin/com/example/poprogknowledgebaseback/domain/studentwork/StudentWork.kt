package com.example.poprogknowledgebaseback.domain.studentwork

data class StudentWork(
    val id: Long?,
    val projectTypeTitle: String,
    val projectTypeHash: String,
    val authors: String,
    val theme: String,
    val published: String
)

data class ProjectType(
    val title: String,
    val hash: String
)
