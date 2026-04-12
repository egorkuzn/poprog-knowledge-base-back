package com.example.poprogknowledgebaseback.application.feedback

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.feedback.SiteFeedbackJpaEntity
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.feedback.SpringDataSiteFeedbackRepository
import java.time.Clock
import java.time.OffsetDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class CreateSiteFeedbackCommand(
    val helpful: Boolean,
    val userName: String,
    val userEmail: String,
    val userAgent: String?,
    val ipAddress: String?,
    val source: String?,
    val comment: String?
)

@Service
class SiteFeedbackService(
    private val repository: SpringDataSiteFeedbackRepository,
    private val clock: Clock
) {

    @Transactional
    fun create(command: CreateSiteFeedbackCommand): Long {
        val saved = repository.save(
            SiteFeedbackJpaEntity(
                helpful = command.helpful,
                userName = command.userName.trim(),
                userEmail = command.userEmail.trim(),
                userAgent = command.userAgent?.trim()?.takeIf { it.isNotBlank() }?.take(512),
                ipAddress = command.ipAddress?.trim()?.takeIf { it.isNotBlank() }?.take(64),
                source = command.source?.trim()?.takeIf { it.isNotBlank() },
                comment = command.comment?.trim()?.takeIf { it.isNotBlank() },
                createdAt = OffsetDateTime.now(clock)
            )
        )
        return saved.id ?: error("Site feedback id was not generated")
    }
}
