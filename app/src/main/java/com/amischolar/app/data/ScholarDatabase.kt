package com.amischolar.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ResearchProject::class, ResearchNote::class, ResearchSource::class, EvidenceLink::class, StudentCourse::class, StudentTask::class, StudentNote::class, StudentDeck::class, StudentCard::class, StudentReview::class, StudentStudySession::class, StudentActiveTimer::class, StudentCalendarEvent::class], version = 4, exportSchema = false)
abstract class ScholarDatabase : RoomDatabase() {
    abstract fun researchProjectDao(): ResearchProjectDao
    abstract fun researchWorkspaceDao(): ResearchWorkspaceDao
    abstract fun studentWorkspaceDao(): StudentWorkspaceDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS research_notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, kind TEXT NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_research_notes_projectId ON research_notes (projectId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS research_sources (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, title TEXT NOT NULL, authors TEXT NOT NULL, year TEXT NOT NULL, doiOrUrl TEXT NOT NULL, publication TEXT NOT NULL, evidenceNotes TEXT NOT NULL, verified INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_research_sources_projectId ON research_sources (projectId)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS evidence_links (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, claimNoteId INTEGER NOT NULL, sourceId INTEGER NOT NULL, status TEXT NOT NULL, rationale TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(claimNoteId) REFERENCES research_notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(sourceId) REFERENCES research_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_links_projectId ON evidence_links (projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_links_claimNoteId ON evidence_links (claimNoteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_links_sourceId ON evidence_links (sourceId)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS student_courses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, code TEXT NOT NULL, semester TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS student_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, type TEXT NOT NULL, priority INTEGER NOT NULL, dueDate TEXT NOT NULL, courseId INTEGER, paperId INTEGER, description TEXT NOT NULL, status TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(courseId) REFERENCES student_courses(id) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(paperId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_tasks_courseId ON student_tasks (courseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_tasks_paperId ON student_tasks (paperId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_tasks_dueDate ON student_tasks (dueDate)")
                db.execSQL("CREATE TABLE IF NOT EXISTS student_notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, courseId INTEGER, paperId INTEGER, tags TEXT NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(courseId) REFERENCES student_courses(id) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(paperId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_notes_courseId ON student_notes (courseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_notes_paperId ON student_notes (paperId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS student_decks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, courseId INTEGER, paperId INTEGER, createdAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(courseId) REFERENCES student_courses(id) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(paperId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_decks_courseId ON student_decks (courseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_decks_paperId ON student_decks (paperId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS student_cards (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, deckId INTEGER NOT NULL, front TEXT NOT NULL, back TEXT NOT NULL, repetitions INTEGER NOT NULL, easeFactor REAL NOT NULL, intervalDays REAL NOT NULL, dueAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(deckId) REFERENCES student_decks(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_cards_deckId ON student_cards (deckId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_cards_dueAtEpochMillis ON student_cards (dueAtEpochMillis)")
                db.execSQL("CREATE TABLE IF NOT EXISTS student_reviews (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, cardId INTEGER NOT NULL, rating TEXT NOT NULL, reviewedAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(cardId) REFERENCES student_cards(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_reviews_cardId ON student_reviews (cardId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_reviews_reviewedAtEpochMillis ON student_reviews (reviewedAtEpochMillis)")
                db.execSQL("CREATE TABLE IF NOT EXISTS student_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, startedAtEpochMillis INTEGER NOT NULL, endedAtEpochMillis INTEGER NOT NULL, durationSeconds INTEGER NOT NULL, courseId INTEGER, paperId INTEGER, taskId INTEGER, notes TEXT NOT NULL, FOREIGN KEY(courseId) REFERENCES student_courses(id) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(paperId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(taskId) REFERENCES student_tasks(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_sessions_courseId ON student_sessions (courseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_sessions_paperId ON student_sessions (paperId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_sessions_taskId ON student_sessions (taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_sessions_startedAtEpochMillis ON student_sessions (startedAtEpochMillis)")
                db.execSQL("CREATE TABLE IF NOT EXISTS student_active_timer (id INTEGER NOT NULL, startedAtEpochMillis INTEGER NOT NULL, endsAtEpochMillis INTEGER NOT NULL, plannedSeconds INTEGER NOT NULL, pausedMillis INTEGER NOT NULL, pausedAtEpochMillis INTEGER, courseId INTEGER, paperId INTEGER, taskId INTEGER, PRIMARY KEY(id), FOREIGN KEY(courseId) REFERENCES student_courses(id) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(paperId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(taskId) REFERENCES student_tasks(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_active_timer_courseId ON student_active_timer (courseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_active_timer_paperId ON student_active_timer (paperId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_active_timer_taskId ON student_active_timer (taskId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS student_calendar_events (uid TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, allDay INTEGER NOT NULL, startDate TEXT NOT NULL, endDateExclusive TEXT NOT NULL, startAtEpochMillis INTEGER, endAtEpochMillis INTEGER, timeZone TEXT NOT NULL, source TEXT NOT NULL, PRIMARY KEY(uid))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_calendar_events_startDate ON student_calendar_events (startDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_calendar_events_startAtEpochMillis ON student_calendar_events (startAtEpochMillis)")
            }
        }
        @Volatile private var instance: ScholarDatabase? = null
        fun getInstance(context: Context): ScholarDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, ScholarDatabase::class.java, "am_i_scholar.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
