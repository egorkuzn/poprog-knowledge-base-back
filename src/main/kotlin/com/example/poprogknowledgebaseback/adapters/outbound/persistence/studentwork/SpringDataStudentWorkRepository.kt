package com.example.poprogknowledgebaseback.adapters.outbound.persistence.studentwork

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Pageable

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

    @Query(
        """
        select sw from StudentWorkJpaEntity sw
        where sw.theme = :theme
          and lower(cast(sw.published as string)) like lower(concat('%', cast(:sourcePage as string), '%'))
        order by sw.id desc
        """
    )
    fun findByThemeAndPublishedContains(
        @Param("theme") theme: String,
        @Param("sourcePage") sourcePage: String,
        pageable: Pageable
    ): List<StudentWorkJpaEntity>
}
