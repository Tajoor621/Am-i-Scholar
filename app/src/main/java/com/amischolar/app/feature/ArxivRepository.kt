package com.amischolar.app.feature

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** arXiv Atom discovery. Results are imported as unverified bibliographic metadata. */
object ArxivRepository {
    fun search(query: String): List<CrossrefRepository.Record> {
        require(query.isNotBlank()) { "Search terms are required" }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val connection = URL("https://export.arxiv.org/api/query?search_query=all:$encoded&start=0&max_results=10").openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/atom+xml")
        connection.setRequestProperty("User-Agent", "AmIScholar/1.0 academic metadata client")
        try {
            if (connection.responseCode !in 200..299) error("arXiv returned HTTP ${connection.responseCode}")
            return connection.inputStream.use { input ->
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                parser.setInput(input, "UTF-8")
                parse(parser)
            }
        } finally { connection.disconnect() }
    }

    fun parse(parser: XmlPullParser): List<CrossrefRepository.Record> {
        val records = mutableListOf<CrossrefRepository.Record>()
        var inEntry = false
        var inAuthor = false
        var title = ""
        var id = ""
        var published = ""
        var doi = ""
        var authors = mutableListOf<String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name.orEmpty()
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (tag == "entry") {
                        inEntry = true; title = ""; id = ""; published = ""; doi = ""
                        authors = mutableListOf()
                    } else if (inEntry && tag == "author") inAuthor = true
                    else if (inEntry && tag in setOf("title","id","published","doi","name")) {
                        val value = parser.nextText().trim().replace(Regex("\\s+"), " ")
                        when {
                            tag == "name" && inAuthor -> authors.add(value)
                            tag == "title" -> title = value
                            tag == "id" -> id = value
                            tag == "published" -> published = value
                            tag == "doi" -> doi = value
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tag == "author") inAuthor = false
                    if (tag == "entry") {
                        if (title.isNotBlank() && id.startsWith("https://arxiv.org/")) {
                            records.add(CrossrefRepository.Record(
                                title = title, authors = authors.joinToString("; "),
                                year = published.take(4).takeIf { it.matches(Regex("\\d{4}")) }.orEmpty(),
                                doi = doi, publication = "arXiv preprint", url = id
                            ))
                        }
                        inEntry = false
                    }
                }
            }
            event = parser.next()
        }
        return records
    }
}
