# Student and Researcher Workspace (Incremental Web Release)

This update preserves the original research dashboard and 12 modules. A **Student Mode** toggle exposes a separate IndexedDB-backed workspace for courses, assignments, Markdown source notes, flashcards with local spaced repetition, a timestamp-based focus timer, and an event-list calendar. Researcher Mode retains the existing localStorage research projects and tools.

## Privacy and limitations

- Student data is stored in IndexedDB on this device. Research data remains in the existing localStorage store.
- No account, remote synchronization, paid API, or automatic network request is introduced by Student Mode.
- Both web modes have separate explicit JSON backup/restore controls. **These are not yet a unified web/Android backup schema.**
- Notes are stored as Markdown text; a rich rendered editor is not yet implemented.
- Calendar currently provides a month-filtered event list, not a graphical month/week grid.
- Focus logging is local and timer restoration uses sessionStorage; background timers are timestamp-derived.
- Android Compose/Room parity is **not yet implemented** in this incremental web release.
- Paper linking uses optional paper ID text and requires a future relational picker.
- Existing research modules and their evidence-verification requirements are unchanged.

## Quality gates

Run `node tests/web-smoke.cjs`, then `npx playwright test` with the static site served at port 8000. New tests cover student mode, course creation, task creation, notes, persistence, research-mode restoration, and flashcard review.

## Next implementation phases

1. Unified, versioned web/Android backup schema with migrations and atomic restore.
2. Cross-workspace paper/course pickers and relational links.
3. Month/week calendar grid, Markdown rendering and safe formatting controls.
4. Compose/Room student feature parity and Android migrations.
5. Browser compatibility, offline behavior and real-device acceptance tests.
