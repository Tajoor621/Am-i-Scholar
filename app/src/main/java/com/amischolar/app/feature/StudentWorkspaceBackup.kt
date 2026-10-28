package com.amischolar.app.feature

import com.amischolar.app.data.*
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Versioned, platform-neutral student backup. IDs and references are strings; timestamps are ISO-8601. */
object StudentWorkspaceBackup {
    const val FORMAT = "am-i-scholar-student"
    const val VERSION = 1
    const val MAX_BYTES = 10 * 1024 * 1024
    private val arrays = listOf("courses", "tasks", "notes", "decks", "cards", "reviews", "sessions", "calendarEvents")

    fun encode(
        courses: List<StudentCourse>, tasks: List<StudentTask>, notes: List<StudentNote>,
        decks: List<StudentDeck>, cards: List<StudentCard>, reviews: List<StudentReview>,
        sessions: List<StudentStudySession>, timer: StudentActiveTimer?, events: List<StudentCalendarEvent>
    ): ByteArray {
        val root = JSONObject().put("format", FORMAT).put("version", VERSION)
            .put("exportedAt", Instant.now().toString())
        val data = JSONObject()
        fun arr(name: String, rows: List<JSONObject>) { data.put(name, JSONArray().also { a -> rows.forEach { a.put(it) } }) }
        arr("courses", courses.map { JSONObject().put("id", it.id.toString()).put("name", it.name).put("code", it.code).put("semester", it.semester).put("createdAt", iso(it.createdAtEpochMillis)).put("updatedAt", iso(it.updatedAtEpochMillis)) })
        arr("tasks", tasks.map { JSONObject().put("id", it.id.toString()).put("title", it.title).put("type", it.type).put("priority", it.priority).put("dueDate", it.dueDate).put("courseId", it.courseId?.toString()).put("paperId", it.paperId?.toString()).put("description", it.description).put("status", it.status).put("createdAt", iso(it.createdAtEpochMillis)).put("updatedAt", iso(it.updatedAtEpochMillis)) })
        arr("notes", notes.map { JSONObject().put("id", it.id.toString()).put("title", it.title).put("content", it.content).put("courseId", it.courseId?.toString()).put("paperId", it.paperId?.toString()).put("tags", it.tags).put("updatedAt", iso(it.updatedAtEpochMillis)) })
        arr("decks", decks.map { JSONObject().put("id", it.id.toString()).put("title", it.title).put("courseId", it.courseId?.toString()).put("paperId", it.paperId?.toString()).put("createdAt", iso(it.createdAtEpochMillis)) })
        arr("cards", cards.map { JSONObject().put("id", it.id.toString()).put("deckId", it.deckId.toString()).put("front", it.front).put("back", it.back).put("repetitions", it.repetitions).put("easeFactor", it.easeFactor).put("intervalDays", it.intervalDays).put("dueAt", iso(it.dueAtEpochMillis)).put("updatedAt", iso(it.updatedAtEpochMillis)) })
        arr("reviews", reviews.map { JSONObject().put("id", it.id.toString()).put("cardId", it.cardId.toString()).put("rating", it.rating).put("reviewedAt", iso(it.reviewedAtEpochMillis)) })
        arr("sessions", sessions.map { JSONObject().put("id", it.id.toString()).put("startedAt", iso(it.startedAtEpochMillis)).put("endedAt", iso(it.endedAtEpochMillis)).put("durationSeconds", it.durationSeconds).put("courseId", it.courseId?.toString()).put("paperId", it.paperId?.toString()).put("taskId", it.taskId?.toString()).put("notes", it.notes) })
        arr("calendarEvents", events.map { JSONObject().put("uid", it.uid).put("title", it.title).put("description", it.description).put("allDay", it.allDay).put("startDate", it.startDate).put("endDateExclusive", it.endDateExclusive).put("startAt", it.startAtEpochMillis?.let(::iso)).put("endAt", it.endAtEpochMillis?.let(::iso)).put("timeZone", it.timeZone).put("source", it.source) })
        data.put("activeTimer", timer?.let { JSONObject().put("startedAt", iso(it.startedAtEpochMillis)).put("endsAt", iso(it.endsAtEpochMillis)).put("plannedSeconds", it.plannedSeconds).put("pausedMillis", it.pausedMillis).put("pausedAt", it.pausedAtEpochMillis?.let(::iso)).put("courseId", it.courseId?.toString()).put("paperId", it.paperId?.toString()).put("taskId", it.taskId?.toString()) }) ?: JSONObject.NULL)
        root.put("data", data)
        return root.toString().toByteArray(Charsets.UTF_8).also { require(it.size in 1..MAX_BYTES) { "Backup is too large" } }
    }

    fun decode(bytes: ByteArray): JSONObject {
        require(bytes.size in 1..MAX_BYTES) { "Backup size is invalid" }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.optString("format") == FORMAT && root.optInt("version") == VERSION) { "Unsupported student backup format" }
        val data = root.getJSONObject("data")
        var count = 0
        for (name in arrays) {
            val rows = data.getJSONArray(name)
            count += rows.length()
            require(count <= 10000) { "Too many records" }
            val ids = HashSet<String>()
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val key = if (name == "calendarEvents") row.getString("uid") else row.getString("id")
                require(key.isNotBlank() && key.length <= 150 && ids.add(key)) { "Invalid or duplicate ID in $name" }
                row.keys().forEach { field -> val value = row.opt(field); if (value is String) require(value.length <= 2_000_000) { "Field too large in $name" } }
            }
        }
        val courses = data.getJSONArray("courses").ids("id")
        val tasks = data.getJSONArray("tasks").ids("id")
        val decks = data.getJSONArray("decks").ids("id")
        val cards = data.getJSONArray("cards").ids("id")
        fun references(name: String, field: String, allowed: Set<String>) {
            val rows = data.getJSONArray(name)
            for (i in 0 until rows.length()) {
                val value = rows.getJSONObject(i).optString(field).takeUnless { it == "" || it == "null" }
                require(value == null || value in allowed) { "Invalid $field reference in $name" }
            }
        }
        references("tasks", "courseId", courses); references("notes", "courseId", courses)
        references("decks", "courseId", courses); references("cards", "deckId", decks)
        references("reviews", "cardId", cards); references("sessions", "courseId", courses)
        references("sessions", "taskId", tasks)
        data.optJSONObject("activeTimer")?.let { timer ->
            for (field in listOf("courseId", "taskId")) {
                val value = timer.optString(field).takeUnless { it.isBlank() || it == "null" } ?: continue
                val allowed = if (field == "courseId") courses else tasks
                require(value in allowed) { "Invalid $field reference in activeTimer" }
            }
        }
        return data
    }

    suspend fun restore(database: ScholarDatabase, data: JSONObject) = database.withTransaction {
        val dao = database.studentWorkspaceDao()
        val projectDao = database.researchProjectDao()
        dao.clearSessions(); dao.clearActiveTimer(); dao.clearTasks(); dao.clearNotes()
        dao.clearReviews(); dao.clearCards(); dao.clearDecks(); dao.clearCourses(); dao.clearCalendarEvents()
        val courses = mutableMapOf<String, Long>()
        val tasks = mutableMapOf<String, Long>()
        val decks = mutableMapOf<String, Long>()
        val cards = mutableMapOf<String, Long>()
        fun rows(name: String) = data.getJSONArray(name)
        fun optRef(row: JSONObject, key: String, map: Map<String, Long>): Long? =
            row.optString(key).takeUnless { it.isBlank() || it == "null" }?.let { map[it] }
        suspend fun paperRef(row: JSONObject): Long? {
            val id = nullableId(row,"paperId") ?: return null
            return if (projectDao.findById(id) != null) id else null
        }
        for (i in 0 until rows("courses").length()) {
            val x = rows("courses").getJSONObject(i)
            courses[x.getString("id")] = dao.insertBackupCourse(StudentCourse(name=x.getString("name"), code=x.optString("code"), semester=x.optString("semester"), createdAtEpochMillis=millis(x,"createdAt"), updatedAtEpochMillis=millis(x,"updatedAt")))
        }
        for (i in 0 until rows("tasks").length()) {
            val x = rows("tasks").getJSONObject(i)
            tasks[x.getString("id")] = dao.insertBackupTask(StudentTask(title=x.getString("title"), type=x.optString("type","todo"), priority=x.optInt("priority",2), dueDate=x.optString("dueDate"), courseId=optRef(x,"courseId",courses), paperId=paperRef(x), description=x.optString("description"), status=x.optString("status","todo"), createdAtEpochMillis=millis(x,"createdAt"), updatedAtEpochMillis=millis(x,"updatedAt")))
        }
        for (i in 0 until rows("notes").length()) {
            val x = rows("notes").getJSONObject(i)
            dao.insertBackupNote(StudentNote(title=x.getString("title"), content=x.optString("content"), courseId=optRef(x,"courseId",courses), paperId=paperRef(x), tags=x.optString("tags"), updatedAtEpochMillis=millis(x,"updatedAt")))
        }
        for (i in 0 until rows("decks").length()) {
            val x = rows("decks").getJSONObject(i)
            decks[x.getString("id")] = dao.insertBackupDeck(StudentDeck(title=x.getString("title"), courseId=optRef(x,"courseId",courses), paperId=paperRef(x), createdAtEpochMillis=millis(x,"createdAt")))
        }
        for (i in 0 until rows("cards").length()) {
            val x = rows("cards").getJSONObject(i)
            val deckId = optRef(x,"deckId",decks) ?: error("Flashcard deck reference could not be restored")
            cards[x.getString("id")] = dao.insertBackupCard(StudentCard(deckId=deckId, front=x.getString("front"), back=x.getString("back"), repetitions=x.optInt("repetitions"), easeFactor=x.optDouble("easeFactor",2.5), intervalDays=x.optDouble("intervalDays"), dueAtEpochMillis=millis(x,"dueAt"), updatedAtEpochMillis=millis(x,"updatedAt")))
        }
        for (i in 0 until rows("reviews").length()) {
            val x = rows("reviews").getJSONObject(i)
            val cardId = optRef(x,"cardId",cards) ?: error("Flashcard review reference could not be restored")
            dao.insertBackupReview(StudentReview(cardId=cardId, rating=x.getString("rating"), reviewedAtEpochMillis=millis(x,"reviewedAt")))
        }
        for (i in 0 until rows("sessions").length()) {
            val x = rows("sessions").getJSONObject(i)
            dao.insertBackupSession(StudentStudySession(startedAtEpochMillis=millis(x,"startedAt"), endedAtEpochMillis=millis(x,"endedAt"), durationSeconds=x.optLong("durationSeconds"), courseId=optRef(x,"courseId",courses), paperId=paperRef(x), taskId=optRef(x,"taskId",tasks), notes=x.optString("notes")))
        }
        val timer = data.optJSONObject("activeTimer")
        if (timer != null) dao.saveActiveTimer(StudentActiveTimer(startedAtEpochMillis=millis(timer,"startedAt"), endsAtEpochMillis=millis(timer,"endsAt"), plannedSeconds=timer.optLong("plannedSeconds"), pausedMillis=timer.optLong("pausedMillis"), pausedAtEpochMillis=if (timer.isNull("pausedAt")) null else millis(timer,"pausedAt"), courseId=optRef(timer,"courseId",courses), paperId=paperRef(timer), taskId=optRef(timer,"taskId",tasks)))
        for (i in 0 until rows("calendarEvents").length()) {
            val x = rows("calendarEvents").getJSONObject(i)
            dao.upsertCalendarEvent(StudentCalendarEvent(uid=x.getString("uid"), title=x.getString("title"), description=x.optString("description"), allDay=x.optBoolean("allDay"), startDate=x.optString("startDate"), endDateExclusive=x.optString("endDateExclusive"), startAtEpochMillis=if (x.isNull("startAt")) null else millis(x,"startAt"), endAtEpochMillis=if (x.isNull("endAt")) null else millis(x,"endAt"), timeZone=x.optString("timeZone"), source=x.optString("source","local")))
        }
    }

    private fun JSONArray.ids(field: String): Set<String> = (0 until length()).map { getJSONObject(it).getString(field) }.toSet()
    private fun iso(epochMillis: Long) = Instant.ofEpochMilli(epochMillis).toString()
    fun millis(value: JSONObject, key: String, fallback: Long = System.currentTimeMillis()): Long =
        value.optString(key).takeUnless { it.isNullOrBlank() || it == "null" }?.let { Instant.parse(it).toEpochMilli() } ?: fallback
    fun nullableId(value: JSONObject, key: String): Long? = value.optString(key).takeUnless { it.isNullOrBlank() || it == "null" }?.toLongOrNull()
}
