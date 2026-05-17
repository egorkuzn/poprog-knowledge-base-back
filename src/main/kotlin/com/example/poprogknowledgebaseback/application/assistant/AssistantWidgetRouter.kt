package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.application.search.SearchResult
import com.example.poprogknowledgebaseback.application.search.SearchUseCase
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole
import com.example.poprogknowledgebaseback.domain.publication.Publication
import com.example.poprogknowledgebaseback.domain.publication.port.PublicationPersistencePort
import com.example.poprogknowledgebaseback.domain.studentwork.StudentWork
import com.example.poprogknowledgebaseback.domain.studentwork.port.StudentWorkPersistencePort
import org.springframework.stereotype.Component

@Component
class AssistantWidgetRouter(
    private val publicationPersistencePort: PublicationPersistencePort,
    private val studentWorkPersistencePort: StudentWorkPersistencePort,
    private val searchUseCase: SearchUseCase
) {

    companion object {
        private const val MAX_ITEMS = 5
        private const val FUZZY_THRESHOLD = 0.68
        private val EXPLANATION_PATTERNS = listOf("объясни", "сравни", "расскажи", "о чем", "о чём", "почему")
        private val PUBLICATION_PATTERNS = listOf(
            "какие есть публикации",
            "какие у вас есть публикации",
            "покажи публикации",
            "что есть по публикациям",
            "публикации"
        )
        private val WORK_PATTERNS = listOf(
            "какие есть студенческие работы",
            "покажи студенческие работы",
            "покажи работы",
            "есть ли студенческие работы",
            "студенческие работы",
            "дипломные работы",
            "диссертации"
        )
        private val SEARCH_PATTERNS = listOf(
            "найди",
            "поищи",
            "что есть",
            "есть ли",
            "покажи",
            "подбери"
        )
        private val LANGUAGE_CATALOG_PATTERNS = listOf(
            "какие языки есть",
            "какие языки у вас есть",
            "языки программирования",
            "покажи языки программирования",
            "покажи языки"
        )
        private val DOCUMENTATION_PATTERNS = listOf(
            "документация",
            "документацию",
            "документации",
            "руководство",
            "гайд",
            "инструкция",
            "мануал",
            "быстрый старт",
            "как начать"
        )
        private val SUPPORTED_LANGUAGES = listOf(
            SupportedLanguage("reflex", "Reflex", listOf("reflex"), "Процесс-ориентированный язык Poprog"),
            SupportedLanguage("post", "poST", listOf("post", "поst"), "Процесс-ориентированное расширение Structured Text"),
            SupportedLanguage("industrial-c", "IndustrialC", listOf("industrialc", "industrial c"), "Промышленная автоматизация и real-time")
        )
    }

    fun resolve(command: AssistantChatCommand): AssistantWidgetResult? {
        val userMessage = command.messages.lastOrNull { it.role == AiChatMessageRole.USER }?.content?.trim().orEmpty()
        if (userMessage.isBlank() || command.documentRef != null) {
            return null
        }

        val normalized = normalize(userMessage)
        if (matchesAnyPattern(normalized, EXPLANATION_PATTERNS, threshold = 0.72)) {
            return null
        }

        if (matchesAnyPattern(normalized, LANGUAGE_CATALOG_PATTERNS)) {
            return buildLanguagesWidget()
        }

        val asksForPublications = containsApproximateKeyword(normalized, listOf("публикации", "публикация"))
        val asksForWorks = containsApproximateKeyword(normalized, listOf("студенческие", "работы", "работа", "дипломные", "диссертации"))
        val asksForDocumentation = matchesAnyPattern(normalized, DOCUMENTATION_PATTERNS, threshold = 0.64)

        detectLanguage(normalized)?.let { language ->
            if (asksForDocumentation) {
                return buildProjectDocumentationWidget(language)
            }
            if (!asksForPublications && !asksForWorks) {
                return buildLanguageBranch(language)
            }
        }

        if (asksForDocumentation) {
            return buildProjectDocumentationCatalogWidget()
        }

        if (containsApproximateKeyword(normalized, listOf("публикации", "публикация"))) {
            return buildPublicationWidget(userMessage)
        }

        if (matchesAnyPattern(normalized, PUBLICATION_PATTERNS)) {
            return buildPublicationWidget(userMessage)
        }

        if (containsApproximateKeyword(normalized, listOf("студенческие", "работы", "работа", "дипломные", "диссертации"))) {
            return buildStudentWorksWidget(userMessage)
        }

        if (matchesAnyPattern(normalized, WORK_PATTERNS)) {
            return buildStudentWorksWidget(userMessage)
        }

        if (matchesAnyPattern(normalized, SEARCH_PATTERNS, threshold = 0.64) || detectLanguage(normalized) != null) {
            return buildSearchWidget(userMessage)
        }

        return null
    }

    private fun buildPublicationWidget(userMessage: String): AssistantWidgetResult {
        val searchQuery = extractSpecificQuery(userMessage, "публикац")
        val useSpecificQuery = isSpecificSearchQuery(searchQuery)
        val items = if (useSpecificQuery) {
            searchUseCase.search(searchQuery, MAX_ITEMS * 2)
                .filter { it.type.contains("publication") }
                .take(MAX_ITEMS)
                .map { it.toPublicationWidgetItem() }
        } else {
            publicationPersistencePort.findAllOrderByYearDescIdAsc()
                .take(MAX_ITEMS)
                .mapNotNull { it.toPublicationWidgetItem() }
        }

        if (items.isEmpty()) {
            return buildEmptyResultWidget(userMessage)
        }

        return AssistantWidgetResult(
            widgetType = "publications_list",
            title = "Публикации",
            subtitle = if (useSpecificQuery) "Подобрали публикации по запросу" else "Первые публикации из базы знаний",
            items = items,
            actions = listOf(
                navigateAction("open-publications", "Открыть публикации", "/publications")
            ),
            followUpOptions = listOf(
                promptAction("publications-reflex", "Публикации по Reflex", "Покажи публикации по Reflex"),
                promptAction("publications-post", "Публикации по poST", "Покажи публикации по poST"),
                promptAction("publications-industrial-c", "Публикации по IndustrialC", "Покажи публикации по IndustrialC")
            )
        )
    }

    private fun buildStudentWorksWidget(userMessage: String): AssistantWidgetResult {
        val searchQuery = extractSpecificQuery(userMessage, "работ")
        val useSpecificQuery = isSpecificSearchQuery(searchQuery)
        val items = if (useSpecificQuery) {
            searchUseCase.search(searchQuery, MAX_ITEMS * 2)
                .filter { it.type.contains("student") || it.type.contains("work") }
                .take(MAX_ITEMS)
                .map { it.toStudentWorkWidgetItem() }
        } else {
            studentWorkPersistencePort.findAllOrdered()
                .take(MAX_ITEMS)
                .mapNotNull { it.toStudentWorkWidgetItem() }
        }

        if (items.isEmpty()) {
            return buildEmptyResultWidget(userMessage)
        }

        return AssistantWidgetResult(
            widgetType = "student_works_list",
            title = "Студенческие работы",
            subtitle = if (useSpecificQuery) "Подобрали работы по запросу" else "Первые работы из каталога",
            items = items,
            actions = listOf(
                navigateAction("open-works", "Открыть раздел работ", "/works")
            ),
            followUpOptions = listOf(
                promptAction("works-reflex", "Работы по Reflex", "Есть ли студенческие работы по Reflex?"),
                promptAction("works-post", "Работы по poST", "Есть ли студенческие работы по poST?"),
                promptAction("works-industrial-c", "Работы по IndustrialC", "Есть ли студенческие работы по IndustrialC?")
            )
        )
    }

    private fun buildLanguagesWidget(): AssistantWidgetResult =
        AssistantWidgetResult(
            widgetType = "languages_list",
            title = "Языки программирования",
            subtitle = "Выберите направление, и чат сразу предложит следующий шаг",
            items = SUPPORTED_LANGUAGES.take(MAX_ITEMS + 3).map { language ->
                AssistantWidgetItemResult(
                    id = language.id,
                    title = language.title,
                    subtitle = language.description,
                    prompt = "Что у вас есть по ${language.title}?"
                )
            },
            followUpOptions = SUPPORTED_LANGUAGES.take(4).map { language ->
                promptAction("lang-${language.id}", language.title, "Что у вас есть по ${language.title}?")
            }
        )

    private fun buildLanguageBranch(language: SupportedLanguage): AssistantWidgetResult =
        AssistantWidgetResult(
            widgetType = "branch",
            title = "Что показать по ${language.title}?",
            subtitle = "Можно сразу перейти к типовым сценариям без обращения к LLM",
            followUpOptions = listOf(
                navigateAction("${language.id}-documentation", "Документация", "/projects/${language.id}/docs"),
                promptAction("${language.id}-publications", "Публикации", "Покажи публикации по ${language.title}"),
                promptAction("${language.id}-works", "Студенческие работы", "Есть ли студенческие работы по ${language.title}?"),
                promptAction("${language.id}-search", "Все материалы", "Найди материалы по ${language.title}"),
                navigateAction("${language.id}-projects", "Проекты", "/projects")
            )
        )

    private fun buildProjectDocumentationWidget(language: SupportedLanguage): AssistantWidgetResult =
        AssistantWidgetResult(
            widgetType = "project_documentation",
            title = "Документация ${language.title}",
            subtitle = "Открываю проектную документацию вместо общей инструкции портала",
            items = listOf(
                AssistantWidgetItemResult(
                    id = "${language.id}-docs",
                    title = "Открыть документацию ${language.title}",
                    subtitle = language.description,
                    meta = "Проектная документация",
                    href = "/projects/${language.id}/docs",
                    sourceType = "project-documentation",
                    sourceId = language.id
                )
            ),
            actions = listOf(
                navigateAction("${language.id}-open-docs", "Открыть документацию", "/projects/${language.id}/docs")
            ),
            followUpOptions = listOf(
                promptAction("${language.id}-publications", "Публикации", "Покажи публикации по ${language.title}"),
                promptAction("${language.id}-works", "Студенческие работы", "Есть ли студенческие работы по ${language.title}?"),
                navigateAction("${language.id}-project", "Карточка проекта", "/projects/${language.id}")
            )
        )

    private fun buildProjectDocumentationCatalogWidget(): AssistantWidgetResult =
        AssistantWidgetResult(
            widgetType = "project_documentation_catalog",
            title = "Документация проектов",
            subtitle = "Выберите технологию Poprog, по которой нужна документация",
            items = SUPPORTED_LANGUAGES.map { language ->
                AssistantWidgetItemResult(
                    id = "${language.id}-docs",
                    title = language.title,
                    subtitle = language.description,
                    href = "/projects/${language.id}/docs",
                    sourceType = "project-documentation",
                    sourceId = language.id
                )
            },
            actions = listOf(
                navigateAction("open-projects", "Открыть все проекты", "/projects")
            ),
            followUpOptions = SUPPORTED_LANGUAGES.map { language ->
                navigateAction("${language.id}-open-docs", language.title, "/projects/${language.id}/docs")
            }
        )

    private fun buildSearchWidget(userMessage: String): AssistantWidgetResult {
        val items = searchUseCase.search(userMessage, MAX_ITEMS)
        if (items.isEmpty()) {
            return buildEmptyResultWidget(userMessage)
        }

        return AssistantWidgetResult(
            widgetType = "search_results",
            title = "Результаты поиска",
            subtitle = "Первые подходящие материалы по вашему запросу",
            items = items.map { it.toSearchWidgetItem() },
            followUpOptions = listOf(
                promptAction("refine-query", "Уточнить запрос", "Подбери материалы точнее: $userMessage"),
                promptAction("explain-results", "Суммаризировать результаты", "Кратко суммаризируй, что есть по запросу: $userMessage")
            )
        )
    }

    private fun buildEmptyResultWidget(userMessage: String): AssistantWidgetResult =
        AssistantWidgetResult(
            widgetType = "empty_result",
            title = "Точных совпадений пока нет",
            subtitle = "Попробуйте один из быстрых вариантов уточнения",
            followUpOptions = listOf(
                promptAction("retry-publications", "Публикации", "Покажи публикации по теме: $userMessage"),
                promptAction("retry-works", "Студенческие работы", "Покажи студенческие работы по теме: $userMessage"),
                promptAction("retry-short", "Сократить запрос", shortenPrompt(userMessage))
            )
        )

    private fun SearchResult.toSearchWidgetItem() = AssistantWidgetItemResult(
        id = id,
        title = theme,
        subtitle = authors,
        meta = published,
        href = when {
            type.contains("publication") -> "/publications?focusType=publication&focusId=$sourceId"
            else -> "/works?focusType=student-work&focusId=$sourceId"
        },
        sourceType = type,
        sourceId = sourceId.toString()
    )

    private fun SearchResult.toPublicationWidgetItem() = AssistantWidgetItemResult(
        id = id,
        title = theme,
        subtitle = authors,
        meta = published,
        href = "/publications?focusType=publication&focusId=$sourceId",
        sourceType = type,
        sourceId = sourceId.toString()
    )

    private fun SearchResult.toStudentWorkWidgetItem() = AssistantWidgetItemResult(
        id = id,
        title = theme,
        subtitle = authors,
        meta = groupTitle,
        href = "/works?focusType=student-work&focusId=$sourceId",
        sourceType = type,
        sourceId = sourceId.toString()
    )

    private fun Publication.toPublicationWidgetItem(): AssistantWidgetItemResult? {
        val sourceId = id ?: return null
        return AssistantWidgetItemResult(
            id = "publication-$sourceId",
            title = theme,
            subtitle = authors,
            meta = published,
            href = "/publications?focusType=publication&focusId=$sourceId",
            sourceType = "publication",
            sourceId = sourceId.toString()
        )
    }

    private fun StudentWork.toStudentWorkWidgetItem(): AssistantWidgetItemResult? {
        val sourceId = id ?: return null
        return AssistantWidgetItemResult(
            id = "student-work-$sourceId",
            title = theme,
            subtitle = authors,
            meta = projectTypeTitle,
            href = "/works?focusType=student-work&focusId=$sourceId",
            sourceType = "student-work",
            sourceId = sourceId.toString()
        )
    }

    private fun normalize(value: String): String =
        " ${value.lowercase().replace(Regex("[^\\p{L}\\p{N}+/# -]"), " ").replace(Regex("\\s+"), " ").trim()} "

    private fun detectLanguage(normalized: String): SupportedLanguage? =
        SUPPORTED_LANGUAGES.firstOrNull { language ->
            language.markers.any { marker -> fuzzyContains(normalized, normalize(marker).trim()) } ||
                fuzzyContains(normalized, normalize(language.title).trim())
        }

    private fun extractSpecificQuery(message: String, domainMarker: String): String {
        val lowered = message.lowercase()
        val cleaned = lowered
            .replace(Regex("какие у вас есть"), " ")
            .replace(Regex("какие есть"), " ")
            .replace(Regex("покажи"), " ")
            .replace(Regex("что у вас есть"), " ")
            .replace(Regex("что есть"), " ")
            .replace(Regex("есть ли"), " ")
            .replace(Regex("по $domainMarker\\p{L}*"), " ")
            .replace(Regex("$domainMarker\\p{L}*"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return cleaned
    }

    private fun shortenPrompt(message: String): String {
        val shortened = message.split(" ").filter { it.length > 2 }.take(4).joinToString(" ")
        return if (shortened.isBlank()) "Найди похожие материалы" else "Найди материалы по теме: $shortened"
    }

    private fun isSpecificSearchQuery(searchQuery: String): Boolean {
        val genericTokens = setOf("какие", "какая", "какое", "какой", "есть", "есь", "у", "вас", "по", "что", "покажи")
        val meaningfulTokens = searchQuery
            .split(" ")
            .filter { it.isNotBlank() }
            .filterNot { genericTokens.contains(it) }

        return meaningfulTokens.size >= 1 && meaningfulTokens.joinToString(" ").length >= 4
    }

    private fun matchesAnyPattern(
        normalizedQuery: String,
        patterns: List<String>,
        threshold: Double = FUZZY_THRESHOLD
    ): Boolean = patterns.any { pattern -> fuzzyContains(normalizedQuery, pattern, threshold) }

    private fun fuzzyContains(
        normalizedQuery: String,
        pattern: String,
        threshold: Double = FUZZY_THRESHOLD
    ): Boolean {
        val normalizedPattern = normalize(pattern).trim()
        val query = normalizedQuery.trim()
        if (query.isBlank() || normalizedPattern.isBlank()) {
            return false
        }

        if (query.contains(normalizedPattern)) {
            return true
        }

        val queryTokens = query.split(" ").filter { it.isNotBlank() }
        val patternTokens = normalizedPattern.split(" ").filter { it.isNotBlank() }
        if (queryTokens.isEmpty() || patternTokens.isEmpty()) {
            return false
        }

        if (phraseSimilarity(query, normalizedPattern) >= threshold) {
            return true
        }

        val minWindow = (patternTokens.size - 1).coerceAtLeast(1)
        val maxWindow = (patternTokens.size + 1).coerceAtMost(queryTokens.size)
        for (windowSize in minWindow..maxWindow) {
            for (start in 0..(queryTokens.size - windowSize)) {
                val window = queryTokens.subList(start, start + windowSize).joinToString(" ")
                if (phraseSimilarity(window, normalizedPattern) >= threshold) {
                    return true
                }
            }
        }

        return false
    }

    private fun phraseSimilarity(left: String, right: String): Double {
        val leftTokens = left.split(" ").filter { it.isNotBlank() }
        val rightTokens = right.split(" ").filter { it.isNotBlank() }
        val intersection = leftTokens.count { token -> rightTokens.contains(token) }
        val tokenCoverage = intersection.toDouble() / rightTokens.size.coerceAtLeast(1)
        val distance = levenshtein(left, right)
        val maxLength = maxOf(left.length, right.length).coerceAtLeast(1)
        val editScore = 1.0 - (distance.toDouble() / maxLength)
        return maxOf(tokenCoverage, editScore)
    }

    private fun containsApproximateKeyword(normalizedQuery: String, keywords: List<String>): Boolean {
        val queryTokens = normalizedQuery.trim().split(" ").filter { it.isNotBlank() }
        return queryTokens.any { token ->
            keywords.any { keyword ->
                phraseSimilarity(token, normalize(keyword).trim()) >= 0.72
            }
        }
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) {
            return 0
        }
        if (left.isEmpty()) {
            return right.length
        }
        if (right.isEmpty()) {
            return left.length
        }

        val previous = IntArray(right.length + 1) { it }
        val current = IntArray(right.length + 1)

        for (leftIndex in left.indices) {
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val cost = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + cost
                )
            }
            for (index in previous.indices) {
                previous[index] = current[index]
            }
        }

        return previous[right.length]
    }

    private fun promptAction(id: String, label: String, prompt: String) = AssistantWidgetActionResult(
        id = id,
        label = label,
        kind = "send_prompt",
        prompt = prompt
    )

    private fun navigateAction(id: String, label: String, href: String) = AssistantWidgetActionResult(
        id = id,
        label = label,
        kind = "navigate",
        href = href
    )
}

private data class SupportedLanguage(
    val id: String,
    val title: String,
    val markers: List<String>,
    val description: String
)
