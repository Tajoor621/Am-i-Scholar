package com.amischolar.app.feature

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.amischolar.app.data.ResearchNote
import com.amischolar.app.data.ResearchSource
import com.amischolar.app.data.ResearchWorkspaceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun GroundedAiHandoffPanel(
    projectId: Long,
    sources: List<ResearchSource>,
    notes: List<ResearchNote>,
    dao: ResearchWorkspaceDao
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val links by dao.observeEvidenceLinks(projectId).collectAsState(initial = emptyList())
    var selectedIds by remember(projectId) { mutableStateOf(emptySet<Long>()) }
    var task by remember(projectId) { mutableStateOf("Summarize the selected evidence and identify limitations.") }
    var provider by remember(projectId) { mutableStateOf("") }
    var response by remember(projectId) { mutableStateOf("") }
    var consent by remember(projectId) { mutableStateOf(false) }
    var copiedPrompt by remember(projectId) { mutableStateOf("") }
    var reviewed by remember(projectId) { mutableStateOf(false) }
    var disclosureText by remember(projectId) { mutableStateOf("")}
    var message by remember(projectId) { mutableStateOf("") }
    val selectedSources = sources.filter { it.id in selectedIds }.take(GroundedAiHandoff.MAX_SOURCES).map { source ->
        val linkedEvidence = links.filter { it.sourceId == source.id }.mapNotNull { link ->
            notes.firstOrNull { it.id == link.claimNoteId && it.kind == "EVIDENCE" }?.let { note ->
                "${note.title}: ${note.content}\nResearcher assessment: ${link.status}. ${link.rationale}"
            }
        }
        GroundedAiHandoff.SourceContext(
            id = source.id.toString(),
            title = source.title,
            identifier = source.doiOrUrl,
            verified = source.verified,
            evidence = listOf(source.evidenceNotes, linkedEvidence.joinToString("\n")).filter { it.isNotBlank() }.joinToString("\n")
        )
    }
    val generatedDisclosure = GroundedAiHandoff.disclosure(provider, task, selectedSources.map { it.id }, reviewed)
    val built = remember(task, selectedIds, sources, notes, links) {
        GroundedAiHandoff.buildPrompt(task, selectedSources)
    }
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Manual AI handoff", style = MaterialTheme.typography.titleMedium)
            Text("This app makes no AI request. Preview the exact prompt, then choose whether to copy it into an external service. External services may charge fees or retain data under their own terms.")
            OutlinedTextField(
                value = task,
                onValueChange = { task = it.take(GroundedAiHandoff.MAX_TASK_CHARS); copiedPrompt = ""; consent = false },
                label = { Text("Task to request") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            Text("Select up to ${GroundedAiHandoff.MAX_SOURCES} source records. Drafts, project notes, PDFs and unselected records are excluded.")
            sources.forEach { source ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = source.id in selectedIds,
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) selectedIds + source.id else selectedIds - source.id
                            copiedPrompt = ""
                            consent = false
                        }
                    )
                    Column {
                        Text(source.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            (source.doiOrUrl.ifBlank { "No DOI or URL recorded" }) +
                                if (source.verified) " · marked verified" else " · UNVERIFIED",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Text("Prompt preview", style = MaterialTheme.typography.titleSmall)
            Text(
                built.text,
                modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall
            )
            if (built.truncated) Text("Context was shortened to stay within the source and character limits.", color = MaterialTheme.colorScheme.error)
            Text("Selected source records: ${built.sourceIds.size} · Prompt: ${built.text.length}/${GroundedAiHandoff.MAX_PROMPT_CHARS} characters")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = consent, onCheckedChange = { consent = it })
                Text("I reviewed this exact prompt and consent to copying these selected notes for possible external processing. This consent applies once.")
            }
            Button(
                enabled = GroundedAiHandoff.canCopy(consent, built),
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Am I Scholar AI handoff", built.text))
                    copiedPrompt = built.text
                    consent = false
                    message = "Prompt copied. Choose an external provider yourself; this app has not sent it."
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Copy reviewed prompt") }
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
            if (copiedPrompt == built.text) {
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it.take(120) },
                    label = { Text("External service name (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it.take(20000) },
                    label = { Text("Paste returned text here") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    minLines = 4
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = reviewed, onCheckedChange = { reviewed = it })
                    Text("I checked the returned claims and citations against the original sources.")
                }
                Button(
                    enabled = response.isNotBlank(),
                    onClick = {
                        val disclosure = disclosureText.ifBlank { generatedDisclosure }
                        val log = ResearchNote(
                            projectId = projectId,
                            title = "AI use log · ${System.currentTimeMillis()}",
                            kind = "ARCHIVE",
                            content = buildString {
                                appendLine("AI-assisted text — unverified by the app")
                                appendLine("External service: ${provider.ifBlank { "not recorded" }}")
                                appendLine("Task: $task")
                                appendLine("Selected source IDs: ${built.sourceIds.joinToString(", ")}")
                                appendLine("Researcher review checkbox: ${if (reviewed) "checked" else "not checked"}")
                                appendLine()
                                appendLine("Exact prompt:")
                                appendLine(built.text)
                                appendLine()
                                appendLine("Returned text:")
                                appendLine(response)
                                appendLine()
                                appendLine(disclosure)
                            }
                        )
                        scope.launch(Dispatchers.IO) {
                            dao.insertNote(log)
                            message = "AI-use log saved locally under Archive."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save AI-use log locally") }
                OutlinedTextField(
                    value = disclosureText.ifBlank { generatedDisclosure },
                    onValueChange = { disclosureText = it },
                    label = { Text("Editable AI-use disclosure statement") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Button(
                    onClick = {
                        val disclosure = GroundedAiHandoff.disclosure(provider, task, built.sourceIds, reviewed)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI assistance disclosure", disclosure))
                        message = "Disclosure copied."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Copy disclosure statement") }
            }
        }
    }
}
