package com.amischolar.app.feature

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * OpenAlex public works search. Only user-initiated network calls.
 * Records remain unverified until a researcher checks the primary publication.
 */
object OpenAlexRepository {
    fun search(query: String): List<CrossrefRepository.Record> {
        require(query.isNotBlank()) { "Search terms are required" }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val connection = URL("https://api.openalex.org/works?search=$encoded&per-page=10").openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "AmIScholar/1.0 (academic research workspace)")
        try {
            val status = connection.responseCode
            if (status !in 200..299) error("OpenAlex returned HTTP $status")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val works = json.getJSONArray("results")
            return (0 until works.length()).mapNotNull { parse(works.optJSONObject(it)) }
        } finally { connection.disconnect() }
    }

    fun parse(item: JSONObject?): CrossrefRepository.Record? {
        if (item == null) return null
        val title = item.optString("display_name").trim()
        if (title.isBlank()) return null
        val authorships = item.optJSONArray("authorships")
        val authors = if (authorships == null) "" else (0 until authorships.length())
            .mapNotNull { authorships.optJSONObject(it)?.optJSONObject("author")?.optString("display_name") }
            .filter { it.isNotBlank() }.joinToString("; ")
        val doi = item.optString("doi").removePrefix("https://doi.org/")
        val location = item.optJSONObject("primary_location")
        val journal = location?.optJSONObject("source")?.optString("display_name").orEmpty()
        return CrossrefRepository.Record(
            title = title, authors = authors,
            year = item.optInt("publication_year").takeIf { it > 0 }?.toString().orEmpty(),
            doi = doi, publication = journal, url = item.optString("id")
        )
    }
}
