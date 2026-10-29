package com.amischolar.app.feature

import androidx.room.withTransaction
import com.amischolar.app.data.EvidenceLink
import com.amischolar.app.data.ResearchProject
import com.amischolar.app.data.ScholarDatabase
import org.json.JSONArray
import org.json.JSONObject

/** Full research + student payload shared by web and Android before encryption. */
object FullBackup {
    const val FORMAT = "am-i-scholar-full"
    const val VERSION = 1
    data class ProjectItem(val sourceId: String, val snapshot: ProjectBackup.Snapshot)
    data class Preview(
        val payload: JSONObject,
        val projects: List<ProjectItem>,
        val student: JSONObject,
        val projectCount: Int,
        val researchRecords: Int,
        val studentRecords: Int
    )

    fun encode(projects: List<Pair<String, ProjectBackup.Snapshot>>, studentBytes: ByteArray): ByteArray {
        val student = JSONObject(studentBytes.toString(Charsets.UTF_8))
        require(student.optString("format") == StudentWorkspaceBackup.FORMAT) { "Student backup is invalid" }
        val projectArray = JSONArray()
        projects.forEach { (sourceId, snapshot) ->
            projectArray.put(JSONObject().put("sourceId", sourceId)
                .put("backup", JSONObject(ProjectBackup.encode(snapshot).toString(Charsets.UTF_8))))
        }
        return JSONObject().put("format", FORMAT).put("version", VERSION)
            .put("androidProjects", projectArray).put("student", student)
            .toString().toByteArray(Charsets.UTF_8)
            .also { require(it.size <= EncryptedBackup.MAX_BYTES) { "Full backup exceeds 10 MB" } }
    }

    fun validate(bytes: ByteArray): Preview {
        require(bytes.size in 1..EncryptedBackup.MAX_BYTES) { "Backup size is invalid" }
        val payload = JSONObject(bytes.toString(Charsets.UTF_8))
        require(payload.optString("format") == FORMAT && payload.optInt("version") == VERSION) { "Unsupported full backup format" }
        val rows = payload.getJSONArray("androidProjects")
        require(rows.length() <= ProjectBackup.MAX_RECORDS) { "Too many projects" }
        val sourceIds = HashSet<String>()
        val projects = (0 until rows.length()).map { index ->
            val entry = rows.getJSONObject(index)
            val sourceId = entry.optString("sourceId", "portable-$index")
            require(sourceId.length <= 150 && sourceIds.add(sourceId)) { "Invalid or duplicate project reference" }
            val backup = entry.optJSONObject("backup") ?: entry
            val snapshot = ProjectBackup.decode(backup.toString().toByteArray(Charsets.UTF_8))
            ProjectItem(sourceId, snapshot)
        }
        val studentRoot = payload.getJSONObject("student")
        val studentData = StudentWorkspaceBackup.decode(studentRoot.toString().toByteArray(Charsets.UTF_8))
        fun validatePaperRefs(name: String) {
            val rows = studentData.optJSONArray(name) ?: return
            for (i in 0 until rows.length()) {
                val paperId = rows.getJSONObject(i).optString("paperId").takeUnless { it.isBlank() || it == "null" } ?: continue
                require(paperId in sourceIds) { "Invalid paperId reference in $name" }
            }
        }
        listOf("tasks", "notes", "decks", "sessions").forEach(::validatePaperRefs)
        studentData.optJSONObject("activeTimer")?.optString("paperId")
            ?.takeUnless { it.isBlank() || it == "null" }
            ?.let { require(it in sourceIds) { "Invalid paperId reference in active timer" } }
        val researchCount = projects.sumOf { it.snapshot.notes.size + it.snapshot.sources.size + it.snapshot.links.size }
        require(researchCount + projects.size <= ProjectBackup.MAX_RECORDS) { "Too many research records" }
        val studentCount = listOf("courses","tasks","notes","decks","cards","reviews","sessions","calendarEvents")
            .sumOf { studentData.optJSONArray(it)?.length() ?: 0 }
        return Preview(payload, projects, studentData, projects.size, researchCount, studentCount)
    }

    /** All sections are decoded before this starts; Room rolls back the complete restore on failure. */
    suspend fun restore(database: ScholarDatabase, preview: Preview): Pair<Int, Int> = database.withTransaction {
        val projectDao = database.researchProjectDao()
        val workspaceDao = database.researchWorkspaceDao()
        val idMap = mutableMapOf<String, Long>()
        preview.projects.forEach { item ->
            val snapshot = item.snapshot
            val id = projectDao.insert(snapshot.project.copy(id = 0))
            idMap[item.sourceId] = id
            val noteIds = snapshot.notes.map { workspaceDao.insertNote(it.copy(id = 0, projectId = id)) }
            val sourceIds = snapshot.sources.map { workspaceDao.insertSource(it.copy(id = 0, projectId = id)) }
            snapshot.links.forEach { link ->
                workspaceDao.insertEvidenceLink(EvidenceLink(projectId = id,
                    claimNoteId = noteIds[link.noteIndex], sourceId = sourceIds[link.sourceIndex],
                    status = link.status, rationale = link.rationale))
            }
        }
        val student = JSONObject(preview.student.toString())
        fun remapPaperRows(name: String) {
            val rows = student.optJSONArray(name) ?: return
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val old = row.optString("paperId").takeUnless { it.isBlank() || it == "null" } ?: continue
                val mapped = idMap[old]
                if (mapped == null) row.put("paperId", JSONObject.NULL) else row.put("paperId", mapped.toString())
            }
        }
        listOf("tasks","notes","decks","sessions").forEach(::remapPaperRows)
        student.optJSONObject("activeTimer")?.let { timer ->
            val old = timer.optString("paperId").takeUnless { it.isBlank() || it == "null" }
            if (old != null) timer.put("paperId", idMap[old]?.toString() ?: JSONObject.NULL)
        }
        StudentWorkspaceBackup.restore(database, student)
        preview.projectCount to preview.studentRecords
    }
}
