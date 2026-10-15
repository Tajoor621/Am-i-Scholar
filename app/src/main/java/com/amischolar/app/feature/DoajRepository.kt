package com.amischolar.app.feature

import org.json.JSONObject
import java.net.URLEncoder

/** Free DOAJ public API: optional journal/article discovery, no API key required. */
object DoajRepository {
    fun search(query: String): List<CrossrefRepository.Record> {
        require(query.isNotBlank()) { "Search terms are required" }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "https://doaj.org/api/search/articles/$encoded?page=1&pageSize=10"
        val connection = ResearchHttp.open(endpoint)
        connection.setRequestProperty("Accept", "application/json")
        try {
            if (connection.responseCode !in 200..299) error("DOAJ returned HTTP ${connection.responseCode}")
            return parse(JSONObject(connection.inputStream.bufferedReader().use { it.readText() }))
        } finally { connection.disconnect() }
    }

    fun parse(payload: JSONObject): List<CrossrefRepository.Record> {
        val results = payload.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { index ->
            val bib = results.optJSONObject(index)?.optJSONObject("bibjson") ?: return@mapNotNull null
            val title = bib.optString("title").trim()
            if (title.isBlank()) return@mapNotNull null
            val authors = bib.optJSONArray("author")
            val names = (0 until (authors?.length() ?: 0)).mapNotNull {
                authors?.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank)
            }.joinToString("; ")
            val identifiers = bib.optJSONArray("identifier")
            var doi = ""
            for (i in 0 until (identifiers?.length() ?: 0)) {
                val item = identifiers?.optJSONObject(i)
                if (item?.optString("type") == "doi") doi = item.optString("id")
            }
            val links = bib.optJSONArray("link")
            val url = (0 until (links?.length() ?: 0)).mapNotNull {
                links?.optJSONObject(it)?.optString("url")?.takeIf(String::isNotBlank)
            }.firstOrNull().orEmpty()
            CrossrefRepository.Record(
                title = title, authors = names,
                year = bib.optString("year"),
                doi = doi,
                publication = bib.optJSONObject("journal")?.optString("title").orEmpty(),
                url = url
            )
        }
    }
}
