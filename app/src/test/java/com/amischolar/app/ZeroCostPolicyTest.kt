package com.amischolar.app
import com.amischolar.app.feature.ZeroCostPolicy
import org.junit.Assert.*
import org.junit.Test
class ZeroCostPolicyTest {
 @Test fun paidServicesAreNotRequired() {
  assertFalse(ZeroCostPolicy.REQUIRES_SUBSCRIPTION)
  assertFalse(ZeroCostPolicy.REQUIRES_PAID_AI_API)
  assertFalse(ZeroCostPolicy.HAS_ADVERTISING)
  assertFalse(ZeroCostPolicy.REQUIRES_CLOUD_ACCOUNT)
 }
 @Test fun researchEndpointsMustUseHttpsAndAllowlist() {
  assertTrue(ZeroCostPolicy.isPermittedResearchEndpoint("https://api.crossref.org/works"))
  assertTrue(ZeroCostPolicy.isPermittedResearchEndpoint("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi"))
  assertFalse(ZeroCostPolicy.isPermittedResearchEndpoint("http://api.crossref.org/works"))
  assertFalse(ZeroCostPolicy.isPermittedResearchEndpoint("https://api.crossref.org.evil.example/works"))
  assertFalse(ZeroCostPolicy.isPermittedResearchEndpoint("https://paid-api.example.com/v1/chat"))
 }
}
