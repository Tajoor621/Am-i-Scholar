package com.amischolar.app.feature

import com.amischolar.app.data.ResearchNote
import com.amischolar.app.data.ResearchProject
import com.amischolar.app.data.ResearchSource
import org.json.JSONArray
import org.json.JSONObject

/** Portable versioned JSON project backup. Imported records always receive fresh local IDs. */
object ProjectBackup {
    const val FORMAT = "am-i-scholar-project"
    const val VERSION = 1
    const val MAX_BYTES = 10 * 1024 * 1024
    const val MAX_RECORDS = 10000

    data class Snapshot(
        val project: ResearchProject,
        val notes: List<ResearchNote>,
        val sources: List<ResearchSource>
    )

    fun encode(snapshot: Snapshot): ByteArray {
        require(snapshot.notes.size + snapshot.sources.size <= MAX_RECORDS)
        val p = snapshot.project
        val root = JSONObject()
            .put("format", FORMAT).put("version", VERSION)
            .put("project", JSONObject()
                .put("title", p.title).put("researchQuestion", p.researchQuestion)
                .put("discipline", p.discipline).put("citationStyle", p.citationStyle))
        val notes = JSONArray()
        snapshot.notes.forEach { n ->
            notes.put(JSONObject().put("title", n.title).put("content", n.content).put("kind", n.kind))
        }
        val sources = JSONArray()
        snapshot.sources.forEach { s ->
            sources.put(JSONObject().put("title", s.title).put("authors", s.authors)
                .put("year", s.year).put("doiOrUrl", s.doiOrUrl)
                .put("publication", s.publication).put("evidenceNotes", s.evidenceNotes)
                .put("verified", s.verified))
        }
        root.put("notes", notes).put("sources", sources)
        return root.toString().toByteArray(Charsets.UTF_8).also { require(it.size <= MAX_BYTES) }
    }

    fun decode(bytes: ByteArray): Snapshot {
        require(bytes.size in 1..MAX_BYTES) { "Backup size is invalid" }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.getString("format") == FORMAT) { "Not an Am I Scholar backup" }
        require(root.getInt("version") == VERSION) { "Unsupported backup version" }
        val p = root.getJSONObject("project")
        val notes = root.getJSONArray("notes")
        val sources = root.getJSONArray("sources")
        require(notes.length() + sources.length() <= MAX_RECORDS) { "Too many records" }
        fun JSONObject.required(key: String): String = getString(key).also {
            require(it.length <= 2_000_000) { "Field too large: $key" }
        }
        val project = ResearchProject(
            title = p.required("title").also { require(it.isNotBlank()) },
            researchQuestion = p.required("researchQuestion"),
            discipline = p.required("discipline"), citationStyle = p.required("citationStyle")
        )
        val restoredNotes = (0 until notes.length()).map { index ->
            val n = notes.getJSONObject(index)
            ResearchNote(projectId = 0, title = n.required("title").also { require(it.isNotBlank()) },
                content = n.required("content"), kind = n.required("kind"))
        }
        val restoredSources = (0 until sources.length()).map { index ->
            val s = sources.getJSONObject(index)
            ResearchSource(projectId = 0, title = s.required("title").also { require(it.isNotBlank()) },
                authors = s.required("authors"), year = s.required("year"),
                doiOrUrl = s.required("doiOrUrl"), publication = s.required("publication"),
                evidenceNotes = s.required("evidenceNotes"), verified = s.getBoolean("verified"))
        }
        return Snapshot(project, restoredNotes, restoredSources)
    }
}
