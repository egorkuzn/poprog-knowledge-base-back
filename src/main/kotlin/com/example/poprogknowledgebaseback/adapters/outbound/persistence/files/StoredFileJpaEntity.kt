package com.example.poprogknowledgebaseback.adapters.outbound.persistence.files

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "stored_file")
class StoredFileJpaEntity(
    @Id
    var id: UUID? = null,

    @Column(nullable = false)
    var category: String,

    @Column(name = "original_filename", nullable = false)
    var originalFilename: String,

    @Column(name = "stored_filename", nullable = false)
    var storedFilename: String,

    @Column(name = "content_type", nullable = false)
    var contentType: String,

    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long,

    @Column(nullable = false)
    var sha256: String,

    @Column(nullable = false, columnDefinition = "bytea")
    var content: ByteArray,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)
