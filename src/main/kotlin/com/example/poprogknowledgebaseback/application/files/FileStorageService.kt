package com.example.poprogknowledgebaseback.application.files

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.files.SpringDataStoredFileRepository
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.files.StoredFileJpaEntity
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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

        val normalizedCategory = category.trim().lowercase()
        val rootPath = Path.of(storageDir).toAbsolutePath().normalize()
        val categoryPath = rootPath.resolve(normalizedCategory).normalize()
        Files.createDirectories(categoryPath)

        val originalName = file.originalFilename?.substringAfterLast('/')?.substringAfterLast('\\').orEmpty()
        val extension = originalName.substringAfterLast('.', missingDelimiterValue = "").lowercase()

        val bytes = file.bytes
        val sha256 = sha256Hex(bytes)

        // Deduplicate by (category, sha256). This makes DB/file migration idempotent and reliable.
        storedFileRepository.findByCategoryAndSha256(normalizedCategory, sha256)?.let { existing ->
            ensureFileOnDisk(categoryPath, existing.storedFilename, bytes)
            return StoredFile(
                fileName = existing.storedFilename,
                url = "${baseUrl.trimEnd('/')}/$normalizedCategory/${existing.storedFilename}"
            )
        }

        val generatedName = buildString {
            append(UUID.randomUUID())
            if (extension.isNotBlank()) {
                append(".")
                append(extension)
            }
        }

        val targetPath = categoryPath.resolve(generatedName).normalize()
        ensureFileOnDisk(categoryPath, generatedName, bytes)

        val id = runCatching { UUID.fromString(generatedName.substringBefore('.')) }.getOrElse { UUID.randomUUID() }
        storedFileRepository.save(
            StoredFileJpaEntity(
                id = id,
                category = normalizedCategory,
                originalFilename = originalName.ifBlank { generatedName },
                storedFilename = generatedName,
                contentType = file.contentType ?: "application/octet-stream",
                sizeBytes = bytes.size.toLong(),
                sha256 = sha256,
                content = bytes
            )
        )

        return StoredFile(
            fileName = generatedName,
            url = "${baseUrl.trimEnd('/')}/$normalizedCategory/$generatedName"
        )
    }

    private fun ensureFileOnDisk(categoryPath: Path, storedFilename: String, bytes: ByteArray) {
        val targetPath = categoryPath.resolve(storedFilename).normalize()
        if (Files.exists(targetPath) && Files.isRegularFile(targetPath)) {
            return
        }
        Files.write(targetPath, bytes)
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}
