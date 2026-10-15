package com.amischolar.app.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amischolar.app.data.ResearchWorkspaceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Results come from Crossref and are always labeled as unverified imported metadata. */
@Composable
fun LiteratureSearchPanel(projectId: Long, dao: ResearchWorkspaceDao) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("Crossref") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CrossrefRepository.Record>>(emptyList()) }
    var imported by remember { mutableStateOf<Set<String>>(emptySet()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Literature Radar · "+provider, style = MaterialTheme.typography.titleMedium)
        Text("Requires internet. Search returns live metadata, not full-text papers or verified research findings.")
        OutlinedTextField(
            query, { query = it }, label = { Text("Topic, paper title, or DOI") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = provider == "Crossref", onClick = { provider = "Crossref" }, label = { Text("Crossref") })
            FilterChip(selected = provider == "OpenAlex", onClick = { provider = "OpenAlex" }, label = { Text("OpenAlex") })
        }
        Button(enabled = !busy && query.isNotBlank(), onClick = {
            busy = true; error = ""; results = emptyList(); imported = emptySet()
            scope.launch {
                try {
                    results = withContext(Dispatchers.IO) {
                        if (provider == "OpenAlex") OpenAlexRepository.search(query)
                        else if (query.trim().startsWith("10.") || query.contains("doi.org/")) {
                            listOfNotNull(CrossrefRepository.resolveDoi(query))
                        } else CrossrefRepository.search(query)
                    }
                    if (results.isEmpty()) error = "No matching metadata found."
                } catch (e: Exception) {
                    error = "Search unavailable: " + (e.message ?: "Network error")
                } finally { busy = false }
            }
        }) { Text(if (busy) "Searching…" else "Search "+provider) }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        results.forEach { record ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(record.title, style = MaterialTheme.typography.titleSmall)
                    Text(listOf(record.authors, record.year, record.publication).filter { it.isNotBlank() }.joinToString(" · "))
                    Text(record.doi.ifBlank { record.url })
                    Text("Imported metadata is unverified.", style = MaterialTheme.typography.labelSmall)
                    Button(enabled = !imported.contains(record.doi.ifBlank { record.url+record.title }), onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { dao.insertSource(record.toSource(projectId)) }
                                imported = imported + record.doi.ifBlank { record.url+record.title }
                            } catch (e: Exception) {
                                error = "Could not save source: " + (e.message ?: "Storage error")
                            }
                        }
                    }) { Text(if (imported.contains(record.doi.ifBlank { record.url+record.title })) "Imported" else "Save source") }
                }
            }
        }
    }
}
