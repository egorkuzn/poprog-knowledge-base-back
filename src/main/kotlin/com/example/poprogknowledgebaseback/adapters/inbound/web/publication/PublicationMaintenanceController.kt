package com.example.poprogknowledgebaseback.adapters.inbound.web.publication

import com.example.poprogknowledgebaseback.application.publication.PublicationMetaRepairService
import com.example.poprogknowledgebaseback.application.publication.PublicationRepairReport
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/publications")
@Tag(name = "Публикации (админ)", description = "Технические операции для очистки и нормализации публикаций")
class PublicationMaintenanceController(
    private val service: PublicationMetaRepairService
) {
    @PostMapping("/repair-and-prune")
    @Operation(summary = "[ADMIN] Восстановить метаданные публикаций и удалить некорректные записи")
    fun repairAndPrune(
        @RequestParam(defaultValue = "true") dryRun: Boolean
    ): PublicationRepairReport = service.repairAndPrune(dryRun = dryRun)
}

