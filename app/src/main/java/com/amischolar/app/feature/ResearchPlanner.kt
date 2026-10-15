package com.amischolar.app.feature

/** Offline, deterministic research-planning feedback. No hosted inference or generated claims. */
object ResearchPlanner {
    data class Check(val name: String, val passed: Boolean, val guidance: String)

    fun questionChecks(question: String): List<Check> {
        val q = question.trim()
        return listOf(
            Check("Specific research question", q.length >= 25,
                "Describe a focused population, material, setting or phenomenon and the relationship you intend to study."),
            Check("Question format", q.endsWith("?"),
                "Write the research objective as a question to make the investigation testable."),
            Check("Scope", q.split(Regex("\\s+")).size in 8..45,
                "Use a concise question with enough context to define the scope."),
            Check("Operational terms", !Regex("\\b(best|perfect|always|everything)\\b", RegexOption.IGNORE_CASE).containsMatchIn(q),
                "Replace absolute or subjective words with observable outcomes.")
        )
    }

    fun evidenceTemplate(): String = """
        Research question:
        Source citation / DOI:
        Study design / method:
        Population or dataset:
        Main findings (quote or paraphrase with page):
        Limitations and potential bias:
        Relevance to research question:
        Verification status (checked original / metadata only):
    """.trimIndent()

    fun responseTemplate(): String = """
        Reviewer comment:
        Response (respectfully address the evidence):
        Manuscript change and section/page:
        Supporting source(s):
        If not changed, evidence-based justification:
        Status (open / addressed / needs verification):
    """.trimIndent()
}
