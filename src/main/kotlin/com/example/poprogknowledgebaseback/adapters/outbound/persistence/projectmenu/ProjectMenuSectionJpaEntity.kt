package com.example.poprogknowledgebaseback.adapters.outbound.persistence.projectmenu

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "project_menu_section")
class ProjectMenuSectionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true, length = 128)
    val hash: String,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,
    @Column(name = "cta_title", nullable = false)
    val ctaTitle: String,
    @Column(name = "cta_url", nullable = false, length = 512)
    val ctaUrl: String,
    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int
)
