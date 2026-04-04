package com.example.poprogknowledgebaseback.adapters.outbound.persistence.projectmenu

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "project_menu_promo")
class ProjectMenuPromoJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "section_id", nullable = false)
    val sectionId: Long,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,
    @Column(nullable = false, length = 512)
    val url: String,
    @Column(name = "image_url", nullable = false, length = 512)
    val imageUrl: String,
    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int
)
