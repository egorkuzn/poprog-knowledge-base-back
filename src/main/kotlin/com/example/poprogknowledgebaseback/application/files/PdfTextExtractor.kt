package com.example.poprogknowledgebaseback.application.files

import java.nio.file.Path
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Service

@Service
class PdfTextExtractor {

    companion object {
        private const val MAX_TEXT_LENGTH = 20000
    }

    fun extractText(path: Path): String? {
        return try {
            PDDocument.load(path.toFile()).use { document ->
                val rawText = PDFTextStripper().getText(document)
                rawText
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.take(MAX_TEXT_LENGTH)
            }
        } catch (ex: Exception) {
            null
        }
    }
}
