package com.example.poprogknowledgebaseback.adapters.inbound.web.importer

import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUser
import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUserParam
import com.example.poprogknowledgebaseback.application.importer.Lab19MetadataSyncReport
import com.example.poprogknowledgebaseback.application.importer.Lab19MetadataSyncService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/import/lab-19/metadata")
@Tag(name = "Импорт данных", description = "Служебные операции импорта данных из доверенных источников")
class Lab19MetadataSyncController(
    private val service: Lab19MetadataSyncService
) {

    @PostMapping
    @Operation(
        summary = "[ADMIN] Обновить авторов и выходные данные из PDF (lab-19)",
        description = "Переизвлекает авторов/источник из PDF по записям staging-таблицы и обновляет уже импортированные публикации и ВКР."
    )
    fun sync(
        @CurrentUserParam currentUser: CurrentUser,
        @RequestParam(defaultValue = "200") limit: Int
    ): Lab19MetadataSyncReport {
        requireAdmin(currentUser)
        return service.syncMetadata(limit = limit)
    }

    private fun requireAdmin(currentUser: CurrentUser) {
        if (!currentUser.hasRole("ADMIN")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required")
        }
    }
}

