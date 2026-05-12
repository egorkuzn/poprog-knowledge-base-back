package com.example.poprogknowledgebaseback.adapters.outbound.persistence.files

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "stored_file")
class StoredFileJpaEntity(
    @Id
    var id: UUID,
    @Column(name = "category", nullable = false)
    var category: String,
    @Column(name = "original_filename", nullable = false)
    var originalFilename: String,
    @Column(name = "stored_filename", nullable = false)
    var storedFilename: String,
    @Column(name = "content_type", nullable = false)
    var contentType: String,
    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long,
    @Column(name = "sha256", nullable = false)
    var sha256: String,
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "content", nullable = false, columnDefinition = "bytea")
    var content: ByteArray,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime
)
