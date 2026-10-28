# Phase 1 — Research Core

## Current baseline

This phase upgrades the research tools additively. The web Literature Radar now searches Crossref, OpenAlex, and PubMed concurrently by default, with provider selection, a 20-result cap per provider, DOI-first deduplication, optional contact email, Unpaywall Open Access enrichment, and local source status/notes. Imported metadata remains unverified. Citation Center supports offline CSL formatting plus CSL-JSON, BibTeX, and RIS import/export; missing-field warnings are separate from formatted output. It also opens local PDFs with pinned PDF.js 6.4.299 and stores selected text highlights and notes in the paper's local records.

Android already contains Crossref, OpenAlex, PubMed, arXiv, and DOAJ repositories plus Room-backed source records and backup/export flows. Federated search, BibTeX/RIS web controls, and the PDF reader are web-only in this increment; Android parity and provider-specific rate/error behavior remain tracked work, not an implied delivery.

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
- Broaden BibTeX/RIS fixtures for less common entry and tag variants.
- Verify PDF.js offline behavior for CMaps and uncommon embedded font cases across browsers.
- Keep Android search, backup, and export behavior covered; add Android scope notes for web-only features.

## Data and behavior constraints

- Preserve the existing source log, localStorage/IndexedDB keys, six paper citation choices, and all current export filenames.
- Keep every search user-initiated. Search queries and the configured email are sent only as disclosed in the UI; imported user research records remain local.
- Do not mark metadata as verified merely because a provider returned it.
- Document changed behavior in `CHANGELOG.md` and update the feature inventory, README feature table, and root/`docs/` static mirrors with only verified capabilities.

## Increment 2: portable references and metadata checks

- Import and export common BibTeX entry types and RIS tags through the existing local citation records; keep the original CSL-JSON and CSL bibliography workflows available.
- Preserve known authors, title, venue, year, volume/issue, pages, DOI, URL, publisher, and abstract. Do not fill absent facts.
- Display advisory warnings for missing core citation fields in a separate panel; the same records must produce unchanged CSL output.
- Acceptance: mocked BibTeX and RIS round trips preserve common fields, citations with missing fields display warnings, and CSL output stays free of warning text.

## Increment 3: local PDF reading

- Vendor the pinned PDF.js 6.4.299 generic build and Apache 2.0 license with its supporting assets; keep the viewer local and offline-capable.
- Open PDF files from the device without uploading or storing the PDF bytes. Save selected passages and optional notes as project-local records; reselect the file after reload to restore their highlights.
- Acceptance: a local sample PDF renders; text selection saves a page-linked highlight and note; the saved highlight reappears after reload and file reselection.
