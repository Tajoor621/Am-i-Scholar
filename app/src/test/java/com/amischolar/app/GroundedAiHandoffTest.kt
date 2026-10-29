package com.amischolar.app

import com.amischolar.app.feature.GroundedAiHandoff
import org.junit.Assert.*
import org.junit.Test

class GroundedAiHandoffTest {
    private fun source(id: String, verified: Boolean = false, evidence: String = "") =
        GroundedAiHandoff.SourceContext(id, "Study $id", "https://example.test/$id", verified, evidence)

    @Test fun selectedSourcesAreExplicitlyLabeledAndNoUnselectedContextIsAdded() {
        val result = GroundedAiHandoff.buildPrompt("Summarize", listOf(source("chosen", evidence = "Observed outcome")))
        assertTrue(result.text.contains("Local source ID: chosen"))
        assertTrue(result.text.contains("Observed outcome"))
        assertTrue(result.text.contains("UNVERIFIED"))
        assertFalse(result.text.contains("not-selected-secret"))
    }

    @Test fun promptCapsSourceCountAndContextLengthWhileKeepingIdentifiers() {
        val sources = (1..7).map { source("id-$it", evidence = "evidence ".repeat(500)) }
        val result = GroundedAiHandoff.buildPrompt("task ".repeat(500), sources)
        assertTrue(result.truncated)
        assertTrue(result.text.length <= GroundedAiHandoff.MAX_PROMPT_CHARS)
        assertEquals(5, result.sourceIds.size)
        result.sourceIds.forEach { assertTrue(result.text.contains("Local source ID: $it")) }
    }

    @Test fun missingIdentifierIsDisclosedAndNeverInvented() {
        val item = source("x").copy(identifier = "")
        val result = GroundedAiHandoff.buildPrompt("Review", listOf(item))
        assertTrue(result.text.contains("[none recorded; do not invent one]"))
        assertFalse(result.text.contains("https://doi.org"))
    }

    @Test fun copyRequiresOneTimeConsentAndSelectedSource() {
        val empty = GroundedAiHandoff.buildPrompt("task", emptyList())
        val nonempty = GroundedAiHandoff.buildPrompt("task", listOf(source("s1")))
        assertFalse(GroundedAiHandoff.canCopy(false, nonempty))
        assertFalse(GroundedAiHandoff.canCopy(true, empty))
        assertTrue(GroundedAiHandoff.canCopy(true, nonempty))
    }

    @Test fun disclosureDoesNotClaimVerificationUnlessResearcherConfirmsIt() {
        assertTrue(GroundedAiHandoff.disclosure("Provider A", "summarization", listOf("s1"), false).contains("have not yet verified"))
        assertTrue(GroundedAiHandoff.disclosure("Provider A", "summarization", listOf("s1"), true).contains("checked its claims"))
    }
}
