package com.example.poprogknowledgebaseback.adapters.outbound.persistence.studentwork

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "project_type")
class ProjectTypeJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, columnDefinition = "text")
    var title: String,

    @Column(nullable = false, unique = true, columnDefinition = "text")
    var hash: String
)
