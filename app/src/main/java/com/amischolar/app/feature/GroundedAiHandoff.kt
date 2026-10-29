package com.amischolar.app.feature

/** Bounded manual handoff builder; it never makes a network request. */
object GroundedAiHandoff {
    const val MAX_SOURCES = 5
    const val MAX_PROMPT_CHARS = 8000
    const val MAX_TASK_CHARS = 600

    data class SourceContext(
        val id: String,
        val title: String,
        val identifier: String,
        val verified: Boolean,
        val evidence: String = ""
    )
    data class Prompt(
        val text: String,
        val sourceIds: List<String>,
        val truncated: Boolean,
        val omittedSources: Int
    )

    fun buildPrompt(task: String, selected: List<SourceContext>): Prompt {
        val chosen = selected.take(MAX_SOURCES)
        var truncated = selected.size > MAX_SOURCES
        val cleanTask = task.replace("\u0000", "").trim()
        val taskText = cleanTask.take(MAX_TASK_CHARS)
        truncated = truncated || cleanTask.length > MAX_TASK_CHARS
        val prefix = """You are assisting with academic research. Use only the explicitly selected source records below. Do not invent facts, findings, quotations, citations, DOIs, or bibliographic details. Separate source-supported statements from suggestions. Mark claims not supported by these records as unsupported and state uncertainty. Preserve each source label [S#] when citing it.

Task:
${taskText.ifBlank { "Summarize the selected evidence and identify limitations." }}

Selected source records:
"""
        val suffix = "\n\nReturn a concise synthesis, a limitations section, and a list of unsupported questions. Do not claim that you verified the publications."
        var remaining = (MAX_PROMPT_CHARS - prefix.length - suffix.length).coerceAtLeast(0)
        val blocks = mutableListOf<String>()
        chosen.forEachIndexed { index, source ->
            val title = source.title.replace("\u0000", "").trim().take(240)
            val identifier = source.identifier.replace("\u0000", "").trim().take(320)
            val evidence = source.evidence.replace("\u0000", "").trim().take(1200)
            if (source.title.trim().length > 240 || source.identifier.trim().length > 320 || source.evidence.trim().length > 1200) truncated = true
            val header = "[S${index + 1}] Local source ID: ${source.id}\nTitle: ${title.ifBlank { "Untitled record" }}\nDOI/URL: ${identifier.ifBlank { "[none recorded; do not invent one]" }}\nVerification: ${if (source.verified) "researcher marked verified" else "UNVERIFIED"}.\n"
            val evidencePrefix = "Selected evidence notes: "
            if (header.length > remaining) {
                truncated = true
            } else {
                var block = header
                remaining -= header.length
                if (evidence.isNotEmpty() && remaining > evidencePrefix.length + 1) {
                    val body = evidence.take((remaining - evidencePrefix.length - 1).coerceAtLeast(0))
                    if (body.length < evidence.length) truncated = true
                    block += evidencePrefix + body + "\n"
                    remaining -= evidencePrefix.length + body.length + 1
                } else if (evidence.isNotEmpty()) {
                    truncated = true
                }
                blocks += block
            }
        }
        var text = prefix + blocks.joinToString("\n") + suffix
        if (text.length > MAX_PROMPT_CHARS) {
            text = text.take(MAX_PROMPT_CHARS - suffix.length) + suffix
            truncated = true
        }
        return Prompt(text, chosen.map { it.id }.filter { it.isNotBlank() }, truncated, (chosen.size - blocks.size).coerceAtLeast(0))
    }

    fun canCopy(consent: Boolean, prompt: Prompt): Boolean =
        consent && prompt.sourceIds.isNotEmpty() && prompt.text.length <= MAX_PROMPT_CHARS

    fun disclosure(provider: String, task: String, sourceIds: List<String>, reviewed: Boolean): String {
        val name = provider.trim().ifBlank { "an external AI service" }
        val work = task.trim().ifBlank { "research drafting" }
        val sources = sourceIds.filter(String::isNotBlank)
        return "AI assistance disclosure: I used $name to assist with $work. " +
            (if (sources.isNotEmpty()) "The handoff included selected local source record IDs: ${sources.joinToString(", ")}. " else "") +
            (if (reviewed) "I reviewed the returned text and checked its claims and citations against original sources." else "I have not yet verified the returned text or its citations.") +
            " The app did not verify the AI output."
    }
}
