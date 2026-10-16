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
    @Test fun evidenceRelationshipsRoundTripAndLegacyBackupsRemainReadable() {
        val project = ResearchProject(id = 77, title = "Evidence project")
        val notes = listOf(ResearchNote(id = 11, projectId = 77, title = "Claim", kind = "EVIDENCE"))
        val sources = listOf(ResearchSource(id = 22, projectId = 77, title = "Verified source"))
        val link = EvidenceLink(id = 33, projectId = 77, claimNoteId = 11, sourceId = 22,
            status = "partially supported", rationale = "Page 12, limited sample")
        val bytes = ProjectBackup.encode(ProjectBackup.Snapshot(project, notes, sources, listOf(link)))
        val restored = ProjectBackup.decode(bytes)
        assertEquals(1, restored.links.size)
        assertEquals(0, restored.links.single().noteIndex)
        assertEquals(0, restored.links.single().sourceIndex)
        assertEquals("partially supported", restored.links.single().status)
        assertEquals("Page 12, limited sample", restored.links.single().rationale)
        val legacy = org.json.JSONObject(bytes.toString(Charsets.UTF_8))
        legacy.remove("evidenceLinks")
        assertTrue(ProjectBackup.decode(legacy.toString().toByteArray()).links.isEmpty())
    }

    @Test fun danglingEvidenceLinkIsRejectedBeforeRestore() {
        val root = org.json.JSONObject()
            .put("format", ProjectBackup.FORMAT).put("version", 1)
            .put("project", org.json.JSONObject().put("title", "Test")
                .put("researchQuestion", "").put("discipline", "").put("citationStyle", "APA"))
            .put("notes", org.json.JSONArray()).put("sources", org.json.JSONArray())
            .put("evidenceLinks", org.json.JSONArray().put(org.json.JSONObject()
                .put("noteIndex", 3).put("sourceIndex", 2)
                .put("status", "supported").put("rationale", "invalid")))
        try { ProjectBackup.decode(root.toString().toByteArray()); fail("Dangling relationship accepted") }
        catch (_: IllegalArgumentException) { }
    }

}
