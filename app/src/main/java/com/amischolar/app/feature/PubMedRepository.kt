package com.amischolar.app.feature

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * PubMed E-utilities: bibliographic discovery via NCBI public endpoints.
 * This provider supplies PubMed IDs and metadata only, not full-text verification.
 */
object PubMedRepository {
    fun search(query: String): List<CrossrefRepository.Record> {
        require(query.isNotBlank()) { "Search terms are required" }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val search = getJson("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&retmode=json&retmax=10&term=$encoded")
        val ids = search.getJSONObject("esearchresult").getJSONArray("idlist")
        if (ids.length() == 0) return emptyList()
        val idList = (0 until ids.length()).map { ids.getString(it) }
        val summary = getJson("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=json&id="+idList.joinToString(","))
        val result = summary.getJSONObject("result")
        return idList.mapNotNull { id ->
            val item = result.optJSONObject(id) ?: return@mapNotNull null
            val title = item.optString("title").trim()
            if (title.isBlank()) return@mapNotNull null
            val authorArray = item.optJSONArray("authors")
            val authors = if (authorArray == null) "" else (0 until authorArray.length())
                .mapNotNull { authorArray.optJSONObject(it)?.optString("name") }
                .filter { it.isNotBlank() }.joinToString("; ")
            val articleIds = item.optJSONArray("articleids")
            var doi = ""
            if (articleIds != null) for (i in 0 until articleIds.length()) {
                val identifier = articleIds.optJSONObject(i)
                if (identifier?.optString("idtype") == "doi") doi = identifier.optString("value")
            }
            val year = Regex("\\b(?:19|20)\\d{2}\\b").find(item.optString("pubdate"))?.value.orEmpty()
            CrossrefRepository.Record(
                title = title, authors = authors, year = year, doi = doi,
                publication = item.optString("fulljournalname"),
                url = "https://pubmed.ncbi.nlm.nih.gov/$id/"
            )
        }
    }

    private fun getJson(endpoint: String): JSONObject {
        val connection = ResearchHttp.open(endpoint)
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "AmIScholar/1.0 (academic research workspace)")
        try {
            val status = connection.responseCode
            if (status !in 200..299) error("PubMed returned HTTP $status")
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }
}
