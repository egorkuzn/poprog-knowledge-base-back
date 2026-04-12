package com.example.poprogknowledgebaseback.adapters.inbound.web.market

import com.example.poprogknowledgebaseback.application.market.MarketCatalogService
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

data class MarketAppResponse(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val tags: List<String>,
    val platform: String,
    val version: String,
    val priceModel: String,
    val downloadUrl: String?
)

data class MarketSearchResponse(
    val query: String,
    val category: String?,
    val total: Int,
    val items: List<MarketAppResponse>
)

data class MarketCategoriesResponse(
    val categories: List<String>
)

@RestController
@RequestMapping("/api/market")
@Tag(name = "Poprog Market", description = "Каталог утилит и инструментов Poprog Market")
class MarketController(
    private val marketCatalogService: MarketCatalogService
) {

    @GetMapping("/apps")
    @Operation(
        summary = "Получить карточки утилит Poprog Market",
        description = "Возвращает список утилит с фильтрацией по категории и текстовым поиском."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Каталог утилит получен",
                content = [Content(schema = Schema(implementation = MarketSearchResponse::class))]
            )
        ]
    )
    fun getApps(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "24") limit: Int
    ): MarketSearchResponse {
        val result = marketCatalogService.searchApps(q, category, limit)

        return MarketSearchResponse(
            query = result.query,
            category = result.category,
            total = result.total,
            items = result.items.map {
                MarketAppResponse(
                    id = it.id,
                    title = it.title,
                    summary = it.summary,
                    category = it.category,
                    tags = it.tags,
                    platform = it.platform,
                    version = it.version,
                    priceModel = it.priceModel,
                    downloadUrl = it.downloadUrl
                )
            }
        )
    }

    @GetMapping("/categories")
    @Operation(summary = "Получить категории Poprog Market")
    fun getCategories(): MarketCategoriesResponse =
        MarketCategoriesResponse(categories = marketCatalogService.getCategories())
}
