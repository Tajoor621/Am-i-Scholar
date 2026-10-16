package com.amischolar.app.feature

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amischolar.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Entirely local claim-to-source relationships; requires an explicit researcher assessment. */
@Composable
fun EvidenceLinksPanel(projectId: Long, dao: ResearchWorkspaceDao, notes: List<ResearchNote>, sources: List<ResearchSource>) {
    val links by dao.observeEvidenceLinks(projectId).collectAsState(initial = emptyList())
    val claims = notes.filter { it.kind == "EVIDENCE" }
    var claimId by remember(projectId) { mutableLongStateOf(0L) }
    var sourceId by remember(projectId) { mutableLongStateOf(0L) }
    var status by remember { mutableStateOf("unsupported") }
    var rationale by remember { mutableStateOf("") }
    var claimsExpanded by remember { mutableStateOf(false) }
    var sourcesExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Persistent evidence relationships", style = MaterialTheme.typography.titleMedium)
        Text("Select a claim and a source. Your support assessment is not independent source verification.")
        Box {
            OutlinedButton(onClick = { claimsExpanded = true }, enabled = claims.isNotEmpty()) {
                Text("Claim: " + (claims.firstOrNull { it.id == claimId }?.title ?: "Choose claim"))
            }
            DropdownMenu(expanded = claimsExpanded, onDismissRequest = { claimsExpanded = false }) {
                claims.forEach { note ->
                    DropdownMenuItem(text = { Text(note.title) }, onClick = { claimId = note.id; claimsExpanded = false })
                }
            }
        }
        Box {
            OutlinedButton(onClick = { sourcesExpanded = true }, enabled = sources.isNotEmpty()) {
                Text("Source: " + (sources.firstOrNull { it.id == sourceId }?.title ?: "Choose source"))
            }
            DropdownMenu(expanded = sourcesExpanded, onDismissRequest = { sourcesExpanded = false }) {
                sources.forEach { source ->
                    DropdownMenuItem(text = { Text(source.title) }, onClick = { sourceId = source.id; sourcesExpanded = false })
                }
            }
        }
        Box {
            OutlinedButton(onClick = { statusExpanded = true }) { Text("Assessment: $status") }
            DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                listOf("supported", "partially supported", "unsupported").forEach { value ->
                    DropdownMenuItem(text = { Text(value) }, onClick = { status = value; statusExpanded = false })
                }
            }
        }
        OutlinedTextField(value = rationale, onValueChange = { rationale = it },
            label = { Text("Rationale, page and limitations") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Button(enabled = claims.any { it.id == claimId } && sources.any { it.id == sourceId },
            onClick = {
                val item = EvidenceLink(projectId = projectId, claimNoteId = claimId,
                    sourceId = sourceId, status = status, rationale = rationale.trim())
                scope.launch(Dispatchers.IO) { dao.insertEvidenceLink(item) }
                rationale = ""
            }) { Text("Save claim-to-source relationship") }
        if (claims.isEmpty() || sources.isEmpty()) Text("Create an evidence record and a source record first.")
        links.forEach { link ->
            val claim = claims.firstOrNull { it.id == link.claimNoteId }
            val source = sources.firstOrNull { it.id == link.sourceId }
            if (claim != null && source != null) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(claim.title + " → " + source.title, style = MaterialTheme.typography.titleSmall)
                        Text("Researcher assessed: " + link.status)
                        Text(link.rationale.ifBlank { "No rationale supplied" })
                        TextButton(onClick = { scope.launch(Dispatchers.IO) { dao.deleteEvidenceLink(link.id) } }) {
                            Text("Remove link")
                        }
                    }
                }
            }
        }
        if (links.any { it.status == "unsupported" }) {
            Text("Warning: unsupported claims remain. Review before submission.", color = MaterialTheme.colorScheme.error)
        }
    }
}
