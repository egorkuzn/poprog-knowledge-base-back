package com.example.poprogknowledgebaseback.application.importer

object Lab19MaterialClassifier {
    private val studentWorkKeywords = listOf(
        "вкр",
        "выпускная квалификационная работа",
        "диплом",
        "бакалавр",
        "магистр",
        "магистерск",
        "диссертаци",
        "автореферат"
    )

    private val scientificPathHints = listOf(
        "/meaningful-results"
    )

    private val excludedPathFragments = listOf(
        "institute-in-mass-media",
        "profsoyuz",
        "konkursy"
    )

    const val SCIENTIFIC_PUBLICATION = "SCIENTIFIC_PUBLICATION"
    const val STUDENT_WORK = "STUDENT_WORK"
    const val NEWS = "NEWS"
    const val IGNORE = "IGNORE"

    fun classify(sourcePage: String, title: String, url: String): String {
        val page = sourcePage.lowercase()
        val lowerTitle = title.lowercase()
        val lowerUrl = url.lowercase()

        if (excludedPathFragments.any { page.contains(it) || lowerUrl.contains(it) }) {
            return IGNORE
        }
        if (studentWorkKeywords.any { lowerTitle.contains(it) }) {
            return STUDENT_WORK
        }
        if (scientificPathHints.any { page.contains(it) }) {
            return SCIENTIFIC_PUBLICATION
        }
        return NEWS
    }
}

