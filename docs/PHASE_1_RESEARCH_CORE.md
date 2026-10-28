# Phase 1 — Research Core

## Current baseline

This phase upgrades the research tools additively. The web Literature Radar now searches Crossref, OpenAlex, and PubMed concurrently by default, with provider selection, a 20-result cap per provider, DOI-first deduplication, optional contact email, Unpaywall Open Access enrichment, and local source status/notes. Imported metadata remains unverified. The Citation Center has offline CSL formatting and CSL-JSON import/export; BibTeX/RIS import/export and a local PDF reader remain later increments.

Android already contains Crossref, OpenAlex, PubMed, arXiv, and DOAJ repositories plus Room-backed source records and backup/export flows. The federated search UI in this increment is web-only; Android parity and its provider-specific rate/error behavior remain tracked work, not an implied delivery.

## Increment 1: federated literature discovery

1. Characterize the current OpenAlex result/import path and offline/error behavior before implementation. These tests are in `tests/characterization.spec.cjs` on the Phase 1 branch and must pass in CI first.
2. Search Crossref, OpenAlex, and PubMed concurrently by default; preserve per-provider selection as an option. (Implemented in this increment.)
3. Normalize titles, authors, year, venue, abstract, DOI, provider, and Open Access information from returned provider metadata. Deduplicate primarily by normalized DOI and secondarily by title/year/first author.
4. Add a local, user-configurable contact email. Send it only with explicit literature searches to providers that request identification and Unpaywall when enriching DOI results. Keep the preference on-device and disclose its use in the UI. (Implemented in this increment.)
5. Show partial-provider failures and offline state clearly. A failed provider must not hide successful results from the other providers.
6. Keep imported metadata marked unverified. Save each result in the existing source library, with researcher-editable status (`to-read`, `reading`, or `cited`) and notes. (Implemented in this increment.)

### Acceptance checks

- A mocked “CRISPR off-target effects” search returns at least 20 DOI-deduplicated records from the three providers.
- Crossref, OpenAlex, and PubMed source failures are independently visible; one provider failure does not discard other results.
- An imported source retains its provider fields, abstract, DOI, Open Access link/status when returned, local status, and notes after reload and backup/restore.
- No citation is synthesized from model memory. Missing bibliographic fields remain missing.
- Verify provider CORS and rate limits with official documentation and browser tests; document any provider-specific limitations.

## Later increments

- Verify live-provider browser CORS and rate behavior beyond the mocked browser acceptance cases; document provider-specific limitations.
- Add BibTeX and RIS import/export while preserving existing exports.
- Add citation-field warnings without changing the existing formatted output for an unchanged input.
- Vendor and pin pdf.js; add local PDF viewing, highlights, and notes.
- Keep Android search, backup, and export behavior covered; add Android scope notes for web-only features.

## Data and behavior constraints

- Preserve the existing source log, localStorage/IndexedDB keys, six paper citation choices, and all current export filenames.
- Keep every search user-initiated. Search queries and the configured email are sent only as disclosed in the UI; imported user research records remain local.
- Do not mark metadata as verified merely because a provider returned it.
- Document changed behavior in `CHANGELOG.md` and update the feature inventory, README feature table, and root/`docs/` static mirrors with only verified capabilities.
