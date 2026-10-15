package com.amischolar.app

import com.amischolar.app.data.ResearchSource
import com.amischolar.app.feature.SourceQuality
import org.junit.Assert.*
import org.junit.Test

class SourceQualityTest {
    @Test fun incompleteSourcesAreFlagged() {
        val source = ResearchSource(projectId = 1, title = "A title")
        assertTrue(SourceQuality.missingFields(source).contains("authors"))
        assertTrue(SourceQuality.missingFields(source).contains("DOI or URL"))
    }
    @Test fun doiAndUrlsHaveConservativeSyntaxChecks() {
        assertTrue(SourceQuality.looksLikeDoiOrUrl("10.1234/abc"))
        assertTrue(SourceQuality.looksLikeDoiOrUrl("https://example.org/paper"))
        assertFalse(SourceQuality.looksLikeDoiOrUrl("not a doi"))
    }
}
