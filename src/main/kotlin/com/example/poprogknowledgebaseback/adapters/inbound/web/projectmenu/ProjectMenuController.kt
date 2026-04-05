package com.example.poprogknowledgebaseback.adapters.inbound.web.projectmenu

import com.example.poprogknowledgebaseback.application.files.FileStorageUseCase
import com.example.poprogknowledgebaseback.application.projectmenu.ProjectMenuItemResult
import com.example.poprogknowledgebaseback.application.projectmenu.ProjectMenuPromoResult
import com.example.poprogknowledgebaseback.application.projectmenu.ProjectMenuResponseModel
import com.example.poprogknowledgebaseback.application.projectmenu.ProjectMenuSectionResult
import com.example.poprogknowledgebaseback.application.projectmenu.ProjectMenuUseCase
import com.example.poprogknowledgebaseback.application.projectmenu.UpsertProjectMenuItemCommand
import com.example.poprogknowledgebaseback.application.projectmenu.UpsertProjectMenuPromoCommand
import com.example.poprogknowledgebaseback.application.projectmenu.UpsertProjectMenuSectionCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/projects/menu")
@Tag(name = "Меню проектов", description = "Операции для получения и редактирования метаданных hover-меню раздела Проекты")
class ProjectMenuController(
    private val projectMenuUseCase: ProjectMenuUseCase,
    private val fileStorageUseCase: FileStorageUseCase
) {

    @GetMapping
    @Operation(
        summary = "Получить метаданные меню проектов",
        description = "Возвращает полную структуру hover-меню раздела Проекты: секции, CTA, карточки и промо-блоки."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Метаданные меню успешно получены",
                content = [Content(schema = Schema(implementation = ProjectMenuResponse::class))]
            )
        ]
    )
    fun getMenu(): ProjectMenuResponse = projectMenuUseCase.getMenu().toDto()

    @PostMapping("/sections")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать секцию меню проектов", description = "Создает новую секцию hover-меню.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Секция успешно создана"),
            ApiResponse(responseCode = "400", description = "Некорректные данные секции"),
            ApiResponse(responseCode = "409", description = "Секция с таким hash уже существует")
        ]
    )
    fun createSection(@Valid @RequestBody request: ProjectMenuSectionRequest): ProjectMenuSectionResponse =
        projectMenuUseCase.createSection(request.toCommand()).toDto()

    @PutMapping("/sections/{id}")
    @Operation(summary = "Обновить секцию меню проектов", description = "Обновляет секцию hover-меню по идентификатору.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Секция успешно обновлена"),
            ApiResponse(responseCode = "400", description = "Некорректные данные секции"),
            ApiResponse(responseCode = "404", description = "Секция не найдена"),
            ApiResponse(responseCode = "409", description = "Секция с таким hash уже существует")
        ]
    )
    fun updateSection(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProjectMenuSectionRequest
    ): ProjectMenuSectionResponse = projectMenuUseCase.updateSection(id, request.toCommand()).toDto()

    @DeleteMapping("/sections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить секцию меню проектов", description = "Удаляет секцию hover-меню по идентификатору.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Секция успешно удалена"),
            ApiResponse(responseCode = "404", description = "Секция не найдена")
        ]
    )
    fun deleteSection(@PathVariable id: Long) {
        projectMenuUseCase.deleteSection(id)
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать карточку меню проектов", description = "Создает карточку проекта или направления внутри секции.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Карточка успешно создана"),
            ApiResponse(responseCode = "400", description = "Некорректные данные карточки"),
            ApiResponse(responseCode = "404", description = "Секция для карточки не найдена")
        ]
    )
    fun createItem(@Valid @RequestBody request: ProjectMenuItemRequest): ProjectMenuItemResponse =
        projectMenuUseCase.createItem(request.toCommand()).toDto()

    @PutMapping("/items/{id}")
    @Operation(summary = "Обновить карточку меню проектов", description = "Обновляет карточку проекта или направления.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Карточка успешно обновлена"),
            ApiResponse(responseCode = "400", description = "Некорректные данные карточки"),
            ApiResponse(responseCode = "404", description = "Карточка или секция не найдена")
        ]
    )
    fun updateItem(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProjectMenuItemRequest
    ): ProjectMenuItemResponse = projectMenuUseCase.updateItem(id, request.toCommand()).toDto()

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить карточку меню проектов", description = "Удаляет карточку проекта или направления.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Карточка успешно удалена"),
            ApiResponse(responseCode = "404", description = "Карточка не найдена")
        ]
    )
    fun deleteItem(@PathVariable id: Long) {
        projectMenuUseCase.deleteItem(id)
    }

    @PostMapping("/promos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать промо-блок меню проектов", description = "Создает промо-карточку внутри секции.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Промо-блок успешно создан"),
            ApiResponse(responseCode = "400", description = "Некорректные данные промо-блока"),
            ApiResponse(responseCode = "404", description = "Секция для промо-блока не найдена")
        ]
    )
    fun createPromo(@Valid @RequestBody request: ProjectMenuPromoRequest): ProjectMenuPromoResponse =
        projectMenuUseCase.createPromo(request.toCommand()).toDto()

    @PutMapping("/promos/{id}")
    @Operation(summary = "Обновить промо-блок меню проектов", description = "Обновляет промо-карточку внутри секции.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Промо-блок успешно обновлён"),
            ApiResponse(responseCode = "400", description = "Некорректные данные промо-блока"),
            ApiResponse(responseCode = "404", description = "Промо-блок или секция не найдена")
        ]
    )
    fun updatePromo(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProjectMenuPromoRequest
    ): ProjectMenuPromoResponse = projectMenuUseCase.updatePromo(id, request.toCommand()).toDto()

    @DeleteMapping("/promos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить промо-блок меню проектов", description = "Удаляет промо-карточку внутри секции.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Промо-блок успешно удалён"),
            ApiResponse(responseCode = "404", description = "Промо-блок не найден")
        ]
    )
    fun deletePromo(@PathVariable id: Long) {
        projectMenuUseCase.deletePromo(id)
    }

    @PostMapping(
        "/resources/upload",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Загрузить ресурс для меню проектов",
        description = "Сохраняет изображение или другой ресурс для меню проектов и возвращает публичный URL."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Ресурс успешно загружен"),
            ApiResponse(responseCode = "400", description = "Файл не передан или некорректен")
        ]
    )
    fun uploadResource(@RequestPart("file") file: MultipartFile): ProjectMenuResourceUploadResponse {
        val stored = fileStorageUseCase.store("projects-menu", file)
        return ProjectMenuResourceUploadResponse(resourceUrl = stored.url)
    }

    private fun ProjectMenuSectionRequest.toCommand() = UpsertProjectMenuSectionCommand(
        hash = hash,
        title = title,
        description = description,
        ctaTitle = ctaTitle,
        ctaUrl = ctaUrl,
        sortOrder = sortOrder
    )

    private fun ProjectMenuItemRequest.toCommand() = UpsertProjectMenuItemCommand(
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        highlighted = highlighted,
        sortOrder = sortOrder
    )

    private fun ProjectMenuPromoRequest.toCommand() = UpsertProjectMenuPromoCommand(
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        sortOrder = sortOrder
    )

    private fun ProjectMenuResponseModel.toDto() = ProjectMenuResponse(
        title = title,
        sections = sections.map { it.toDto() }
    )

    private fun ProjectMenuSectionResult.toDto() = ProjectMenuSectionResponse(
        id = id,
        hash = hash,
        title = title,
        description = description,
        cta = LinkResponse(title = ctaTitle, url = ctaUrl),
        sortOrder = sortOrder,
        items = items.map { it.toDto() },
        promos = promos.map { it.toDto() }
    )

    private fun ProjectMenuItemResult.toDto() = ProjectMenuItemResponse(
        id = id,
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        highlighted = highlighted,
        sortOrder = sortOrder
    )

    private fun ProjectMenuPromoResult.toDto() = ProjectMenuPromoResponse(
        id = id,
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        sortOrder = sortOrder
    )
}
