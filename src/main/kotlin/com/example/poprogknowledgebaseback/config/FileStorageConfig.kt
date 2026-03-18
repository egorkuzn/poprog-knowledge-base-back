package com.example.poprogknowledgebaseback.config

import java.nio.file.Path
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class FileStorageConfig(
    @Value("\${app.files.storage-dir}") private val storageDir: String,
    @Value("\${app.files.base-url}") private val baseUrl: String
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val normalizedBaseUrl = "${baseUrl.trimEnd('/')}/**"
        val location = "file:${Path.of(storageDir).toAbsolutePath().normalize()}/"
        registry.addResourceHandler(normalizedBaseUrl).addResourceLocations(location)
    }
}
