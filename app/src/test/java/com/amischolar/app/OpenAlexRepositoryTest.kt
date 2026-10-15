package com.amischolar.app

import com.amischolar.app.feature.OpenAlexRepository
import org.junit.Assert.*
import org.junit.Test

class OpenAlexRepositoryTest {
    @Test fun emptySearchIsRejectedBeforeNetworkAccess() {
        try {
            OpenAlexRepository.search(" ")
            fail("Expected validation")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("required"))
        }
    }
}
