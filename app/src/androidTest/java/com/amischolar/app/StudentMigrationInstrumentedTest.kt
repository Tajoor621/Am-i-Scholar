package com.amischolar.app

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amischolar.app.data.ScholarDatabase
import com.amischolar.app.data.StudentCourse
import com.amischolar.app.data.StudentTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentMigrationInstrumentedTest {
    @Test fun populatedV3MigratesToStudentSchemaWithoutLosingResearch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-student-test.db"
        context.deleteDatabase(name)
        val raw = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        raw.execSQL("CREATE TABLE research_projects (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, researchQuestion TEXT NOT NULL, discipline TEXT NOT NULL, citationStyle TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)")
        raw.execSQL("CREATE TABLE research_notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, kind TEXT NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        raw.execSQL("CREATE INDEX index_research_notes_projectId ON research_notes(projectId)")
        raw.execSQL("CREATE TABLE research_sources (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, title TEXT NOT NULL, authors TEXT NOT NULL, year TEXT NOT NULL, doiOrUrl TEXT NOT NULL, publication TEXT NOT NULL, evidenceNotes TEXT NOT NULL, verified INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        raw.execSQL("CREATE INDEX index_research_sources_projectId ON research_sources(projectId)")
        raw.execSQL("CREATE TABLE evidence_links (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, claimNoteId INTEGER NOT NULL, sourceId INTEGER NOT NULL, status TEXT NOT NULL, rationale TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(claimNoteId) REFERENCES research_notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(sourceId) REFERENCES research_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        raw.execSQL("CREATE INDEX index_evidence_links_projectId ON evidence_links(projectId)")
        raw.execSQL("CREATE INDEX index_evidence_links_claimNoteId ON evidence_links(claimNoteId)")
        raw.execSQL("CREATE INDEX index_evidence_links_sourceId ON evidence_links(sourceId)")
        raw.execSQL("INSERT INTO research_projects VALUES (1,'Research before Phase 2','Question?','Biology','APA',1000,1000)")
        raw.version = 3
        raw.close()

        val db = Room.databaseBuilder(context, ScholarDatabase::class.java, name)
            .addMigrations(ScholarDatabase.MIGRATION_1_2, ScholarDatabase.MIGRATION_2_3, ScholarDatabase.MIGRATION_3_4).build()
        try {
            runBlocking(Dispatchers.IO) {
                assertEquals("Research before Phase 2", db.researchProjectDao().findById(1)?.title)
                val dao = db.studentWorkspaceDao()
                val courseId = dao.upsertCourse(StudentCourse(name = "Biology 301"))
                dao.upsertTask(StudentTask(title = "Lab report", courseId = courseId, dueDate = "2026-11-03"))
                assertEquals("Lab report", dao.observeTasks().first().single().title)
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
