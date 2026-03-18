package com.example.poprogknowledgebaseback.adapters.inbound.web.publication

import com.example.poprogknowledgebaseback.application.files.FileStorageUseCase
import com.example.poprogknowledgebaseback.application.publication.PublicationUseCase
import com.example.poprogknowledgebaseback.application.publication.UpsertPublicationCommand
import com.example.poprogknowledgebaseback.domain.publication.PublicationsByDate
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/publications")
@Tag(name = "Публикации", description = "Операции для управления публикациями")
class PublicationController(
    private val publicationUseCase: PublicationUseCase,
    private val fileStorageUseCase: FileStorageUseCase
) {

    @GetMapping("/grouped")
    @Operation(
        summary = "Получить публикации, сгруппированные по годам",
        description = "Возвращает список публикаций в формате для фронтенда, сгруппированный по году публикации."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список публикаций успешно получен",
                content = [Content(schema = Schema(implementation = PublicationsByDateDto::class))]
            )
        ]
    )
    fun getGrouped(): List<PublicationsByDateDto> =
        publicationUseCase.getGroupedPublications().map { it.toDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Создать публикацию",
        description = "Создает новую запись публикации."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Публикация успешно создана",
                content = [Content(schema = Schema(implementation = PublicationResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Некорректные входные данные")
        ]
    )
    fun create(@Valid @RequestBody request: PublicationCreateUpdateRequest): PublicationResponse =
        publicationUseCase.create(request.toCommand()).toDto()

    @PostMapping(
        "/upload",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Создать публикацию с документом",
        description = "Сохраняет документ в файловое хранилище, формирует публичную ссылку и затем создает запись публикации."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Публикация с документом успешно создана",
                content = [Content(schema = Schema(implementation = PublicationResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Некорректные входные данные")
        ]
    )
    fun upload(
        @Valid @RequestPart("metadata") request: PublicationUploadRequest,
        @RequestPart("file") file: MultipartFile
    ): PublicationResponse {
        val storedFile = fileStorageUseCase.store("publications", file)
        return publicationUseCase.create(request.toCommand(storedFile.url)).toDto()
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить публикацию",
        description = "Обновляет существующую публикацию по её идентификатору."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Публикация успешно обновлена",
                content = [Content(schema = Schema(implementation = PublicationResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            ApiResponse(responseCode = "404", description = "Публикация не найдена")
        ]
    )
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: PublicationCreateUpdateRequest
    ): PublicationResponse = publicationUseCase.update(id, request.toCommand()).toDto()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Удалить публикацию",
        description = "Удаляет публикацию по её идентификатору."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Публикация успешно удалена"),
            ApiResponse(responseCode = "404", description = "Публикация не найдена")
        ]
    )
    fun delete(@PathVariable id: Long) {
        publicationUseCase.delete(id)
    }

    private fun PublicationCreateUpdateRequest.toCommand() = UpsertPublicationCommand(
        year = year,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )

    private fun PublicationUploadRequest.toCommand(link: String) = UpsertPublicationCommand(
        year = year,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )

    private fun com.example.poprogknowledgebaseback.application.publication.PublicationResult.toDto() = PublicationResponse(
        id = id,
        year = year,
        authors = authors,
        theme = theme,
        published = published,
        link = link
    )

    private fun PublicationsByDate.toDto() = PublicationsByDateDto(
        date = date,
        publications = publications.map {
            PublicationModelDto(
                authors = it.authors,
                theme = it.theme,
                published = it.published,
                link = it.link
            )
        }
    )
}
