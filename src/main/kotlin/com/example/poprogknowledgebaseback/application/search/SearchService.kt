package com.example.poprogknowledgebaseback.application.search

import com.example.poprogknowledgebaseback.domain.publication.port.PublicationPersistencePort
import com.example.poprogknowledgebaseback.domain.search.SearchItem
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType
import com.example.poprogknowledgebaseback.domain.search.port.SearchIndexPort
import com.example.poprogknowledgebaseback.domain.studentwork.port.StudentWorkPersistencePort
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val searchIndexPort: SearchIndexPort,
    private val publicationPersistencePort: PublicationPersistencePort,
    private val studentWorkPersistencePort: StudentWorkPersistencePort
) : SearchUseCase {

    companion object {
        private const val MIN_SEARCH_QUERY_LENGTH = 3
    }

    @PostConstruct
    override fun reindex() {
        val publicationItems = publicationPersistencePort.findAllOrderByYearDescIdAsc().map { publication ->
            SearchItem(
                id = "publication-${publication.id}",
                sourceType = SearchSourceType.PUBLICATION,
                sourceId = publication.id ?: error("Publication id is missing"),
                groupTitle = publication.year.toString(),
                groupHash = null,
                authors = publication.authors,
                theme = publication.theme,
                published = publication.published,
                link = publication.link.ifBlank { null }
            )
        }

        val studentWorkItems = studentWorkPersistencePort.findAllOrdered().map { studentWork ->
            SearchItem(
                id = "student-work-${studentWork.id}",
                sourceType = SearchSourceType.STUDENT_WORK,
                sourceId = studentWork.id ?: error("Student work id is missing"),
                groupTitle = studentWork.projectTypeTitle,
                groupHash = studentWork.projectTypeHash,
                authors = studentWork.authors,
                theme = studentWork.theme,
                published = studentWork.published,
                link = studentWork.documentLink
            )
        }

        searchIndexPort.replaceAll(publicationItems + studentWorkItems)
    }

    override fun search(query: String, limit: Int): List<SearchResult> =
        if (query.trim().length < MIN_SEARCH_QUERY_LENGTH) {
            emptyList()
        } else {
            searchIndexPort.search(query = query.trim(), limit = limit).map {
            SearchResult(
                id = it.id,
                type = it.sourceType.name.lowercase(),
                sourceId = it.sourceId,
                groupTitle = it.groupTitle,
                groupHash = it.groupHash,
                authors = it.authors,
                theme = it.theme,
                published = it.published,
                link = it.link
            )
        }
        }
}
