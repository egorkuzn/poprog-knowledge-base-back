package com.example.poprogknowledgebaseback.application.publication

data class UpsertPublicationCommand(
    val year: Int,
    val authors: String,
    val theme: String,
    val published: String,
    val link: String
)

data class PublicationResult(
    val id: Long,
    val year: Int,
    val authors: String,
    val theme: String,
    val published: String,
    val link: String
)
