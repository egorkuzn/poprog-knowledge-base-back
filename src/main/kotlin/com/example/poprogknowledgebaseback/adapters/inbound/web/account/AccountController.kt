package com.example.poprogknowledgebaseback.adapters.inbound.web.account

import com.example.poprogknowledgebaseback.application.account.AccountProfileResult
import com.example.poprogknowledgebaseback.application.account.AccountPasswordResetService
import com.example.poprogknowledgebaseback.application.account.AccountProfileService
import com.example.poprogknowledgebaseback.application.account.KeycloakAccountRegistrationService
import com.example.poprogknowledgebaseback.application.account.RegisterAccountCommand
import com.example.poprogknowledgebaseback.application.account.UpdateAccountProfileCommand
import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUser
import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUserParam
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/api/account")
@Tag(name = "Личный кабинет", description = "Операции профиля текущего пользователя")
class AccountController(
    private val accountProfileService: AccountProfileService,
    private val keycloakAccountRegistrationService: KeycloakAccountRegistrationService,
    private val accountPasswordResetService: AccountPasswordResetService
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Создать учетную запись пользователя",
        description = "Создает пользователя в Keycloak realm портала и сохраняет локальный профиль."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Учетная запись успешно создана",
                content = [Content(schema = Schema(implementation = AccountProfileResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            ApiResponse(responseCode = "409", description = "Пользователь уже существует"),
            ApiResponse(responseCode = "503", description = "Регистрация через Keycloak не настроена")
        ]
    )
    fun register(@Valid @RequestBody request: RegisterAccountRequest): AccountProfileResponse {
        return keycloakAccountRegistrationService.register(
            RegisterAccountCommand(
                name = request.name,
                email = request.email,
                password = request.password
            )
        ).toDto()
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "Запросить восстановление пароля",
        description = "Отправляет письмо для смены пароля через Keycloak. Ответ всегда нейтральный и не раскрывает существование email."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Запрос принят",
                content = [Content(schema = Schema(implementation = AccountPasswordResetResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Некорректный email")
        ]
    )
    fun requestPasswordReset(@Valid @RequestBody request: AccountPasswordResetRequest): AccountPasswordResetResponse {
        accountPasswordResetService.requestReset(request.email)
        return AccountPasswordResetResponse(
            status = "accepted",
            message = "Если аккаунт с таким email существует, инструкция по восстановлению пароля отправлена."
        )
    }

    @GetMapping("/profile")
    @Operation(
        summary = "Получить профиль текущего пользователя",
        description = "Возвращает профиль текущего аутентифицированного пользователя."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Профиль успешно получен",
                content = [Content(schema = Schema(implementation = AccountProfileResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Требуется авторизация")
        ]
    )
    fun getProfile(@CurrentUserParam currentUser: CurrentUser): AccountProfileResponse {
        return accountProfileService.getOrCreateProfile(currentUser).toDto()
    }

    @PutMapping("/profile")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Обновить профиль текущего пользователя",
        description = "Обновляет имя и email текущего пользователя."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Профиль успешно обновлен",
                content = [Content(schema = Schema(implementation = AccountProfileResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            ApiResponse(responseCode = "401", description = "Требуется авторизация")
        ]
    )
    fun updateProfile(
        @CurrentUserParam currentUser: CurrentUser,
        @Valid @RequestBody request: UpdateAccountProfileRequest
    ): AccountProfileResponse {
        return accountProfileService.updateProfile(
            currentUser,
            UpdateAccountProfileCommand(
                name = request.name,
                email = request.email
            )
        ).toDto()
    }

    private fun AccountProfileResult.toDto() = AccountProfileResponse(
        subject = subject,
        name = name,
        email = email,
        roles = roles
    )
}
