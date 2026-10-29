# Phase 3 — Opt-in, source-grounded AI handoff

## Goal

Add a user-controlled AI-assisted research handoff while preserving Am I Scholar's local-first, zero-required-fee, and no-background-network behavior. The app will prepare bounded, source-grounded prompts and record any AI-assisted text the user brings back. It will not silently send project data to a model.

## Boundary before implementation

- **No in-app cloud provider or hosted inference in this increment.** There are no API keys, provider SDKs, automatic requests, accounts, or background calls.
- **Manual handoff only.** The user reviews the exact prompt, explicitly confirms the cloud-sharing disclosure for that handoff, then chooses to copy it. They select and contact any external AI service themselves; fees, retention, and provider terms are outside the app's control.
- **Local-only default.** Opening Draft Studio, creating a prompt, previewing it, and reviewing existing notes make no network request. The consent choice is not a standing consent and does not persist between handoffs.
- **Bounded context.** Include only source records and evidence notes the user selects. Do not include full drafts, project notes, PDFs, credentials, contact details, or unselected records. Enforce fixed source-count and character limits; show truncation before copy.
- **Evidence integrity.** Each included source has its saved title and DOI/URL, clearly labeled as verified or unverified. The generated prompt asks the model to use only those records, distinguish source facts from suggestions, and mark unsupported claims. It never fabricates citations or treats generated text as verified.
- **Human review.** Returned text is pasted by the user, visibly labeled AI-assisted/unverified, and saved separately in the local archive. It does not replace a draft or evidence record automatically.
- **Local AI-use log and disclosure.** Store the user-selected task, date, selected source identifiers, exact prompt, provider name entered by the user, returned text, and review status locally. Offer an editable disclosure statement derived from that log. No analytics or sync.
- **Cross-platform and backup.** Keep records compatible with the existing research backup and web/Android project backup without changing older backup formats. Preserve unsupported fields during import/export where practical.

## Implementation increments

1. Prompt-building rules, policy copy, and a small evaluation fixture land before any user-facing handoff.
2. Add the reviewed prompt preview, one-time consent, clipboard action, pasted-response review, local log, and disclosure export on web and Android.
3. Verify strict context bounds, no implicit content inclusion, consent gating, source attribution preservation, and backup round trips. Keep provider network integration out of scope until the user explicitly chooses a provider, pricing, and data-handling design.

## Evaluation set

The test fixture must cover:
1. Selected source metadata is included with its identifier and verification state.
2. Unselected sources and unrelated notes are excluded.
3. Drafts and full documents are never included by default.
4. Prompt size and selected-source count stay within documented limits.
5. Truncation is explicit and never silently removes citation identifiers.
6. Missing DOI/URL is reported without inventing one.
7. Unverified records remain labeled unverified.
8. Instructions request an uncertainty/unsupported-claim section.
9. Generated text is not saved as verified evidence or an ordinary manuscript draft.
10. Cloud-sharing consent is required separately for every copy action.
11. Declining consent leaves clipboard and stored records unchanged.
12. Log and disclosure export include the provider label and selected source identifiers but make no accuracy guarantee.
13. No app code calls a model endpoint or sends manuscript text in the handoff flow.
14. Existing research and student backup fixtures remain readable.

## Acceptance criteria

- Existing web E2E, characterization, PWA, Android unit/lint, Room migration, and emulator suites pass.
- New prompt/evaluation tests pass on web and Android.
- Tests prove consent is required at the copy boundary and that only explicitly selected records enter the prompt.
- AI response logs round-trip through research/project backups and remain separate from ordinary drafts.
- Privacy docs disclose clipboard handoff, user-managed external provider terms/fees, and the local-only default.
- No claim implies that AI text, citations, sources, or scientific conclusions have been verified by the app.

## First implementation slice

The web and Android handoff surfaces use the same limits: no more than five selected sources, a 600-character task, and an 8,000-character prompt. The app includes each selected source's local record ID, saved DOI/URL when present, verification status, and bounded evidence text. The user must review and consent before each copy. Pasted replies are saved as Archive records, not manuscript drafts. The evaluation fixture is `tests/fixtures/phase3-ai-eval.json`.
