package com.example.poprogknowledgebaseback.adapters.outbound.persistence.publication

import com.example.poprogknowledgebaseback.domain.publication.Publication
import com.example.poprogknowledgebaseback.domain.publication.port.PublicationPersistencePort
import org.springframework.stereotype.Component

@Component
class PublicationPersistenceAdapter(
    private val repository: SpringDataPublicationRepository
) : PublicationPersistencePort {

    override fun findAllOrderByYearDescIdAsc(): List<Publication> =
        repository.findAllByOrderByYearDescIdAsc().map { it.toDomain() }

    override fun findById(id: Long): Publication? = repository.findById(id).orElse(null)?.toDomain()

    override fun save(publication: Publication): Publication =
        repository.save(publication.toEntity()).toDomain()

    override fun deleteById(id: Long) {
        repository.deleteById(id)
    }

    private fun PublicationJpaEntity.toDomain() = Publication(
        id = id,
        year = year,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )

    private fun Publication.toEntity() = PublicationJpaEntity(
        id = id,
        year = year,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )
}
