package com.example.poprogknowledgebaseback.domain.publication

data class PublicationModel(
    val authors: String,
    val theme: String,
    val published: String,
    val link: String
)

data class PublicationsByDate(
    val date: String,
    val publications: List<PublicationModel>
)
