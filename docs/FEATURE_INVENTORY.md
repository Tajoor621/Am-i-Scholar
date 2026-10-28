# Feature inventory (baseline: `main`)

This inventory records observable entry points, persistence touched, and existing tests before feature work. “Test” names refer to files in `tests/` or Android test source sets. UNVERIFIED means the repository contains no test that directly establishes the stated behavior; code inspection is not a substitute for a passing characterization test.

## Web research workspace

| Feature | Current entry point | Reads / writes | Existing test coverage |
| --- | --- | --- | --- |
| Dashboard and summary counts | `index.html` `#homePage`; `app.js` `renderHome` | Reads `amischolar.web.v2` (fallback `amischolar.web.v1`); no separate write | `web-e2e.spec.cjs`: mobile research journey checks dashboard and module count. Count accuracy: UNVERIFIED. |
| Create paper | `#headerNew`, `#heroNew`, `#projectNew`; `#projectForm` | Reads/writes project fields in localStorage key `amischolar.web.v2` | Mobile research journey creates a paper. |
| Edit paper | Workspace `#editPaper` | Reads current project; writes its title, question, discipline, citation style to `amischolar.web.v2` | `characterization.spec.cjs`: edits title and confirms it in My papers. Question/style edit round trip: UNVERIFIED. |
| Switch paper | Workspace `#switchPaper`; routes to My papers | Reads project list; sets active `projectId` in memory | `characterization.spec.cjs`: opens a selected paper from a two-paper list and checks workspace title. The direct `Switch paper` button: UNVERIFIED. |
| My papers | `aside [data-nav=projects]`; `#projectsPage` | Reads `projects`; opening a paper selects its ID in memory | Mobile research journey navigates to My papers after reload and checks the created title. Multi-paper behavior: UNVERIFIED. |
| Research modules index | Dashboard `#homeModules`; nav `#modulesPage` / `#allModules` | Reads module definitions in `app.js`; paper data unchanged | Mobile journey checks 12 cards; contextual-help test visits all 12. |
| Topic Lab | Open Topic Lab in the modules index | Reads current paper question and module records; writes researcher-authored records to `amischolar.web.v2` | Mobile journey saves its template; characterization checks its question guidance and template. Question-check edge cases: UNVERIFIED. |
| Literature Radar | Open Literature Radar | Reads project records; queries Crossref or OpenAlex when explicitly searched; imports returned metadata as an unverified source record | Mocked Crossref search/import test covers metadata labeling; characterization checks workflow guidance/template. OpenAlex and offline/error handling: UNVERIFIED. |
| Evidence Matrix | Open Evidence Matrix | Reads claim/source records and `evidenceLinks`; writes records and links in `amischolar.web.v2` | Tests cover template, CSV export, link persistence after reload, and Web-to-Android link snapshot; characterization checks workflow guidance/template. Markdown export and all link statuses: UNVERIFIED. |
| Outline Architect | Open Outline Architect | Reads/writes project records in `amischolar.web.v2` | Contextual-help and characterization tests check its guidance/template. Saved-outline analysis: UNVERIFIED. |
| Draft Studio | Open Draft Studio | Reads/writes project records; exports draft text, Markdown, HTML, and print dialog content | Tests cover Markdown, TXT, HTML download, and print-window content. Actual PDF printing depends on the browser print dialog: UNVERIFIED. |
| Style Coach | Open Style Coach | Reads draft records; writes revision records in `amischolar.web.v2` | Contextual-help and characterization tests check its guidance/template. Signal output with varied drafts: UNVERIFIED. |
| Citation Center | Open Citation Center | Reads/writes citation/source records; uses bundled citeproc and CSL assets; imports/exports CSL-JSON | Tests cover metadata export, CSL-JSON import, all 7 bundled CSL style identifiers, bibliography formatting for all 7, and bibliography Markdown download; characterization checks workflow guidance/template and asserts the six paper-form choices. |
| Originality & Integrity | Open Originality & Integrity | Reads/writes project records; reports checklist-style guidance, not external plagiarism results | Contextual-help and characterization tests check guidance/template. Saved-record behavior: UNVERIFIED. |
| Review Simulator | Open Review Simulator | Reads/writes researcher records; simulated checklist only | Contextual-help and characterization tests check checklist/template. Saved-record behavior: UNVERIFIED. |
| Submission Kit | Open Submission Kit | Reads/writes project records | Contextual-help and characterization tests check due-diligence guidance/template. Saved-record checks: UNVERIFIED. |
| Revision & Response | Open Revision & Response | Reads/writes project records; compares saved revision material in-app | Contextual-help and characterization tests check guidance/template. Revision comparison: UNVERIFIED. |
| Archive & Reproducibility | Open Archive & Reproducibility | Reads all records for selected paper; writes reproducibility records; exports Android-format single-paper JSON | Contextual-help and characterization tests check guidance/template; Android-format download and restore are tested separately. Full Android round trip: UNVERIFIED. |
| Contextual Help | “Help & research guidance” in each research module | Reads module guidance and current question/records; no persistent write | Contextual-help test covers all 12 modules, four steps, and personalized suggestion area. |
| Research records (title/content, save/edit/delete/clear) | Workspace form `#recordForm`; record actions in `#recordList` | Reads/writes/deletes records and dependent evidence links in `amischolar.web.v2`; clear resets the form | Existing mobile journey saves a record. `characterization.spec.cjs` covers edit/save and Clear. Delete confirmation: UNVERIFIED. |
| Source library | `aside [data-nav=library]`; `#libraryPage` | Reads records categorized as `literature-radar` or `citation-center`; “Open record” routes to workspace | `characterization.spec.cjs` covers listing and opening a saved source. |
| Backup & settings | `aside [data-nav=settings]`; `#settingsPage` | Web backup reads projects, records, links; restore replaces current research after confirmation. Android backup snapshots one paper. | Existing mobile journey checks web-backup download filename. `characterization.spec.cjs` covers web v1 and Android project restore, and web/Android exports. Web v2 restore: UNVERIFIED. |
| Connection status | Header `#connection`; `app.js` `connection()` | Reads `navigator.onLine`; listens for `online` and `offline`; no persistence | `characterization.spec.cjs` dispatches offline/online events and checks the visible indicator. |
| Researcher/student mode switch | Buttons injected by `student.js` after header | Reads/writes localStorage `amischolar.workspace.mode`; visibility changes; no research data migration | Student E2E checks student/researcher switch and retained 12 research modules. |

