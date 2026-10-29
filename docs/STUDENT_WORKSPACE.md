# Student and Researcher Workspace (Incremental Web Release)

This update preserves the original research dashboard and 12 modules. A **Student Mode** toggle exposes a separate IndexedDB-backed workspace for courses, assignments, Markdown source notes, flashcards with local spaced repetition, a timestamp-based focus timer, and an event-list calendar. Researcher Mode retains the existing localStorage research projects and tools.

## Privacy and limitations

- Student data is stored in IndexedDB on this device. Research data remains in the existing localStorage store.
- No account, remote synchronization, paid API, or automatic network request is introduced by Student Mode.
- Research projects retain their existing separate backup. Student workspaces use the versioned `am-i-scholar-student` JSON backup for web/Android transfer; restore validates references before replacing records atomically. Research projects are not included.
- Notes are stored as Markdown text; a rich rendered editor is not yet implemented.
- Calendar currently provides a month-filtered event list, not a graphical month/week grid.
- Focus logging, active timer recovery, and study summaries are local. Web recovery uses IndexedDB and timestamp-derived timers; Android recovery uses Room.
- Android has a Room-backed Student Workspace for courses, tasks, notes, flashcards, focus sessions, and calendar events. Android build, migration and emulator checks run in Phase 2 CI.
- Paper linking uses optional paper ID text and requires a future relational picker.
- Existing research modules and their evidence-verification requirements are unchanged.

## Portable student backup

The shared backup contains courses, tasks, notes, decks, cards, review history, focus sessions, an active timer, and imported calendar events. IDs are portable strings and timestamps are ISO-8601. Restore remaps local database IDs while keeping internal relationships. A research-paper link is retained on Android only when that paper ID already exists locally; research records remain in their separate backup. Invalid files are rejected before restore, and Android applies a valid restore in one database transaction.

## Quality gates

Run `node tests/web-smoke.cjs`, then `npx playwright test` with the static site served at port 8000. New tests cover student mode, course creation, task creation, notes, persistence, research-mode restoration, and flashcard review.

## Next implementation phases

1. Finish Phase 2 CI and cross-platform restore acceptance checks.
2. Cross-workspace paper/course pickers and relational links.
3. Month/week calendar grid, Markdown rendering and safe formatting controls.
4. Browser compatibility and offline behavior; defer real-device acceptance until the formal APK/testing URL is ready.
