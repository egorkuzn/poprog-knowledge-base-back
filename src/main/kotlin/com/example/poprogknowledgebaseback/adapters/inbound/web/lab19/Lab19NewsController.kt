package com.example.poprogknowledgebaseback.adapters.inbound.web.lab19

import com.example.poprogknowledgebaseback.application.lab19.Lab19NewsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class Lab19NewsItemResponse(
    val id: Long,
    val title: String,
    val sourceUrl: String,
    val sourcePage: String,
    val year: Int?,
    val contentType: String?,
    val materialKind: String,
    val status: String
)

@RestController
@RequestMapping("/api/lab-19/news")
@Tag(name = "Лаборатория 19", description = "Новостные и событийные материалы лаборатории 19")
class Lab19NewsController(
    private val lab19NewsService: Lab19NewsService
) {

    @GetMapping
    @Operation(
        summary = "Получить новостные материалы лаборатории 19",
        description = "Возвращает материалы из staging-таблицы lab-19. Научные публикации и ВКР импортируются отдельно, а событийные материалы остаются здесь."
    )
    fun getNews(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) kind: String?,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): List<Lab19NewsItemResponse> =
        lab19NewsService.search(q, year, kind, limit, offset).map {
            Lab19NewsItemResponse(
                id = it.id,
                title = it.title,
                sourceUrl = it.sourceUrl,
                sourcePage = it.sourcePage,
                year = it.year,
                contentType = it.contentType,
                materialKind = it.materialKind,
                status = it.status
            )
        }
}
