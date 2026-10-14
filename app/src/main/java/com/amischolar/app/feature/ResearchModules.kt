package com.amischolar.app.feature

data class ResearchModule(val route: String, val title: String, val description: String)

object ResearchModules {
    val all = listOf(
        ResearchModule("topic-lab", "Topic Lab", "Narrow a broad idea into feasible research questions."),
        ResearchModule("literature-radar", "Literature Radar", "Find and organize traceable research sources."),
        ResearchModule("evidence-matrix", "Evidence Matrix", "Compare methods, findings, limitations, and relevance."),
        ResearchModule("outline-architect", "Outline Architect", "Plan a logical structure before drafting."),
        ResearchModule("draft-studio", "Draft Studio", "Develop prose grounded in your evidence and notes."),
        ResearchModule("style-coach", "Humaniser & Style Coach", "Improve clarity and flow without evading AI detection."),
        ResearchModule("citation-center", "Citation Center", "Manage references and check metadata."),
        ResearchModule("integrity", "Originality & Integrity", "Check attribution, disclosure, and integrity reminders."),
        ResearchModule("review-simulator", "Review Simulator", "Practice responding to simulated reviewer feedback."),
        ResearchModule("submission-kit", "Journal Match & Submission Kit", "Prepare a journal checklist and submission materials."),
        ResearchModule("revision-response", "Revision & Response", "Track revisions and point-by-point responses."),
        ResearchModule("archive", "Archive & Reproducibility", "Keep versions, source notes, and research records.")
    )
}
