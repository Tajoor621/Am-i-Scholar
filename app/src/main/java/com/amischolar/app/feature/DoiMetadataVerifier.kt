package com.amischolar.app.feature

import com.amischolar.app.data.ResearchSource

/**
 * Compares a locally saved source against Crossref DOI metadata.
 * A metadata match does not verify full-text claims, peer-review status, or research quality.
 */
object DoiMetadataVerifier {
    data class Result(
        val doiResolved: Boolean,
        val titleMatches: Boolean,
        val yearMatches: Boolean?,
        val journalMatches: Boolean?,
        val warnings: List<String>
    ) {
        val metadataConsistent: Boolean get() =
            doiResolved && titleMatches && yearMatches != false && journalMatches != false
    }

    fun compare(source: ResearchSource, record: CrossrefRepository.Record?): Result {
        if (record == null) return Result(false,false,null,null,listOf("DOI not resolved by Crossref"))
        fun normalized(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{N}]+")," ").trim()
        val titleMatches = normalized(source.title) == normalized(record.title)
        val yearMatches = source.year.takeIf { it.isNotBlank() }?.let { it == record.year }
        val journalMatches = source.publication.takeIf { it.isNotBlank() }?.let {
            normalized(it) == normalized(record.publication)
        }
        val warnings = buildList {
            if (!titleMatches) add("Title differs from Crossref")
            if (yearMatches == false) add("Publication year differs from Crossref")
            if (journalMatches == false) add("Journal differs from Crossref")
            if (source.authors.isBlank() || record.authors.isBlank()) add("Author comparison unavailable")
            add("Check the original article and its claims manually")
        }
        return Result(true,titleMatches,yearMatches,journalMatches,warnings)
    }

    fun check(source: ResearchSource): Result {
        val doi = source.doiOrUrl.trim()
        require(doi.startsWith("10.") || doi.contains("doi.org/")) { "A DOI is required" }
        return compare(source, CrossrefRepository.resolveDoi(doi))
    }
}
