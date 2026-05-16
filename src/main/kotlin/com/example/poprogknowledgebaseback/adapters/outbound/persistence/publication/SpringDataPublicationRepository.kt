package com.example.poprogknowledgebaseback.adapters.outbound.persistence.publication

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpringDataPublicationRepository : JpaRepository<PublicationJpaEntity, Long> {
    fun findAllByOrderByYearDescIdAsc(): List<PublicationJpaEntity>

    @Query(
        """
        select p from PublicationJpaEntity p
        where p.theme = :theme
          and lower(cast(p.published as string)) like lower(concat('%', cast(:sourcePage as string), '%'))
        order by p.id desc
        """
    )
    fun findByThemeAndPublishedContains(
        @Param("theme") theme: String,
        @Param("sourcePage") sourcePage: String,
        pageable: Pageable
    ): List<PublicationJpaEntity>
}
