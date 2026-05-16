package com.example.poprogknowledgebaseback.application.files

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.files.SpringDataStoredFileRepository
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.files.StoredFileJpaEntity
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class FileStorageService(
    @Value("\${app.files.storage-dir}") private val storageDir: String,
    @Value("\${app.files.base-url}") private val baseUrl: String,
    private val storedFileRepository: SpringDataStoredFileRepository
) : FileStorageUseCase {

    @Transactional
    override fun store(category: String, file: MultipartFile): StoredFile {
        require(!file.isEmpty) { "Uploaded file is empty" }

        val normalizedCategory = normalizeCategory(category)
        val originalName = sanitizeFileName(file.originalFilename)
        val extension = originalName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        val content = file.bytes
        val sha256 = sha256Hex(content)
        val categoryPath = storageRoot().resolve(normalizedCategory).normalize()
        Files.createDirectories(categoryPath)

        storedFileRepository.findByCategoryAndSha256(normalizedCategory, sha256)?.let { existing ->
            ensureFileOnDisk(categoryPath, existing.storedFilename, existing.content)
            return existing.toStoredFile()
        }

        val generatedName = buildString {
            append(UUID.randomUUID())
            if (extension.isNotBlank()) {
                append('.')
                append(extension)
            }
        }
        ensureFileOnDisk(categoryPath, generatedName, content)

        val entity = StoredFileJpaEntity(
            id = UUID.fromString(generatedName.substringBefore('.')),
            category = normalizedCategory,
            originalFilename = originalName.ifBlank { generatedName },
            storedFilename = generatedName,
            contentType = file.contentType?.trim().takeUnless { it.isNullOrBlank() } ?: "application/octet-stream",
            sizeBytes = content.size.toLong(),
            sha256 = sha256,
            content = content
        )

        val saved = try {
            storedFileRepository.save(entity)
        } catch (ex: DataIntegrityViolationException) {
            val existing = storedFileRepository.findByCategoryAndSha256(normalizedCategory, sha256) ?: throw ex
            ensureFileOnDisk(categoryPath, existing.storedFilename, existing.content)
            existing
        }

        return saved.toStoredFile()
    }

    @Transactional(readOnly = true)
    override fun load(relativePath: String): StoredFileContent? {
        val normalized = normalizeRelativePath(relativePath) ?: return null
        val category = normalized.substringBefore('/')
        val storedFilename = normalized.substringAfter('/')

        storedFileRepository.findByCategoryAndStoredFilename(category, storedFilename)?.let { entity ->
            return entity.toStoredFileContent()
        }

        val targetPath = storageRoot().resolve(normalized).normalize()
        if (!targetPath.startsWith(storageRoot()) || !Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            return null
        }

        val content = Files.readAllBytes(targetPath)
        return StoredFileContent(
            fileName = targetPath.fileName.toString(),
            contentType = Files.probeContentType(targetPath) ?: "application/octet-stream",
            sizeBytes = content.size.toLong(),
            sha256 = sha256Hex(content),
            content = content
        )
    }

    @Transactional(readOnly = true)
    override fun loadFromUrl(url: String): StoredFileContent? {
        val relativePath = extractRelativePath(url) ?: return null
        return load(relativePath)
    }

    private fun StoredFileJpaEntity.toStoredFile(): StoredFile = StoredFile(
        fileName = storedFilename,
        url = "${baseUrl.trimEnd('/')}/$category/$storedFilename"
    )

    private fun StoredFileJpaEntity.toStoredFileContent(): StoredFileContent = StoredFileContent(
        fileName = originalFilename,
        contentType = contentType,
        sizeBytes = sizeBytes,
        sha256 = sha256,
        content = content
    )

    private fun ensureFileOnDisk(categoryPath: Path, storedFilename: String, bytes: ByteArray) {
        Files.createDirectories(categoryPath)
        val targetPath = categoryPath.resolve(storedFilename).normalize()
        if (Files.exists(targetPath) && Files.isRegularFile(targetPath)) {
            return
        }
        Files.write(targetPath, bytes)
    }

    private fun storageRoot(): Path = Path.of(storageDir).toAbsolutePath().normalize()

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

        val relativePath = requestPath.removePrefix("$basePath/").takeIf { it != requestPath } ?: return null
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

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}
