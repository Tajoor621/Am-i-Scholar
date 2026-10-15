package com.amischolar.app

import com.amischolar.app.feature.CrossrefRepository
import org.junit.Assert.*
import org.junit.Test

class CrossrefRepositoryTest {
    @Test fun importedRecordsAreNeverMarkedVerified() {
        val record = CrossrefRepository.Record(
            title = "Evidence and integrity", authors = "A Researcher",
            year = "2024", doi = "10.1234/test",
            publication = "Research Methods", url = "https://doi.org/10.1234/test"
        )
        val source = record.toSource(42)
        assertEquals("Evidence and integrity", source.title)
        assertEquals("10.1234/test", source.doiOrUrl)
        assertFalse(source.verified)
    }

    @Test fun emptySearchIsRejectedBeforeNetworkAccess() {
        try {
            CrossrefRepository.search("  ")
            fail("Empty search should be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("required"))
        }
    }
}
