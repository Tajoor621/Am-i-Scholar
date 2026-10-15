package com.amischolar.app.feature

import com.amischolar.app.data.ResearchNote
import com.amischolar.app.data.ResearchProject
import com.amischolar.app.data.ResearchSource

/** Transparent checklist; not an actual journal referee or an AI-generated peer review. */
object SubmissionAudit {
    data class Check(val area: String, val passed: Boolean, val guidance: String)

    fun inspect(project: ResearchProject?, notes: List<ResearchNote>, sources: List<ResearchSource>): List<Check> {
        val drafts = notes.filter { it.kind == "DRAFT" }
        val draftText = drafts.joinToString("\n") { it.content }
        return listOf(
            Check("Research question", !project?.researchQuestion.isNullOrBlank(), "State a clear, answerable research question."),
            Check("Manuscript draft", draftText.length >= 500, "Develop your own substantive manuscript draft."),
            Check("Outline", notes.any { it.kind == "OUTLINE" }, "Create an outline linking arguments to evidence."),
            Check("References", sources.isNotEmpty(), "Collect primary references and verify them."),
            Check("Complete metadata", sources.isNotEmpty() && sources.all { SourceQuality.missingFields(it).isEmpty() }, "Fill all required reference fields."),
            Check("Source checking", sources.isNotEmpty() && sources.all { it.verified }, "Check every reference against its original publication."),
            Check("Evidence notes", sources.isNotEmpty() && sources.all { it.evidenceNotes.isNotBlank() }, "Record what each source supports and its limitations."),
            Check("Claims and language", draftText.isNotBlank() && !Regex("\\b(proves|always|never|everyone)\\b", RegexOption.IGNORE_CASE).containsMatchIn(draftText), "Review absolute claims and confirm evidence supports them.")
        )
    }

    fun report(project: ResearchProject?, notes: List<ResearchNote>, sources: List<ResearchSource>): String =
        buildString {
            appendLine("SUBMISSION READINESS CHECKLIST")
            appendLine("Project: "+(project?.title ?: "Untitled"))
            appendLine("Automated heuristic checks only — NOT peer review, acceptance advice or plagiarism certification.")
            appendLine()
            inspect(project,notes,sources).forEach { check ->
                appendLine((if(check.passed) "[PASS] " else "[REVIEW] ")+check.area)
                appendLine("  "+check.guidance)
            }
            appendLine()
            appendLine("Before submission: check journal scope, author guidelines, ethics approval, disclosure policies, figures, data availability, authorship and conflicts of interest.")
        }
}
