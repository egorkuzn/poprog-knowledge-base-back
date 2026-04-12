package com.example.poprogknowledgebaseback.adapters.outbound.persistence.feedback

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataSiteFeedbackRepository : JpaRepository<SiteFeedbackJpaEntity, Long>
