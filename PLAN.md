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
| Phase 0 PR CI at code/test commit `4e84189` (2026-10-28) | PASS for the configured workflow, but it ran only `web-e2e.spec.cjs`; `student-e2e.spec.cjs` and `characterization.spec.cjs` were not included. |
| Phase 0 PR CI at test commit `11e657f` (2026-10-28) | PASS: all configured web, student, and characterization specs; Web PWA passed. Android CI/emulator checks were still running at the time Phase 1 characterization began. |
| Android tests locally | NOT RUN: no Gradle executable or wrapper is checked in. Baseline and PR Android build/unit/emulator workflows passed remotely. |

The original CI results did not establish a complete browser baseline because their workflow selected only `web-e2e.spec.cjs`. The config and workflow now include all three Playwright spec files, and the complete 20-test browser suite passed at `11e657f`. Local Chromium installation is blocked by the runner CDN download failure. No application behavior has changed on the Phase 0 branch.

## Gaps before implementation

1. Keep `web-e2e.spec.cjs`, `student-e2e.spec.cjs`, and `characterization.spec.cjs` green in CI; add and run characterization for every new research-core behavior before modifying it.
2. Add the remaining characterization coverage listed in `docs/FEATURE_INVENTORY.md` before refactoring those behaviors.
3. Add Android characterization coverage where user-visible behavior is otherwise unverified; latest baseline Android CI and emulator runs passed.
4. Determine past web backup schemas from repository history and add fixtures/tests before altering persistence or restore code.
5. Compare current root web assets against `docs/` copies and define a repeatable mirror check.
6. Verify current CSP, external origins, accessibility/PWA scores, and live API CORS/rate limits before making claims or changing trust/privacy behavior.

## Phase sequence

1. **Phase 0 — baseline and characterization:** complete inventories, get existing tests passing, add characterization tests, identify and repair pre-existing failures without changing behavior.
2. **Phase 1 — research core:** upgrade literature search/source workflow and citations additively; preserve current exports and verify backup interoperability.
3. **Phase 2 — student tools:** add offline ICS import/export and calendar handling, durable linked focus sessions with streak/time summaries, improve task/course/calendar flow, and deliver Student Mode parity on Android while keeping existing modules and backups interoperable. Acceptance criteria are tracked in [`docs/PHASE_2_STUDENT_TOOLS.md`](docs/PHASE_2_STUDENT_TOOLS.md).
4. **Phase 3 — opt-in AI:** implement only after the source-grounded, privacy-preserving boundary and evaluation set are in place.
5. **Phase 4 — lectures:** local recording/transcription with a visible recording state.
6. **Phase 5 — data ownership and sync:** encrypted backup and opt-in user-owned sync with prior backups still restorable.
7. **Phase 6 — Android and distribution:** keep Android coverage/build green and automate release packaging.
8. **Phase 7 — trust and growth:** verified landing-page claims, public roadmap, and citation-method explanation.

## Acceptance gate

Do not begin a feature phase until Phase 0 has a green regression baseline and all tests needed to characterize the touched behaviors pass in CI. For every phase, preserve existing feature entry points and data, document behavior changes in `CHANGELOG.md`, update the feature inventory and README only with verified capabilities, and record deferred items with reasons.

Phase 1 and Phase 2 implementations are complete on separate review-ready branches. Phase 3 manual AI handoff follows Phase 2 on its own dependent branch after the source-grounded boundary and evaluation set were committed. Web E2E/PWA, Android CI, and emulator checks pass on Phase 3's current head. Cloud-provider APIs and local model runtimes remain deferred; no user-device testing begins before the app and formal APK/testing URL are ready.
