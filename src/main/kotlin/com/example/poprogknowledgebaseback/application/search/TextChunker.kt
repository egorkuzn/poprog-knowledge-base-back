package com.example.poprogknowledgebaseback.application.search

import org.springframework.stereotype.Component

@Component
class TextChunker {

    companion object {
        private const val MAX_CHUNK_CHARS = 1500
        private const val OVERLAP_CHARS = 200
        private const val MAX_CHUNKS = 400
    }

    fun chunk(text: String): List<String> {
        val normalized = text.replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank()) {
            return emptyList()
        }

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < normalized.length && chunks.size < MAX_CHUNKS) {
            val maxEnd = minOf(start + MAX_CHUNK_CHARS, normalized.length)
            var end = maxEnd
            if (end < normalized.length) {
                val lastSpace = normalized.lastIndexOf(' ', maxEnd)
                if (lastSpace > start + MAX_CHUNK_CHARS / 2) {
                    end = lastSpace
                }
            }

            val chunk = normalized.substring(start, end).trim()
            if (chunk.isNotBlank()) {
                chunks.add(chunk)
            }

            if (end >= normalized.length) {
                break
            }

            val nextStart = end - OVERLAP_CHARS
            start = if (nextStart <= start) end else nextStart
        }

        return chunks
    }
}
