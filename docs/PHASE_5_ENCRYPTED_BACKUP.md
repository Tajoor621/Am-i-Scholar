# Phase 5 — Encrypted backup ownership

## Goal

Give users a portable, encrypted backup they can explicitly save and restore across web and Android. Preserve every existing plaintext backup and restore path for compatibility.

## Binding privacy boundary

- Backup creation and restore happen only after an explicit user action.
- No account, cloud sync, automatic folder save, background export, analytics, or remote backup service is added.
- Lecture audio is excluded. Lecture metadata/transcripts remain outside legacy backup formats until a separate portable-audio decision is made.
- Passwords and key material are used only in memory and are never stored or logged.

## Encrypted file format

- A versioned JSON envelope identifies the format and crypto parameters.
- Derive a 256-bit key from the user passphrase using PBKDF2-HMAC-SHA-256 and a per-backup random salt.
- Encrypt the portable payload using AES-256-GCM with a random 96-bit IV and a 128-bit authentication tag.
- Authenticate the fixed envelope header as additional authenticated data; reject unknown versions, unsupported parameters, corrupt ciphertext, and wrong passphrases before restore.
- Use matching UTF-8 and base64 encoding on web and Android so one encrypted file can be opened by both apps.

## Restore and compatibility

- Decrypt and validate the full payload before writing any data.
- Show a conflict preview with source, record counts, destination actions, and warnings before confirmation.
- Apply only after confirmation; failed validation/decryption must leave current data unchanged.
- Retain all historical unencrypted project, web, and student backup formats and their fixtures.
- Exclude recordings/audio blobs. The full encrypted payload contains web research data, portable Android project records, and the shared student-workspace backup.

## Evaluation set

1. Explicit export produces a versioned encrypted file; no plaintext backup payload is present outside its authenticated encryption.
2. Wrong passphrase, modified header, modified ciphertext, oversized files, and unknown format versions are rejected before writes.
3. A file encrypted on web decrypts on Android and a file encrypted on Android decrypts on web.
4. Same-platform restore previews the record counts and preserves content hashes.
5. Cross-platform restore retains portable research records and student records with valid references.
6. Cancelling preview or any preflight failure leaves existing web and Room data unchanged.
7. Every prior backup fixture remains readable by its original restore path.
8. Existing web E2E/PWA, Android build/lint/unit, migration, and emulator suites remain green.

## Exclusions

Cloud sync, Google Drive, private Gist, accounts, automatic folder autosave, and audio backup are out of scope under the current explicit-export and no-sync constraints.

## Device gate

Physical-device verification remains deferred until the app and formal APK/testing URL are ready.

## Current implementation status

The current Phase 5 PR implements the encrypted browser research-backup slice. It uses a 310,000-iteration PBKDF2-HMAC-SHA-256 derivation, a 256-bit AES-GCM key, random 16-byte salts and 12-byte IVs, and authenticated envelope metadata. Browser restore decrypts and checks the web-backup structure before showing a replacement-count confirmation. Existing unencrypted formats remain available.

The remaining Phase 5 work is Android-native encrypted export/import using the same envelope, student-workspace inclusion and restore, and full cross-platform round-trip evaluation. These are intentionally called out as pending; this PR must not be treated as completing the phase. No student or audio records are included in this browser research-backup slice.
