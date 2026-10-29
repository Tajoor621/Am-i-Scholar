package com.amischolar.app.feature

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amischolar.app.data.StudentLecture
import com.amischolar.app.data.StudentWorkspaceDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun LectureCaptureScreen(dao: StudentWorkspaceDao) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val lectures by dao.observeLectures().collectAsState(initial = emptyList())
    var title by remember { mutableStateOf("") }
    var pendingTitle by remember { mutableStateOf("") }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingId by remember { mutableStateOf("") }
    var recordingStarted by remember { mutableStateOf(0L) }
    var isRecording by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var message by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<StudentLecture?>(null) }
    var pendingExport by remember { mutableStateOf<File?>(null) }
    var pendingText by remember { mutableStateOf("") }

    fun finishRecording() {
        val active = recorder ?: return
        val file = recordingFile
        val id = recordingId
        val started = recordingStarted
        isRecording = false
        recorder = null
        try { active.stop() } catch (_: RuntimeException) { message = "The recording ended before audio could be finalized." }
        runCatching { active.reset() }
        active.release()
        if (file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            message = "No audio was saved. Check microphone access and available device storage."
            return
        }
        val row = StudentLecture(
            id = id,
            title = pendingTitle.ifBlank { "Lecture recording" },
            fileName = file.name,
            createdAtEpochMillis = started,
            durationSeconds = ((System.currentTimeMillis() - started) / 1000).coerceAtLeast(0)
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dao.upsertLecture(row)
                message = "Recording saved in this app on this device. No audio was uploaded."
            } catch (error: Exception) {
                file.delete()
                message = "Could not save lecture details: ${error.message ?: "storage error"}"
            }
        }
    }
    val finishLatest by rememberUpdatedState(finishRecording)
    val requestMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try {
                val id = UUID.randomUUID().toString()
                val file = File(context.filesDir, "lecture-$id.m4a")
                val media = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
                media.setAudioSource(MediaRecorder.AudioSource.MIC)
                media.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                media.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                media.setOutputFile(file.absolutePath)
                media.setMaxDuration(15 * 60 * 1000)
                media.setMaxFileSize(100L * 1024L * 1024L)
                media.setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                        what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) finishLatest()
                }
                media.prepare()
                media.start()
                recordingId = id
                recordingFile = file
                recordingStarted = System.currentTimeMillis()
                recorder = media
                isRecording = true
                message = "Recording is active. Stop is always available below."
            } catch (error: Exception) {
                runCatching { File(context.filesDir, "lecture-$recordingId.m4a").delete() }
                message = "Could not start recording: ${error.message ?: "microphone unavailable"}"
            }
        } else {
            message = "Microphone permission was denied. Allow it in Android settings to record."
        }
    }
    val exportAudio = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")) { uri ->
        val file = pendingExport
        if (uri != null && file != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } }
                        ?: error("Could not open export destination")
                }
            }.onSuccess { message = "Audio exported to the destination you selected." }
                .onFailure { message = "Audio export failed: ${it.message}" }
        }
    }
    val exportTranscript = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingText) }
                        ?: error("Could not open export destination")
                }
            }.onSuccess { message = "Transcript exported to the destination you selected." }
                .onFailure { message = "Transcript export failed: ${it.message}" }
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) { now = System.currentTimeMillis(); delay(1000) }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) finishLatest()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            finishLatest()
        }
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Lecture recordings", style = MaterialTheme.typography.titleLarge)
        Text("Audio stays in this app's private storage on this device. Microphone permission is requested only when you start. Nothing is uploaded or synced.")
        Text("Transcripts are editable and local. Add or paste a transcript manually; speech services are not used.")
        OutlinedTextField(title, { title = it }, label = { Text("Lecture title") }, modifier = Modifier.fillMaxWidth(), enabled = !isRecording)
        if (isRecording) {
            val seconds = ((now - recordingStarted) / 1000).coerceAtLeast(0)
            val time = "%02d:%02d".format(seconds / 60, seconds % 60)
            Text("● RECORDING · $time · microphone active", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            Button(onClick = ::finishRecording) { Text("Stop and save") }
        } else {
            Button(enabled = title.isNotBlank(), onClick = { pendingTitle = title.trim(); requestMic.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Start recording") }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)

        if (lectures.isEmpty()) Text("No recordings saved yet.")
        lectures.forEach { lecture ->
            var editTitle by remember(lecture.id, lecture.title) { mutableStateOf(lecture.title) }
            var transcript by remember(lecture.id, lecture.transcript) { mutableStateOf(lecture.transcript) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${lecture.createdAtEpochMillis.let { java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it)) }} · ${lecture.durationSeconds / 60}:${(lecture.durationSeconds % 60).toString().padStart(2, '0')}")
                    OutlinedTextField(editTitle, { editTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(transcript, { transcript = it }, label = { Text("Editable transcript") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            val file = File(context.filesDir, lecture.fileName)
                            runCatching { MediaPlayer.create(context, file).also { player -> player?.setOnCompletionListener { it.release() }; player?.start() } }
                                .onFailure { message = "Playback failed: ${it.message}" }
                        }) { Text("Play") }
                        OutlinedButton(onClick = { pendingExport = File(context.filesDir, lecture.fileName); exportAudio.launch(safeName(lecture.title) + ".m4a") }) { Text("Export audio") }
                        OutlinedButton(onClick = { pendingText = transcript; exportTranscript.launch(safeName(editTitle) + "-transcript.txt") }) { Text("Export transcript") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            scope.launch { runCatching { withContext(Dispatchers.IO) { dao.upsertLecture(lecture.copy(title = editTitle.trim().ifBlank { lecture.title }, transcript = transcript)) } }
                                .onSuccess { message = "Lecture and transcript saved locally." }
                                .onFailure { message = "Could not save changes: ${it.message}" } }
                        }) { Text("Save changes") }
                        TextButton(onClick = { deleting = lecture }) { Text("Delete") }
                    }
                }
            }
        }
    }
    deleting?.let { lecture ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete lecture?") },
            text = { Text("This removes the saved audio and transcript from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                dao.deleteLecture(lecture.id)
                                File(context.filesDir, lecture.fileName).delete()
                            }
                        }.onSuccess { message = "Lecture deleted from this device." }
                            .onFailure { message = "Could not delete lecture: ${it.message}" }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") }
            }
        )
    }
}

private fun safeName(value: String): String =
    value.replace(Regex("[^A-Za-z0-9_-]+"), "-").trim('-').take(60).ifBlank { "lecture" }
