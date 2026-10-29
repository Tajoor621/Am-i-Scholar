package com.amischolar.app.feature

import com.amischolar.app.data.StudentCalendarEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Offline RFC 5545 subset. Calendar data is read only when the user selects an ICS file. */
object CalendarIcs {
    data class Event(
        val uid: String,
        val title: String,
        val description: String = "",
        val allDay: Boolean,
        val startDate: String = "",
        val endDateExclusive: String = "",
        val startAtEpochMillis: Long? = null,
        val endAtEpochMillis: Long? = null,
        val timeZone: String = "",
        val source: String = "ics"
    ) {
        fun toRoom() = StudentCalendarEvent(uid, title, description, allDay, startDate,
            endDateExclusive, startAtEpochMillis, endAtEpochMillis, timeZone, source)
    }

    data class ParseResult(val events: List<Event>, val warnings: List<String>)

    private val dateFormat = DateTimeFormatter.BASIC_ISO_DATE
    private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.ROOT)
    private val utcFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC)

    fun parse(text: String, fallbackZone: ZoneId = ZoneId.systemDefault()): ParseResult {
        val unfolded = mutableListOf<String>()
        text.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n').lines().forEach { line ->
            if ((line.startsWith(' ') || line.startsWith('\t')) && unfolded.isNotEmpty()) {
                unfolded[unfolded.lastIndex] += line.drop(1)
            } else unfolded += line
        }
        val events = mutableListOf<Event>()
        val warnings = mutableListOf<String>()
        var properties: MutableMap<String, Property>? = null
        var nested = 0
        unfolded.forEach { line ->
            val property = parseProperty(line) ?: return@forEach
            if (property.name == "BEGIN" && property.value.equals("VEVENT", true)) {
                properties = linkedMapOf()
                nested = 0
                return@forEach
            }
            val current = properties ?: return@forEach
            if (property.name == "BEGIN") { nested++; return@forEach }
            if (property.name == "END" && property.value.equals("VEVENT", true) && nested == 0) {
                try {
                    val start = parseDate(current["DTSTART"] ?: error("Event has no DTSTART"), fallbackZone)
                    val end = current["DTEND"]?.let { parseDate(it, fallbackZone) }
                    val title = current["SUMMARY"]?.value?.let(::unescape).orEmpty().ifBlank { "Untitled calendar event" }
                    val uid = current["UID"]?.value?.let(::unescape)?.trim().orEmpty()
                        .ifBlank { "local:${title}:${start.date ?: start.epochMillis}" }
                    val description = current["DESCRIPTION"]?.value?.let(::unescape).orEmpty()
                    val event = if (start.date != null) {
                        val endDate = end?.date ?: start.date.plusDays(1)
                        Event(uid, title, description, true, start.date.toString(), endDate.toString())
                    } else {
                        Event(uid, title, description, false,
                            startAtEpochMillis = start.epochMillis,
                            endAtEpochMillis = end?.epochMillis ?: (start.epochMillis!! + 3_600_000),
                            timeZone = start.zoneId)
                    }
                    events += event
                    if (current.containsKey("RRULE")) warnings += "$title: repeating events are imported as their first occurrence only."
                } catch (error: Exception) {
                    warnings += error.message ?: "One event could not be imported."
                }
                properties = null
                nested = 0
                return@forEach
            }
            if (property.name == "END") { nested = (nested - 1).coerceAtLeast(0); return@forEach }
            if (nested == 0) current[property.name] = property
        }
        val unique = events.distinctBy { it.uid }.sortedBy { it.startAtEpochMillis ?: runCatching { LocalDate.parse(it.startDate).toEpochDay() * 86_400_000 }.getOrDefault(0) }
        return ParseResult(unique, warnings)
    }

    fun serialize(events: Iterable<Event>): String = buildString {
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("PRODID:-//Am I Scholar//Student Tools//EN")
        appendLine("CALSCALE:GREGORIAN")
        events.forEachIndexed { index, event ->
            val startAt = event.startAtEpochMillis
            if (!event.allDay && startAt == null) return@forEachIndexed
            appendLine("BEGIN:VEVENT")
            appendLine("UID:${escape(event.uid.ifBlank { "amischolar-$index" })}")
            appendLine("DTSTAMP:${utcFormat.format(Instant.now())}")
            appendLine("SUMMARY:${escape(event.title)}")
            if (event.description.isNotBlank()) appendLine("DESCRIPTION:${escape(event.description)}")
            if (event.allDay) {
                appendLine("DTSTART;VALUE=DATE:${dateFormat.format(LocalDate.parse(event.startDate))}")
                val endDate = event.endDateExclusive.takeIf(String::isNotBlank)?.let(LocalDate::parse) ?: LocalDate.parse(event.startDate).plusDays(1)
                appendLine("DTEND;VALUE=DATE:${dateFormat.format(endDate)}")
            } else {
                appendLine("DTSTART:${utcFormat.format(Instant.ofEpochMilli(startAt!!))}")
                appendLine("DTEND:${utcFormat.format(Instant.ofEpochMilli(event.endAtEpochMillis ?: (startAt + 3_600_000)))}")
            }
            appendLine("END:VEVENT")
        }
        append("END:VCALENDAR\r\n")
    }.replace("\n", "\r\n").replace("\r\r\n", "\r\n")

    private data class Property(val name: String, val params: Map<String, String>, val value: String)
    private data class ParsedDate(val date: LocalDate? = null, val epochMillis: Long? = null, val zoneId: String = "")

    private fun parseProperty(line: String): Property? {
        val colon = line.indexOf(':')
        if (colon <= 0) return null
        val bits = line.substring(0, colon).split(';')
        val params = bits.drop(1).mapNotNull { segment ->
            val index = segment.indexOf('=')
            if (index <= 0) null else segment.substring(0, index).uppercase(Locale.ROOT) to segment.substring(index + 1).trim('"')
        }.toMap()
        return Property(bits.first().uppercase(Locale.ROOT), params, line.substring(colon + 1))
    }

    private fun parseDate(property: Property, fallbackZone: ZoneId): ParsedDate {
        val raw = property.value.trim()
        if (property.params["VALUE"]?.equals("DATE", true) == true || raw.length == 8) {
            val date = LocalDate.parse(raw, dateFormat)
            return ParsedDate(date = date)
        }
        val compact = raw.removeSuffix("Z")
        val local = LocalDateTime.parse(if (compact.length == 13) compact + "00" else compact, dateTimeFormat)
        val epoch = if (raw.endsWith("Z")) local.toInstant(ZoneOffset.UTC).toEpochMilli() else {
            val zone = property.params["TZID"]?.let(ZoneId::of) ?: fallbackZone
            local.atZone(zone).toInstant().toEpochMilli()
        }
        return ParsedDate(epochMillis = epoch, zoneId = property.params["TZID"].orEmpty())
    }

    private fun unescape(value: String) = value.replace(Regex("\\\\[nN]"), "\n")
        .replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

    private fun escape(value: String) = value.replace("\\", "\\\\").replace("\r\n", "\n")
        .replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")
}
