package com.example.poprogknowledgebaseback.adapters.outbound.persistence.studentwork

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpringDataStudentWorkRepository : JpaRepository<StudentWorkJpaEntity, Long> {

    @Query("""
        select sw from StudentWorkJpaEntity sw
        join fetch sw.projectType pt
        order by pt.id asc, sw.id asc
    """)
    fun findAllOrdered(): List<StudentWorkJpaEntity>

    @Query("""
        select sw from StudentWorkJpaEntity sw
        join fetch sw.projectType pt
        where sw.id = :id
    """)
    fun findByIdWithProjectType(@Param("id") id: Long): StudentWorkJpaEntity?
}
