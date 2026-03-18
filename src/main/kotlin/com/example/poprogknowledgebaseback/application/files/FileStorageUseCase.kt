package com.example.poprogknowledgebaseback.application.files

import org.springframework.web.multipart.MultipartFile

interface FileStorageUseCase {
    fun store(category: String, file: MultipartFile): StoredFile
}
