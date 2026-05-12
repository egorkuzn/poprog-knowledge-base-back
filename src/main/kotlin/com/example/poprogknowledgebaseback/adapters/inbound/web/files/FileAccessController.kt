package com.example.poprogknowledgebaseback.adapters.inbound.web.files

import com.example.poprogknowledgebaseback.application.files.FileStorageUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/files")
@Tag(name = "Файлы", description = "Публичный просмотр PDF-файлов и ресурсов меню из PostgreSQL-хранилища")
class FileAccessController(
    private val fileStorageUseCase: FileStorageUseCase
) {

    @GetMapping("/{*path}")
    @Operation(
        summary = "Получить PDF-файл по относительному пути",
        description = "Публично возвращает PDF-файл из PostgreSQL-хранилища. Успешный ответ отдается как inline для просмотра в браузере."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "PDF-файл успешно найден и отдан",
                content = [Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = Schema(type = "string", format = "binary"))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Некорректный путь или неподдерживаемый тип файла",
                content = [Content(mediaType = MediaType.TEXT_HTML_VALUE)]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Файл не найден",
                content = [Content(mediaType = MediaType.TEXT_HTML_VALUE)]
            )
        ]
    )
    fun getFile(@PathVariable path: String): ResponseEntity<*> {
        val relativePath = normalizePath(path)
            ?: return htmlError(
                status = HttpStatus.BAD_REQUEST,
                title = "Некорректный путь к файлу",
                message = "Проверьте ссылку и повторите запрос."
            )

        val normalizedLower = relativePath.lowercase()
        if (!normalizedLower.endsWith(".pdf") && !normalizedLower.startsWith("projects-menu/")) {
            return htmlError(
                status = HttpStatus.BAD_REQUEST,
                title = "Неподдерживаемый тип файла",
                message = "Сервис поддерживает PDF-документы и изображения меню проекта."
            )
        }

        val storedFile = fileStorageUseCase.load(relativePath) ?: run {
            return htmlError(
                status = HttpStatus.NOT_FOUND,
                title = "Файл не найден",
                message = "Запрошенный PDF-файл отсутствует в хранилище."
            )
        }
        val responseContentType = resolveContentType(relativePath, storedFile.contentType)
            ?: return htmlError(
                status = HttpStatus.BAD_REQUEST,
                title = "Неподдерживаемый тип файла",
                message = "Сервис поддерживает PDF-документы и изображения меню проекта."
            )

        return ResponseEntity
            .ok()
            .contentType(responseContentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${storedFile.fileName}\"")
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
            .header("X-Content-SHA256", storedFile.sha256)
            .contentLength(storedFile.sizeBytes)
            .body(storedFile.content)
    }

    private fun resolveContentType(relativePath: String, storedContentType: String): MediaType? {
        if (relativePath.lowercase().endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF
        }

        if (!relativePath.startsWith("projects-menu/") || !storedContentType.lowercase().startsWith("image/")) {
            return null
        }

        return runCatching { MediaType.parseMediaType(storedContentType) }.getOrNull()
    }

    private fun normalizePath(input: String): String? {
        val normalized = input.trim().removePrefix("/").replace('\\', '/')
        if (normalized.isBlank()) {
            return null
        }

        val segments = normalized.split('/')
        if (segments.any { it.isBlank() || it == "." || it == ".." }) {
            return null
        }

        return normalized
    }

    private fun htmlError(status: HttpStatus, title: String, message: String): ResponseEntity<String> {
        val body = """
            <!doctype html>
            <html lang="ru">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>$title</title>
              <style>
                body { margin: 0; font-family: "Tilda Sans VF", sans-serif; background: #f7f7f8; color: #1a1a1a; }
                main { max-width: 760px; margin: 64px auto; padding: 0 20px; }
                h1 { margin: 0 0 12px; font-size: 34px; line-height: 1.1; }
                p { margin: 0; font-size: 18px; line-height: 1.5; color: #3a3a3a; }
              </style>
            </head>
            <body>
              <main>
                <h1>$title</h1>
                <p>$message</p>
              </main>
            </body>
            </html>
        """.trimIndent()

        return ResponseEntity
            .status(status)
            .contentType(MediaType.TEXT_HTML)
            .body(body)
    }
}
