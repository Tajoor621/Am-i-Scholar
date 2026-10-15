package com.amischolar.app.feature

import com.amischolar.app.data.ResearchNote
import com.amischolar.app.data.ResearchSource

/** Explainable offline research analysis; never substitutes for reading original studies. */
object ResearchAnalysis {
    data class EvidenceRow(val title: String, val sourceKey: String, val finding: String, val limitation: String)
    data class DraftDifference(val removed: List<String>, val added: List<String>)

    fun evidenceRows(notes: List<ResearchNote>): List<EvidenceRow> =
        notes.filter { it.kind == "EVIDENCE" }.map { note ->
            val fields = note.content.lines().mapNotNull { line ->
                val index = line.indexOf(':')
                if (index < 0) null else line.substring(0, index).trim().lowercase() to line.substring(index + 1).trim()
            }.toMap()
            EvidenceRow(
                title = note.title,
                sourceKey = fields["source citation / doi"].orEmpty(),
                finding = fields["main findings (quote or paraphrase with page)"].orEmpty(),
                limitation = fields["limitations and potential bias"].orEmpty()
            )
        }

    fun evidenceWarnings(rows: List<EvidenceRow>, sources: List<ResearchSource>): List<String> = buildList {
        if (rows.isEmpty()) add("No evidence records yet. Add a source-linked evidence record.")
        val keys = sources.map { it.doiOrUrl.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        rows.forEach { row ->
            if (row.sourceKey.isBlank()) add("${row.title}: missing source citation or DOI.")
            else if (row.sourceKey.lowercase() !in keys) add("${row.title}: source identifier does not match a saved source.")
            if (row.finding.isBlank()) add("${row.title}: main finding not documented.")
            if (row.limitation.isBlank()) add("${row.title}: limitations not documented.")
        }
    }

    fun outlineWarnings(outline: String, sources: List<ResearchSource>): List<String> = buildList {
        if (outline.isBlank()) add("Create an outline.")
        val lower = outline.lowercase()
        listOf("introduction", "method", "conclusion").forEach { section ->
            if (!lower.contains(section)) add("Consider adding a $section section.")
        }
        if (sources.isNotEmpty() && sources.none { source ->
                source.doiOrUrl.isNotBlank() && outline.contains(source.doiOrUrl, ignoreCase = true)
            }) add("No saved DOI or URL appears in the outline. Link key claims to sources.")
    }

    fun difference(previous: String, current: String): DraftDifference {
        val before = previous.lines().map(String::trim).filter(String::isNotEmpty)
        val after = current.lines().map(String::trim).filter(String::isNotEmpty)
        return DraftDifference(before.filterNot { it in after }, after.filterNot { it in before })
    }

    fun topicFeasibility(question: String, sources: List<ResearchSource>): List<String> = buildList {
        ResearchPlanner.questionChecks(question).filterNot { it.passed }.forEach { add(it.guidance) }
        if (sources.isEmpty()) add("No source metadata saved yet. Check whether relevant published research exists.")
        if (sources.none { it.verified }) add("No saved source has been checked against its original publication.")
        add("Confirm ethics, data access, budget, timeline and methodological feasibility with a supervisor.")
    }

    fun journalChecklist(journal: String, officialGuidelines: String): List<String> = buildList {
        if (journal.isBlank()) add("Choose and verify a target journal.")
        if (!officialGuidelines.startsWith("https://")) add("Record an official HTTPS author-guidelines link.")
        add("Check scope, indexing, fees, editorial board and publisher identity on official sites.")
        add("Verify reporting guideline, ethics, word limits, references and data availability requirements.")
    }
}
