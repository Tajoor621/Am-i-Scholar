package com.amischolar.app

import com.amischolar.app.data.ResearchSource
import com.amischolar.app.feature.CitationAudit
import org.junit.Assert.*
import org.junit.Test

class CitationAuditTest {
 @Test fun duplicatesAndUnverifiedSourcesAreReported(){
  val s=ResearchSource(projectId=1,title="A",doiOrUrl="10.1234/a")
  val issues=CitationAudit.inspect(listOf(s,s))
  assertTrue(issues.any{it.issue.contains("duplicate")})
  assertTrue(issues.any{it.issue.contains("not checked")})
 }
}
