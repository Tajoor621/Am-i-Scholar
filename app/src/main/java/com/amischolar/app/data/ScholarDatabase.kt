package com.amischolar.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ResearchProject::class, ResearchNote::class, ResearchSource::class, EvidenceLink::class], version = 3, exportSchema = false)
abstract class ScholarDatabase : RoomDatabase() {
    abstract fun researchProjectDao(): ResearchProjectDao
    abstract fun researchWorkspaceDao(): ResearchWorkspaceDao

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
        @Volatile private var instance: ScholarDatabase? = null
        fun getInstance(context: Context): ScholarDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, ScholarDatabase::class.java, "am_i_scholar.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
