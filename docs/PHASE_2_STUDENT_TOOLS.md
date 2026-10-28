# Phase 2 — Student Tools

## Goal

Complete the offline-first student workspace across web and Android without changing the research workspace's records or behavior. Keep the feature set free: no paid APIs, accounts, cloud synchronization, or automatic network requests.

## Existing baseline

- Web Student Mode has IndexedDB stores for courses, tasks, notes, flashcards, reviews, focus sessions, milestones, links, and preferences. Browser coverage exercises student records, ICS round-trip, focus timer recovery, and shared student-backup restore.
- Android Student Mode now uses Room for courses, tasks, notes, flashcards, reviews, focus sessions, active timers, and calendar events, with a Room v3→v4 migration and native screens.
- Remaining work is CI completion and final cross-platform restore acceptance. Real-device testing remains deferred until the formal APK/testing URL is ready.

## Delivery increments

1. **Calendar portability:** add offline ICS import/export for task deadlines, study sessions, and calendar events. Parse folded lines, escaped text, UTC and `TZID` date-times, floating date-times, and all-day dates. Deduplicate imports by calendar UID. Do not fetch feeds automatically or require API keys. Recurrence expansion and custom `VTIMEZONE` rules are deferred and must be disclosed.
2. **Focus continuity:** persist active/paused timer state, link sessions to a course, research paper, or task, and show local study streaks and a seven-day time summary. Keep flashcard scheduling and reviews entirely local and test it across reloads.
3. **Tasks and calendar:** make course/task status and due-date flows complete, show tasks, milestones, imported events, and focus sessions in a usable calendar, and provide links back to their records.
4. **Android parity and portability:** add Room entities/migrations and native Student Mode flows for courses, tasks, notes, flashcards, focus sessions, and calendar. Use the shared `am-i-scholar-student` version 1 JSON shape for web/Android transfer; keep the existing research backup formats separate.

## Acceptance criteria

- The full existing web/student/characterization suite passes after each increment; new student flows have regression tests before code changes.
- Flashcard review works offline and review scheduling survives page/app restart.
- Focus timer state survives restart; saved sessions retain selected links and appear consistently in summaries and calendars.
- ICS fixture tests cover at least two IANA timezones, a daylight-saving transition, folded/escaped text, UTC timestamps, and all-day end-date semantics. Imported records retain correct instants and display in the active device timezone.
- Web-to-Android and Android-to-web backups preserve supported student entities and their internal links; invalid backups do not partially overwrite existing data. Research-project links are retained only when a matching project ID exists on the destination because research backups remain separate.
- Android CI, unit tests, Room migration tests, and emulator UI checks pass. Real-device testing remains deferred until the app is complete and a formal APK/testing URL is ready.
- Update privacy and feature documentation only for verified behavior; record unsupported recurrence/VTIMEZONE behavior clearly.
