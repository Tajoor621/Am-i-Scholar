# Student and Researcher Workspace (Incremental Web Release)

This update preserves the original research dashboard and 12 modules. A **Student Mode** toggle exposes a separate IndexedDB-backed workspace for courses, assignments, rich Markdown notes, flashcards with local spaced repetition, a timestamp-based focus timer, task reminders, and a calendar. Researcher Mode retains the existing localStorage research projects and tools.

## Privacy and limitations

- Student data is stored in IndexedDB on this device. Research data remains in the existing localStorage store.
- No account, remote synchronization, paid API, or automatic network request is introduced by Student Mode.
- Research projects retain their existing separate backup. Student workspaces use the versioned `am-i-scholar-student` JSON backup for web/Android transfer; restore validates references before replacing records atomically. Research projects are not included.
- Notes remain Markdown text and add safe formatting preview, local resized image embeds, and web-only local PDF text extraction.
- Calendar currently provides a month-filtered event list, not a graphical month/week grid.
- Focus logging, active timer recovery, streaks, and seven-day study summaries are local. Web recovery uses IndexedDB and timestamp-derived timers; Android recovery uses Room.
- Optional task reminders are stored with each task. Web notifications require the app page to remain open; Android uses on-device alarms and notifications, subject to notification permission and OS delivery limits.
- Android has a Room-backed Student Workspace for courses, tasks, notes, flashcards, focus sessions, reminders, and calendar events. Task reminders add a nullable Room field through the v5→v6 migration.
- Paper linking uses optional paper ID text and requires a future relational picker.
- Existing research modules and their evidence-verification requirements are unchanged.

## Portable student backup

The shared backup contains courses, tasks (including an optional ISO-8601 reminder), notes, decks, cards, review history, focus sessions, an active timer, and imported calendar events. IDs are portable strings and timestamps are ISO-8601. Restore remaps local database IDs while keeping internal relationships; older backups without reminders remain valid. A research-paper link is retained on Android only when that paper ID already exists locally; research records remain in their separate backup. Invalid files are rejected before restore, and Android applies a valid restore in one database transaction.

## Quality gates

Run `node tests/web-smoke.cjs`, then `npx playwright test` with the static site served at port 8000. Student tests cover task reminders, student mode, course/task creation, notes, persistence, research-mode restoration, flashcard review, and the network boundary.

## Next implementation phases

1. Continue with the planned opt-in study generation and speech tools.
2. Add local lecture transcription where browser/device support permits.
3. Evaluate read-only LMS sync only after documenting provider and privacy limits.
4. Defer real-device acceptance until the formal APK/testing URL is ready.
