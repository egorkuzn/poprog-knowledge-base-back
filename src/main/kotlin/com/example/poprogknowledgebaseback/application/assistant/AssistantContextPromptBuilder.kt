package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.domain.search.SearchChunk
import org.springframework.stereotype.Component

@Component
class AssistantContextPromptBuilder {

    fun buildSystemPrompt(matches: List<DocumentSearchResult>): String {
        if (matches.isEmpty()) {
            return ""
        }

        val header = "Ты — ассистент по научным публикациям и студенческим работам. " +
            "Отвечай строго на основе предоставленных фрагментов документов. " +
            "Если в фрагментах нет ответа, скажи, что данных недостаточно."

        val context = matches.joinToString("\n\n") { match ->
            buildString {
                append("Документ: ")
                append(match.theme)
                append(". Авторы: ")
                append(match.authors)
                append(". Источник: ")
                append(match.published)
                if (!match.link.isNullOrBlank()) {
                    append(". Ссылка: ")
                    append(match.link)
                }
                append("\nФрагмент: ")
                append(match.snippet)
            }
        }

        return header + "\n\n" + context
    }

    fun buildSystemPromptWithChunks(matches: List<SearchChunk>): String {
        if (matches.isEmpty()) {
            return ""
        }

        val header = "Ты — ассистент по научным публикациям и студенческим работам. " +
            "Отвечай строго на основе предоставленных фрагментов документов. " +
            "Если в фрагментах нет ответа, скажи, что данных недостаточно."

        val context = matches.joinToString("\n\n") { match ->
            buildString {
                append("Документ: ")
                append(match.theme)
                append(". Авторы: ")
                append(match.authors)
                append(". Источник: ")
                append(match.published)
                if (!match.link.isNullOrBlank()) {
                    append(". Ссылка: ")
                    append(match.link)
                }
                append("\nФрагмент: ")
                append(match.content)
            }
        }

        return header + "\n\n" + context
    }
}
