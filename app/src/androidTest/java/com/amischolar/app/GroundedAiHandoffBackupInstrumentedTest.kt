package com.amischolar.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amischolar.app.data.ResearchNote
import com.amischolar.app.data.ResearchProject
import com.amischolar.app.feature.ProjectBackup
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroundedAiHandoffBackupInstrumentedTest {
    @Test fun archiveAiUseLogSurvivesPortableResearchProjectBackup() {
        val original = ResearchNote(
            projectId = 7,
            title = "AI-use log",
            content = "Selected source IDs: source-1\nReturned text: unverified draft",
            kind = "ARCHIVE"
        )
        val encoded = ProjectBackup.encode(
            ProjectBackup.Snapshot(
                project = ResearchProject(id = 7, title = "Test project"),
                notes = listOf(original),
                sources = emptyList()
            )
        )
        val restored = ProjectBackup.decode(encoded).notes.single()
        assertEquals("ARCHIVE", restored.kind)
        assertEquals(original.content, restored.content)
    }
}
