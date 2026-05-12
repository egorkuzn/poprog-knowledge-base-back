package com.example.poprogknowledgebaseback.adapters.outbound.persistence.lab19

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "lab19_news_item")
class Lab19NewsItemJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "title", nullable = false)
    var title: String,
    @Column(name = "source_url", nullable = false, unique = true)
    var sourceUrl: String,
    @Column(name = "source_page", nullable = false)
    var sourcePage: String,
    @Column(name = "year")
    var year: Int?,
    @Column(name = "content_type")
    var contentType: String?,
    @Column(name = "material_kind", nullable = false)
    var materialKind: String = "NEWS",
    @Column(name = "status", nullable = false)
    var status: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime
)
