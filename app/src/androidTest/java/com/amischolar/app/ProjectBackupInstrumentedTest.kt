package com.amischolar.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amischolar.app.data.*
import com.amischolar.app.feature.ProjectBackup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectBackupInstrumentedTest {
    @Test fun backupRoundTripPreservesResearchContentWithoutIds() {
        val original = ProjectBackup.Snapshot(
            ResearchProject(id = 99, title = "Genome architecture", researchQuestion = "How is DNA organized?"),
            listOf(ResearchNote(id = 18, projectId = 99, title = "Draft", content = "Evidence and limitations", kind = "DRAFT")),
            listOf(ResearchSource(id = 42, projectId = 99, title = "Paper", authors = "A; B",
                doiOrUrl = "10.1234/example", verified = true))
        )
        val restored = ProjectBackup.decode(ProjectBackup.encode(original))
        assertEquals("Genome architecture", restored.project.title)
        assertEquals("Evidence and limitations", restored.notes.single().content)
        assertEquals("10.1234/example", restored.sources.single().doiOrUrl)
        assertTrue(restored.sources.single().verified)
        assertEquals(0L, restored.project.id)
        assertEquals(0L, restored.notes.single().id)
    }

    @Test fun malformedAndOversizedBackupsAreRejected() {
        for (payload in listOf("{}", "{\"format\":\"unknown\",\"version\":1}", "not json")) {
            try { ProjectBackup.decode(payload.toByteArray()); fail("Invalid backup accepted") }
            catch (_: Exception) { }
        }
        try { ProjectBackup.decode(ByteArray(ProjectBackup.MAX_BYTES + 1)); fail("Oversized backup accepted") }
        catch (_: IllegalArgumentException) { }
    }
}
