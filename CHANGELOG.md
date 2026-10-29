# Phase 5: Encrypted Full Workspace Backup

- Add user-triggered encrypted export/import across web and Android using PBKDF2-HMAC-SHA-256 and AES-256-GCM, with passwords requested only for the current operation.
- Include web research, portable Android project snapshots, and shared student workspace records. Preserve all existing plaintext backup formats.
- Authenticate encryption metadata, enforce supported parameters and a 10 MB limit, validate complete payloads before restore, and show a restore preview.
- Keep cloud sync, accounts, automatic saves, analytics, and lecture audio excluded.
- Add browser and Android tests for authenticated encryption, cross-platform decryption, full payload validation, wrong passphrases, and metadata tampering.

# Changelog

## Unreleased — Phase 1 federated literature discovery

- Search Crossref, OpenAlex, and PubMed concurrently by default, with individual provider selection and partial-failure reporting.
- Normalize and merge provider metadata, deduplicating by DOI or title/year/first author.
- Add optional on-device contact email for provider polite access and Unpaywall Open Access lookups.
- Import metadata as explicitly unverified local source records with provider, abstract, Open Access details, fetched time, reading status, and notes.
- Update privacy disclosure, source-library status display, feature inventory, and mirrored GitHub Pages app files.
- Add mocked browser coverage for federated results, deduplication, provider email parameters, and source import fields.
- Add local BibTeX/RIS import-export for common bibliographic fields and advisory citation metadata warnings separate from CSL output.
- Add mocked round-trip and warning-isolation coverage; include the new converter in the offline PWA cache.
- Vendor PDF.js 6.4.299 and supporting assets for local PDF viewing, with page-linked text highlights and notes saved in project records.
- Keep PDF bytes in memory only; add the runtime and worker/supporting data to the PWA offline cache.

## Unreleased — Phase 3 opt-in AI handoff

- Add a manual, bounded source-grounded prompt preview on web and Android with per-copy consent; the app sends no AI requests.
- Save pasted AI-assisted text separately under Archive and provide an editable disclosure statement.
- Add evaluation cases for selected-only context, citation integrity, prompt limits, consent gating, disclosure wording, network boundary, and backup compatibility.

## Unreleased — Phase 4 local lecture capture

- Add user-started, locally stored lecture recordings in Student Mode on web and Android, with an active indicator, elapsed time, 15-minute and 100 MB limits, and stop-on-background behavior.
- Add local playback, title editing, manual transcript editing, separate audio/transcript export, and confirmed deletion. Audio remains outside the existing JSON backup formats.
- Add a Room v4→v5 lecture metadata migration and browser storage version 2; web and Android make no speech-service requests.
- Add browser E2E coverage for recording, persistence, transcript editing, deletion, and network boundary; Android migration coverage preserves existing research/student data.
