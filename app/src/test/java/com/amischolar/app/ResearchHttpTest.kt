package com.amischolar.app
import com.amischolar.app.feature.ResearchHttp
import org.junit.Assert.*
import org.junit.Test
class ResearchHttpTest {
 @Test fun blocksUnapprovedAndInsecureEndpointsWithoutNetwork() {
  listOf("http://api.crossref.org/works",
         "https://paid-ai.example.com/chat",
         "https://api.crossref.org.evil.example/works").forEach { url ->
   try { ResearchHttp.open(url); fail("Allowed $url") }
   catch (expected: IllegalArgumentException) { assertTrue(expected.message!!.contains("allowlist")) }
  }
 }
 @Test fun permittedEndpointDisablesAutomaticRedirects() {
  val connection = ResearchHttp.open("https://api.crossref.org/works")
  try { assertFalse(connection.instanceFollowRedirects) }
  finally { connection.disconnect() }
 }
}
