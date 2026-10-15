package com.amischolar.app
import com.amischolar.app.feature.PubMedRepository
import org.junit.Assert.*
import org.junit.Test
class PubMedRepositoryTest {
 @Test fun blankQueryRejectedBeforeNetwork(){
  try { PubMedRepository.search(" "); fail("Expected validation") }
  catch (expected: IllegalArgumentException) { assertTrue(expected.message!!.contains("required")) }
 }
}
