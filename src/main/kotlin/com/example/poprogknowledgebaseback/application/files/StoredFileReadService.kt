package com.example.poprogknowledgebaseback.application.files

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.files.SpringDataStoredFileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StoredFileReadService(
    private val repository: SpringDataStoredFileRepository
) {
    @Transactional(readOnly = true)
    fun findContent(category: String, storedFilename: String): StoredFileContent? {
        val entity = repository.findByCategoryAndStoredFilename(category, storedFilename) ?: return null
        return StoredFileContent(
            fileName = entity.originalFilename,
            contentType = entity.contentType,
            sizeBytes = entity.sizeBytes,
            sha256 = entity.sha256,
            content = entity.content
        )
    }
}
