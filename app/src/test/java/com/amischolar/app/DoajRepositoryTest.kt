package com.amischolar.app
import com.amischolar.app.feature.DoajRepository
import org.junit.Assert.*
import org.junit.Test
class DoajRepositoryTest {
 @Test fun rejectsBlankQueryBeforeNetwork() {
  try { DoajRepository.search(" "); fail("Expected validation") }
  catch (expected: IllegalArgumentException) { assertTrue(expected.message!!.contains("required")) }
 }
}
