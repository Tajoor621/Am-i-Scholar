# Phase 0 plan

## Feature inventory summary

Baseline reviewed: `main` at the current checkout. The web contains Dashboard, My papers, 12 research modules, Source library, Backup & settings, a six-choice paper citation-style form, saved research records, student mode, and local connection status. Android has a separate Room-backed research workspace, 12 module routes, source/citation/evidence tools, backups, and document exports. `docs/FEATURE_INVENTORY.md` records entry points, data stores, existing tests, export formats, and all identified characterization gaps.

## Current test status

| Check | Result |
| --- | --- |
| `node tests/web-smoke.cjs` | PASS: static/syntax checks for root web app and `docs/` copy. |
| `node --check tests/characterization.spec.cjs` | PASS: new Playwright specification parses. |
| `npx playwright test` locally | BLOCKED: browser executable missing; Chromium CDN download repeatedly returned a zero-byte/truncated archive. |
| Main CI at baseline commit `73e5340` (2026-10-17) | PASS: Web browser E2E, Web PWA verification, Android CI, and Android Emulator Verification runs all completed successfully. |
| Android tests locally | NOT RUN: no Gradle executable or wrapper is checked in. Baseline Android CI and emulator workflows passed remotely at `73e5340`. |

Remote CI establishes a passing baseline for the repository's existing tests at `73e5340`. The local container could not launch Chromium or Gradle. The application code was not changed in this phase, so no application failure has been repaired. The initial four new web characterization tests passed in PR CI; the latest fifth module-wide test awaits PR CI.

## Gaps before implementation

1. Run existing and newly added browser tests on this branch in CI; fix any discovered failures before feature work.
2. Add the remaining characterization coverage listed in `docs/FEATURE_INVENTORY.md` before refactoring those behaviors.
3. Add Android characterization coverage where user-visible behavior is otherwise unverified; latest baseline Android CI and emulator runs passed.
4. Determine past web backup schemas from repository history and add fixtures/tests before altering persistence or restore code.
5. Compare current root web assets against `docs/` copies and define a repeatable mirror check.
6. Verify current CSP, external origins, accessibility/PWA scores, and live API CORS/rate limits before making claims or changing trust/privacy behavior.

## Phase sequence

1. **Phase 0 — baseline and characterization:** complete inventories, get existing tests passing, add characterization tests, identify and repair pre-existing failures without changing behavior.
2. **Phase 1 — research core:** upgrade literature search/source workflow and citations additively; preserve current exports and verify backup interoperability.
3. **Phase 2 — student tools:** expand cards, timer, tasks/calendar, and ICS flows while keeping current student modules working.
4. **Phase 3 — opt-in AI:** implement only after the source-grounded, privacy-preserving boundary and evaluation set are in place.
5. **Phase 4 — lectures:** local recording/transcription with a visible recording state.
6. **Phase 5 — data ownership and sync:** encrypted backup and opt-in user-owned sync with prior backups still restorable.
7. **Phase 6 — Android and distribution:** keep Android coverage/build green and automate release packaging.
8. **Phase 7 — trust and growth:** verified landing-page claims, public roadmap, and citation-method explanation.

## Acceptance gate

Do not begin a feature phase until Phase 0 has a green regression baseline and all tests needed to characterize the touched behaviors pass in CI. For every phase, preserve existing feature entry points and data, document behavior changes in `CHANGELOG.md`, update the feature inventory and README only with verified capabilities, and record deferred items with reasons.
