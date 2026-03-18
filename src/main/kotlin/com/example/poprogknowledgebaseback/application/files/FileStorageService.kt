package com.example.poprogknowledgebaseback.application.files

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class FileStorageService(
    @Value("\${app.files.storage-dir}") private val storageDir: String,
    @Value("\${app.files.base-url}") private val baseUrl: String
) : FileStorageUseCase {

    override fun store(category: String, file: MultipartFile): StoredFile {
        require(!file.isEmpty) { "Uploaded file is empty" }

        val normalizedCategory = category.trim().lowercase()
        val rootPath = Path.of(storageDir).toAbsolutePath().normalize()
        val categoryPath = rootPath.resolve(normalizedCategory).normalize()
        Files.createDirectories(categoryPath)

        val originalName = file.originalFilename?.substringAfterLast('/')?.substringAfterLast('\\').orEmpty()
        val extension = originalName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        val generatedName = buildString {
            append(UUID.randomUUID())
            if (extension.isNotBlank()) {
                append(".")
                append(extension)
            }
        }

        val targetPath = categoryPath.resolve(generatedName).normalize()
        file.inputStream.use { inputStream ->
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        return StoredFile(
            fileName = generatedName,
            url = "${baseUrl.trimEnd('/')}/$normalizedCategory/$generatedName"
        )
    }
}
