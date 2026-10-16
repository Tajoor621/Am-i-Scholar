# Android CSL parity and evidence restore — incremental implementation

The Android Citation Center now includes an offline-only WebView hosting the **same citeproc-js engine, en-US locale and seven style XML definitions** as the web PWA. The app intercepts only its local virtual asset origin and blocks all other web requests; no JavaScript bridge or remote account is required. Source metadata is sent as JSON from Room records to the local renderer.

This adds real CSL processing to Android while retaining the legacy Kotlin draft templates in the full-workspace source cards for compatibility. Bibliography output remains researcher-reviewed. No cloud AI, subscriptions, or required keys were added.

Android Room v3 adds a non-destructive evidence relationship table, indexed foreign keys and cascading deletion. Android and web project backups continue to use `am-i-scholar-project` version 1 with an **optional** `evidenceLinks` array containing portable note/source indexes. Old backups without relationships still decode. Import is transactional on Android.

## Verification gates

- Web browser E2E now exercises local CSL APA and IEEE and portable evidence-link backup. Seven styles are packaged and selectable.
- Android instrumentation checks bundled CSL asset presence, project backup link roundtrip, and a populated Room v2 → v3 migration with cascade cleanup. Passing CI and emulator runs are required before declaring verification.
- **Not yet verified:** golden bibliographic fixtures for all seven styles, full WebView rendering on real devices, migration from a populated Room v2 database, Unicode and journal-specific edge cases, and accessibility with TalkBack.
- Remaining product features from the original 10/10 definition of done (DOCX/PDF quality, signed release APK, full submission/archive flow) require separate implementation and acceptance.

Third-party license notices for citeproc-js (CPAL/AGPL) and CSL style XML are preserved under `vendor/`. Verify license obligations before distributing binaries.
