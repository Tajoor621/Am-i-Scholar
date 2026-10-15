package com.amischolar.app
import com.amischolar.app.feature.ResearchPlanner
import org.junit.Assert.*
import org.junit.Test
class ResearchPlannerTest {
 @Test fun flagsVagueQuestion() { assertTrue(ResearchPlanner.questionChecks("Why is everything best?").any { !it.passed }) }
 @Test fun evidenceTemplateHasTraceability() { assertTrue(ResearchPlanner.evidenceTemplate().contains("Verification status")) }
 @Test fun reviewerResponseRequiresEvidence() { assertTrue(ResearchPlanner.responseTemplate().contains("Supporting source")) }
}
