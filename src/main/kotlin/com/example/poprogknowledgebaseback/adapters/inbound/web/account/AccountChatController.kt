package com.example.poprogknowledgebaseback.adapters.inbound.web.account

import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUser
import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUserParam
import com.example.poprogknowledgebaseback.application.account.AccountChatService
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

data class AccountChatSummaryResponse(
    val chatId: String,
    val createdAt: String,
    val messageCount: Int,
    val lastMessagePreview: String?
)

@RestController
@RequestMapping("/api/account/chats")
@Tag(name = "Личный кабинет", description = "Чаты текущего пользователя")
class AccountChatController(
    private val accountChatService: AccountChatService
) {

    @GetMapping
    @Operation(
        summary = "Получить список чатов текущего пользователя",
        description = "Возвращает последние чаты пользователя из личного кабинета."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список чатов успешно получен",
                content = [Content(schema = Schema(implementation = AccountChatSummaryResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Требуется авторизация")
        ]
    )
    fun getChats(
        @CurrentUserParam currentUser: CurrentUser,
        @RequestParam(defaultValue = "50") limit: Int
    ): List<AccountChatSummaryResponse> =
        accountChatService.getChats(currentUser.subject, limit)
            .map {
                AccountChatSummaryResponse(
                    chatId = it.chatId,
                    createdAt = it.createdAt,
                    messageCount = it.messageCount,
                    lastMessagePreview = it.lastMessagePreview
                )
            }
}
