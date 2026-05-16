package com.example.poprogknowledgebaseback.application.files

data class StoredFile(
    val fileName: String,
    val url: String
)

data class StoredFileContent(
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val sha256: String,
    val content: ByteArray
)
