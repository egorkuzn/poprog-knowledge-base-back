package com.example.poprogknowledgebaseback.adapters.inbound.web.search

import com.example.poprogknowledgebaseback.application.search.SearchResult
import com.example.poprogknowledgebaseback.application.search.SearchUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
@Tag(name = "Поиск", description = "Полнотекстовый поиск по публикациям и студенческим работам")
class SearchController(
    private val searchUseCase: SearchUseCase
) {

    @GetMapping
    @Operation(
        summary = "Найти материалы по текстовому запросу",
        description = "Ищет по авторам, теме, описанию публикации и контексту группы. Возвращает смешанный список публикаций и студенческих работ."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Результаты поиска успешно получены",
                content = [Content(schema = Schema(implementation = SearchResponse::class))]
            )
        ]
    )
    fun search(
        @RequestParam q: String,
        @RequestParam(required = false, defaultValue = "20") limit: Int
    ): SearchResponse {
        val normalizedQuery = q.trim()
        if (normalizedQuery.isBlank()) {
            return SearchResponse(query = normalizedQuery, total = 0, items = emptyList())
        }

        val normalizedLimit = limit.coerceIn(1, 100)
        val items = searchUseCase.search(normalizedQuery, normalizedLimit).map { it.toDto() }

        return SearchResponse(
            query = normalizedQuery,
            total = items.size,
            items = items
        )
    }

    private fun SearchResult.toDto() = SearchItemDto(
        id = id,
        type = type,
        sourceId = sourceId,
        groupTitle = groupTitle,
        groupHash = groupHash,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )
}
