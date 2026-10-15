package com.amischolar.app.feature

import com.amischolar.app.data.ResearchSource
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Crossref REST API. No API keys or fabricated fallback records.
 * Network access is invoked only when the user presses Search or Resolve.
 */
object CrossrefRepository {
    data class Record(
        val title: String,
        val authors: String,
        val year: String,
        val doi: String,
        val publication: String,
        val url: String
    ) {
        fun toSource(projectId: Long): ResearchSource = ResearchSource(
            projectId = projectId, title = title, authors = authors, year = year,
            doiOrUrl = doi.ifBlank { url }, publication = publication, verified = false
        )
    }

    fun search(query: String): List<Record> {
        require(query.isNotBlank()) { "Search terms are required" }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val json = request("https://api.crossref.org/works?query.bibliographic=$encoded&rows=10")
        val items = json.getJSONObject("message").getJSONArray("items")
        return (0 until items.length()).mapNotNull { parse(items.optJSONObject(it)) }
    }

    fun resolveDoi(doi: String): Record? {
        val clean = doi.trim().removePrefix("https://doi.org/").removePrefix("http://doi.org/")
        require(Regex("^10\\.\\d{4,9}/\\S+$").matches(clean)) { "Enter a valid DOI" }
        val path = clean.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8") }
        val json = request("https://api.crossref.org/works/$path")
        return parse(json.getJSONObject("message"))
    }

    private fun request(endpoint: String): JSONObject {
        val connection = ResearchHttp.open(endpoint)
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "AmIScholar/1.0 (academic research workspace)")
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw IllegalStateException("Crossref returned HTTP $status")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    fun parse(item: JSONObject?): Record? {
        if (item == null) return null
        val title = item.optJSONArray("title")?.optString(0).orEmpty().trim()
        if (title.isBlank()) return null
        val authorsArray = item.optJSONArray("author")
        val authors = if (authorsArray == null) "" else (0 until authorsArray.length())
            .mapNotNull { authorsArray.optJSONObject(it) }
            .map { listOf(it.optString("given"), it.optString("family")).filter { p -> p.isNotBlank() }.joinToString(" ") }
            .filter { it.isNotBlank() }.joinToString("; ")
        val issued = item.optJSONObject("issued")?.optJSONArray("date-parts")?.optJSONArray(0)
        val year = issued?.optInt(0)?.takeIf { it > 0 }?.toString().orEmpty()
        return Record(
            title = title,
            authors = authors,
            year = year,
            doi = item.optString("DOI"),
            publication = item.optJSONArray("container-title")?.optString(0).orEmpty(),
            url = item.optString("URL")
        )
    }
}
