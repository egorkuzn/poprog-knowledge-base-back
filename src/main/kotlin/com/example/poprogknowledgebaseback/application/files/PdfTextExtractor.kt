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
                normalize(PDFTextStripper().getText(document))
            }
        } catch (ex: Exception) {
            null
        }
    }

    fun extractText(content: ByteArray): String? {
        return try {
            PDDocument.load(content).use { document ->
                normalize(PDFTextStripper().getText(document))
            }
        } catch (ex: Exception) {
            null
        }
    }

    private fun normalize(rawText: String): String? =
        rawText
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf { it.isNotBlank() }
            ?.take(MAX_TEXT_LENGTH)
}
