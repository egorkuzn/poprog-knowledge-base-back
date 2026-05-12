package com.example.poprogknowledgebaseback.application.metrics

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.metrics.ProductMetricEventJpaEntity
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.metrics.SpringDataProductMetricEventRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.OffsetDateTime
import java.time.temporal.TemporalAdjusters
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

data class ProductMetricEventCommand(
    val eventType: String,
    val route: String,
    val referrer: String?,
    val sessionId: String,
    val userKey: String?,
    val timestampClient: OffsetDateTime,
    val payload: Map<String, Any?>
)

data class DailyActiveUsersPoint(
    val day: String,
    val uniqueUsers: Long
)

data class WeeklyActiveUsersPoint(
    val weekStart: String,
    val uniqueUsers: Long
)

data class DauWauReportResult(
    val uniqueUserDefinition: String,
    val daily: List<DailyActiveUsersPoint>,
    val weekly: List<WeeklyActiveUsersPoint>
)

data class SearchSuccessDayResult(
    val day: String,
    val submittedCount: Long,
    val successfulCount: Long,
    val searchSuccessRatePercent: String
)

data class SearchSuccessReportResult(
    val daily: List<SearchSuccessDayResult>
)

data class SourceTypeCtrResult(
    val sourceType: String,
    val impressions: Long,
    val clicks: Long,
    val clickThroughRatePercent: String
)

data class SourceTypeCtrReportResult(
    val items: List<SourceTypeCtrResult>
)

@Service
class ProductMetricsService(
    private val repository: SpringDataProductMetricEventRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) {

    @Transactional
    fun saveEvent(command: ProductMetricEventCommand): Long {
        val normalizedUserKey = command.userKey?.trim()?.takeIf { it.isNotBlank() } ?: command.sessionId.trim()
        val entity = ProductMetricEventJpaEntity(
            eventType = command.eventType.trim(),
            route = command.route.trim(),
            referrer = command.referrer?.trim()?.takeIf { it.isNotBlank() },
            sessionId = command.sessionId.trim(),
            userKey = normalizedUserKey,
            timestampClient = command.timestampClient,
            timestampServer = OffsetDateTime.now(clock),
            payloadJson = objectMapper.writeValueAsString(command.payload)
        )
        return repository.save(entity).id ?: 0L
    }

    @Transactional(readOnly = true)
    fun getDauWauReport(from: OffsetDateTime?, to: OffsetDateTime?): DauWauReportResult {
        val events = findInRange(from, to)
        val daily = events
            .groupBy { it.timestampClient.toLocalDate() }
            .toSortedMap()
            .map { (day, values) ->
                DailyActiveUsersPoint(
                    day = day.toString(),
                    uniqueUsers = values.map { it.userKey }.toSet().size.toLong()
                )
            }
        val weekly = events
            .groupBy { it.timestampClient.toLocalDate().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)) }
            .toSortedMap()
            .map { (weekStart, values) ->
                WeeklyActiveUsersPoint(
                    weekStart = weekStart.toString(),
                    uniqueUsers = values.map { it.userKey }.toSet().size.toLong()
                )
            }

        return DauWauReportResult(
            uniqueUserDefinition = "subject авторизованного пользователя или sessionId анонимной сессии",
            daily = daily,
            weekly = weekly
        )
    }

    @Transactional(readOnly = true)
    fun getSearchSuccessReport(from: OffsetDateTime?, to: OffsetDateTime?): SearchSuccessReportResult {
        val events = findInRange(from, to)
        val successfulQueries = events
            .filter { it.eventType == "search_result_opened" || it.eventType == "search_result_click" }
            .mapNotNull { event ->
                val payload = payloadNode(event)
                val queryId = payload["queryId"]?.asText()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                "${event.sessionId}::$queryId"
            }
            .toSet()

        val submittedByDay = events
            .filter { it.eventType == "search_query_submitted" || it.eventType == "search_submitted" }
            .groupBy { it.timestampClient.toLocalDate() }
            .toSortedMap()

        val daily = submittedByDay.map { (day, submittedEvents) ->
            val successfulCount = submittedEvents.count { event ->
                val queryId = payloadNode(event)["queryId"]?.asText()?.takeIf { it.isNotBlank() } ?: return@count false
                successfulQueries.contains("${event.sessionId}::$queryId")
            }.toLong()
            val submittedCount = submittedEvents.size.toLong()

            SearchSuccessDayResult(
                day = day.toString(),
                submittedCount = submittedCount,
                successfulCount = successfulCount,
                searchSuccessRatePercent = percentage(successfulCount, submittedCount)
            )
        }

        return SearchSuccessReportResult(daily = daily)
    }

    @Transactional(readOnly = true)
    fun getCtrReport(from: OffsetDateTime?, to: OffsetDateTime?): SourceTypeCtrReportResult {
        val events = findInRange(from, to)
        val impressions = mutableMapOf<String, Long>()
        val clicks = mutableMapOf<String, Long>()

        events.filter { it.eventType == "search_result_shown" }.forEach { event ->
            val payload = payloadNode(event)
            mergeCount(impressions, "publication", payload["publicationCount"]?.asLong() ?: 0L)
            mergeCount(impressions, "student-work", payload["studentWorkCount"]?.asLong() ?: 0L)
        }

        events.filter { it.eventType == "search_result_click" }.forEach { event ->
            val sourceType = payloadNode(event)["sourceType"]?.asText()?.trim()?.lowercase()
            if (!sourceType.isNullOrBlank()) {
                mergeCount(clicks, sourceType, 1L)
            }
        }

        val sourceTypes = (impressions.keys + clicks.keys).toSortedSet()
        val items = sourceTypes.map { sourceType ->
            val impressionCount = impressions[sourceType] ?: 0L
            val clickCount = clicks[sourceType] ?: 0L
            SourceTypeCtrResult(
                sourceType = sourceType,
                impressions = impressionCount,
                clicks = clickCount,
                clickThroughRatePercent = percentage(clickCount, impressionCount)
            )
        }

        return SourceTypeCtrReportResult(items = items)
    }

    private fun findInRange(from: OffsetDateTime?, to: OffsetDateTime?): List<ProductMetricEventJpaEntity> {
        if (from == null && to == null) {
            return repository.findAll().sortedBy { it.timestampServer }
        }
        val normalizedTo = to ?: OffsetDateTime.now(clock)
        val normalizedFrom = from ?: normalizedTo.minusDays(30)
        return repository.findAll()
            .sortedBy { it.timestampServer }
            .filter { event ->
                !event.timestampClient.isBefore(normalizedFrom) && !event.timestampClient.isAfter(normalizedTo)
            }
    }

    private fun payloadNode(event: ProductMetricEventJpaEntity): JsonNode =
        objectMapper.readTree(event.payloadJson)

    private fun mergeCount(target: MutableMap<String, Long>, key: String, delta: Long) {
        if (delta <= 0L) {
            return
        }
        target[key] = (target[key] ?: 0L) + delta
    }

    private fun percentage(numerator: Long, denominator: Long): String {
        if (denominator <= 0L) {
            return "0.00"
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
            .toPlainString()
    }
}
