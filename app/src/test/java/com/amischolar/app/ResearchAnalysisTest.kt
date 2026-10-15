package com.amischolar.app
import com.amischolar.app.feature.ResearchAnalysis
import org.junit.Assert.*
import org.junit.Test
class ResearchAnalysisTest {
 @Test fun diffTracksRemovedAndAddedLines() {
  val result = ResearchAnalysis.difference("one\ntwo", "two\nthree")
  assertEquals(listOf("one"), result.removed)
  assertEquals(listOf("three"), result.added)
 }
 @Test fun evidenceWarnsWhenMissing() {
  assertTrue(ResearchAnalysis.evidenceWarnings(emptyList(), emptyList()).isNotEmpty())
 }
 @Test fun outlineChecksStructure() {
  assertTrue(ResearchAnalysis.outlineWarnings("Introduction", emptyList()).any { it.contains("method") })
 }
 @Test fun journalChecklistRejectsUnverifiedLink() {
  assertTrue(ResearchAnalysis.journalChecklist("Example", "http://example.org").any { it.contains("HTTPS") })
 }
}
