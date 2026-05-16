package com.example.poprogknowledgebaseback.adapters.inbound.web.files

import com.example.poprogknowledgebaseback.application.files.StoredFileReadService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.nio.file.Files
import java.nio.file.Path
import java.text.Normalizer
import java.util.Locale
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ContentDisposition
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/files")
@Tag(name = "Файлы", description = "Публичный просмотр PDF-файлов по пути из storage")
class FileAccessController(
    @Value("\${app.files.storage-dir}") private val storageDir: String,
    private val storedFileReadService: StoredFileReadService
) {

    @GetMapping("/{*path}")
    @Operation(
        summary = "Получить PDF-файл по относительному пути",
        description = "Публично возвращает PDF-файл из локального storage. Успешный ответ отдается как inline для просмотра в браузере."
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

        if (!relativePath.lowercase().endsWith(".pdf")) {
            return htmlError(
                status = HttpStatus.BAD_REQUEST,
                title = "Неподдерживаемый тип файла",
                message = "Сервис поддерживает только PDF-файлы."
            )
        }

        // Prefer DB-backed files to make deployments/migrations reliable.
        val segments = relativePath.split('/')
        if (segments.size >= 2) {
            val category = segments.first()
            val storedFilename = segments.drop(1).joinToString("/")
            storedFileReadService.findContent(category = category, storedFilename = storedFilename)?.let { stored ->
                val fileName = safeAsciiFileName(stored.storedFilename)
                val headers = HttpHeaders()
                headers.contentDisposition = ContentDisposition.inline().filename(fileName).build()
                return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(stored.bytes)
            }
        }

        val storageRoot = Path.of(storageDir).toAbsolutePath().normalize()
        val targetPath = storageRoot.resolve(relativePath).normalize()
        if (!targetPath.startsWith(storageRoot)) {
            return htmlError(
                status = HttpStatus.BAD_REQUEST,
                title = "Некорректный путь к файлу",
                message = "Запрошенный путь содержит недопустимые сегменты."
            )
        }

        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            return htmlError(
                status = HttpStatus.NOT_FOUND,
                title = "Файл не найден",
                message = "Запрошенный PDF-файл отсутствует в хранилище."
            )
        }

        val resource = UrlResource(targetPath.toUri())
        val fileName = safeAsciiFileName(targetPath.fileName.toString())
        val headers = HttpHeaders()
        // Avoid non-ASCII filenames in headers (Tomcat will fail to encode them).
        headers.contentDisposition = ContentDisposition.inline().filename(fileName).build()
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .headers(headers)
            .body(resource)
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

    private fun safeAsciiFileName(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFKD)
        val ascii = buildString {
            for (ch in normalized) {
                when {
                    ch.code in 0x20..0x7E -> append(ch)
                    else -> append('_')
                }
            }
        }
            .replace(Regex("\\s+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .lowercase(Locale.ROOT)
            .ifBlank { "file.pdf" }
        return if (ascii.endsWith(".pdf")) ascii else "$ascii.pdf"
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
