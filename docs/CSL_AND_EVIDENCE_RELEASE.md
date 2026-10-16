# Offline CSL and evidence relationships — implementation and acceptance

## CSL citation processor (web PWA)

The web Citation Center uses **citeproc-js**, a real CSL 1.0 processor, with locally vendored style XML and the en-US locale. All required assets are precached by the service worker for offline use after successful installation. A researcher must explicitly select **Format bibliography offline**. Citation rendering does not contact an external server.

Seven bundled styles:
- APA (American Psychological Association, 7th edition)
- MLA (Modern Language Association, 9th edition)
- Chicago author-date
- Chicago notes/bibliography, 17th edition
- IEEE
- Harvard, Cite Them Right variant
- Vancouver, Elsevier/NLM variant

**Formatting correctness depends on complete and accurate input metadata.** Existing record text is parsed into CSL-JSON items; unknown fields are omitted, never invented. The generated bibliography carries a verification warning. CSL-JSON is exported separately and uses stable source record IDs. Golden output fixtures and complete journal-specific acceptance remain to be completed.

### Third-party licenses and attribution

- citeproc-js: Copyright Frank Bennett and contributors, distributed under CPAL 1.0 or AGPL 3.0 (upstream dual-license notice). See `vendor/citeproc-CPAL.txt`, `vendor/citeproc-AGPLv3.txt`, and upstream `https://github.com/Juris-M/citeproc-js`. Keep attribution and comply with the chosen license in any redistribution.
- CSL style definitions: Citation Style Language styles repository `https://github.com/citation-style-language/styles`; each style includes its own copyright and license notices (typically Creative Commons BY-SA).
- Locale: `https://github.com/citation-style-language/locales`, licensed according to upstream notices.

This repository does not yet provide a comprehensive legal assessment of license compatibility; do not remove third-party attribution.

## Android evidence persistence

Room database upgraded from **v2 to v3**, adding `evidence_links` with foreign keys to projects, notes, and sources and cascading cleanup. The original tables are not modified. The Evidence Matrix screen includes a local claim-to-source editor. All support statuses are explicitly researcher-authored.

The existing `am-i-scholar-project` JSON format stays **version 1**; it gains an optional `evidenceLinks` array with `noteIndex`, `sourceIndex`, `status`, and `rationale`. Legacy backups without links continue to decode. Restore inserts the project, notes, sources, and links transactionally. Invalid indexes are rejected before insertion.

The web project export/import uses the same optional index-based relationship format. The web research backup stays at version 2 with optional `evidenceLinks` referencing web record IDs.

## Remaining blockers before 10/10

1. Android CSL bibliography output still uses simplified Kotlin draft templates; full native parity requires integration with the same CSL engine or independently validated equivalent.
2. CSL golden tests for all seven style definitions, Unicode names, multi-author, missing metadata, and DOI/URL edge cases need independent fixtures.
3. Android Room migration test from an actual populated v2 database and on-device Android↔web restore acceptance.
4. Complete DOCX/PDF rendering, accessibility/TalkBack, privacy/security, and signed release verification.
5. End-to-end idea → sources → evidence → manuscript → integrity → submission/archive acceptance.

No production-ready or 10/10 rating is asserted until all acceptance gates pass.
