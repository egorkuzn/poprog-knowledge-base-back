package com.example.poprogknowledgebaseback.adapters.inbound.web.importer

import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUser
import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUserParam
import com.example.poprogknowledgebaseback.application.importer.Lab19ImportCommand
import com.example.poprogknowledgebaseback.application.importer.Lab19ImportReport
import com.example.poprogknowledgebaseback.application.importer.Lab19ImportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/import/lab-19")
@Tag(name = "Импорт данных", description = "Служебные операции импорта данных из доверенных источников")
class Lab19ImportController(
    private val lab19ImportService: Lab19ImportService
) {

    @PostMapping
    @Operation(
        summary = "[ADMIN] Проверить и импортировать данные lab-19",
        description = "Обходит страницу лаборатории 19, проверяет внутренние ссылки и в режиме dryRun=false импортирует PDF-кандидаты через API базы знаний."
    )
    fun run(
        @CurrentUserParam currentUser: CurrentUser,
        @RequestParam(defaultValue = "true") dryRun: Boolean,
        @RequestParam(defaultValue = "30") maxPages: Int,
        @RequestParam(defaultValue = "2") maxDepth: Int,
        @RequestParam(defaultValue = "http://localhost:8080/api") importBaseUrl: String
    ): Lab19ImportReport {
        requireAdmin(currentUser)
        return lab19ImportService.run(
            Lab19ImportCommand(
                dryRun = dryRun,
                maxPages = maxPages,
                maxDepth = maxDepth,
                importBaseUrl = importBaseUrl
            )
        )
    }

    private fun requireAdmin(currentUser: CurrentUser) {
        if (!currentUser.hasRole("ADMIN")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required")
        }
    }
}
