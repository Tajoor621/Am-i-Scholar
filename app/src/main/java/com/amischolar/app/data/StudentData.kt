package com.amischolar.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "student_courses")
data class StudentCourse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val semester: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "student_tasks",
    foreignKeys = [
        ForeignKey(entity = StudentCourse::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ResearchProject::class, parentColumns = ["id"], childColumns = ["paperId"], onDelete = ForeignKey.SET_NULL)
    ], indices = [Index("courseId"), Index("paperId"), Index("dueDate")]
)
data class StudentTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String = "todo",
    val priority: Int = 2,
    val dueDate: String = "",
    val courseId: Long? = null,
    val paperId: Long? = null,
    val description: String = "",
    val status: String = "todo",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "student_notes",
    foreignKeys = [
        ForeignKey(entity = StudentCourse::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ResearchProject::class, parentColumns = ["id"], childColumns = ["paperId"], onDelete = ForeignKey.SET_NULL)
    ], indices = [Index("courseId"), Index("paperId")]
)
data class StudentNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    val courseId: Long? = null,
    val paperId: Long? = null,
    val tags: String = "",
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "student_decks",
    foreignKeys = [
        ForeignKey(entity = StudentCourse::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ResearchProject::class, parentColumns = ["id"], childColumns = ["paperId"], onDelete = ForeignKey.SET_NULL)
    ], indices = [Index("courseId"), Index("paperId")]
)
data class StudentDeck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val courseId: Long? = null,
    val paperId: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "student_cards", foreignKeys = [ForeignKey(entity = StudentDeck::class, parentColumns = ["id"], childColumns = ["deckId"], onDelete = ForeignKey.CASCADE)], indices = [Index("deckId"), Index("dueAtEpochMillis")])
data class StudentCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    val repetitions: Int = 0,
    val easeFactor: Double = 2.5,
    val intervalDays: Double = 0.0,
    val dueAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "student_reviews", foreignKeys = [ForeignKey(entity = StudentCard::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE)], indices = [Index("cardId"), Index("reviewedAtEpochMillis")])
data class StudentReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val rating: String,
    val reviewedAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "student_sessions",
    foreignKeys = [
        ForeignKey(entity = StudentCourse::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ResearchProject::class, parentColumns = ["id"], childColumns = ["paperId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = StudentTask::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.SET_NULL)
    ], indices = [Index("courseId"), Index("paperId"), Index("taskId"), Index("startedAtEpochMillis")]
)
data class StudentStudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationSeconds: Long,
    val courseId: Long? = null,
    val paperId: Long? = null,
    val taskId: Long? = null,
    val notes: String = ""
)

