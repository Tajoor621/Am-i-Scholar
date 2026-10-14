# Am I Scholar: implementation status

The app is an offline-first Android research workspace. The public GitHub Pages URL hosts documentation; it is **not** the native Android app.

## Implemented in development branch

- Local research projects: create, list, edit, delete with confirmation.
- Local notes, outlines and drafts attached to a project.
- Local source records with title, author, year, journal/publisher, DOI/URL and evidence notes.
- Explicit user-attested metadata verification flag (not automatic verification).
- Simplified bibliography drafts for APA, MLA, Chicago, IEEE, Harvard and Vancouver.
- Markdown, BibTeX and RIS exports through the Android document picker.
- Room schema migration from v1 to v2, preserving existing projects.
- CI build and JVM unit-test tasks.

## Not yet implemented

- Online discovery via OpenAlex, Crossref, PubMed, Semantic Scholar, arXiv, CORE or other APIs.
- PDF ingestion and text extraction, automatic DOI resolution and citation verification.
- Full standards-compliant CSL formatting and DOCX/PDF export.
- Local LLM inference, opt-in cloud AI, originality scans and advanced style coaching.
- Review simulator, journal matching, submission and reproducibility tools.
- Comprehensive accessibility testing, database migration instrumentation tests, device testing and signed release.

**Academic integrity:** Bibliography output is a metadata-based draft, not a claim that a source exists or was verified. Users must inspect original papers and verify each entry before submission. The app does not invent citations, research results or peer-review outcomes.

**Privacy:** The app does not request Internet permission in this milestone. Android backup is disabled to reduce inadvertent cloud copying of local research data. Export is user-initiated using Android's document picker.

**Release gate:** Do not merge or publish a production release until CI passes, source/data tests and accessibility checks are expanded, and the agreed initial feature set is completed. Device testing remains deferred by user instruction.
