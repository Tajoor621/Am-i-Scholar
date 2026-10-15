package com.amischolar.app

import com.amischolar.app.feature.DocumentExporter
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class DocumentConformanceTest {
    private fun parseXml(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    }

    @Test fun docxIsValidZipWithWellFormedOpenXmlParts() {
        val original = "Research & evidence <original> \"quoted\"\nSecond paragraph"
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(DocumentExporter.docx(original))).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertEquals(setOf("[Content_Types].xml", "_rels/.rels", "word/document.xml"), entries.keys)
        entries.values.forEach { parseXml(it) }
        val doc = parseXml(entries.getValue("word/document.xml"))
        assertEquals(2, doc.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "p").length)
        assertTrue(doc.documentElement.textContent.contains("Research & evidence <original>"))
        assertTrue(doc.documentElement.textContent.contains("Second paragraph"))
    }

    @Test fun docxEscapesUnicodeAndPreservesLineBreaks() {
        val doc = parseXml(unzipDocument(DocumentExporter.docx("Café — بحث\nline 2")))
        assertTrue(doc.documentElement.textContent.contains("Café — بحث"))
        assertEquals(2, doc.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "p").length)
    }

    private fun unzipDocument(bytes: ByteArray): ByteArray {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") return zip.readBytes()
                entry = zip.nextEntry
            }
        }
        error("Missing word/document.xml")
    }
}
