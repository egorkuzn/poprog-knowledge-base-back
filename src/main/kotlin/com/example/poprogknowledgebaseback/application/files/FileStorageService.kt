package com.example.poprogknowledgebaseback.application.files

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.files.SpringDataStoredFileRepository
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.files.StoredFileJpaEntity
import java.net.URI
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.HexFormat
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class FileStorageService(
    private val storedFileRepository: SpringDataStoredFileRepository,
    @Value("\${app.files.base-url}") private val baseUrl: String
) : FileStorageUseCase {

    @Transactional
    override fun store(category: String, file: MultipartFile): StoredFile {
        require(!file.isEmpty) { "Uploaded file is empty" }

        val id = UUID.randomUUID()
        val normalizedCategory = normalizeCategory(category)
        val originalName = sanitizeFileName(file.originalFilename)
        val extension = originalName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        val storedName = buildString {
            append(id)
            if (extension.isNotBlank()) {
                append(".")
                append(extension)
            }
        }
        val content = file.bytes
        val sha256 = sha256(content)
        storedFileRepository.findFirstByCategoryAndSha256(normalizedCategory, sha256)?.let { existing ->
            return StoredFile(
                fileName = existing.storedFilename,
                url = "${baseUrl.trimEnd('/')}/${existing.category}/${existing.storedFilename}"
            )
        }
        val contentType = file.contentType?.trim().takeUnless { it.isNullOrBlank() } ?: "application/octet-stream"
        val entity = StoredFileJpaEntity(
            id = id,
            category = normalizedCategory,
            originalFilename = originalName,
            storedFilename = storedName,
            contentType = contentType,
            sizeBytes = content.size.toLong(),
            sha256 = sha256,
            content = content,
            createdAt = OffsetDateTime.now()
        )
        try {
            storedFileRepository.save(entity)
        } catch (ex: DataIntegrityViolationException) {
            // In concurrent uploads, unique constraint on (category, sha256) may race.
            val existing = storedFileRepository.findFirstByCategoryAndSha256(normalizedCategory, sha256)
                ?: throw ex
            return StoredFile(
                fileName = existing.storedFilename,
                url = "${baseUrl.trimEnd('/')}/${existing.category}/${existing.storedFilename}"
            )
        }

        return StoredFile(
            fileName = storedName,
            url = "${baseUrl.trimEnd('/')}/$normalizedCategory/$storedName"
        )
    }

    @Transactional(readOnly = true)
    override fun load(relativePath: String): StoredFileContent? {
        val normalized = normalizeRelativePath(relativePath) ?: return null
        val requestedCategory = normalized.substringBefore('/')
        val storedName = normalized.substringAfterLast('/')
        val id = storedName.substringBefore('.').let { runCatching { UUID.fromString(it) }.getOrNull() } ?: return null
        val entity = storedFileRepository.findById(id).orElse(null) ?: return null

        if (entity.category != requestedCategory) {
            return null
        }

        return StoredFileContent(
            fileName = entity.originalFilename,
            contentType = entity.contentType,
            sizeBytes = entity.sizeBytes,
            sha256 = entity.sha256,
            content = entity.content
        )
    }

    @Transactional(readOnly = true)
    override fun loadFromUrl(url: String): StoredFileContent? {
        val relativePath = extractRelativePath(url) ?: return null
        return load(relativePath)
    }

    private fun normalizeCategory(category: String): String =
        category.trim().lowercase().replace(Regex("[^a-z0-9-]"), "-").trim('-').ifBlank { "general" }

    private fun sanitizeFileName(fileName: String?): String =
        fileName?.substringAfterLast('/')?.substringAfterLast('\\')?.trim()?.takeIf { it.isNotBlank() } ?: "document.pdf"

    private fun extractRelativePath(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return null
        }

        normalizeRelativePath(trimmed)?.let { return it }

        val requestPath = extractUrlPath(trimmed)
        val basePath = extractUrlPath(baseUrl).trimEnd('/')
        if (basePath.isBlank() || requestPath.isBlank()) {
            return null
        }

        val relativePath = requestPath
            .removePrefix("$basePath/")
            .takeIf { it != requestPath }
            ?: return null

        return normalizeRelativePath(relativePath)
    }

    private fun extractUrlPath(value: String): String =
        try {
            if (value.startsWith("http://") || value.startsWith("https://")) {
                URI(value).path.orEmpty()
            } else {
                value
            }
        } catch (ex: Exception) {
            value
        }.trim().removePrefix("/")

    private fun normalizeRelativePath(input: String): String? {
        val normalized = input.trim().removePrefix("/").replace('\\', '/')
        if (normalized.isBlank()) {
            return null
        }
        val segments = normalized.split('/')
        if (segments.size != 2 || segments.any { it.isBlank() || it == "." || it == ".." }) {
            return null
        }
        return normalized
    }

    private fun sha256(content: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))
}
