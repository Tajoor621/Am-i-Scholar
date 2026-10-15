package com.amischolar.app.feature

/** Transparent local style checks; no generative AI, detector evasion or external data transfer. */
object WritingCoach {
    data class Finding(val label: String, val advice: String)

    fun review(text: String): List<Finding> {
        if (text.isBlank()) return listOf(Finding("Empty draft", "Write your own evidence-based paragraph to review."))
        val findings = mutableListOf<Finding>()
        val words = Regex("\\b[\\p{L}]+(?:'[\\p{L}]+)?\\b").findAll(text).count()
        val sentences = text.split(Regex("[.!?]+")).map { it.trim() }.filter { it.isNotEmpty() }
        if (sentences.isNotEmpty() && words.toDouble() / sentences.size > 30) {
            findings += Finding("Long sentences", "Consider splitting long sentences to clarify the argument.")
        }
        if (Regex("\\b(very|really|basically|obviously|clearly)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            findings += Finding("Precision", "Replace vague intensifiers with precise, evidence-supported descriptions.")
        }
        if (Regex("\\b(proves|always|never|everyone|no one)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            findings += Finding("Overclaiming", "Check whether the cited evidence justifies absolute language.")
        }
        if (!Regex("\\([A-Z][^)]*\\d{4}[^)]*\\)|\\[\\d+\\]").containsMatchIn(text)) {
            findings += Finding("Citation reminder", "Check whether factual claims need citations; absence of this pattern does not prove missing attribution.")
        }
        if (findings.isEmpty()) findings += Finding("Manual review", "Review argument structure, source attribution and journal requirements yourself.")
        return findings
    }
}
