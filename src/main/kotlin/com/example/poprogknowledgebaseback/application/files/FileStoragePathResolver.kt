package com.example.poprogknowledgebaseback.application.files

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

interface FileStoragePathResolver {
    fun resolveLocalPath(url: String): Path?
}

@Service
class LocalFileStoragePathResolver(
    @Value("\${app.files.storage-dir}") private val storageDir: String,
    @Value("\${app.files.base-url}") private val baseUrl: String
) : FileStoragePathResolver {

    override fun resolveLocalPath(url: String): Path? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return null
        }

        val base = baseUrl.trimEnd('/')
        val pathPart = extractPath(trimmed)
        val normalizedPath = pathPart.trim()
        if (!normalizedPath.startsWith("$base/")) {
            return null
        }

        val relativePath = normalizedPath.removePrefix("$base/").trimStart('/')
        if (relativePath.isBlank()) {
            return null
        }

        val rootPath = Path.of(storageDir).toAbsolutePath().normalize()
        val resolved = rootPath.resolve(relativePath).normalize()
        if (!resolved.startsWith(rootPath)) {
            return null
        }

        return if (Files.exists(resolved)) resolved else null
    }

    private fun extractPath(value: String): String =
        try {
            if (value.startsWith("http://") || value.startsWith("https://")) {
                URI(value).path ?: value
            } else {
                value
            }
        } catch (ex: Exception) {
            value
        }
}
