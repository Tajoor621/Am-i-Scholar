package com.amischolar.app

import com.amischolar.app.feature.CalendarIcs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class CalendarIcsTest {
    @Test fun parsesTzidAcrossDaylightSavingAndDisplaysInDifferentZones() {
        val source = """BEGIN:VCALENDAR
VERSION:2.0
BEGIN:VEVENT
UID:dst
DTSTART;TZID=America/New_York:20260308T013000
DTEND;TZID=America/New_York:20260308T033000
SUMMARY:Spring-forward seminar
DESCRIPTION:Bring slides\, questions\; and notes
END:VEVENT
END:VCALENDAR"""
        val result = CalendarIcs.parse(source)
        val event = result.events.single()
        assertEquals(Instant.parse("2026-03-08T06:30:00Z").toEpochMilli(), event.startAtEpochMillis)
        assertEquals(Instant.parse("2026-03-08T07:30:00Z").toEpochMilli(), event.endAtEpochMillis)
        assertEquals("America/New_York", event.timeZone)
        assertEquals("Bring slides, questions; and notes", event.description)
        assertEquals("2026-03-08T10:30", Instant.ofEpochMilli(event.startAtEpochMillis!!).atZone(ZoneId.of("Asia/Dubai")).toLocalDateTime().toString().take(16))
        assertEquals("2026-03-07T22:30", Instant.ofEpochMilli(event.startAtEpochMillis!!).atZone(ZoneId.of("America/Los_Angeles")).toLocalDateTime().toString().take(16))
    }

    @Test fun parsesFoldedLinesUtcAndExclusiveAllDayEndAndRoundTrips() {
        val source = """BEGIN:VCALENDAR
BEGIN:VEVENT
UID:fold
DTSTART;TZID=Asia/Dubai:20261029T090000
DTEND;TZID=Asia/Dubai:20261029T100000
SUMMARY:Folded conference session
  continues here
END:VEVENT
BEGIN:VEVENT
UID:all-day
DTSTART;VALUE=DATE:20260309
DTEND;VALUE=DATE:20260310
SUMMARY:Study fair
END:VEVENT
BEGIN:VEVENT
UID:utc
DTSTART:20261028T120000Z
DTEND:20261028T130000Z
SUMMARY:UTC workshop
END:VEVENT
END:VCALENDAR"""
        val parsed = CalendarIcs.parse(source)
        assertEquals(3, parsed.events.size)
        assertEquals("Folded conference session continues here", parsed.events.first { it.uid == "fold" }.title)
        assertTrue(parsed.events.any { it.allDay && it.startDate == "2026-03-09" && it.endDateExclusive == "2026-03-10" })
        assertTrue(parsed.events.any { it.uid == "utc" && it.startAtEpochMillis == Instant.parse("2026-10-28T12:00:00Z").toEpochMilli() })
        assertEquals(3, CalendarIcs.parse(CalendarIcs.serialize(parsed.events)).events.size)
    }

    @Test fun warnsForRecurrenceAndDeduplicatesByUid() {
        val source = """BEGIN:VCALENDAR
BEGIN:VEVENT
UID:same
DTSTART;VALUE=DATE:20260101
RRULE:FREQ=DAILY
SUMMARY:Repeat
END:VEVENT
BEGIN:VEVENT
UID:same
DTSTART;VALUE=DATE:20260102
SUMMARY:Duplicate
END:VEVENT
END:VCALENDAR"""
        val result = CalendarIcs.parse(source)
        assertEquals(1, result.events.size)
        assertEquals(1, result.warnings.size)
    }
}
