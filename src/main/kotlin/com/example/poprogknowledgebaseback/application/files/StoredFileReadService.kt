package com.example.poprogknowledgebaseback.application.files

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.files.SpringDataStoredFileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class StoredFileContent(
    val storedFilename: String,
    val contentType: String,
    val bytes: ByteArray
)

@Service
class StoredFileReadService(
    private val repository: SpringDataStoredFileRepository
) {
    @Transactional(readOnly = true)
    fun findContent(category: String, storedFilename: String): StoredFileContent? {
        val entity = repository.findByCategoryAndStoredFilename(category, storedFilename) ?: return null
        return StoredFileContent(
            storedFilename = entity.storedFilename,
            contentType = entity.contentType,
            bytes = entity.content
        )
    }
}

