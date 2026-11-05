# Am I Scholar

Offline-first academic research and writing workspace for the browser and Android. Papers, sources, citations, evidence, and study tools stay on the device. The app does not invent citations, data, or peer reviews.

**Live workspace:** [https://tajoor621.github.io/Am-i-Scholar/](https://tajoor621.github.io/Am-i-Scholar/)

The GitHub Pages site is the web app. It is not a separate marketing page.

## What you can do

| Area | What it does | Limit |
| --- | --- | --- |
| Research papers | Question, discipline, and citation style, then twelve modules from Topic Lab through Archive | You write the scholarship |
| Literature Radar | Searches Crossref, OpenAlex, and PubMed when you ask, and can check Unpaywall for open access | Metadata only. Imports stay unverified until you read the original. Browser CORS and provider limits apply |
| Evidence, outline, draft, style | Local records, evidence links, and draft export to Markdown, HTML, or print | Checks are heuristics, not a judgment of truth |
| Citation Center | Offline CSL drafts for APA, MLA, Chicago, IEEE, Harvard, and Vancouver; CSL-JSON, BibTeX, and RIS; local PDF highlights | A draft is not the journal’s house style |
| AI handoff | Builds a prompt from sources you select and stores a pasted reply in the Archive | This app does not call a model. The provider you choose has its own terms |
| Review Simulator | A checklist you fill in | Not a peer review and not a generated review |
| Student workspace | Courses, tasks, notes, flashcards, focus timer, ICS calendar, local lecture recording | Transcripts are manual. Recordings are not uploaded and are excluded from JSON backups |
| Backups | Web JSON, one-paper Android JSON, student JSON, and a passphrase-encrypted full backup | The passphrase cannot be recovered. Browser storage can be cleared |
| Android app | Room database, the same twelve modules, and literature providers that also include arXiv and DOAJ | Not a byte-for-byte copy of every web control |

Appearance (light, dark, or match this device), paper search, and source search are saved only as local preferences. They are not part of a research backup.

## Principles

- Evidence before prose. Draft support stays tied to notes and sources you provide.
- No fabricated scholarship. The app does not invent citations, DOIs, statistics, or reviewer comments.
- Local-first privacy. Core notes stay on the device. A literature search or a prompt you copy is an explicit action.
- You remain the author. The tools plan, check, and export. They do not ghostwrite a submission.
- Readable and usable offline after the first load, including from the installed web app.

## Use the web app

The site works from GitHub Pages. To run this repository locally:

```bash
python3 -m http.server 8000
```

Open [http://127.0.0.1:8000/](http://127.0.0.1:8000/). For the copy that GitHub Pages publishes, serve the `docs/` directory the same way.

After you change a web file, mirror it into `docs/` with `scripts/sync-docs.sh`. Pages publishes `docs/`.

Install the app from the browser when the device offers it. The service worker caches the workspace for offline use. Live literature search still needs a network.

## Android

1. Install Android Studio with Android SDK 35.
2. Open this repository and let Gradle sync.
3. Run the `app` configuration on Android 8.0 or newer (minimum SDK 26).

Generated build output and local machine settings are not committed. The CI workflow provisions Gradle and the Android SDK. A Gradle wrapper is not checked in.

## Checks

```bash
node tests/web-smoke.cjs
```

Browser coverage is Playwright (`tests/web-e2e.spec.cjs`, `tests/student-e2e.spec.cjs`, `tests/characterization.spec.cjs`). GitHub Actions runs those tests, a PWA smoke check, and the Android build and emulator workflows.

## Layout

- `index.html`, `app.js`, `student.js`, and the modules next to them are the web workspace.
- `docs/` is the GitHub Pages copy of that workspace, plus the longer design notes.
- `app/` is the Android project (Kotlin, Jetpack Compose, Material 3, Room).
- `brand/` holds the approved emblem and generated icons.
- `vendor/` includes PDF.js and citeproc, which keep their own licenses.

## Privacy and integrity

- [Privacy notes](docs/PRIVACY_POLICY.md)
- [Academic integrity](docs/ACADEMIC_INTEGRITY.md)
- [Feature inventory](docs/FEATURE_INVENTORY.md)
- [Architecture](docs/ARCHITECTURE.md)

This project is not affiliated with a university, publisher, or research database. Journal rules, fees, and provider limits have to be checked at the time you submit.

## License

Original code in this repository is under the [MIT license](LICENSE). Copyright (c) 2026 Tajoor621.

Bundled citeproc-js is dual-licensed under CPAL-1.0 or AGPL-3.0. See `vendor/` and `app/src/main/assets/csl/`. The MIT license applies to original code. Use of citeproc-js follows the license you choose for that library.
