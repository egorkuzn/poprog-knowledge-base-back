package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.application.search.SearchChunkUseCase
import com.example.poprogknowledgebaseback.domain.publication.port.PublicationPersistencePort
import com.example.poprogknowledgebaseback.domain.search.SearchChunk
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType
import com.example.poprogknowledgebaseback.domain.studentwork.port.StudentWorkPersistencePort
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class DocumentQuestionResolver(
    private val searchChunkUseCase: SearchChunkUseCase,
    private val publicationPersistencePort: PublicationPersistencePort,
    private val studentWorkPersistencePort: StudentWorkPersistencePort
) {

    companion object {
        private const val MAX_MATCHES = 6
        private const val MAX_RESULTS = 3
    }

    fun resolveBestDocuments(question: String): List<DocumentSearchResult> {
        val matches = searchChunkUseCase.search(question, limit = MAX_MATCHES)
        if (matches.isEmpty()) {
            return emptyList()
        }

        val grouped = matches.groupBy { it.sourceType to it.sourceId }
        val scored = grouped.map { (key, chunks) ->
            val first = chunks.minByOrNull { it.chunkIndex } ?: return@map null
            DocumentSearchResult(
                sourceType = key.first,
                sourceId = key.second,
                sourceUuid = extractUuid(first.link),
                scoreHint = chunks.size,
                groupTitle = first.groupTitle,
                groupHash = first.groupHash,
                authors = first.authors,
                theme = first.theme,
                published = first.published,
                link = first.link,
                snippet = buildSnippet(question, first)
            )
        }.filterNotNull()

        return scored.sortedWith(
            compareByDescending<DocumentSearchResult> { it.scoreHint }
                .thenBy { it.sourceType.name }
                .thenBy { it.sourceId }
        ).take(MAX_RESULTS)
    }

    fun resolveCandidatesByUuid(
        sourceUuid: UUID,
        preferredType: SearchSourceType?
    ): List<DocumentCandidate> {
        val uuidString = sourceUuid.toString()
        val publicationCandidates = if (preferredType == null || preferredType == SearchSourceType.PUBLICATION) {
            publicationPersistencePort.findAllOrderByYearDescIdAsc()
                .mapNotNull { publication ->
                    val publicationId = publication.id ?: return@mapNotNull null
                    val linkUuid = extractUuid(publication.link) ?: return@mapNotNull null
                    if (!linkUuid.equals(uuidString, ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    DocumentCandidate(
                        sourceType = SearchSourceType.PUBLICATION,
                        sourceId = publicationId,
                        sourceUuid = linkUuid
                    )
                }
        } else {
            emptyList()
        }

        val studentWorkCandidates = if (preferredType == null || preferredType == SearchSourceType.STUDENT_WORK) {
            studentWorkPersistencePort.findAllOrdered()
                .mapNotNull { studentWork ->
                    val studentWorkId = studentWork.id ?: return@mapNotNull null
                    val linkUuid = extractUuid(studentWork.documentLink) ?: return@mapNotNull null
                    if (!linkUuid.equals(uuidString, ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    DocumentCandidate(
                        sourceType = SearchSourceType.STUDENT_WORK,
                        sourceId = studentWorkId,
                        sourceUuid = linkUuid
                    )
                }
        } else {
            emptyList()
        }

        return publicationCandidates + studentWorkCandidates
    }

    fun resolveChunksForDocument(
        question: String,
        sourceType: SearchSourceType,
        sourceId: Long
    ): List<SearchChunk> {
        val matches = searchChunkUseCase.search(question, limit = MAX_MATCHES, sourceType = sourceType, sourceId = sourceId)
        if (matches.isNotEmpty()) {
            return matches
        }
        return searchChunkUseCase.findBySource(sourceType = sourceType, sourceId = sourceId, limit = MAX_MATCHES)
    }

    fun extractUuids(messages: List<String>): List<UUID> {
        val pattern = Regex("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}\\b")
        return messages
            .flatMap { message -> pattern.findAll(message).map { it.value }.toList() }
            .mapNotNull { uuidString -> runCatching { UUID.fromString(uuidString) }.getOrNull() }
            .distinct()
    }

    fun extractUuid(link: String?): String? {
        if (link.isNullOrBlank()) {
            return null
        }
        val pattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")
        return pattern.find(link)?.value?.lowercase()
    }

    private fun buildSnippet(question: String, chunk: SearchChunk): String {
        val normalized = chunk.content.replace(Regex("\\s+"), " ").trim()
        if (normalized.length <= 320) {
            return normalized
        }

        val lower = normalized.lowercase()
        val queryLower = question.lowercase()
        val idx = lower.indexOf(queryLower)
        if (idx < 0) {
            return normalized.take(320) + "…"
        }

        val start = (idx - 120).coerceAtLeast(0)
        val end = (idx + 180).coerceAtMost(normalized.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < normalized.length) "…" else ""
        return prefix + normalized.substring(start, end).trim() + suffix
    }
}

data class DocumentCandidate(
    val sourceType: SearchSourceType,
    val sourceId: Long,
    val sourceUuid: String
)
