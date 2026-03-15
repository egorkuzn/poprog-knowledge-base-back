package com.example.poprogknowledgebaseback.application.publication

import com.example.poprogknowledgebaseback.domain.publication.Publication
import com.example.poprogknowledgebaseback.domain.publication.PublicationModel
import com.example.poprogknowledgebaseback.domain.publication.PublicationNotFoundException
import com.example.poprogknowledgebaseback.domain.publication.PublicationsByDate
import com.example.poprogknowledgebaseback.domain.publication.port.PublicationPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PublicationService(
    private val publicationPersistencePort: PublicationPersistencePort
) : PublicationUseCase {

    @Transactional(readOnly = true)
    override fun getGroupedPublications(): List<PublicationsByDate> {
        val publications = publicationPersistencePort.findAllOrderByYearDescIdAsc()

        return publications
            .groupBy { it.year }
            .map { (year, yearPublications) ->
                PublicationsByDate(
                    date = year.toString(),
                    publications = yearPublications.map { publication ->
                        PublicationModel(
                            authors = publication.authors,
                            theme = publication.theme,
                            published = publication.published,
                            link = publication.link
                        )
                    }
                )
            }
    }

    @Transactional
    override fun create(command: UpsertPublicationCommand): PublicationResult {
        val saved = publicationPersistencePort.save(
            Publication(
                id = null,
                year = command.year,
                authors = command.authors,
                theme = command.theme,
                published = command.published,
                link = command.link
            )
        )

        return saved.toResult()
    }

    @Transactional
    override fun update(id: Long, command: UpsertPublicationCommand): PublicationResult {
        val current = publicationPersistencePort.findById(id)
            ?: throw PublicationNotFoundException(id)

        val updated = publicationPersistencePort.save(
            current.copy(
                year = command.year,
                authors = command.authors,
                theme = command.theme,
                published = command.published,
                link = command.link
            )
        )

        return updated.toResult()
    }

    @Transactional
    override fun delete(id: Long) {
        val current = publicationPersistencePort.findById(id)
            ?: throw PublicationNotFoundException(id)
        publicationPersistencePort.deleteById(current.id ?: id)
    }

    private fun Publication.toResult() = PublicationResult(
        id = id ?: error("Entity id was not generated"),
        year = year,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )
}
