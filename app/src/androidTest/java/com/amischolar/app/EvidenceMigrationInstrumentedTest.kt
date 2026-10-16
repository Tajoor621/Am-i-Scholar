package com.amischolar.app

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amischolar.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Creates an actual populated v2 database, upgrades it and checks that old records survive. */
@RunWith(AndroidJUnit4::class)
class EvidenceMigrationInstrumentedTest {
    @Test fun populatedV2MigratesToV3WithoutLosingResearch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-evidence-test.db"
        context.deleteDatabase(name)
        val raw = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        raw.execSQL("CREATE TABLE research_projects (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, researchQuestion TEXT NOT NULL, discipline TEXT NOT NULL, citationStyle TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)")
        raw.execSQL("CREATE TABLE research_notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, kind TEXT NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        raw.execSQL("CREATE INDEX index_research_notes_projectId ON research_notes(projectId)")
        raw.execSQL("CREATE TABLE research_sources (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, title TEXT NOT NULL, authors TEXT NOT NULL, year TEXT NOT NULL, doiOrUrl TEXT NOT NULL, publication TEXT NOT NULL, evidenceNotes TEXT NOT NULL, verified INTEGER NOT NULL, FOREIGN KEY(projectId) REFERENCES research_projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        raw.execSQL("CREATE INDEX index_research_sources_projectId ON research_sources(projectId)")
        raw.execSQL("INSERT INTO research_projects VALUES (1,'Research before migration','Question?','Microbiology','APA',1000,1000)")
        raw.execSQL("INSERT INTO research_notes VALUES (2,1,'Claim before migration','Claim content','EVIDENCE',1000)")
        raw.execSQL("INSERT INTO research_sources VALUES (3,1,'Source before migration','Author','2025','10.1234/example','Journal','Evidence',0)")
        raw.version = 2
        raw.close()
        val db = Room.databaseBuilder(context, ScholarDatabase::class.java, name)
            .addMigrations(ScholarDatabase.MIGRATION_2_3).build()
        try {
            runBlocking(Dispatchers.IO) {
                val dao = db.researchWorkspaceDao()
                assertEquals("Claim before migration", dao.observeNotes(1).first().single().title)
                assertEquals("Source before migration", dao.observeSources(1).first().single().title)
                dao.insertEvidenceLink(EvidenceLink(projectId = 1, claimNoteId = 2, sourceId = 3,
                    status = "partially supported", rationale = "Limited evidence"))
                assertEquals(1, dao.getEvidenceLinks(1).size)
                dao.deleteSource(3)
                assertTrue(dao.getEvidenceLinks(1).isEmpty())
                assertEquals(1, dao.observeNotes(1).first().size)
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
