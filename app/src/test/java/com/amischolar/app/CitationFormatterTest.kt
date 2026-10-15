package com.amischolar.app
import com.amischolar.app.data.ResearchSource
import com.amischolar.app.feature.CitationFormatter
import org.junit.Assert.*
import org.junit.Test
class CitationFormatterTest {
 private val s=ResearchSource(projectId=1,title="Evidence matters",authors="A. Researcher",year="2024",doiOrUrl="10.1234/example")
 @Test fun sixStyles(){assertEquals(6,CitationFormatter.styles.size);CitationFormatter.styles.forEach{assertTrue(CitationFormatter.format(s,it).contains("Evidence matters"))}}
 @Test fun exports(){assertTrue(CitationFormatter.bibtex(s,1).contains("10.1234/example"));assertTrue(CitationFormatter.ris(s).contains("TI  - Evidence matters"))}
 @Test fun missingMetadata(){assertTrue(CitationFormatter.format(s.copy(authors=""),"APA").contains("[Author not supplied]"))}
}
