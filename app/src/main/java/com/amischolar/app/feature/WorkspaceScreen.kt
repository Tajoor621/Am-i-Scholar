package com.amischolar.app.feature

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.amischolar.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(projectId: Long, database: ScholarDatabase, onBack: () -> Unit) {
    val dao = database.researchWorkspaceDao()
    val projectDao = database.researchProjectDao()
    var project by remember(projectId) { mutableStateOf<ResearchProject?>(null) }
    LaunchedEffect(projectId) { project = withContext(Dispatchers.IO) { projectDao.findById(projectId) } }
    val notes by dao.observeNotes(projectId).collectAsState(initial = emptyList())
    val sources by dao.observeSources(projectId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var editor by remember { mutableStateOf("") }
    var selectedNote by remember { mutableStateOf<ResearchNote?>(null) }
    var selectedSource by remember { mutableStateOf<ResearchSource?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var authors by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var locator by remember { mutableStateOf("") }
    var publication by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf("") }
    var exportContent by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = notes.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true) }
    val filteredSources = sources.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.authors.contains(searchQuery, ignoreCase = true) || it.doiOrUrl.contains(searchQuery, ignoreCase = true) || it.evidenceNotes.contains(searchQuery, ignoreCase = true) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(exportContent) } }
            }
        }
    }
    fun startNote(note: ResearchNote? = null, kind: String = "NOTE") {
        selectedNote = note; title = note?.title.orEmpty(); body = note?.content.orEmpty()
        editor = if (note == null) kind else note.kind
    }
    fun startSource(source: ResearchSource? = null) {
        selectedSource = source
        title = source?.title.orEmpty(); authors = source?.authors.orEmpty()
        year = source?.year.orEmpty(); locator = source?.doiOrUrl.orEmpty()
        publication = source?.publication.orEmpty(); evidence = source?.evidenceNotes.orEmpty()
        verified = source?.verified ?: false; editor = "SOURCE"
    }
    if (deleteTarget.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { deleteTarget = "" },
            title = { Text("Remove this record?") },
            text = { Text("This action permanently deletes the selected local record.") },
            confirmButton = { Button(onClick = {
                val target = deleteTarget; deleteTarget = ""
                scope.launch(Dispatchers.IO) {
                    if (target == "SOURCE") selectedSource?.let { dao.deleteSource(it.id) }
                    else selectedNote?.let { dao.deleteNote(it.id) }
                }
                editor = ""
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = "" }) { Text("Cancel") } }
        )
    }
    if (editor.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { editor = "" },
            title = { Text(if (editor == "SOURCE") "Source record" else "Research "+editor.lowercase()) },
            text = {
                Column(modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth())
                    if (editor == "SOURCE") {
                        OutlinedTextField(authors, { authors = it }, label = { Text("Authors (semicolon-separated)") })
                        OutlinedTextField(year, { year = it }, label = { Text("Publication year") })
                        OutlinedTextField(locator, { locator = it }, label = { Text("DOI or URL") }, isError = !SourceQuality.looksLikeDoiOrUrl(locator))
                        if (!SourceQuality.looksLikeDoiOrUrl(locator)) Text("Use a DOI beginning 10. or an http(s) URL.", color = MaterialTheme.colorScheme.error)
                        OutlinedTextField(publication, { publication = it }, label = { Text("Journal / publisher") })
                        OutlinedTextField(evidence, { evidence = it }, label = { Text("Evidence and limitations") })
                        Row { Checkbox(verified, { verified = it }); Text("I checked these details against the original source") }
                    } else {
                        OutlinedTextField(body, { body = it }, label = { Text("Content") }, minLines = 4)
                    }
                }
            },
            confirmButton = {
                Button(enabled = title.isNotBlank() && (editor != "SOURCE" || SourceQuality.looksLikeDoiOrUrl(locator)), onClick = {
                    val kind = editor
                    scope.launch(Dispatchers.IO) {
                        if (kind == "SOURCE") {
                            val item = ResearchSource(
                                id = selectedSource?.id ?: 0, projectId = projectId, title = title.trim(),
                                authors = authors.trim(), year = year.trim(), doiOrUrl = locator.trim(),
                                publication = publication.trim(), evidenceNotes = evidence.trim(), verified = verified
                            )
                            if (selectedSource == null) dao.insertSource(item) else dao.updateSource(item)
                        } else {
                            val item = ResearchNote(
                                id = selectedNote?.id ?: 0, projectId = projectId, title = title.trim(),
                                content = body, kind = kind, updatedAtEpochMillis = System.currentTimeMillis()
                            )
                            if (selectedNote == null) dao.insertNote(item) else dao.updateNote(item)
                        }
                    }
                    editor = ""
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if ((editor == "SOURCE" && selectedSource != null) || (editor != "SOURCE" && selectedNote != null)) {
                        TextButton(onClick = { deleteTarget = if (editor == "SOURCE") "SOURCE" else "NOTE" }) { Text("Delete") }
                    }
                    TextButton(onClick = { editor = "" }) { Text("Cancel") }
                }
            }
        )
    }
    Scaffold(topBar = { TopAppBar(title = { Text(project?.title ?: "Research workspace") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Local-first research workspace", style = MaterialTheme.typography.titleLarge)
                Text("Drafts and source metadata stay on this device. Source verification is your responsibility.")
                OutlinedButton(onClick = onBack) { Text("Back to projects") }
                OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Search notes and sources") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            item {
                Text("Writing and research notes", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { startNote(kind = "NOTE") }) { Text("New note") }
                    Button(onClick = { startNote(kind = "OUTLINE") }) { Text("Outline") }
                    Button(onClick = { startNote(kind = "DRAFT") }) { Text("Draft") }
                }
            }
            items(filteredNotes, key = { "note"+it.id }) { note ->
                OutlinedCard(onClick = { startNote(note) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(note.kind+" · "+note.title, style = MaterialTheme.typography.titleSmall)
                        Text(note.content.take(160), maxLines = 3)
                    }
                }
            }
            item {
                Text("Evidence and sources", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { startSource() }) { Text("Add source") }
            }
            items(filteredSources, key = { "source"+it.id }) { source ->
                OutlinedCard(onClick = { startSource(source) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(source.title, style = MaterialTheme.typography.titleSmall)
                        Text(if (source.verified) "User-checked metadata (not independently verified)" else "Unverified metadata")
                        Text(CitationFormatter.format(source, project?.citationStyle ?: "APA"))
                        val missing = SourceQuality.missingFields(source)
                        if (missing.isNotEmpty()) Text("Missing: "+missing.joinToString(", "), color = MaterialTheme.colorScheme.error)
                        if (source.evidenceNotes.isNotBlank()) Text(source.evidenceNotes)
                    }
                }
            }
            item {
                Text("Export", style = MaterialTheme.typography.titleMedium)
                Text("Bibliography entries are simplified drafts, not publication-ready CSL citations.")
                Text("Source completeness: "+sources.count { SourceQuality.missingFields(it).isEmpty() }+"/"+sources.size+" records have required metadata.")
                Button(onClick = {
                    val p = project
                    exportContent = buildString {
                        appendLine("# "+(p?.title ?: "Research project"))
                        appendLine()
                        appendLine("Research question: "+(p?.researchQuestion ?: ""))
                        appendLine()
                        notes.forEach { appendLine("## "+it.kind+": "+it.title); appendLine(); appendLine(it.content); appendLine() }
                        appendLine("## Sources")
                        sources.forEachIndexed { index, source ->
                            appendLine(CitationFormatter.format(source,p?.citationStyle ?: "APA",index+1))
                            appendLine("Metadata checked by user: "+source.verified)
                            appendLine()
                        }
                    }
                    exportLauncher.launch("am-i-scholar-project-"+projectId+".md")
                }) { Text("Export Markdown") }
                OutlinedButton(onClick = {
                    exportContent = sources.mapIndexed { index, s -> CitationFormatter.bibtex(s,index+1) }.joinToString("\n\n")
                    exportLauncher.launch("am-i-scholar-references-"+projectId+".bib")
                }) { Text("Export BibTeX") }
                OutlinedButton(onClick = {
                    exportContent = sources.joinToString("\n") { CitationFormatter.ris(it) }
                    exportLauncher.launch("am-i-scholar-references-"+projectId+".ris")
                }) { Text("Export RIS") }
                OutlinedButton(onClick = {
                    fun csv(value: String) = "\"" + value.replace("\"", "\"\"").replace("\n", " ") + "\""
                    exportContent = buildString {
                        appendLine("Title,Authors,Year,DOI or URL,Publication,Evidence notes,User verified")
                        sources.forEach { source ->
                            appendLine(listOf(source.title,source.authors,source.year,source.doiOrUrl,source.publication,source.evidenceNotes,source.verified.toString()).joinToString(",") { csv(it) })
                        }
                    }
                    exportLauncher.launch("am-i-scholar-evidence-"+projectId+".csv")
                }) { Text("Export evidence CSV") }
            }
            item { Text("Integrity: Never treat AI output as evidence. Check original sources and disclose AI assistance when required.", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
