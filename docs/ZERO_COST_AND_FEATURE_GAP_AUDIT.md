# Zero-cost feasibility and feature cross-check — 2026-10-15

## Cost policy
Am I Scholar targets **zero required user fees**: no subscriptions, ads, paywalled AI calls, compulsory backend or mandatory paid cloud account. Offline writing and local persistence remain the default. Network requests must be user initiated. No API key should be bundled in an APK.

**Zero-cost is a design goal, not an unconditional financial guarantee.** GitHub Actions quotas, service policies and rate limits can change. Google Play distribution may involve a registration fee; a GitHub-hosted APK can avoid that distribution channel. An external LLM, hosted inference, private server or premium research API must never silently incur charges.

## Actual provider coverage
- Crossref, OpenAlex, PubMed, arXiv: integrations coded; production reliability and response fixtures not fully verified.
- Semantic Scholar, DOAJ, Unpaywall, CORE, ORCID: still missing; some provider endpoints require keys or have quota/access restrictions. Do not claim universal anonymous free access.
- DOI comparison: Crossref metadata consistency check, not proof of full-text findings, provenance, peer review or publication quality.

## Required master-prompt acceptance matrix
| Requirement | Status | Acceptance requirement |
|---|---|---|
| Topic Lab | Partial | Topic ideation and research-question validation with tests |
| Literature Radar | Partial | Provider fixtures, retries, limits, deduplication, provenance |
| Evidence Matrix | Partial | Structured evidence, limitations, linkage to claims |
| Outline Architect | Partial | Editable sections and traceable sources |
| Draft Studio | Partial | Offline drafts, version history, citations, autosave |
| Style Coach | Partial | Transparent checks, documented limitations |
| Citation Center | Blocked | Correct CSL implementation and golden tests for six styles |
| BibTeX/RIS/CSL-JSON | Partial | Conformance and round-trip fixtures |
| Originality | Missing | User-safe similarity support without unsupported plagiarism claims |
| Simulated peer review | Partial | Clear simulation labeling and detailed rubrics |
| Journal matching | Missing | Current journal directory and verifiable criteria |
| Submission kit | Partial | Editable ethics, disclosures and author checklists |
| Revision workflow | Missing | Versioned response-to-reviewers and tracked changes |
| Archive/reproducibility | Partial | Exportable data manifest, restore testing |
| Research APIs | Partial | Five requested providers still missing |
| DOCX/PDF | Partial | Unicode, long text, ZIP/PDF reader validation |
| Accessibility | Unverified | TalkBack, dynamic type, touch target, contrast, RTL tests |
| Security | Unverified | Dependency audit, storage, TLS, exported components, privacy |
| Release | Blocked | CI green, lint green, emulator smoke tests, signed test APK |

## Release gate
No 10/10 certification until all required features and tests pass, including on-device acceptance. Do not imply a GitHub Actions debug APK is a signed production build.
