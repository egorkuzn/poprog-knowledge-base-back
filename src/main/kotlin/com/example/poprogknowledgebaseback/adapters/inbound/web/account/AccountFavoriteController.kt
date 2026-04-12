package com.example.poprogknowledgebaseback.adapters.inbound.web.account

import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUser
import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUserParam
import com.example.poprogknowledgebaseback.application.account.AccountFavoriteService
import com.example.poprogknowledgebaseback.application.account.UpsertFavoriteCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class AccountFavoriteResponse(
    val id: Long,
    val itemType: String,
    val itemId: String,
    val title: String,
    val link: String?,
    val createdAt: String
)

data class UpsertFavoriteRequest(
    @field:NotBlank
    @field:Size(max = 32)
    val itemType: String,
    @field:NotBlank
    @field:Size(max = 128)
    val itemId: String,
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,
    @field:Size(max = 2048)
    val link: String? = null
)

@RestController
@RequestMapping("/api/account/favorites")
@Tag(name = "Личный кабинет", description = "Избранные материалы текущего пользователя")
class AccountFavoriteController(
    private val accountFavoriteService: AccountFavoriteService
) {

    @GetMapping
    @Operation(summary = "Получить избранные материалы")
    fun getFavorites(@CurrentUserParam currentUser: CurrentUser): List<AccountFavoriteResponse> =
        accountFavoriteService.getFavorites(currentUser.subject)
            .map {
                AccountFavoriteResponse(
                    id = it.id,
                    itemType = it.itemType,
                    itemId = it.itemId,
                    title = it.title,
                    link = it.link,
                    createdAt = it.createdAt
                )
            }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Добавить или обновить материал в избранном")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Материал добавлен в избранное",
                content = [Content(schema = Schema(implementation = AccountFavoriteResponse::class))]
            )
        ]
    )
    fun upsertFavorite(
        @CurrentUserParam currentUser: CurrentUser,
        @Valid @RequestBody request: UpsertFavoriteRequest
    ): AccountFavoriteResponse {
        val result = accountFavoriteService.upsertFavorite(
            currentUser.subject,
            UpsertFavoriteCommand(
                itemType = request.itemType,
                itemId = request.itemId,
                title = request.title,
                link = request.link
            )
        )

        return AccountFavoriteResponse(
            id = result.id,
            itemType = result.itemType,
            itemId = result.itemId,
            title = result.title,
            link = result.link,
            createdAt = result.createdAt
        )
    }

    @DeleteMapping("/{itemType}/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить материал из избранного")
    fun deleteFavorite(
        @CurrentUserParam currentUser: CurrentUser,
        @PathVariable itemType: String,
        @PathVariable itemId: String
    ) {
        accountFavoriteService.deleteFavorite(currentUser.subject, itemType, itemId)
    }
}
