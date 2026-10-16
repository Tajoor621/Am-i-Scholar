package com.amischolar.app.feature

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.amischolar.app.data.ResearchSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

/**
 * Sandboxed local CSL processor: same vendored citeproc-js and style XML as the web PWA.
 * No remote navigation or network asset loading. No JS-to-native bridge.
 */
@Composable
fun OfflineCitationPanel(sources: List<ResearchSource>) {
    val context = LocalContext.current
    val items = remember(sources) {
        JSONArray().also { array ->
            sources.forEach { source ->
                val item = JSONObject().put("id", source.id.toString())
                    .put("type", "article-journal").put("title", source.title)
                val authors = JSONArray()
                source.authors.split(';').map { it.trim() }.filter { it.isNotBlank() }.forEach { name ->
                    val comma = name.indexOf(',')
                    authors.put(if (comma > 0) JSONObject().put("family", name.substring(0, comma).trim())
                        .put("given", name.substring(comma + 1).trim()) else JSONObject().put("literal", name))
                }
                if (authors.length() > 0) item.put("author", authors)
                val year = source.year.toIntOrNull()
                if (year != null && year in 1..2999) item.put("issued", JSONObject().put("date-parts", JSONArray().put(JSONArray().put(year))))
                if (source.publication.isNotBlank()) item.put("container-title", source.publication)
                val raw = source.doiOrUrl.trim()
                val doi = raw.replace(Regex("^https?://(dx\\.)?doi\\.org/", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^doi:", RegexOption.IGNORE_CASE), "")
                if (Regex("^10\\.\\d{4,9}/").containsMatchIn(doi)) item.put("DOI", doi)
                if (raw.startsWith("https://") || raw.startsWith("http://")) item.put("URL", raw)
                array.put(item)
            }
        }.toString()
    }
    val currentJson by rememberUpdatedState(items)
    Column {
        Text("CSL bibliography — offline", style = MaterialTheme.typography.titleMedium)
        Text("This uses locally bundled CSL styles. Verify all metadata and target journal rules.")
        AndroidView(modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp),
            factory = {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.domStorageEnabled = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse {
                            val url = request?.url
                            val path = url?.path.orEmpty()
                            if (url?.host != "appassets.androidplatform.net" || !path.startsWith("/assets/csl/")) {
                                return WebResourceResponse("text/plain", "utf-8", 403,
                                    "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                            }
                            val asset = path.removePrefix("/assets/csl/")
                            if (asset.contains("..") || asset.isBlank()) {
                                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                            }
                            val mime = when {
                                asset.endsWith(".js") -> "text/javascript"
                                asset.endsWith(".xml") || asset.endsWith(".csl") -> "application/xml"
                                else -> "text/html"
                            }
                            return try { WebResourceResponse(mime, "utf-8", context.assets.open("csl/" + asset)) }
                            catch (_: Exception) { WebResourceResponse("text/plain", "utf-8", 404,
                                "Missing asset", emptyMap(), ByteArrayInputStream(ByteArray(0))) }
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            view.evaluateJavascript("window.setSources(JSON.parse(" + JSONObject.quote(currentJson) + "))", null)
                        }
                    }
                    loadUrl("https://appassets.androidplatform.net/assets/csl/index.html")
                }
            },
            update = { web ->
                if (web.progress == 100) web.evaluateJavascript(
                    "window.setSources(JSON.parse(" + JSONObject.quote(currentJson) + "))", null)
            })
    }
}
