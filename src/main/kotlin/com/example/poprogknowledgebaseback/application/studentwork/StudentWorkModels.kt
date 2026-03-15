package com.example.poprogknowledgebaseback.application.studentwork

data class UpsertStudentWorkCommand(
    val projectTypeHash: String,
    val authors: String,
    val theme: String,
    val published: String
)

data class StudentWorkResult(
    val id: Long,
    val projectTypeTitle: String,
    val projectTypeHash: String,
    val authors: String,
    val theme: String,
    val published: String
)
