package com.amischolar.app.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amischolar.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Module-specific local workspaces. All content remains on-device unless the user
 * explicitly starts a public metadata search. No AI-generated facts are implied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchModuleWorkspace(
    projectId: Long,
    route: String,
    database: ScholarDatabase,
    onBack: () -> Unit,
    onFullWorkspace: () -> Unit
) {
    val module = ResearchModules.all.firstOrNull { it.route == route }
    val dao = database.researchWorkspaceDao()
    val projectDao = database.researchProjectDao()
    val notes by dao.observeNotes(projectId).collectAsState(initial = emptyList())
    val sources by dao.observeSources(projectId).collectAsState(initial = emptyList())
    var project by remember(projectId) { mutableStateOf<ResearchProject?>(null) }
    LaunchedEffect(projectId) { project = withContext(Dispatchers.IO) { projectDao.findById(projectId) } }
    val scope = rememberCoroutineScope()
    val kind = when (route) {
        "topic-lab" -> "TOPIC"
        "evidence-matrix" -> "EVIDENCE"
        "outline-architect" -> "OUTLINE"
        "draft-studio" -> "DRAFT"
        "style-coach" -> "STYLE"
        "integrity" -> "INTEGRITY"
        "review-simulator" -> "REVIEW"
        "submission-kit" -> "SUBMISSION"
        "revision-response" -> "REVISION"
        "archive" -> "ARCHIVE"
        else -> "NOTE"
    }
    var selected by remember { mutableStateOf<ResearchNote?>(null) }
    var editing by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    fun openEditor(note: ResearchNote? = null) {
        selected = note
        title = note?.title.orEmpty()
        content = note?.content ?: when (route) {
            "evidence-matrix" -> ResearchPlanner.evidenceTemplate()
            "review-simulator", "revision-response" -> ResearchPlanner.responseTemplate()
            "outline-architect" -> "Introduction\nResearch question\nMethods\nEvidence and analysis\nLimitations\nConclusion\nReferences"
            "submission-kit" -> "Target journal and official author-guidelines URL:\nWord limit:\nEthics and consent:\nData availability:\nFunding and conflicts:\nAuthor contributions:\nCover letter:\nChecklist status:"
            "archive" -> "Version:\nDate:\nDataset location (local):\nMethods and environment:\nReproduction steps:\nChanges since last version:"
            else -> ""
        }
        editing = true
    }
    if (editing) AlertDialog(
        onDismissRequest = { editing = false },
        title = { Text("Edit " + (module?.title ?: "research record")) },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Record title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(content, { content = it }, label = { Text("Research content") }, modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp), minLines = 5)
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank(), onClick = {
                val item = ResearchNote(
                    id = selected?.id ?: 0L, projectId = projectId, title = title.trim(),
                    content = content, kind = kind, updatedAtEpochMillis = System.currentTimeMillis()
                )
                scope.launch(Dispatchers.IO) {
                    if (selected == null) dao.insertNote(item) else dao.updateNote(item)
                }
                editing = false
            }) { Text("Save locally") }
        },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } }
    )
    val records = notes.filter { it.kind == kind }
    Scaffold(topBar = { TopAppBar(title = { Text(module?.title ?: "Research module") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(project?.title ?: "Research project", style = MaterialTheme.typography.titleLarge)
                Text(module?.description.orEmpty())
                Text("Offline workspace. Review scientific claims and verify sources yourself.", style = MaterialTheme.typography.bodySmall)
            }
            if (route == "topic-lab") item {
                Text("Current research question", style = MaterialTheme.typography.titleMedium)
                Text(project?.researchQuestion?.ifBlank { "No research question recorded." } ?: "Loading…")
                ResearchPlanner.questionChecks(project?.researchQuestion.orEmpty()).forEach {
                    Text((if (it.passed) "✓ " else "Needs work: ") + it.name + " — " + it.guidance)
                }
                Text("Record alternative questions below; edit the final project question in the main workspace.")
            }
            if (route == "literature-radar") item { LiteratureSearchPanel(projectId, dao) }
            if (route == "citation-center") item {
                Text("Citation metadata review", style = MaterialTheme.typography.titleMedium)
                if (sources.isEmpty()) Text("No saved references. Import or add sources in the full workspace.")
                CitationAudit.inspect(sources).forEach { Text(it.sourceTitle + ": " + it.issue) }
                sources.forEachIndexed { index, source ->
                    Text(CitationFormatter.format(source, project?.citationStyle ?: "APA", index + 1))
                }
                Text("Formatting is a draft, not certified CSL compliance.")
            }
            if (route == "integrity" || route == "submission-kit") item {
                Text("Manuscript readiness", style = MaterialTheme.typography.titleMedium)
                SubmissionAudit.inspect(project, notes, sources).forEach {
                    Text((if (it.passed) "✓ " else "Needs review: ") + it.area + " — " + it.guidance)
                }
                Text("These are heuristic checks, not plagiarism screening or peer review.")
            }
            if (route == "style-coach") item {
                Text("Offline draft clarity checks", style = MaterialTheme.typography.titleMedium)
                val drafts = notes.filter { it.kind == "DRAFT" }
                if (drafts.isEmpty()) Text("Create a draft in Draft Studio to review it.")
                drafts.forEach { draft ->
                    Text(draft.title, style = MaterialTheme.typography.titleSmall)
                    WritingCoach.review(draft.content).forEach { Text(it.label + ": " + it.advice) }
                }
            }
            if (route == "review-simulator") item {
                Text("Simulated review responses only. No actual reviewer has evaluated this manuscript.")
            }
            if (route == "archive") item {
                Text("Local notes: ${notes.size} · saved sources: ${sources.size}")
                Text("Export a backup from the full workspace. App removal may delete local data.")
            }
            if (route !in setOf("literature-radar", "citation-center")) item {
                Button(onClick = { openEditor() }, modifier = Modifier.fillMaxWidth()) {
                    Text("New " + (module?.title ?: "research") + " record")
                }
                Text("Saved records", style = MaterialTheme.typography.titleMedium)
                if (records.isEmpty()) Text("No records saved in this module yet.")
            }
            if (route !in setOf("literature-radar", "citation-center")) items(records, key = { it.id }) { note ->
                OutlinedCard(onClick = { openEditor(note) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(note.title, style = MaterialTheme.typography.titleSmall)
                        Text(note.content.take(350), maxLines = 8)
                        Text("Tap to edit", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item {
                OutlinedButton(onClick = onFullWorkspace, modifier = Modifier.fillMaxWidth()) { Text("Open full paper workspace") }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to modules") }
            }
        }
    }
}