The web researcher store currently uses localStorage key `amischolar.web.v2`, with legacy fallback `amischolar.web.v1`. Its objects contain `version`, `projects`, `records`, and `evidenceLinks`. Initialization currently sets `version: 2`; no explicit versioned migration log was found: UNVERIFIED by tests.

## Student workspace

Student mode is a separate offline workspace in `student.js`. It uses IndexedDB database `amischolar-academic-v1`, version 1, with stores `semesters`, `courses`, `tasks`, `notes`, `decks`, `cards`, `reviews`, `sessions`, `milestones`, `links`, and `preferences`. It also uses localStorage `amischolar.workspace.mode` and sessionStorage `amischolar.focus.active` for mode and active timer state.

| Feature | Current entry point | Reads / writes | Existing test coverage |
| --- | --- | --- | --- |
| Student overview | Student Mode → Overview | Reads courses, tasks, cards, sessions from IndexedDB | Student E2E covers entry and reload, but does not assert overview totals: UNVERIFIED. |
| Courses and semesters | Student Mode → Courses | Reads/writes `courses` (the `semesters` store has no visible workflow located) | Student E2E creates and finds a course. Semester workflow: UNVERIFIED. |
| Tasks and assignments | Student Mode → Tasks | Reads/writes `tasks`; may link course and paper IDs | Student E2E creates a task. Due dates/status transitions/linking: UNVERIFIED. |
| Notes | Student Mode → Notes | Reads/writes `notes`; Markdown source retained locally; can link course/paper | Student E2E creates a note and verifies it after reload. Search/edit/delete: UNVERIFIED. |
| Flashcards and decks | Student Mode → Flashcards | Reads/writes `decks`, `cards`, `reviews`; review scheduling is local | Student E2E creates a deck/card, reviews offline, and checks no due card remains. Manual card management and scheduler history: UNVERIFIED. |
| Focus timer and session log | Student Mode → Focus | Reads/writes `sessions`; active timer uses sessionStorage | UNVERIFIED. |
| Calendar | Student Mode → Calendar | Reads student tasks/coursework; writes are not confirmed from current tests | UNVERIFIED. ICS import/export requested in the brief is not present in the inspected implementation: UNVERIFIED/absent. |
| Student backup and restore | Backup & settings → Student workspace backup | Reads/writes all student IndexedDB stores; restore replaces all stores after confirmation | `characterization.spec.cjs` covers JSON backup download. Student restore: UNVERIFIED. |

## Android application (`app/`)

