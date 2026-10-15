package com.amischolar.app.feature

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.content.Context
import android.net.Uri
import com.amischolar.app.data.ResearchNote
import com.amischolar.app.data.ResearchProject
import com.amischolar.app.data.ResearchSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Document generation is offline and initiated explicitly by the user. */
object DocumentExporter {
    fun content(project: ResearchProject?, notes: List<ResearchNote>, sources: List<ResearchSource>): String =
        buildString {
            appendLine(project?.title ?: "Research project")
            appendLine()
            appendLine("Research question: "+(project?.researchQuestion ?: ""))
            appendLine()
            notes.forEach { appendLine(it.kind+": "+it.title);appendLine(it.content);appendLine() }
            appendLine("References (metadata-based drafts; verify against originals)")
            sources.forEachIndexed { index, s -> appendLine(CitationFormatter.format(s,project?.citationStyle ?: "APA",index+1)) }
        }

    fun cslJson(sources: List<ResearchSource>): String {
        val items = JSONArray()
        sources.forEachIndexed { index, s ->
            val item = JSONObject()
            item.put("id", "source"+(index+1))
            item.put("type", "article-journal")
            item.put("title", s.title)
            if (s.publication.isNotBlank()) item.put("container-title", s.publication)
            val author = JSONArray()
            s.authors.split(';').map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                author.put(JSONObject().put("literal",it))
            }
            item.put("author", author)
            val year = s.year.toIntOrNull()
            if (year != null) item.put("issued",JSONObject().put("date-parts",JSONArray().put(JSONArray().put(year))))
            if (s.doiOrUrl.startsWith("10.")) item.put("DOI",s.doiOrUrl)
            else if (s.doiOrUrl.isNotBlank()) item.put("URL",s.doiOrUrl)
            items.put(item)
        }
        return items.toString(2)
    }

    private fun xml(text: String) = text.replace("&","&amp;").replace("<","&lt;")
        .replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
        .filter { it == '\n' || it == '\t' || it.code >= 32 }

    fun docx(text: String): ByteArray {
        val document = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""")
            text.lines().forEach { line ->
                append("<w:p><w:r><w:t xml:space=\"preserve\">")
                append(xml(line))
                append("</w:t></w:r></w:p>")
            }
            append("<w:sectPr/></w:body></w:document>")
        }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun entry(path: String, value: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(value.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""")
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
            entry("word/document.xml",document)
        }
        return out.toByteArray()
    }

    fun pdf(text: String): ByteArray {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = android.graphics.Color.BLACK }
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val lineHeight = 16f
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = margin
        fun nextPage() {
            page?.let { document.finishPage(it) }
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth,pageHeight,pageNumber).create())
            canvas = page!!.canvas
            y = margin
        }
        try {
            nextPage()
            val maxWidth = pageWidth - margin * 2
            text.lines().forEach { line ->
                val words = line.split(' ')
                var current = ""
                val wrapped = mutableListOf<String>()
                words.forEach { word ->
                    val proposed = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(proposed) > maxWidth && current.isNotEmpty()) {
                        wrapped.add(current);current=word
                    } else current=proposed
                }
                wrapped.add(current)
                wrapped.forEach { part ->
                    if (y > pageHeight-margin) nextPage()
                    canvas!!.drawText(part.take(1000),margin,y,paint)
                    y+=lineHeight
                }
            }
            page?.let { document.finishPage(it) }
            val out = ByteArrayOutputStream()
            document.writeTo(out)
            return out.toByteArray()
        } finally { document.close() }
    }

    fun save(context: Context, uri: Uri, bytes: ByteArray) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error("Cannot open selected document destination")
    }
}
