# Professional engineering audit — 2026-10-15

## Evidence standard
This is a repository/source-level audit. CI builds and JVM tests are evidence of compilation and selected logic, **not** proof of all user journeys, accessibility, source accuracy or production security. The user explicitly deferred device testing until the initial formal test release.

## Feature matrix

| Feature | Current evidence | Assessment |
|---|---|---|
| Android Compose shell/navigation | Compiles in prior CI | Implemented, device behavior untested |
| Room project persistence | DAO and v1→v2 migration | Implemented, migration instrumentation test missing |
| Notes, outline, draft, source records | Workspace UI/Room | Implemented, device behavior untested |
| Live Crossref search/DOI lookup | HTTPS integration, import | Implemented, API contract/error testing incomplete |
| Live OpenAlex search | HTTPS integration, import | Implemented, API contract/error testing incomplete |
| PubMed/arXiv/Semantic Scholar/DOAJ/Unpaywall/CORE/ORCID | No corresponding integrations | Missing |
| Automatic source verification | Metadata syntax/completeness checks only | **Not implemented**; user-attested source checking is not independent verification |
| Citation styles | Six simplified templates | **Not CSL-compliant**; not publication-ready |
| BibTeX, RIS, CSL-JSON | Basic exporters | Implemented, standards/roundtrip tests incomplete |
| Markdown, CSV | Text exporters | Implemented |
| DOCX/PDF | Local basic generators | Implemented, document rendering and conformance unverified |
| Writing assistance | Local heuristic Writing Coach | Partial; no LLM |
| Simulated review/submission kit | Rule-based checklist and export | Partial; no real journal matching |
| Plagiarism/originality checking | No detector | Missing; do not claim certification |
| Reproducibility/archive | Local records only | Partial |
| Privacy | User-triggered network requests, backup disabled | Good foundation; security audit pending |
| Accessibility | Material components used | Screen reader, scaling and contrast audits missing |
| Release | CI debug APK artifacts | No signed release, no formal release gate or device acceptance |
| Comprehensive QA | JVM tests and CI | Instrumentation, network mocking, UI/E2E and performance tests missing |

## Blocking risks
1. Current citation formatting is deliberately approximate; never advertise it as compliant with APA, MLA, Chicago, IEEE, Harvard or Vancouver specifications.
2. Source metadata returned by Crossref/OpenAlex can be missing, stale or wrong; this is not independent source verification.
3. The generated DOCX and PDF require document-open/rendering tests with multiline, Unicode, long paragraphs and real references.
4. Local persistence needs automated upgrade tests and backup/export recovery testing.
5. The debug APK must not be presented as a signed production release.
6. Cloud AI must require explicit consent, user-selected provider, clear privacy disclosure and careful data handling before integration.
7. Verify Android target SDK requirements, dependency updates and supply-chain security before release.

## Scoring
**Do not assign a 10/10 or production-ready certification.** Feature coverage, release security and device-level quality have not met the acceptance gate. A numeric score without a tested rubric would misrepresent the state of this application.

## Release gate
- Build, static checks, JVM tests and Android instrumentation tests pass.
- Every required master-prompt capability has a tested acceptance criterion, or is explicitly documented as deferred and approved.
- Validate exported documents and citation formats with independent fixtures and standards.
- Complete privacy, accessibility, performance and recovery audits.
- Publish a versioned, signed test APK and release notes.
- Only then begin user-authorized device testing.
