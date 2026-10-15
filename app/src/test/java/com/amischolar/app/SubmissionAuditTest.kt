package com.amischolar.app
import com.amischolar.app.feature.SubmissionAudit
import org.junit.Assert.*
import org.junit.Test
class SubmissionAuditTest {
 @Test fun emptyProjectCannotBeMarkedReady(){
  val checks=SubmissionAudit.inspect(null,emptyList(),emptyList())
  assertTrue(checks.isNotEmpty())
  assertTrue(checks.none{it.passed})
 }
}
