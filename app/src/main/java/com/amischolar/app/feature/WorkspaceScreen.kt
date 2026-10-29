package com.amischolar.app.feature

import androidx.room.withTransaction
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
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
fun WorkspaceScreen(projectId: Long, database: ScholarDatabase, onBack: () -> Unit, activeModule: String = "") {
    val dao = database.researchWorkspaceDao()
    val projectDao = database.researchProjectDao()
    var project by remember(projectId) { mutableStateOf<ResearchProject?>(null) }
    LaunchedEffect(projectId) { project = withContext(Dispatchers.IO) { projectDao.findById(projectId) } }
    val notes by dao.observeNotes(projectId).collectAsState(initial = emptyList())
    val sources by dao.observeSources(projectId).collectAsState(initial = emptyList())
    val evidenceLinks by dao.observeEvidenceLinks(projectId).collectAsState(initial = emptyList())
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
    var exportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var doiCheckMessage by remember { mutableStateOf("") }
    var checkingDoi by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = notes.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true) }
    val filteredSources = sources.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.authors.contains(searchQuery, ignoreCase = true) || it.doiOrUrl.contains(searchQuery, ignoreCase = true) || it.evidenceNotes.contains(searchQuery, ignoreCase = true) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching { DocumentExporter.save(context, uri, exportContent.toByteArray(Charsets.UTF_8)) }
            }
        }
    }
    val docxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            runCatching { exportBytes?.let { DocumentExporter.save(context, uri, it) } }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            runCatching { exportBytes?.let { DocumentExporter.save(context, uri, it) } }
        }
    }
    var backupStatus by remember { mutableStateOf("") }
    var encryptedAction by remember { mutableStateOf("") }
    var encryptedPassphrase by remember { mutableStateOf("") }
    var encryptedConfirm by remember { mutableStateOf("") }
    var encryptedImportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingProjectRestore by remember { mutableStateOf<ProjectBackup.Snapshot?>(null) }
    val backupExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) {
                    val plain = ProjectBackup.encode(ProjectBackup.Snapshot(
                        requireNotNull(project) { "Project unavailable" }, notes, sources, evidenceLinks
                    ))
                    if (encryptedAction == "export-ready") EncryptedBackup.encrypt(plain, encryptedPassphrase) else plain
                }
                withContext(Dispatchers.IO) { DocumentExporter.save(context, uri, bytes) }
                backupStatus = if (encryptedAction == "export-ready") "Encrypted project backup saved." else "Project backup saved. Keep this file in a safe location."
            } catch (e: Exception) { backupStatus = "Backup failed: " + (e.message ?: "Unknown error") }
            finally { encryptedAction = ""; encryptedPassphrase = ""; encryptedConfirm = "" }
        }
    }
    val backupImport = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val output = java.io.ByteArrayOutputStream()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val chunk = ByteArray(8192); var total = 0
                        while (true) { val count = input.read(chunk); if (count < 0) break; total += count
                            require(total <= EncryptedBackup.MAX_BYTES) { "Backup exceeds 10 MB" }; output.write(chunk, 0, count) }
                    } ?: error("Could not open backup")
                    output.toByteArray()
                }
                if (EncryptedBackup.isEnvelope(bytes)) { encryptedImportBytes = bytes; encryptedAction = "import" }
                else pendingProjectRestore = withContext(Dispatchers.IO) { ProjectBackup.decode(bytes) }
            } catch (e: Exception) { backupStatus = "Backup validation failed: " + (e.message ?: "Invalid backup") }
        }
    }
    if (encryptedAction == "export" || encryptedAction == "import") AlertDialog(
        onDismissRequest = { encryptedAction = ""; encryptedPassphrase = ""; encryptedConfirm = ""; encryptedImportBytes = null },
        title = { Text(if (encryptedAction == "export") "Encrypt project backup" else "Decrypt project backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (encryptedAction == "export") "Use a passphrase of at least 12 characters. Keep it separately; it cannot be recovered." else "Enter the passphrase used when this file was exported.")
                OutlinedTextField(encryptedPassphrase, { encryptedPassphrase = it }, label = { Text("Passphrase") }, modifier = Modifier.fillMaxWidth())
                if (encryptedAction == "export") OutlinedTextField(encryptedConfirm, { encryptedConfirm = it }, label = { Text("Confirm passphrase") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = {
            if (encryptedPassphrase.length < 12) backupStatus = "Passphrase must be at least 12 characters."
            else if (encryptedAction == "export" && encryptedPassphrase != encryptedConfirm) backupStatus = "Passphrases do not match."
            else if (encryptedAction == "export") { encryptedAction = "export-ready"; backupExport.launch("am-i-scholar-encrypted-project-$projectId.json") }
            else {
                val bytes = encryptedImportBytes
                scope.launch {
                    try {
                        val plain = withContext(Dispatchers.Default) { EncryptedBackup.decrypt(requireNotNull(bytes), encryptedPassphrase) }
                        pendingProjectRestore = withContext(Dispatchers.IO) { ProjectBackup.decode(plain) }
                        backupStatus = "Encrypted file authenticated. Review the restore preview."
                    } catch (e: Exception) { backupStatus = "Decryption failed; no data changed: " + (e.message ?: "Invalid backup") }
                    finally { encryptedAction = ""; encryptedPassphrase = ""; encryptedImportBytes = null }
                }
                encryptedAction = ""
            }
        }) { Text(if (encryptedAction == "export") "Continue" else "Decrypt") } },
        dismissButton = { TextButton(onClick = { encryptedAction = ""; encryptedPassphrase = ""; encryptedConfirm = ""; encryptedImportBytes = null }) { Text("Cancel") } }
    )
    if (pendingProjectRestore != null) {
        val snapshot = pendingProjectRestore
        AlertDialog(
            onDismissRequest = { pendingProjectRestore = null },
            title = { Text("Restore preview") },
            text = { Text("Import “${snapshot?.project?.title}” as a new project with ${snapshot?.notes?.size ?: 0} notes and ${snapshot?.sources?.size ?: 0} sources? Existing data will remain.") },
            confirmButton = { TextButton(onClick = {
                pendingProjectRestore = null
                scope.launch {
                    try {
                        val restored = requireNotNull(snapshot)
                        val newId = withContext(Dispatchers.IO) {
                            database.withTransaction {
                                val id = projectDao.insert(restored.project.copy(id = 0))
                                val noteIds = restored.notes.map { dao.insertNote(it.copy(id = 0, projectId = id)) }
                                val sourceIds = restored.sources.map { dao.insertSource(it.copy(id = 0, projectId = id)) }
                                restored.links.forEach { link -> dao.insertEvidenceLink(EvidenceLink(projectId = id,
                                    claimNoteId = noteIds[link.noteIndex], sourceId = sourceIds[link.sourceIndex],
                                    status = link.status, rationale = link.rationale)) }
                                id
                            }
                        }
                        backupStatus = "Restored as a separate project (ID $newId)."
                    } catch (e: Exception) { backupStatus = "Restore failed; no partial project saved: " + (e.message ?: "Invalid backup") }
                }
            }) { Text("Import") } },
            dismissButton = { TextButton(onClick = { pendingProjectRestore = null }) { Text("Cancel") } }
        )
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
                        Row(
                            modifier = Modifier.fillMaxWidth().toggleable(value = verified, role = Role.Checkbox, onValueChange = { verified = it }),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Checkbox(checked = verified, onCheckedChange = null)
                            Text("I checked these details against the original source")
                        }
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
                Text("Project backup and recovery", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(enabled = project != null, onClick = {
                    encryptedAction = "plain-export"
                    backupExport.launch("am-i-scholar-project-$projectId.json")
                }) { Text("Export complete project backup") }
                OutlinedButton(enabled = project != null, onClick = {
                    encryptedAction = "export"; encryptedPassphrase = ""; encryptedConfirm = ""
                }) { Text("Export encrypted project") }
                OutlinedButton(onClick = { backupImport.launch(arrayOf("application/json", "*/*")) }) {
                    Text("Restore backup as a new project")
                }
                if (backupStatus.isNotBlank()) Text(backupStatus)

                ResearchModules.all.firstOrNull { it.route == activeModule }?.let { module ->
                    Text(module.title, style = MaterialTheme.typography.titleMedium)
                    Text(module.description)
                    when (activeModule) {
                        "topic-lab" -> Text("Edit the project's research question to refine its scope; the checks below use saved project data.")
                        "literature-radar" -> Text("Search free research metadata providers below, then save sources.")
                        "evidence-matrix" -> { Button(onClick = { startNote(kind = "EVIDENCE"); body = ResearchPlanner.evidenceTemplate() }) { Text("Create evidence record") }; EvidenceLinksPanel(projectId, dao, notes, sources) }
                        "outline-architect" -> Button(onClick = { startNote(kind = "OUTLINE") }) { Text("Create outline") }
                        "draft-studio" -> Button(onClick = { startNote(kind = "DRAFT") }) { Text("Create draft") }
                        "style-coach" -> Text("Open a draft below to see local writing feedback.")
                        "citation-center" -> Text("Check metadata and export draft references below. Verify before publication.")
                        "integrity" -> Text("Use source verification and readiness checks below; no plagiarism database is provided.")
                        "review-simulator" -> Button(onClick = { startNote(kind = "REVIEW"); body = ResearchPlanner.responseTemplate() }) { Text("Create simulated review response") }
                        "submission-kit" -> Text("Submission readiness checks are below. Journal matching is not automated.")
                        "revision-response" -> Button(onClick = { startNote(kind = "REVISION") }) { Text("Create revision log") }
                        "archive" -> Text("Local notes and sources appear below. Export a separate copy for preservation.")
                    }
                }
                project?.researchQuestion?.let { question ->
                    Text("Research question checks", style = MaterialTheme.typography.titleSmall)
                    ResearchPlanner.questionChecks(question).forEach { check ->
                        Text((if (check.passed) "✓ " else "Review: ") + check.name + " — " + check.guidance)
                    }
                }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { startNote(kind = "REVISION") }) { Text("Revision log") }
                    OutlinedButton(onClick = { startNote(kind = "REVIEW") }) { Text("Reviewer response") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { startNote(kind = "EVIDENCE"); body = ResearchPlanner.evidenceTemplate() }) { Text("Evidence matrix") }
                    OutlinedButton(onClick = { startNote(kind = "REVIEW"); body = ResearchPlanner.responseTemplate() }) { Text("Response template") }
                }
            }
            items(filteredNotes, key = { "note"+it.id }) { note ->
                OutlinedCard(onClick = { startNote(note) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(note.kind+" · "+note.title, style = MaterialTheme.typography.titleSmall)
                        Text(note.content.take(160), maxLines = 3)
                        if (note.kind == "DRAFT") {
                            Text("Local writing coach", style = MaterialTheme.typography.labelMedium)
                            WritingCoach.review(note.content).forEach { finding ->
                                Text(finding.label+": "+finding.advice, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            item { LiteratureSearchPanel(projectId, dao) }
            item {
                if (doiCheckMessage.isNotBlank()) Text(doiCheckMessage)
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
                        if (source.doiOrUrl.trim().startsWith("10.") || source.doiOrUrl.contains("doi.org/")) {
                            TextButton(enabled = !checkingDoi, onClick = {
                                checkingDoi = true
                                scope.launch {
                                    try {
                                        val result = withContext(Dispatchers.IO) { DoiMetadataVerifier.check(source) }
                                        doiCheckMessage = source.title+": "+(if (result.metadataConsistent) "Crossref metadata consistent" else "Metadata discrepancies")+
                                            " — "+result.warnings.joinToString("; ")
                                    } catch (e: Exception) {
                                        doiCheckMessage = "DOI check failed: "+(e.message ?: "network unavailable")
                                    } finally { checkingDoi = false }
                                }
                            }) { Text("Compare DOI metadata") }
                        }
                    }
                }
            }
            item {
                Text("Submission readiness · simulated checklist", style = MaterialTheme.typography.titleMedium)
                Text("Not a real peer review or journal recommendation.")
                SubmissionAudit.inspect(project, notes, sources).forEach { check ->
                    Text((if (check.passed) "✓ " else "Needs review: ")+check.area+" — "+check.guidance, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = {
                    exportBytes = null
                    exportContent = SubmissionAudit.report(project, notes, sources)
                    exportLauncher.launch("am-i-scholar-submission-checklist-"+projectId+".txt")
                }) { Text("Export submission checklist") }
            }
            item {
                Text("Export", style = MaterialTheme.typography.titleMedium)
                Text("Bibliography entries are simplified drafts, not publication-ready CSL citations.")
                Text("Citation audit: "+CitationAudit.inspect(sources).size+" metadata issues or review reminders.")
                OutlinedButton(onClick = {
                    exportContent = CitationAudit.report(sources)
                    exportLauncher.launch("am-i-scholar-citation-audit-"+projectId+".txt")
                }) { Text("Export citation audit") }
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
