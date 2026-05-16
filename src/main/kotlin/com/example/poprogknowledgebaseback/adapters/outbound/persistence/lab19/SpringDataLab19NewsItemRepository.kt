package com.example.poprogknowledgebaseback.adapters.outbound.persistence.lab19

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SpringDataLab19NewsItemRepository : JpaRepository<Lab19NewsItemJpaEntity, Long> {
    fun findBySourceUrl(sourceUrl: String): Lab19NewsItemJpaEntity?
    fun findAllByOrderByYearDescIdDesc(): List<Lab19NewsItemJpaEntity>

    @Query(
        """
        select i from Lab19NewsItemJpaEntity i
        where (:year is null or i.year = :year)
          and (:materialKind is null or i.materialKind = :materialKind)
          and (
                :query is null
                or lower(cast(i.title as string)) like lower(concat('%', cast(:query as string), '%'))
                or lower(cast(i.sourceUrl as string)) like lower(concat('%', cast(:query as string), '%'))
                or lower(cast(i.sourcePage as string)) like lower(concat('%', cast(:query as string), '%'))
              )
        """
    )
    fun search(
        @Param("query") query: String?,
        @Param("year") year: Int?,
        @Param("materialKind") materialKind: String?,
        pageable: Pageable
    ): Page<Lab19NewsItemJpaEntity>
}
