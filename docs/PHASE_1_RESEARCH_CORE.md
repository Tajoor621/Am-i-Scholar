# Phase 1 — Research Core

## Current baseline

This phase upgrades the current web and Android research tools additively. The web Literature Radar currently searches one provider at a time (Crossref or OpenAlex), caps each request at 20 results, and imports records into the existing local source log as unverified metadata. The web does not currently search PubMed, request contact email, provide Open Access links, or store source status/notes as structured fields. The Citation Center has offline CSL formatting and CSL-JSON import/export; it does not yet import/export BibTeX or RIS. No PDF reader is present in the web app.

Android already contains Crossref, OpenAlex, PubMed, arXiv, and DOAJ repositories plus Room-backed source records and backup/export flows. New web capabilities must be scoped for Android in the same phase or explicitly recorded as web-only.

## Increment 1: federated literature discovery

1. Characterize the current OpenAlex result/import path and offline/error behavior before implementation. These tests are in `tests/characterization.spec.cjs` on the Phase 1 branch and must pass in CI first.
2. Search Crossref, OpenAlex, and PubMed concurrently by default; preserve the existing per-provider selection as an option.
3. Normalize titles, authors, year, venue, abstract, DOI, provider, and Open Access information from returned provider metadata. Deduplicate primarily by normalized DOI and secondarily by title/year/first author.
4. Add a local, user-configurable contact email. Send it only with explicit literature searches to the providers that request identification. Keep the preference on-device and disclose its use in the UI.
5. Show partial-provider failures and offline state clearly. A failed provider must not hide successful results from the other providers.
6. Keep imported metadata marked unverified. Save each result in the existing source library, with researcher-editable status (`to-read`, `reading`, or `cited`) and notes.

### Acceptance checks

- A mocked “CRISPR off-target effects” search returns at least 20 DOI-deduplicated records from the three providers.
- Crossref, OpenAlex, and PubMed source failures are independently visible; one provider failure does not discard other results.
- An imported source retains its provider fields, abstract, DOI, Open Access link/status when returned, local status, and notes after reload and backup/restore.
- No citation is synthesized from model memory. Missing bibliographic fields remain missing.
- Verify provider CORS and rate limits with official documentation and browser tests; document any provider-specific limitations.

## Later increments

- Add Unpaywall DOI lookups for Open Access locations when the user has provided a contact email. Do not call Unpaywall's retired search endpoint.
- Add BibTeX and RIS import/export while preserving existing exports.
- Add citation-field warnings without changing the existing formatted output for an unchanged input.
- Vendor and pin pdf.js; add local PDF viewing, highlights, and notes.
- Keep Android search, backup, and export behavior covered; add Android scope notes for web-only features.

## Data and behavior constraints

- Preserve the existing source log, localStorage/IndexedDB keys, six paper citation choices, and all current export filenames.
- Keep every search user-initiated. Search queries and the configured email are sent only as disclosed in the UI; imported user research records remain local.
- Do not mark metadata as verified merely because a provider returned it.
- Document changed behavior in `CHANGELOG.md` and update the feature inventory, README feature table, and root/`docs/` static mirrors with only verified capabilities.
