package com.amischolar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amischolar.app.feature.WorkspaceScreen
import com.amischolar.app.feature.ResearchModuleWorkspace
import com.amischolar.app.feature.StudentWorkspaceScreen
import com.amischolar.app.data.ResearchProject
import com.amischolar.app.data.ScholarDatabase
import com.amischolar.app.feature.ZeroCostPolicy
import com.amischolar.app.feature.ResearchModules
import com.amischolar.app.ui.theme.AmIScholarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = ScholarDatabase.getInstance(applicationContext).researchProjectDao()
        setContent {
            AmIScholarTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        val projects by dao.observeAll().collectAsState(initial = emptyList())
                        HomeScreen(
                            projects = projects,
                            onNewPaper = { navController.navigate("new-paper") },
                            onOpenModule = { navController.navigate("module/$it") },
                            onOpenProject = { navController.navigate("project/$it") },
                            onWorkspace = { navController.navigate("workspace/$it") },
                            onStudentWorkspace = { navController.navigate("student") }
                        )
                    }
                    composable("student") {
                        StudentWorkspaceScreen(
                            database = ScholarDatabase.getInstance(applicationContext),
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "project/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: -1L
                        var project by remember(id) { mutableStateOf<ResearchProject?>(null) }
                        androidx.compose.runtime.LaunchedEffect(id) {
                            project = withContext(Dispatchers.IO) { dao.findById(id) }
                        }
                        project?.let { current ->
                            ProjectEditorScreen(
                                project = current,
                                onBack = { navController.popBackStack() },
                                onSave = { updated ->
                                    lifecycleScope.launch {
                                        withContext(Dispatchers.IO) { dao.update(updated) }
                                        navController.popBackStack()
                                    }
                                },
                                onDelete = {
                                    lifecycleScope.launch {
                                        withContext(Dispatchers.IO) { dao.deleteById(id) }
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                    composable(
                        route = "workspace/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: -1L
                        WorkspaceScreen(
                            projectId = id,
                            database = ScholarDatabase.getInstance(applicationContext),
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "workspace/{id}/{module}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.LongType },
                            navArgument("module") { type = NavType.StringType }
                        )
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: -1L
                        val module = entry.arguments?.getString("module").orEmpty()
                        ResearchModuleWorkspace(
                            projectId = id,
                            route = module,
                            database = ScholarDatabase.getInstance(applicationContext),
                            onBack = { navController.popBackStack() },
                            onFullWorkspace = { navController.navigate("workspace/$id") }
                        )
                    }
                    composable("new-paper") {
                        NewPaperScreen(
                            onBack = { navController.popBackStack() },
                            onCreate = { title, question, discipline, style ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        dao.insert(
                                            ResearchProject(
                                                title = title.trim(),
                                                researchQuestion = question.trim(),
                                                discipline = discipline.trim().ifBlank { "Not set" },
                                                citationStyle = style
                                            )
                                        )
                                    }
                                    if (!isFinishing && !isDestroyed) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        )
                    }
                    composable(
                        route = "module/{route}",
                        arguments = listOf(navArgument("route") { type = NavType.StringType })
                    ) { entry ->
                        val route = entry.arguments?.getString("route").orEmpty()
                        val module = ResearchModules.all.firstOrNull { it.route == route }
                        val projects by dao.observeAll().collectAsState(initial = emptyList())
                        ModuleScreen(
                            title = module?.title ?: "Research module",
                            description = module?.description ?: "Research workspace",
                            projects = projects,
                            onSelectProject = { navController.navigate("workspace/$it/$route") },
                            onCreateProject = { navController.navigate("new-paper") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    projects: List<ResearchProject>,
    onNewPaper: () -> Unit,
    onOpenModule: (String) -> Unit,
    onOpenProject: (Long) -> Unit,
    onWorkspace: (Long) -> Unit,
    onStudentWorkspace: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Am I Scholar") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Your research workspace", style = MaterialTheme.typography.headlineSmall)
                Text("Organise evidence, shape ideas, and keep your research verifiable.")
                Text(ZeroCostPolicy.userDisclosure(), style = MaterialTheme.typography.bodySmall)
                Button(onClick = onNewPaper, modifier = Modifier.fillMaxWidth()) {
                    Text("＋  Start a new paper")
                }
                OutlinedButton(onClick = onStudentWorkspace, modifier = Modifier.fillMaxWidth()) {
                    Text("🎓  Open student workspace")
                }
            }
            item {
                Text("My papers", style = MaterialTheme.typography.titleLarge)
                if (projects.isEmpty()) Text("No projects yet. Create one to begin your workspace.")
            }
            items(projects, key = { it.id }) { project ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(project.title, style = MaterialTheme.typography.titleMedium)
                        if (project.researchQuestion.isNotBlank()) Text(project.researchQuestion)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${project.discipline} • ${project.citationStyle}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onWorkspace(project.id) }) { Text("Workspace") }
                            OutlinedButton(onClick = { onOpenProject(project.id) }) { Text("Edit") }
                        }
                    }
                }
            }
            item {
                Text("Research workflow", style = MaterialTheme.typography.titleLarge)
                Text("Select a module and research paper to open its dedicated workspace.")
            }
            items(ResearchModules.all, key = { it.route }) { module ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(module.title, style = MaterialTheme.typography.titleMedium)
                            Text(module.description, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { onOpenModule(module.route) }) { Text("Open") }
                    }
                }
            }
            item {
                Text(
                    "Integrity note: AI suggestions are not evidence. Verify every source, claim, and citation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPaperScreen(
    onBack: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var discipline by remember { mutableStateOf("") }
    var citationStyle by remember { mutableStateOf("APA") }
    val styles = listOf("APA", "MLA", "Chicago", "IEEE", "Harvard", "Vancouver")

    Scaffold(topBar = { TopAppBar(title = { Text("New paper") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Start with a working title. Refine it as your research develops.")
            OutlinedTextField(
                title, { title = it }, label = { Text("Working title *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                question, { question = it }, label = { Text("Research question (optional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            OutlinedTextField(
                discipline, { discipline = it }, label = { Text("Discipline (optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Text("Citation style", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                styles.take(3).forEach { style ->
                    if (citationStyle == style) Button(onClick = { citationStyle = style }) {
                        Text(style)
                    } else OutlinedButton(onClick = { citationStyle = style }) { Text(style) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                styles.drop(3).forEach { style ->
                    if (citationStyle == style) Button(onClick = { citationStyle = style }) {
                        Text(style)
                    } else OutlinedButton(onClick = { citationStyle = style }) { Text(style) }
                }
            }
            Button(
                onClick = { onCreate(title, question, discipline, citationStyle) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create research project") }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            Text("Your project is saved locally on this device.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleScreen(
    title: String,
    description: String,
    projects: List<ResearchProject>,
    onSelectProject: (Long) -> Unit,
    onCreateProject: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(description)
                Text("Choose a paper to use this module's research tools.")
                if (projects.isEmpty()) Text("No projects yet. Create a paper to begin.")
                Button(onClick = onCreateProject) { Text("Create paper") }
            }
            items(projects, key = { it.id }) { project ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(project.title, style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { onSelectProject(project.id) }) { Text("Open $title") }
                    }
                }
            }
            item { OutlinedButton(onClick = onBack) { Text("Back") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectEditorScreen(
    project: ResearchProject,
    onBack: () -> Unit,
    onSave: (ResearchProject) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(project.id) { mutableStateOf(project.title) }
    var question by remember(project.id) { mutableStateOf(project.researchQuestion) }
    var discipline by remember(project.id) { mutableStateOf(project.discipline) }
    var citationStyle by remember(project.id) { mutableStateOf(project.citationStyle) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete research project?") },
            text = { Text("This removes the project from local storage and cannot be undone.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirmation = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Edit project") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { OutlinedTextField(title, { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(question, { question = it }, label = { Text("Research question") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            item { OutlinedTextField(discipline, { discipline = it }, label = { Text("Discipline") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Text("Citation style", style = MaterialTheme.typography.titleSmall)
                CitationStyleSelector(citationStyle, { citationStyle = it })
                Text("Bibliography formatting is a draft and must be checked.", style = MaterialTheme.typography.bodySmall)
            }
            item {
                Button(
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onSave(project.copy(
                            title = title.trim(),
                            researchQuestion = question.trim(),
                            discipline = discipline.trim().ifBlank { "Not set" },
                            citationStyle = citationStyle,
                            updatedAtEpochMillis = System.currentTimeMillis()
                        ))
                    }
                ) { Text("Save changes") }
            }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
            item { OutlinedButton(onClick = { showDeleteConfirmation = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete project") } }
        }
    }
}

@Composable
private fun CitationStyleSelector(value: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("APA", "MLA", "Chicago").forEach { style ->
            Row {
                androidx.compose.material3.RadioButton(selected = value == style, onClick = { onSelect(style) })
                Text(style, modifier = Modifier.padding(top = 12.dp))
            }
        }
        listOf("IEEE", "Harvard", "Vancouver").forEach { style ->
            Row {
                androidx.compose.material3.RadioButton(selected = value == style, onClick = { onSelect(style) })
                Text(style, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}
