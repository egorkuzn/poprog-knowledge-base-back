package com.example.poprogknowledgebaseback.adapters.outbound.persistence.studentwork

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "student_work")
class StudentWorkJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_type_id", nullable = false)
    var projectType: ProjectTypeJpaEntity,

    @Column(nullable = false, columnDefinition = "text")
    var authors: String,

    @Column(nullable = false, columnDefinition = "text")
    var theme: String,

    @Column(nullable = false, columnDefinition = "text")
    var published: String,

    @Column(name = "document_link", columnDefinition = "text")
    var documentLink: String? = null
)
