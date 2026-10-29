# Phase 4 — Local lecture capture

## Goal

Let students record lecture audio with an unmistakable recording state and keep transcripts editable and local. Recording begins only after a direct user action and microphone permission. The app must never upload, sync, or send audio to speech or AI services.

## Privacy and platform boundary

- Microphone access is requested only after the user taps Start recording.
- Show a persistent recording indicator and elapsed time while the microphone is active; stopping is always one tap away.
- Save recordings in local browser storage or Android app-private storage. No analytics, network requests, background recording, or automatic upload.
- Web supports local recording and manual transcript entry. Do not use Web Speech or another service unless it can prove on-device operation; this phase has no web speech service.
- Android may offer Android's on-device recognizer only when the OS reports it available. Prefer on-device recognition explicitly and never fall back to cloud recognition. When unavailable, leave the transcript editable for manual entry.
- Limit capture duration and storage size. Surface quota/full-storage errors without losing existing recordings.
- Let users play, rename, edit transcripts, export audio/transcript locally, and delete with confirmation.
- Existing research/student backups must stay compatible; lecture audio is not silently embedded into the existing JSON backups.

## Evaluation set

1. No recording begins before user action and permission.
2. Visible recording state and elapsed time appear while active.
3. Stop ends microphone capture and finalizes a playable local file.
4. Reload/reopen retains saved recording metadata and transcript.
5. Browser storage and Android app-private storage contain the recording; no network call occurs.
6. Permission denial, unsupported codec/API, quota exhaustion, and interrupted capture produce clear recoverable states.
7. Android transcription uses only an on-device recognizer when available; unsupported devices use manual transcript entry.
8. Transcript edits persist and export independently from the audio file.
9. Deletion removes metadata and local audio.
10. Existing Phase 0–3 E2E, PWA, Android unit/lint, migration, and emulator suites remain green.

## Increment increments

1. Add the tested privacy contract, model/storage schema, and platform capability checks.
2. Add local web recording/manual transcript and Android recording/on-device transcription/manual fallback.
3. Add export/delete, migration, and regression tests; document platform-specific support.

## Acceptance criteria

- Web and Android controls have matching user-facing recording state, transcript editing, local export, and deletion.
- The app makes no network request for audio or transcripts.
- Android migration tests preserve research and student records through the new schema.
- E2E and emulator tests cover start/stop, persistence, transcript, export, and delete flows.
- User-device testing remains deferred until the app and formal APK/testing URL are ready.
