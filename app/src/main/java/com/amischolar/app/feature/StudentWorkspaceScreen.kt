package com.amischolar.app.feature

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.amischolar.app.data.ResearchProject
import com.amischolar.app.data.ScholarDatabase
import com.amischolar.app.data.StudentActiveTimer
import com.amischolar.app.data.StudentCard
import com.amischolar.app.data.StudentCourse
import com.amischolar.app.data.StudentDeck
import com.amischolar.app.data.StudentNote
import com.amischolar.app.data.StudentStudySession
import com.amischolar.app.data.StudentTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONObject

private val studentTabs = listOf("Overview", "Courses", "Tasks", "Notes", "Flashcards", "Focus", "Calendar", "Lectures")

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StudentWorkspaceScreen(database: ScholarDatabase, onBack: () -> Unit) {
    val studentDao = database.studentWorkspaceDao()
    val projectDao = database.researchProjectDao()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val courses by studentDao.observeCourses().collectAsState(initial = emptyList())
    val tasks by studentDao.observeTasks().collectAsState(initial = emptyList())
    val notes by studentDao.observeNotes().collectAsState(initial = emptyList())
    val decks by studentDao.observeDecks().collectAsState(initial = emptyList())
    val cards by studentDao.observeCards().collectAsState(initial = emptyList())
    val reviews by studentDao.observeReviews().collectAsState(initial = emptyList())
    val sessions by studentDao.observeSessions().collectAsState(initial = emptyList())
    val timer by studentDao.observeActiveTimer().collectAsState(initial = null)
    val calendarEvents by studentDao.observeCalendarEvents().collectAsState(initial = emptyList())
    val activeTimer = timer
    val projects by projectDao.observeAll().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf("Overview") }
    var selectedCourse by remember { mutableStateOf<Long?>(null) }
    var selectedPaper by remember { mutableStateOf<Long?>(null) }
    var selectedTask by remember { mutableStateOf<Long?>(null) }
    var selectedDeck by remember { mutableStateOf<Long?>(null) }
    var courseName by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var taskTitle by remember { mutableStateOf("") }
    var taskDate by remember { mutableStateOf("") }
    var noteTitle by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var deckTitle by remember { mutableStateOf("") }
    var cardFront by remember { mutableStateOf("") }
    var cardBack by remember { mutableStateOf("") }
    var focusMinutes by remember { mutableStateOf("25") }
    var message by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<JSONObject?>(null) }
    var backupBytes by remember { mutableStateOf<ByteArray?>(null) }
    var encryptedAction by remember { mutableStateOf("") }
    var encryptedPassphrase by remember { mutableStateOf("") }
    var encryptedConfirm by remember { mutableStateOf("") }
    var encryptedImportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var fullBackupBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingFullRestore by remember { mutableStateOf<FullBackup.Preview?>(null) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val importIcs = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val content = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Could not open the selected calendar.") }
                val parsed = CalendarIcs.parse(content)
                withContext(Dispatchers.IO) { parsed.events.forEach { studentDao.upsertCalendarEvent(it.toRoom()) } }
                message = "${parsed.events.size} calendar event(s) imported locally." + if (parsed.warnings.isEmpty()) "" else " ${parsed.warnings.size} recurrence warning(s)."
            } catch (error: Exception) { message = "ICS import failed: ${error.message ?: "invalid calendar"}" }
        }
    }
    var pendingIcs by remember { mutableStateOf("") }
    val exportIcs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingIcs) } }
            .onSuccess { message = "Calendar exported." }.onFailure { message = "Calendar export failed: ${it.message}" }
    }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(backupBytes ?: error("No backup data")) } }
            .onSuccess { message = "Student backup exported." }.onFailure { message = "Backup export failed: ${it.message}" }
    }
    val exportFullBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(fullBackupBytes ?: error("No encrypted backup data")) } }
            .onSuccess { message = "Encrypted full backup exported." }.onFailure { message = "Encrypted backup export failed: ${it.message}" }
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { input ->
                    val output=java.io.ByteArrayOutputStream();val chunk=ByteArray(8192);var total=0
                    while(true){val count=input.read(chunk);if(count<0)break;total+=count;require(total<=EncryptedBackup.MAX_BYTES){"Backup exceeds 10 MB"};output.write(chunk,0,count)}
                    output.toByteArray()
                } ?: error("Could not open the selected backup.") }
                if (EncryptedBackup.isEnvelope(bytes)) { encryptedImportBytes=bytes; encryptedAction="import" }
                else pendingRestore = StudentWorkspaceBackup.decode(bytes)
            } catch (error: Exception) { message = "Backup validation failed: ${error.message ?: "invalid backup"}" }
        }
    }
    val dueCards = cards.filter { it.dueAtEpochMillis <= now && (selectedDeck == null || it.deckId == selectedDeck) }

    Scaffold(topBar = { TopAppBar(title = { Text("Student workspace") }, navigationIcon = { OutlinedButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = studentTabs.indexOf(selectedTab).coerceAtLeast(0)) {
                studentTabs.forEach { label -> Tab(selected = selectedTab == label, onClick = { selectedTab = label }, modifier = Modifier.testTag("student-tab-${label.lowercase()}"), text = { Text(label) }) }
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
                when (selectedTab) {
                    "Overview" -> {
                        Text("Your coursework and study progress stays on this device.", style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryCard("Courses", courses.size.toString())
                            SummaryCard("Open tasks", tasks.count { it.status != "done" }.toString())
                            SummaryCard("Cards due", dueCards.size.toString())
                        }
                        Text("Focus time: ${sessions.sumOf { it.durationSeconds } / 60} minutes · ${studyStreak(sessions)} day streak")
                        Text("Portable student backup", style = MaterialTheme.typography.titleMedium)
                        Text("Move courses, tasks, notes, flashcards, study logs and calendar events between web and Android.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { scope.launch { try { backupBytes = withContext(Dispatchers.IO) { StudentWorkspaceBackup.encode(studentDao.backupCourses(), studentDao.backupTasks(), studentDao.backupNotes(), studentDao.backupDecks(), studentDao.backupCards(), studentDao.backupReviews(), studentDao.backupSessions(), studentDao.backupActiveTimer(), studentDao.getCalendarEvents()) }; exportBackup.launch("am-i-scholar-student-backup.json") } catch (error: Exception) { message = "Backup export failed: ${error.message}" } } }) { Text("Export backup") }
                            OutlinedButton(onClick = { encryptedAction="export";encryptedPassphrase="";encryptedConfirm="" }) { Text("Export encrypted full backup") }
                            OutlinedButton(onClick = { importBackup.launch(arrayOf("application/json", "text/*")) }) { Text("Restore backup") }
                        }
                        tasks.filter { it.status != "done" }.take(5).forEach { Text("• ${it.title} · ${it.dueDate.ifBlank { "No due date" }}") }
                    }
                    "Courses" -> {
                        Text("Courses", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(courseName, { courseName = it }, label = { Text("Course name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(courseCode, { courseCode = it }, label = { Text("Course code") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(semester, { semester = it }, label = { Text("Semester") }, modifier = Modifier.fillMaxWidth())
                        Button(enabled = courseName.isNotBlank(), onClick = { scope.launch(Dispatchers.IO) { studentDao.upsertCourse(StudentCourse(name = courseName.trim(), code = courseCode.trim(), semester = semester.trim())) }; courseName = ""; courseCode = ""; semester = "" }) { Text("Add course") }
                        courses.forEach { course -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(course.name, style = MaterialTheme.typography.titleMedium); Text(listOf(course.code, course.semester).filter(String::isNotBlank).joinToString(" · ")); OutlinedButton(onClick = { scope.launch(Dispatchers.IO) { studentDao.deleteCourse(course) } }) { Text("Delete course") } } } }
                    }
                    "Tasks" -> {
                        Text("Tasks and assignments", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(taskTitle, { taskTitle = it }, label = { Text("Task title") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(taskDate, { taskDate = it }, label = { Text("Due date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                        SelectionPicker("Course", courses.map { it.id to it.name }, selectedCourse) { selectedCourse = it }
                        SelectionPicker("Research paper", projects.map { it.id to it.title }, selectedPaper) { selectedPaper = it }
                        Button(enabled = taskTitle.isNotBlank(), onClick = { scope.launch(Dispatchers.IO) { studentDao.upsertTask(StudentTask(title = taskTitle.trim(), dueDate = taskDate.trim(), courseId = selectedCourse, paperId = selectedPaper)) }; taskTitle = ""; taskDate = "" }) { Text("Add task") }
                        tasks.forEach { task -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(task.title, style = MaterialTheme.typography.titleMedium); Text("${task.dueDate.ifBlank { "No due date" }} · ${courses.firstOrNull { it.id == task.courseId }?.name ?: "No course"} · ${task.status}"); Row { OutlinedButton(onClick = { scope.launch(Dispatchers.IO) { studentDao.upsertTask(task.copy(status = if (task.status == "done") "todo" else "done", updatedAtEpochMillis = System.currentTimeMillis())) } }) { Text(if (task.status == "done") "Reopen" else "Complete") }; OutlinedButton(onClick = { scope.launch(Dispatchers.IO) { studentDao.deleteTask(task) } }) { Text("Delete") } } } } }
                    }
                    "Notes" -> {
                        Text("Local notes", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(noteTitle, { noteTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(noteText, { noteText = it }, label = { Text("Markdown note") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                        SelectionPicker("Course", courses.map { it.id to it.name }, selectedCourse) { selectedCourse = it }
                        SelectionPicker("Research paper", projects.map { it.id to it.title }, selectedPaper) { selectedPaper = it }
                        Button(enabled = noteTitle.isNotBlank(), onClick = { scope.launch(Dispatchers.IO) { studentDao.upsertNote(StudentNote(title = noteTitle.trim(), content = noteText, courseId = selectedCourse, paperId = selectedPaper)) }; noteTitle = ""; noteText = "" }) { Text("Save note") }
                        notes.forEach { Text("${it.title}\n${it.content}", style = MaterialTheme.typography.bodyMedium) }
                    }
                    "Flashcards" -> {
                        Text("Flashcards · ${reviews.size} reviews", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(deckTitle, { deckTitle = it }, label = { Text("New deck title") }, modifier = Modifier.fillMaxWidth())
                        Button(enabled = deckTitle.isNotBlank(), onClick = { scope.launch(Dispatchers.IO) { selectedDeck = studentDao.upsertDeck(StudentDeck(title = deckTitle.trim(), courseId = selectedCourse, paperId = selectedPaper)) }; deckTitle = "" }) { Text("Create deck") }
                        SelectionPicker("Deck", decks.map { it.id to it.title }, selectedDeck) { selectedDeck = it; revealed = false }
                        OutlinedTextField(cardFront, { cardFront = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(cardBack, { cardBack = it }, label = { Text("Answer") }, modifier = Modifier.fillMaxWidth())
                        Button(enabled = selectedDeck != null && cardFront.isNotBlank() && cardBack.isNotBlank(), onClick = { scope.launch(Dispatchers.IO) { studentDao.upsertCard(StudentCard(deckId = selectedDeck!!, front = cardFront.trim(), back = cardBack.trim())) }; cardFront = ""; cardBack = "" }) { Text("Add flashcard") }
                        val due = dueCards.firstOrNull()
                        if (due == null) Text("No cards due. Reviews are stored locally.") else {
                            Text(due.front, style = MaterialTheme.typography.titleMedium)
                            if (revealed) Text(due.back)
                            OutlinedButton(onClick = { revealed = true }) { Text("Reveal answer") }
                            if (revealed) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("again", "hard", "good", "easy").forEach { rating -> Button(onClick = { val updated = StudentScheduler.review(due, rating, now); scope.launch(Dispatchers.IO) { studentDao.upsertCard(updated); studentDao.insertReview(com.amischolar.app.data.StudentReview(cardId = due.id, rating = rating)) }; revealed = false }) { Text(rating.replaceFirstChar { it.uppercase() }) } }
                            }
                        }
                    }
                    "Focus" -> {
                        Text("Pomodoro focus · timer recovery is saved locally", style = MaterialTheme.typography.titleLarge)
                        SelectionPicker("Course", courses.map { it.id to it.name }, selectedCourse) { selectedCourse = it }
                        SelectionPicker("Research paper", projects.map { it.id to it.title }, selectedPaper) { selectedPaper = it }
                        SelectionPicker("Task", tasks.map { it.id to it.title }, selectedTask) { selectedTask = it }
                        if (activeTimer == null) {
                            OutlinedTextField(focusMinutes, { focusMinutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutes (1–180)") }, modifier = Modifier.fillMaxWidth())
                            Button(enabled = (focusMinutes.toIntOrNull() ?: 0) in 1..180, onClick = { val start = System.currentTimeMillis(); val seconds = focusMinutes.toLong() * 60; scope.launch(Dispatchers.IO) { studentDao.saveActiveTimer(StudentActiveTimer(startedAtEpochMillis = start, endsAtEpochMillis = start + seconds * 1000, plannedSeconds = seconds, courseId = selectedCourse, paperId = selectedPaper, taskId = selectedTask)) } }) { Text("Start focus") }
                        } else {
                            val runningTimer = requireNotNull(activeTimer)
                            val remaining = ((runningTimer.endsAtEpochMillis - (runningTimer.pausedAtEpochMillis ?: now) + 999) / 1000).coerceAtLeast(0)
                            Text("%02d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.headlineLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { val current = System.currentTimeMillis(); val updated = if (runningTimer.pausedAtEpochMillis == null) runningTimer.copy(pausedAtEpochMillis = current) else runningTimer.copy(endsAtEpochMillis = runningTimer.endsAtEpochMillis + current - runningTimer.pausedAtEpochMillis, pausedMillis = runningTimer.pausedMillis + current - runningTimer.pausedAtEpochMillis, pausedAtEpochMillis = null); scope.launch(Dispatchers.IO) { studentDao.saveActiveTimer(updated) } }) { Text(if (runningTimer.pausedAtEpochMillis == null) "Pause" else "Resume") }
                                OutlinedButton(onClick = { val end = System.currentTimeMillis(); val elapsed = (((runningTimer.pausedAtEpochMillis ?: end) - runningTimer.startedAtEpochMillis - runningTimer.pausedMillis) / 1000).coerceAtLeast(0); scope.launch(Dispatchers.IO) { studentDao.insertSession(StudentStudySession(startedAtEpochMillis = runningTimer.startedAtEpochMillis, endedAtEpochMillis = end, durationSeconds = elapsed, courseId = runningTimer.courseId, paperId = runningTimer.paperId, taskId = runningTimer.taskId)); studentDao.clearActiveTimer() } }) { Text("Finish & log") }
                            }
                        }
                        Text("${sessions.sumOf { it.durationSeconds } / 60} minutes logged · ${studyStreak(sessions)} day local streak")
                        val thisWeek = (0..6).map { offset -> LocalDate.now().minusDays((6 - offset).toLong()) }
                        thisWeek.forEach { day -> val seconds = sessions.filter { Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == day }.sumOf { it.durationSeconds }; Column { Text("${day.dayOfWeek.name.take(3)} · ${seconds / 60} min"); LinearProgressIndicator(progress = (seconds.toFloat() / 3600f).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth()) } }
                    }
                    "Lectures" -> LectureCaptureScreen(studentDao)
                    "Calendar" -> {
                        Text("Unified calendar · device timezone", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { importIcs.launch(arrayOf("text/calendar", "text/*", "application/octet-stream")) }) { Text("Import ICS") }
                            Button(onClick = {
                                val events = buildList {
                                    tasks.filter { it.dueDate.isNotBlank() }.forEach { task -> runCatching { val day = LocalDate.parse(task.dueDate); add(CalendarIcs.Event("task-${task.id}", task.title, task.description, true, day.toString(), day.plusDays(1).toString())) } }
                                    calendarEvents.forEach { add(CalendarIcs.Event(it.uid, it.title, it.description, it.allDay, it.startDate, it.endDateExclusive, it.startAtEpochMillis, it.endAtEpochMillis, it.timeZone, it.source)) }
                                    sessions.forEach { add(CalendarIcs.Event("focus-${it.id}", "Focus session", it.notes, false, startAtEpochMillis = it.startedAtEpochMillis, endAtEpochMillis = it.endedAtEpochMillis)) }
                                }
                                pendingIcs = CalendarIcs.serialize(events); exportIcs.launch("am-i-scholar-calendar.ics")
                            }) { Text("Export ICS") }
                        }
                        tasks.filter { it.dueDate.isNotBlank() }.forEach { Text("${it.dueDate} · ${it.title} · Task") }
                        calendarEvents.forEach { event -> Text((if (event.allDay) event.startDate else event.startAtEpochMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }.orEmpty()) + " · ${event.title} · Imported") }
                        sessions.forEach { session -> Text("${Instant.ofEpochMilli(session.startedAtEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)} · ${session.durationSeconds / 60} minutes focus") }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (encryptedAction == "export" || encryptedAction == "import") AlertDialog(
        onDismissRequest = { encryptedAction="";encryptedPassphrase="";encryptedConfirm="";encryptedImportBytes=null },
        title = { Text(if(encryptedAction=="export") "Encrypt full workspace backup" else "Decrypt full workspace backup") },
        text = { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(if(encryptedAction=="export") "The encrypted file includes research projects and student records, but excludes lecture audio. Keep the passphrase separately; it cannot be recovered." else "Enter the passphrase used to encrypt this backup.")
            OutlinedTextField(encryptedPassphrase,{encryptedPassphrase=it},label={Text("Passphrase")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
            if(encryptedAction=="export") OutlinedTextField(encryptedConfirm,{encryptedConfirm=it},label={Text("Confirm passphrase")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        } },
        confirmButton = { TextButton(onClick={
            if(encryptedPassphrase.length<12) message="Passphrase must be at least 12 characters."
            else if(encryptedAction=="export"&&encryptedPassphrase!=encryptedConfirm) message="Passphrases do not match."
            else if(encryptedAction=="export") {
                encryptedAction="export-ready"
                scope.launch {
                    try {
                        val plain=withContext(Dispatchers.IO) {
                            val student=StudentWorkspaceBackup.encode(studentDao.backupCourses(),studentDao.backupTasks(),studentDao.backupNotes(),studentDao.backupDecks(),studentDao.backupCards(),studentDao.backupReviews(),studentDao.backupSessions(),studentDao.backupActiveTimer(),studentDao.getCalendarEvents())
                            val workspace=database.researchWorkspaceDao()
                            val portable=projects.map { p ->
                                val ns=workspace.observeNotes(p.id).first()
                                val ss=workspace.observeSources(p.id).first()
                                val links=workspace.observeEvidenceLinks(p.id).first()
                                p.id.toString() to ProjectBackup.Snapshot(p,ns,ss,links)
                            }
                            FullBackup.encode(portable,student)
                        }
                        fullBackupBytes=withContext(Dispatchers.Default){EncryptedBackup.encrypt(plain,encryptedPassphrase)}
                        exportFullBackup.launch("am-i-scholar-full-backup-encrypted.json")
                    } catch(error:Exception){message="Encrypted backup export failed: ${error.message}";encryptedAction=""}
                }
            } else {
                val bytes=encryptedImportBytes
                encryptedAction=""
                scope.launch {
                    try {
                        val plain=withContext(Dispatchers.Default){EncryptedBackup.decrypt(requireNotNull(bytes),encryptedPassphrase)}
                        pendingFullRestore=withContext(Dispatchers.IO){FullBackup.validate(plain)}
                        message="Encrypted backup authenticated. Review its contents before restoring."
                    } catch(error:Exception){message="Decryption or validation failed; no data changed: ${error.message}"}
                    finally{encryptedPassphrase="";encryptedImportBytes=null}
                }
            }
        }) { Text(if(encryptedAction=="export") "Continue" else "Decrypt") } },
        dismissButton = { TextButton(onClick={encryptedAction="";encryptedPassphrase="";encryptedConfirm="";encryptedImportBytes=null}){Text("Cancel")} }
    )
    pendingFullRestore?.let { preview ->
        AlertDialog(
            onDismissRequest={pendingFullRestore=null},
            title={Text("Full backup restore preview")},
            text={Text("Import ${preview.projectCount} research projects (${preview.researchRecords} notes, sources and evidence links) as new projects, and replace the student workspace (${preview.studentRecords} records). Lecture recordings are excluded.")},
            confirmButton={TextButton(onClick={
                pendingFullRestore=null
                scope.launch {
                    try {
                        val result=withContext(Dispatchers.IO){FullBackup.restore(database,preview)}
                        message="Full backup restored: ${result.first} projects imported and student workspace restored."
                    } catch(error:Exception){message="Restore failed; transaction rolled back: ${error.message}"}
                }
            }){Text("Restore full backup")}},
            dismissButton={TextButton(onClick={pendingFullRestore=null}){Text("Cancel")}}
        )
    }
    pendingRestore?.let { data ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Replace student workspace?") },
            text = { Text("This validated backup will replace courses, tasks, notes, flashcards, study history and calendar events on this device. Research projects are unchanged.") },
            confirmButton = { TextButton(onClick = { pendingRestore = null; scope.launch { try { withContext(Dispatchers.IO) { StudentWorkspaceBackup.restore(database, data) }; message = "Student backup restored." } catch (error: Exception) { message = "Backup restore failed: ${error.message}" } } }) { Text("Replace student data") } },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SummaryCard(label: String, value: String) {
    Card(Modifier.width(105.dp)) { Column(Modifier.padding(10.dp)) { Text(label, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleLarge) } }
}

@Composable
private fun SelectionPicker(label: String, items: List<Pair<Long, String>>, selected: Long?, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = items.firstOrNull { it.first == selected }?.second ?: "None"
    androidx.compose.foundation.layout.Box {
        OutlinedButton(onClick = { expanded = true }) { Text("$label: $selectedName") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); expanded = false })
            items.forEach { item -> DropdownMenuItem(text = { Text(item.second) }, onClick = { onSelect(item.first); expanded = false }) }
        }
    }
}

internal object StudentScheduler {
    fun review(card: StudentCard, rating: String, now: Long): StudentCard {
        val quality = mapOf("again" to 1, "hard" to 3, "good" to 4, "easy" to 5)[rating] ?: error("Invalid rating")
        val ease = (card.easeFactor + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)).coerceAtLeast(1.3)
        val repetitions = if (quality < 3) 0 else card.repetitions + 1
        var interval = when { quality < 3 -> 10.0 / 1440.0; repetitions == 1 -> 1.0; repetitions == 2 -> 6.0; else -> (card.intervalDays * ease).coerceAtLeast(1.0) }
        if (rating == "hard") interval = (interval * 0.8).coerceAtLeast(1.0)
        return card.copy(repetitions = repetitions, easeFactor = ease, intervalDays = interval, dueAtEpochMillis = now + (interval * 86_400_000).toLong(), updatedAtEpochMillis = now)
    }
}

private fun studyStreak(sessions: List<StudentStudySession>): Int {
    val days = sessions.map { Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
    var day = LocalDate.now()
    if (day !in days) day = day.minusDays(1)
    var streak = 0
    while (day in days && streak < 365) { streak++; day = day.minusDays(1) }
    return streak
}
