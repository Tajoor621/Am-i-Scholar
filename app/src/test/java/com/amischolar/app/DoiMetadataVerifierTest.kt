package com.amischolar.app
import com.amischolar.app.data.ResearchSource
import com.amischolar.app.feature.CrossrefRepository
import com.amischolar.app.feature.DoiMetadataVerifier
import org.junit.Assert.*
import org.junit.Test
class DoiMetadataVerifierTest {
 @Test fun detectsTitleMismatch(){
  val source=ResearchSource(projectId=1,title="Incorrect title",year="2022",doiOrUrl="10.1234/example")
  val record=CrossrefRepository.Record(title="Actual title",authors="",year="2022",doi="10.1234/example",publication="",url="")
  val result=DoiMetadataVerifier.compare(source,record)
  assertFalse(result.metadataConsistent)
  assertTrue(result.warnings.any{it.contains("Title")})
 }
 @Test fun missingDoiResolutionIsNotVerification(){
  val source=ResearchSource(projectId=1,title="Paper")
  assertFalse(DoiMetadataVerifier.compare(source,null).doiResolved)
 }
}