| Feature | Current entry point | Reads / writes | Existing test coverage |
| --- | --- | --- | --- |
| Android research workspace and navigation | `MainActivity.kt`; Compose routes and `ResearchModules.all` | Room `ScholarDatabase`, project/note/source/evidence DAOs | Unit tests include `ResearchModulesTest`; emulator/UI coverage is limited to `app/src/androidTest`. Full navigation characterization: UNVERIFIED. |
| Android paper/project persistence | Dashboard/New Paper and workspace | Room tables `research_projects`, `research_notes`, `research_sources`, `evidence_links` | `EvidenceMigrationInstrumentedTest`, `ProjectBackupInstrumentedTest`; project CRUD coverage: UNVERIFIED. |
| Android research modules | 12 module routes defined by `ResearchModules.kt`; module UI in `ResearchModuleWorkspace.kt` and `WorkspaceScreen.kt` | Project and workspace Room records | Unit tests cover planner/analysis/quality helpers; module-by-module UI behavior: UNVERIFIED. |
| Literature metadata | Literature Search panel; Crossref, OpenAlex, PubMed, arXiv, DOAJ repositories | Public provider metadata; imported records stored locally in Room | Repository unit tests exist for Crossref, OpenAlex, PubMed, DOAJ; live provider/CORS behavior: UNVERIFIED. |
| Citation tools | Offline Citation panel and citation feature classes | Bundled CSL assets and local source metadata | `CitationFormatterTest`, `CitationAuditTest`, `OfflineCslAssetsInstrumentedTest`; full UI style selection: UNVERIFIED. |
| Evidence links | Evidence Links panel | Room `evidence_links` with project/note/source references | Helper tests and migration instrumented test exist; full relationship UI characterization: UNVERIFIED. |
| Backup/restore | Project backup controls in workspace | Room project, notes, sources, links; restore creates a new project transactionally | `ProjectBackupInstrumentedTest`; cross-format web↔Android compatibility: UNVERIFIED. |
| Document export | `DocumentExporter.kt` / workspace export controls | Reads current project and saved notes/sources | `DocumentConformanceTest` and `ExportInstrumentedTest`; every exported file behavior: UNVERIFIED. |

## Exported file formats found in code

| Format / filename | Entry point | Data read | Existing test |
| --- | --- | --- | --- |
| Full web backup JSON (`am-i-scholar-web-backup.json`; format versions 1 and 2 accepted on restore) | Backup & settings | All web projects, records, evidence links | Filename only in mobile journey; restore compatibility: UNVERIFIED. |
| Android-compatible single-paper JSON (`am-i-scholar-android-project.json`) | Settings or Archive module | One paper, notes, sources, evidence links | Snapshot partially checked by `web-e2e.spec.cjs`; export/download and Android import: UNVERIFIED. |
| Student workspace JSON (`am-i-scholar-academic-backup.json`; format v1) | Student backup | All student IndexedDB stores | UNVERIFIED. |
| Evidence CSV (`am-i-scholar-evidence.csv`) | Evidence Matrix | Current paper’s evidence rows | Filename tested; field/value conformance: UNVERIFIED. |
| Evidence Markdown (`am-i-scholar-evidence.md`) | Evidence Matrix | Current paper’s evidence rows | UNVERIFIED. |
| Draft text (`am-i-scholar-draft.txt`) | Draft Studio | Current paper’s draft records | UNVERIFIED. |
| Manuscript Markdown (`am-i-scholar-manuscript.md`) | Draft Studio | Paper title and draft records | Filename tested. |
| Print-ready manuscript HTML (`am-i-scholar-manuscript.html`) | Draft Studio | Paper title and draft records | UNVERIFIED. |
| Print / Save PDF (browser print dialog; no fixed filename) | Draft Studio | Paper title and draft records | UNVERIFIED. |
| CSL-JSON metadata (`am-i-scholar-csl-data.json`) | Citation Center | Current paper’s citation/source records | Filename tested; roundtrip field preservation tested. |
| CSL bibliography Markdown (`am-i-scholar-csl-bibliography.md`) | Citation Center | Formatted bibliography from saved metadata | UNVERIFIED. |
| Android document exports (plain text, DOCX, PDF, and project backup JSON via Android document provider) | Android workspace export controls | Current project, notes and sources | `DocumentConformanceTest`, `ExportInstrumentedTest`, `ProjectBackupInstrumentedTest`; export-by-export: UNVERIFIED. |

## Baseline evidence and open gaps

- Existing web browser tests: 14 Playwright cases across `web-e2e.spec.cjs` and `student-e2e.spec.cjs`; one Node static/syntax smoke test. Five additional characterization cases are in `characterization.spec.cjs`; a fifth case checks distinct workflow guidance and templates across all 12 modules.
- Existing Android tests: 16 Kotlin unit-test files, 4 instrumented-test files, and CI workflows for Android build/unit tests and emulator tests.
- Current characterization remains incomplete for cases marked UNVERIFIED above. Close gaps before refactoring the corresponding feature.
- The checked-in `docs/` static-site copy mirrors the web app; any web changes must preserve or deliberately regenerate that copy.
