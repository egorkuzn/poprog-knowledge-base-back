package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.application.search.SearchChunkUseCase
import com.example.poprogknowledgebaseback.domain.search.SearchChunk
import com.example.poprogknowledgebaseback.domain.search.SearchSourceType
import org.springframework.stereotype.Component

@Component
class DocumentQuestionResolver(
    private val searchChunkUseCase: SearchChunkUseCase
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

    fun resolveChunksForDocument(
        question: String,
        sourceType: SearchSourceType,
        sourceId: Long
    ): List<SearchChunk> =
        searchChunkUseCase.search(question, limit = MAX_MATCHES, sourceType = sourceType, sourceId = sourceId)

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
