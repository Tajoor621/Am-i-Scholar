# MVP roadmap

## Stage 0 — Bootstrap
- [x] Android/Kotlin/Compose project skeleton
- [x] Material 3 theme and navigation foundation
- [x] Room persistence for basic research projects
- [x] Workflow metadata for planned research areas
- [x] Initial CI workflow and project documentation
- [ ] Confirm CI build and tests pass
- [ ] Add and validate Gradle wrapper for reproducible local builds

## Stage 1 — Local workspace
- Project editing and deletion with confirmation
- Settings and citation-style preference persistence
- Room migration tests and backup/export
- Onboarding, integrity pledge, and author voice card
- Accessibility pass: TalkBack, font scaling, contrast, target sizes

## Stage 2 — Sources and evidence
- OpenAlex and Crossref search with provider attribution and rate-limit handling
- DOI metadata lookup and explicit verification status
- Source saving, duplicate detection, RIS/BibTeX/CSL-JSON import/export
- Evidence matrix based on researcher-confirmed notes
- PDF import and local attachment management

## Stage 3 — Draft and citations
- Structured outline editor
- Draft paragraphs linked to source/evidence records
- Unsupported-claim warnings
- APA, MLA, Chicago, IEEE, Harvard, and Vancouver formatting
- Markdown export, then DOCX/PDF

## Stage 4 — Optional AI
- Provider abstraction with local-first policy
- Explicit consent for cloud operations
- Source-grounded prompts and context limits
- AI-use log and disclosure export
- Tests for citation fabrication and unsupported claims where feasible

## Stage 5 — Submission and reproducibility
- Journal discovery with source/date labels
- Fee and policy verification reminders
- Submission kit and revision tracker
- Reproducibility archive
- Security, privacy, licensing, and accessibility review

A milestone is complete only after implementation, tests, documentation, and verification.
