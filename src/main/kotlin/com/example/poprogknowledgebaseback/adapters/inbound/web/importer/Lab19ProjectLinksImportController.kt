package com.example.poprogknowledgebaseback.adapters.inbound.web.importer

import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUser
import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUserParam
import com.example.poprogknowledgebaseback.application.importer.Lab19ProjectLinksImportCommand
import com.example.poprogknowledgebaseback.application.importer.Lab19ProjectLinksImportReport
import com.example.poprogknowledgebaseback.application.importer.Lab19ProjectLinksImportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/import/lab-19/project-links")
@Tag(name = "Импорт данных", description = "Служебные операции импорта данных из доверенных источников")
class Lab19ProjectLinksImportController(
    private val service: Lab19ProjectLinksImportService
) {

    @PostMapping
    @Operation(
        summary = "[ADMIN] Импортировать ссылки для раздела проектов (lab-19)",
        description = "Извлекает ссылки со страниц лаборатории 19, проверяет их работоспособность и добавляет только рабочие ссылки в меню проектов."
    )
    fun run(
        @CurrentUserParam currentUser: CurrentUser,
        @RequestParam(defaultValue = "true") dryRun: Boolean,
        @RequestParam(defaultValue = "30") maxPages: Int,
        @RequestParam(defaultValue = "2") maxDepth: Int,
        @RequestParam(defaultValue = "languages") sectionHash: String
    ): Lab19ProjectLinksImportReport {
        requireAdmin(currentUser)
        return service.run(
            Lab19ProjectLinksImportCommand(
                dryRun = dryRun,
                maxPages = maxPages,
                maxDepth = maxDepth,
                sectionHash = sectionHash
            )
        )
    }

    private fun requireAdmin(currentUser: CurrentUser) {
        if (!currentUser.hasRole("ADMIN")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required")
        }
    }
}

