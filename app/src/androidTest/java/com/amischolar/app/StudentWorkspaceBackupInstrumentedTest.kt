package com.amischolar.app

import com.amischolar.app.feature.StudentWorkspaceBackup
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentWorkspaceBackupInstrumentedTest {
    private fun fixture(courseId: String = "course-web-1"): ByteArray = """
        {"format":"am-i-scholar-student","version":1,"data":{
          "courses":[{"id":"course-web-1","name":"History 201","code":"HIS201","semester":"Fall","createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-01T00:00:00Z"}],
          "tasks":[{"id":"task-web-1","title":"Essay","type":"todo","priority":2,"dueDate":"2026-11-03","courseId":"$courseId","paperId":null,"description":"Draft","status":"todo","createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-01T00:00:00Z"}],
          "notes":[],"decks":[{"id":"deck-web-1","title":"Terms","courseId":"course-web-1","paperId":null,"createdAt":"2026-10-01T00:00:00Z"}],
          "cards":[{"id":"card-web-1","deckId":"deck-web-1","front":"Question","back":"Answer","repetitions":1,"easeFactor":2.5,"intervalDays":1,"dueAt":"2026-10-29T00:00:00Z","updatedAt":"2026-10-28T00:00:00Z"}],
          "reviews":[{"id":"review-web-1","cardId":"card-web-1","rating":"good","reviewedAt":"2026-10-28T00:00:00Z"}],
          "sessions":[{"id":"session-web-1","startedAt":"2026-10-28T16:00:00Z","endedAt":"2026-10-28T16:25:00Z","durationSeconds":1500,"courseId":"course-web-1","paperId":null,"taskId":"task-web-1","notes":""}],
          "calendarEvents":[{"uid":"event-web-1","title":"Seminar","description":"Room 2","allDay":false,"startDate":"","endDateExclusive":"","startAt":"2026-10-29T10:00:00Z","endAt":"2026-10-29T11:00:00Z","timeZone":"UTC","source":"ics"}],
          "activeTimer":null
        }}
    """.trimIndent().toByteArray()

    @Test fun acceptsWebPortableRecordsAndKeepsReferences() {
        val data = StudentWorkspaceBackup.decode(fixture())
        assertEquals("History 201", data.getJSONArray("courses").getJSONObject(0).getString("name"))
        assertEquals("course-web-1", data.getJSONArray("tasks").getJSONObject(0).getString("courseId"))
        assertEquals("card-web-1", data.getJSONArray("reviews").getJSONObject(0).getString("cardId"))
        assertEquals("event-web-1", data.getJSONArray("calendarEvents").getJSONObject(0).getString("uid"))
    }

    @Test fun rejectsBrokenReferencesBeforeRestore() {
        assertThrows(IllegalArgumentException::class.java) { StudentWorkspaceBackup.decode(fixture("missing-course")) }
    }

    @Test fun rejectsWrongFormatAndOversizedPayload() {
        assertThrows(IllegalArgumentException::class.java) { StudentWorkspaceBackup.decode("{}".toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { StudentWorkspaceBackup.decode(ByteArray(StudentWorkspaceBackup.MAX_BYTES + 1)) }
    }
}
