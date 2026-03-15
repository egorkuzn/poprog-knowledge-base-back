package com.example.poprogknowledgebaseback.adapters.outbound.persistence.publication

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "publication")
class PublicationJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "publication_year", nullable = false)
    var year: Int,

    @Column(nullable = false, columnDefinition = "text")
    var authors: String,

    @Column(nullable = false, columnDefinition = "text")
    var theme: String,

    @Column(nullable = false, columnDefinition = "text")
    var published: String,

    @Column(nullable = false, columnDefinition = "text")
    var link: String
)
