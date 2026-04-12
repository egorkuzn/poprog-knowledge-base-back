package com.example.poprogknowledgebaseback.application.market

import org.springframework.stereotype.Service

data class MarketAppResult(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val tags: List<String>,
    val platform: String,
    val version: String,
    val priceModel: String,
    val downloadUrl: String?
)

data class MarketSearchResult(
    val query: String,
    val category: String?,
    val total: Int,
    val items: List<MarketAppResult>
)

@Service
class MarketCatalogService {

    fun searchApps(query: String?, category: String?, limit: Int = 24): MarketSearchResult {
        val normalizedQuery = query?.trim().orEmpty()
        val normalizedCategory = category?.trim()?.takeIf { it.isNotBlank() }

        val filtered = seededApps
            .asSequence()
            .filter { app ->
                normalizedCategory == null || app.category.equals(normalizedCategory, ignoreCase = true)
            }
            .filter { app ->
                if (normalizedQuery.isBlank()) {
                    true
                } else {
                    val haystack = buildString {
                        append(app.title).append(' ')
                        append(app.summary).append(' ')
                        append(app.category).append(' ')
                        append(app.tags.joinToString(" "))
                    }.lowercase()

                    haystack.contains(normalizedQuery.lowercase())
                }
            }
            .toList()

        val safeLimit = limit.coerceIn(1, 100)
        val items = filtered.take(safeLimit)

        return MarketSearchResult(
            query = normalizedQuery,
            category = normalizedCategory,
            total = filtered.size,
            items = items
        )
    }

    fun getCategories(): List<String> = seededApps
        .map { it.category }
        .distinct()
        .sorted()

    companion object {
        private val seededApps = listOf(
            MarketAppResult(
                id = "plc-emu-lab",
                title = "PLC Emulator Lab",
                summary = "Локальный эмулятор ПЛК для тестирования программ и учебных стендов.",
                category = "Симуляторы",
                tags = listOf("plc", "emulator", "testing"),
                platform = "Windows / Linux",
                version = "0.9.4",
                priceModel = "FREE",
                downloadUrl = null
            ),
            MarketAppResult(
                id = "trace-lens",
                title = "Trace Lens",
                summary = "Утилита для анализа трасс выполнения и поиска аномалий в циклах управления.",
                category = "Аналитика",
                tags = listOf("trace", "diagnostics", "analysis"),
                platform = "Windows / macOS / Linux",
                version = "1.2.1",
                priceModel = "FREE",
                downloadUrl = null
            ),
            MarketAppResult(
                id = "modbus-sniffer",
                title = "Modbus Sniffer",
                summary = "Перехват и визуализация Modbus RTU/TCP трафика для наладки промышленных сетей.",
                category = "Сети и протоколы",
                tags = listOf("modbus", "network", "monitoring"),
                platform = "Windows / Linux",
                version = "2.0.0",
                priceModel = "FREE",
                downloadUrl = null
            ),
            MarketAppResult(
                id = "safety-checker",
                title = "Safety Checker",
                summary = "Проверка правил безопасности и базовых ограничений для управляющих программ.",
                category = "Верификация",
                tags = listOf("safety", "rules", "verification"),
                platform = "Linux",
                version = "0.6.8",
                priceModel = "FREE",
                downloadUrl = null
            ),
            MarketAppResult(
                id = "signal-replay",
                title = "Signal Replay",
                summary = "Воспроизведение сигналов датчиков и сценариев аварий для стендового теста.",
                category = "Тестирование",
                tags = listOf("signals", "replay", "qa"),
                platform = "Windows / Linux",
                version = "1.0.3",
                priceModel = "FREE",
                downloadUrl = null
            ),
            MarketAppResult(
                id = "iot-bridge-cli",
                title = "IoT Bridge CLI",
                summary = "CLI-утилита для публикации метрик ПЛК в MQTT/HTTP каналы.",
                category = "Интеграции",
                tags = listOf("iot", "mqtt", "cli"),
                platform = "Cross-platform",
                version = "0.5.7",
                priceModel = "FREE",
                downloadUrl = null
            )
        )
    }
}
