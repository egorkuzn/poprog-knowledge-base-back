package com.example.poprogknowledgebaseback.application.search

import com.example.poprogknowledgebaseback.domain.publication.Publication
import com.example.poprogknowledgebaseback.domain.search.SearchChunk
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType
import com.example.poprogknowledgebaseback.domain.search.port.SearchChunkIndexPort
import com.example.poprogknowledgebaseback.domain.studentwork.StudentWork
import org.springframework.stereotype.Service

@Service
class SearchChunkIndexingService(
    private val searchChunkIndexPort: SearchChunkIndexPort,
    private val textChunker: TextChunker
) {

    fun reindex(publications: List<Publication>, studentWorks: List<StudentWork>) {
        val chunks = publications.flatMap { publication ->
            chunksForPublication(publication)
        } + studentWorks.flatMap { studentWork ->
            chunksForStudentWork(studentWork)
        }

        searchChunkIndexPort.replaceAll(chunks)
    }

    fun indexPublication(publication: Publication) {
        val publicationId = publication.id ?: return
        searchChunkIndexPort.deleteBySource(SearchSourceType.PUBLICATION, publicationId)

        val chunks = chunksForPublication(publication)
        if (chunks.isNotEmpty()) {
            searchChunkIndexPort.index(chunks)
        }
    }

    fun indexStudentWork(studentWork: StudentWork) {
        val workId = studentWork.id ?: return
        searchChunkIndexPort.deleteBySource(SearchSourceType.STUDENT_WORK, workId)

        val chunks = chunksForStudentWork(studentWork)
        if (chunks.isNotEmpty()) {
            searchChunkIndexPort.index(chunks)
        }
    }

    fun deletePublication(id: Long) {
        searchChunkIndexPort.deleteBySource(SearchSourceType.PUBLICATION, id)
    }

    fun deleteStudentWork(id: Long) {
        searchChunkIndexPort.deleteBySource(SearchSourceType.STUDENT_WORK, id)
    }

    private fun chunksForPublication(publication: Publication): List<SearchChunk> {
        val publicationId = publication.id ?: return emptyList()
        val pdfText = publication.pdfText?.trim().orEmpty()
        if (pdfText.isBlank()) {
            return emptyList()
        }

        return textChunker.chunk(pdfText).mapIndexed { index, chunk ->
            SearchChunk(
                id = chunkId(SearchSourceType.PUBLICATION, publicationId, index),
                sourceType = SearchSourceType.PUBLICATION,
                sourceId = publicationId,
                groupTitle = publication.year.toString(),
                groupHash = null,
                authors = publication.authors,
                theme = publication.theme,
                published = publication.published,
                link = publication.link.ifBlank { null },
                chunkIndex = index,
                content = chunk
            )
        }
    }

    private fun chunksForStudentWork(studentWork: StudentWork): List<SearchChunk> {
        val workId = studentWork.id ?: return emptyList()
        val pdfText = studentWork.pdfText?.trim().orEmpty()
        if (pdfText.isBlank()) {
            return emptyList()
        }

        return textChunker.chunk(pdfText).mapIndexed { index, chunk ->
            SearchChunk(
                id = chunkId(SearchSourceType.STUDENT_WORK, workId, index),
                sourceType = SearchSourceType.STUDENT_WORK,
                sourceId = workId,
                groupTitle = studentWork.projectTypeTitle,
                groupHash = studentWork.projectTypeHash,
                authors = studentWork.authors,
                theme = studentWork.theme,
                published = studentWork.published,
                link = studentWork.documentLink,
                chunkIndex = index,
                content = chunk
            )
        }
    }

    private fun chunkId(type: SearchSourceType, id: Long, index: Int): String =
        when (type) {
            SearchSourceType.PUBLICATION -> "publication-$id-chunk-$index"
            SearchSourceType.STUDENT_WORK -> "student-work-$id-chunk-$index"
        }
}
