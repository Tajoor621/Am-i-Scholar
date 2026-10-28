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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val studentTabs = listOf("Overview", "Courses", "Tasks", "Notes", "Flashcards", "Focus", "Calendar")

@Composable
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
    val now by produceState(System.currentTimeMillis(), timer) {
        while (true) { value = System.currentTimeMillis(); delay(1000) }
    }
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
    val dueCards = cards.filter { it.dueAtEpochMillis <= now && (selectedDeck == null || it.deckId == selectedDeck) }

    Scaffold(topBar = { TopAppBar(title = { Text("Student workspace") }, navigationIcon = { OutlinedButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = studentTabs.indexOf(selectedTab).coerceAtLeast(0)) {
                studentTabs.forEach { label -> Tab(selected = selectedTab == label, onClick = { selectedTab = label }, text = { Text(label) }) }
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
                        if (timer == null) {
                            OutlinedTextField(focusMinutes, { focusMinutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutes (1–180)") }, modifier = Modifier.fillMaxWidth())
                            Button(enabled = (focusMinutes.toIntOrNull() ?: 0) in 1..180, onClick = { val start = System.currentTimeMillis(); val seconds = focusMinutes.toLong() * 60; scope.launch(Dispatchers.IO) { studentDao.saveActiveTimer(StudentActiveTimer(startedAtEpochMillis = start, endsAtEpochMillis = start + seconds * 1000, plannedSeconds = seconds, courseId = selectedCourse, paperId = selectedPaper, taskId = selectedTask)) } }) { Text("Start focus") }
                        } else {
                            val remaining = ((timer.endsAtEpochMillis - (timer.pausedAtEpochMillis ?: now) + 999) / 1000).coerceAtLeast(0)
                            Text("%02d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.headlineLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { val active = timer; val current = System.currentTimeMillis(); val updated = if (active.pausedAtEpochMillis == null) active.copy(pausedAtEpochMillis = current) else active.copy(endsAtEpochMillis = active.endsAtEpochMillis + current - active.pausedAtEpochMillis, pausedMillis = active.pausedMillis + current - active.pausedAtEpochMillis, pausedAtEpochMillis = null); scope.launch(Dispatchers.IO) { studentDao.saveActiveTimer(updated) } }) { Text(if (timer.pausedAtEpochMillis == null) "Pause" else "Resume") }
                                OutlinedButton(onClick = { val active = timer; val end = System.currentTimeMillis(); val elapsed = (((active.pausedAtEpochMillis ?: end) - active.startedAtEpochMillis - active.pausedMillis) / 1000).coerceAtLeast(0); scope.launch(Dispatchers.IO) { studentDao.insertSession(StudentStudySession(startedAtEpochMillis = active.startedAtEpochMillis, endedAtEpochMillis = end, durationSeconds = elapsed, courseId = active.courseId, paperId = active.paperId, taskId = active.taskId)); studentDao.clearActiveTimer() } }) { Text("Finish & log") }
                            }
                        }
                        Text("${sessions.sumOf { it.durationSeconds } / 60} minutes logged · ${studyStreak(sessions)} day local streak")
                        val thisWeek = (0..6).map { offset -> LocalDate.now().minusDays((6 - offset).toLong()) }
                        thisWeek.forEach { day -> val seconds = sessions.filter { Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == day }.sumOf { it.durationSeconds }; Column { Text("${day.dayOfWeek.name.take(3)} · ${seconds / 60} min"); LinearProgressIndicator(progress = (seconds.toFloat() / 3600f).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth()) } }
                    }
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
