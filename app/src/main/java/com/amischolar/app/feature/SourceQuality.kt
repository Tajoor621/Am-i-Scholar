package com.amischolar.app.feature

import com.amischolar.app.data.ResearchSource

/** Local metadata checks only; these checks cannot establish that a publication exists. */
object SourceQuality {
    fun missingFields(source: ResearchSource): List<String> = buildList {
        if (source.title.isBlank()) add("title")
        if (source.authors.isBlank()) add("authors")
        if (source.year.isBlank()) add("year")
        if (source.publication.isBlank()) add("publication")
        if (source.doiOrUrl.isBlank()) add("DOI or URL")
    }

    fun looksLikeDoiOrUrl(value: String): Boolean {
        val text = value.trim()
        return text.isBlank() || text.startsWith("https://", ignoreCase = true) ||
            text.startsWith("http://", ignoreCase = true) ||
            Regex("^10\\.\\d{4,9}/\\S+$").matches(text)
    }
}
