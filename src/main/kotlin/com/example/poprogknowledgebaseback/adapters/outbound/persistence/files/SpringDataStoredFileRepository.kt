package com.example.poprogknowledgebaseback.adapters.outbound.persistence.files

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataStoredFileRepository : JpaRepository<StoredFileJpaEntity, UUID> {
    fun findFirstByCategoryAndSha256(category: String, sha256: String): StoredFileJpaEntity?
}
