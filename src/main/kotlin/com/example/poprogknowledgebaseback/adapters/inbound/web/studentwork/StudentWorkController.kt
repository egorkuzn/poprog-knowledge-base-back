package com.example.poprogknowledgebaseback.adapters.inbound.web.studentwork

import com.example.poprogknowledgebaseback.application.studentwork.StudentWorkResult
import com.example.poprogknowledgebaseback.application.studentwork.StudentWorkUseCase
import com.example.poprogknowledgebaseback.application.studentwork.UpsertStudentWorkCommand
import com.example.poprogknowledgebaseback.domain.studentwork.WorksByProjectType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/student-works")
@Tag(name = "Студенческие работы", description = "Операции для управления студенческими работами")
class StudentWorkController(
    private val studentWorkUseCase: StudentWorkUseCase
) {

    @GetMapping("/grouped")
    @Operation(
        summary = "Получить студенческие работы, сгруппированные по типу проекта",
        description = "Возвращает список работ в формате для фронтенда, сгруппированный по типу проекта (title/hash)."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список работ успешно получен",
                content = [Content(schema = Schema(implementation = WorksByProjectTypeDto::class))]
            )
        ]
    )
    fun getGrouped(): List<WorksByProjectTypeDto> = studentWorkUseCase.getGroupedWorks().map { it.toDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Создать студенческую работу",
        description = "Создает новую запись студенческой работы и привязывает её к типу проекта по hash."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Работа успешно создана",
                content = [Content(schema = Schema(implementation = StudentWorkResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            ApiResponse(responseCode = "404", description = "Тип проекта не найден")
        ]
    )
    fun create(@Valid @RequestBody request: StudentWorkCreateUpdateRequest): StudentWorkResponse =
        studentWorkUseCase.create(request.toCommand()).toDto()

    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить студенческую работу",
        description = "Обновляет существующую работу по идентификатору."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Работа успешно обновлена",
                content = [Content(schema = Schema(implementation = StudentWorkResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            ApiResponse(responseCode = "404", description = "Работа или тип проекта не найдены")
        ]
    )
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: StudentWorkCreateUpdateRequest
    ): StudentWorkResponse = studentWorkUseCase.update(id, request.toCommand()).toDto()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Удалить студенческую работу",
        description = "Удаляет работу по идентификатору."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Работа успешно удалена"),
            ApiResponse(responseCode = "404", description = "Работа не найдена")
        ]
    )
    fun delete(@PathVariable id: Long) {
        studentWorkUseCase.delete(id)
    }

    private fun StudentWorkCreateUpdateRequest.toCommand() = UpsertStudentWorkCommand(
        projectTypeHash = projectTypeHash,
        authors = authors,
        theme = theme,
        published = published
    )

    private fun StudentWorkResult.toDto() = StudentWorkResponse(
        id = id,
        title = projectTypeTitle,
        hash = projectTypeHash,
        authors = authors,
        theme = theme,
        published = published
    )

    private fun WorksByProjectType.toDto() = WorksByProjectTypeDto(
        title = title,
        hash = hash,
        works = works.map {
            WorkModelDto(
                authors = it.authors,
                theme = it.theme,
                published = it.published
            )
        }
    )
}
