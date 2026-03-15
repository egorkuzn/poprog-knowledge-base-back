package com.example.poprogknowledgebaseback.domain.publication

data class Publication(
    val id: Long?,
    val year: Int,
    val authors: String,
    val theme: String,
    val published: String,
    val link: String
)
