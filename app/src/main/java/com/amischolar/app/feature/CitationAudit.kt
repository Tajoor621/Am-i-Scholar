package com.amischolar.app.feature

import com.amischolar.app.data.ResearchSource

/** Basic citation quality audit, not a substitute for a CSL processor or a style manual. */
object CitationAudit {
    data class Issue(val sourceTitle: String, val issue: String)

    fun inspect(sources: List<ResearchSource>): List<Issue> {
        val issues = mutableListOf<Issue>()
        val seen = mutableSetOf<String>()
        sources.forEach { source ->
            SourceQuality.missingFields(source).forEach { field ->
                issues += Issue(source.title, "Missing $field")
            }
            val key = source.doiOrUrl.trim().lowercase().ifBlank { source.title.trim().lowercase() }
            if (!seen.add(key)) issues += Issue(source.title, "Potential duplicate source")
            if (!source.verified) issues += Issue(source.title, "Metadata not checked against original")
            val year = source.year.toIntOrNull()
            if (source.year.isNotBlank() && (year == null || year !in 1500..2100)) {
                issues += Issue(source.title, "Invalid publication year")
            }
        }
        return issues
    }

    fun report(sources: List<ResearchSource>): String = buildString {
        appendLine("CITATION METADATA AUDIT")
        appendLine("Not a guarantee of bibliographic accuracy or style compliance.")
        val issues = inspect(sources)
        if (issues.isEmpty()) appendLine("No basic metadata issues detected; manual citation review is still required.")
        issues.forEach { appendLine(it.sourceTitle+": "+it.issue) }
    }
}
