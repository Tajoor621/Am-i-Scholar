package com.amischolar.app.feature

import java.net.HttpURLConnection
import java.net.URL

/**
 * Mandatory transport guard for optional, user-initiated metadata lookups.
 * Prevents paid/unapproved endpoints and redirect-based host bypasses.
 */
object ResearchHttp {
    fun open(endpoint: String): HttpURLConnection {
        require(ZeroCostPolicy.isPermittedResearchEndpoint(endpoint)) {
            "Research endpoint is not on the free-service HTTPS allowlist"
        }
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        return connection
    }
}
