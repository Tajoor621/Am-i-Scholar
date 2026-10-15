package com.amischolar.app.feature

/**
 * Mandatory zero-cost policy for every app build.
 *
 * Network providers are optional and must be initiated by the user.
 * No subscription, ad SDK, hosted inference, analytics, or paid AI endpoint is required.
 */
object ZeroCostPolicy {
    const val REQUIRES_SUBSCRIPTION = false
    const val REQUIRES_PAID_AI_API = false
    const val HAS_ADVERTISING = false
    const val REQUIRES_CLOUD_ACCOUNT = false

    val permittedMetadataHosts = setOf(
        "api.crossref.org", "api.openalex.org", "eutils.ncbi.nlm.nih.gov",
        "export.arxiv.org", "doaj.org"
    )

    fun isPermittedResearchEndpoint(rawUrl: String): Boolean = try {
        val uri = java.net.URI(rawUrl)
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host?.lowercase() in permittedMetadataHosts &&
            uri.userInfo == null &&
            uri.port == -1 &&
            uri.fragment == null &&
            uri.rawPath?.startsWith("/") != false
    } catch (_: Exception) { false }

    fun userDisclosure(): String =
        "Am I Scholar works offline for local research. Optional publication searches " +
        "connect to free public research services when you tap Search. No paid AI " +
        "API, subscription, account, or advertising is required. Free providers may " +
        "limit requests or change access rules. Research notes and drafts are not " +
        "sent to research services automatically."
}