@Entity(
    tableName = "student_active_timer",
    foreignKeys = [
        ForeignKey(entity = StudentCourse::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ResearchProject::class, parentColumns = ["id"], childColumns = ["paperId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = StudentTask::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.SET_NULL)
    ], indices = [Index("courseId"), Index("paperId"), Index("taskId")]
)
data class StudentActiveTimer(
    @PrimaryKey val id: Int = 1,
    val startedAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val plannedSeconds: Long,
    val pausedMillis: Long = 0,
    val pausedAtEpochMillis: Long? = null,
    val courseId: Long? = null,
    val paperId: Long? = null,
    val taskId: Long? = null
)

@Entity(tableName = "student_calendar_events", indices = [Index("startDate"), Index("startAtEpochMillis")])
data class StudentCalendarEvent(
    @PrimaryKey val uid: String,
    val title: String,
    val description: String = "",
    val allDay: Boolean,
    val startDate: String = "",
    val endDateExclusive: String = "",
    val startAtEpochMillis: Long? = null,
    val endAtEpochMillis: Long? = null,
    val timeZone: String = "",
    val source: String = "local"
)

@Dao
interface StudentWorkspaceDao {
    @Query("SELECT * FROM student_courses ORDER BY id") suspend fun backupCourses(): List<StudentCourse>
    @Query("SELECT * FROM student_tasks ORDER BY id") suspend fun backupTasks(): List<StudentTask>
    @Query("SELECT * FROM student_notes ORDER BY id") suspend fun backupNotes(): List<StudentNote>
    @Query("SELECT * FROM student_decks ORDER BY id") suspend fun backupDecks(): List<StudentDeck>
    @Query("SELECT * FROM student_cards ORDER BY id") suspend fun backupCards(): List<StudentCard>
    @Query("SELECT * FROM student_reviews ORDER BY id") suspend fun backupReviews(): List<StudentReview>
    @Query("SELECT * FROM student_sessions ORDER BY id") suspend fun backupSessions(): List<StudentStudySession>
    @Query("SELECT * FROM student_active_timer WHERE id=1") suspend fun backupActiveTimer(): StudentActiveTimer?
    @Query("SELECT * FROM student_courses ORDER BY name COLLATE NOCASE") fun observeCourses(): Flow<List<StudentCourse>>
    @Query("SELECT * FROM student_courses ORDER BY name COLLATE NOCASE") suspend fun getCourses(): List<StudentCourse>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCourse(course: StudentCourse): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBackupCourse(course: StudentCourse): Long
    @Delete suspend fun deleteCourse(course: StudentCourse)

    @Query("SELECT * FROM student_tasks ORDER BY CASE WHEN dueDate='' THEN '9999-12-31' ELSE dueDate END, priority") fun observeTasks(): Flow<List<StudentTask>>
    @Query("SELECT * FROM student_tasks ORDER BY dueDate, priority") suspend fun getTasks(): List<StudentTask>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertTask(task: StudentTask): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBackupTask(task: StudentTask): Long
    @Delete suspend fun deleteTask(task: StudentTask)

    @Query("SELECT * FROM student_notes ORDER BY updatedAtEpochMillis DESC") fun observeNotes(): Flow<List<StudentNote>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertNote(note: StudentNote): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBackupNote(note: StudentNote): Long
    @Delete suspend fun deleteNote(note: StudentNote)

    @Query("SELECT * FROM student_decks ORDER BY title COLLATE NOCASE") fun observeDecks(): Flow<List<StudentDeck>>
    @Query("SELECT * FROM student_cards ORDER BY dueAtEpochMillis") fun observeCards(): Flow<List<StudentCard>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDeck(deck: StudentDeck): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCard(card: StudentCard): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBackupDeck(deck: StudentDeck): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBackupCard(card: StudentCard): Long
    @Insert suspend fun insertReview(review: StudentReview): Long
    @Insert suspend fun insertBackupReview(review: StudentReview): Long
    @Query("SELECT * FROM student_reviews ORDER BY reviewedAtEpochMillis DESC") fun observeReviews(): Flow<List<StudentReview>>

    @Query("SELECT * FROM student_sessions ORDER BY startedAtEpochMillis DESC") fun observeSessions(): Flow<List<StudentStudySession>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSession(session: StudentStudySession): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBackupSession(session: StudentStudySession): Long
    @Query("SELECT * FROM student_active_timer WHERE id=1") fun observeActiveTimer(): Flow<StudentActiveTimer?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveActiveTimer(timer: StudentActiveTimer)
    @Query("DELETE FROM student_courses") suspend fun clearCourses()
    @Query("DELETE FROM student_tasks") suspend fun clearTasks()
    @Query("DELETE FROM student_notes") suspend fun clearNotes()
    @Query("DELETE FROM student_decks") suspend fun clearDecks()
    @Query("DELETE FROM student_cards") suspend fun clearCards()
    @Query("DELETE FROM student_reviews") suspend fun clearReviews()
    @Query("DELETE FROM student_sessions") suspend fun clearSessions()
    @Query("DELETE FROM student_active_timer WHERE id=1") suspend fun clearActiveTimer()

    @Query("SELECT * FROM student_calendar_events ORDER BY CASE WHEN allDay=1 THEN startDate ELSE date(startAtEpochMillis/1000,'unixepoch','localtime') END") fun observeCalendarEvents(): Flow<List<StudentCalendarEvent>>
    @Query("SELECT * FROM student_calendar_events") suspend fun getCalendarEvents(): List<StudentCalendarEvent>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCalendarEvent(event: StudentCalendarEvent)

    @Transaction suspend fun replaceCalendarEvents(events: List<StudentCalendarEvent>) {
        clearCalendarEvents()
        events.forEach { upsertCalendarEvent(it) }
    }
    @Query("DELETE FROM student_calendar_events") suspend fun clearCalendarEvents()
}
