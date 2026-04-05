package com.example.poprogknowledgebaseback.application.search

import com.example.poprogknowledgebaseback.application.files.FileStoragePathResolver
import com.example.poprogknowledgebaseback.application.files.PdfTextExtractor
import com.example.poprogknowledgebaseback.domain.publication.Publication
import com.example.poprogknowledgebaseback.domain.publication.port.PublicationPersistencePort
import com.example.poprogknowledgebaseback.domain.search.SearchItem
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType
import com.example.poprogknowledgebaseback.domain.search.port.SearchIndexPort
import com.example.poprogknowledgebaseback.domain.studentwork.StudentWork
import com.example.poprogknowledgebaseback.domain.studentwork.port.StudentWorkPersistencePort
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val searchIndexPort: SearchIndexPort,
    private val publicationPersistencePort: PublicationPersistencePort,
    private val studentWorkPersistencePort: StudentWorkPersistencePort,
    private val fileStoragePathResolver: FileStoragePathResolver,
    private val pdfTextExtractor: PdfTextExtractor
) : SearchUseCase {

    companion object {
        private const val MIN_SEARCH_QUERY_LENGTH = 3
    }

    @PostConstruct
    override fun reindex() {
        val publicationItems = publicationPersistencePort.findAllOrderByYearDescIdAsc().map { publication ->
            val pdfText = resolvePdfText(publication.pdfText, publication.link) { extracted ->
                publicationPersistencePort.save(publication.copy(pdfText = extracted))
            }
            SearchItem(
                id = searchId(SearchSourceType.PUBLICATION, publication.id),
                sourceType = SearchSourceType.PUBLICATION,
                sourceId = publication.id ?: error("Publication id is missing"),
                groupTitle = publication.year.toString(),
                groupHash = null,
                authors = publication.authors,
                theme = publication.theme,
                published = publication.published,
                link = publication.link.ifBlank { null },
                pdfText = pdfText
            )
        }

        val studentWorkItems = studentWorkPersistencePort.findAllOrdered().map { studentWork ->
            val pdfText = resolvePdfText(studentWork.pdfText, studentWork.documentLink) { extracted ->
                studentWorkPersistencePort.save(studentWork.copy(pdfText = extracted))
            }
            SearchItem(
                id = searchId(SearchSourceType.STUDENT_WORK, studentWork.id),
                sourceType = SearchSourceType.STUDENT_WORK,
                sourceId = studentWork.id ?: error("Student work id is missing"),
                groupTitle = studentWork.projectTypeTitle,
                groupHash = studentWork.projectTypeHash,
                authors = studentWork.authors,
                theme = studentWork.theme,
                published = studentWork.published,
                link = studentWork.documentLink,
                pdfText = pdfText
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

    override fun indexPublication(publication: Publication) {
        searchIndexPort.index(publication.toSearchItem())
    }

    override fun indexStudentWork(studentWork: StudentWork) {
        searchIndexPort.index(studentWork.toSearchItem())
    }

    override fun removePublication(id: Long) {
        searchIndexPort.delete(searchId(SearchSourceType.PUBLICATION, id))
    }

    override fun removeStudentWork(id: Long) {
        searchIndexPort.delete(searchId(SearchSourceType.STUDENT_WORK, id))
    }

    private fun Publication.toSearchItem() = SearchItem(
        id = searchId(SearchSourceType.PUBLICATION, id),
        sourceType = SearchSourceType.PUBLICATION,
        sourceId = id ?: error("Publication id is missing"),
        groupTitle = year.toString(),
        groupHash = null,
        authors = authors,
        theme = theme,
        published = published,
        link = link.ifBlank { null },
        pdfText = pdfText
    )

    private fun StudentWork.toSearchItem() = SearchItem(
        id = searchId(SearchSourceType.STUDENT_WORK, id),
        sourceType = SearchSourceType.STUDENT_WORK,
        sourceId = id ?: error("Student work id is missing"),
        groupTitle = projectTypeTitle,
        groupHash = projectTypeHash,
        authors = authors,
        theme = theme,
        published = published,
        link = documentLink,
        pdfText = pdfText
    )

    private fun resolvePdfText(
        existing: String?,
        link: String?,
        onExtracted: (String) -> Unit
    ): String? {
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val normalizedLink = link?.trim().orEmpty()
        if (normalizedLink.isBlank() || !normalizedLink.lowercase().endsWith(".pdf")) {
            return null
        }
        val localPath = fileStoragePathResolver.resolveLocalPath(normalizedLink) ?: return null
        val extracted = pdfTextExtractor.extractText(localPath) ?: return null
        onExtracted(extracted)
        return extracted
    }

    private fun searchId(type: SearchSourceType, id: Long?): String =
        when (type) {
            SearchSourceType.PUBLICATION -> "publication-${id ?: "unknown"}"
            SearchSourceType.STUDENT_WORK -> "student-work-${id ?: "unknown"}"
        }
}
