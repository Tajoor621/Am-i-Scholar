# Am I Scholar vs Skolar Scorecard

Last updated: 2026-11-05 (Phase 0 start)

| Category | Row | Am I Scholar | Skolar | Status | Evidence |
| --- | --- | --- | --- | --- | --- |
| Research | Citation styles and correctness | Bundled CSL (7 styles) + golden fixtures planned | AI-assisted citations | UNVERIFIED | No golden fixtures yet (G6) |
| Research | Literature sources | Crossref, OpenAlex, PubMed, Unpaywall (web); +arXiv, DOAJ (Android) | Multiple + AI | MATCHED (partial) | characterization.spec.cjs |
| Research | DOI handling | Paste + fetch planned | Yes | NOT MATCHED | Phase 1 |
| Research | Duplicate detection | Planned | Yes | NOT MATCHED | Phase 1 |
| Research | PDF reading | Local PDF.js with highlights/notes | Yes | MATCHED | citation tests |
| Research | Evidence linking | Evidence Matrix + links | Limited | WON | tests |
| Research | Backup and portability | JSON + encrypted backups, web↔Android | Cloud | WON | backup tests |
| Student | Flashcards | SM-2, local | AI + import | MATCHED | student E2E |
| Student | Focus timer | Pomodoro, sessionStorage | Yes | MATCHED | characterization |
| Student | Calendar and LMS | ICS import/export | Canvas sync | SUBSTITUTED | no broad permissions |
| Student | Tasks and courses | Local tasks, courses | Yes | MATCHED | student tests |
| Student | Analytics | Streaks, weekly | Yes | MATCHED | local only |
| Student | Notes | Local Markdown | AI notes | MATCHED | student E2E |
| Student | App blocking | In-app focus lock only | Yes | SUBSTITUTED | privacy rule |
| Student | Web access | Full PWA | App only | WON | GitHub Pages |
| AI | Source grounding | Manual handoff + planned BM25 | Full AI | SUBSTITUTED | Phase 3 |
| AI | Answer attribution | Disclosure required | Yes | MATCHED (manual) | ai-handoff.js |
| AI | On-device option | Planned | Limited | UNVERIFIED | Phase 3 |
| AI | Cost | Zero required | In-app purchases | WON | no paid APIs |
| AI | Privacy | No network by default | Cloud | WON | PRIVACY_POLICY.md |
| Lectures | Recording | Local, 15min/100MB limits | Yes | MATCHED | Phase 4 |
| Lectures | Transcription | Manual + planned on-device | AI | SUBSTITUTED | no upload |
| Lectures | Summaries | Via handoff | AI | SUBSTITUTED | Phase 3 |
| Lectures | Audio output | Planned TTS | AI podcasts | SUBSTITUTED | zero cost |
| Trust | Privacy | Local-first, CSP added | Cloud | WON | this scorecard + CSP |
| Trust | Security | Planned scanning | Unknown | UNVERIFIED | G10, G14 |
| Trust | Accessibility | Partial (focus-visible) | Unknown | UNVERIFIED | G13 |
| Trust | Open license | MIT + citeproc dual | Proprietary | WON | LICENSE |
| Trust | Documentation accuracy | Improving | Marketing | UNVERIFIED | docs vs code |
| Distribution | Store listing | GitHub Pages + planned F-Droid | Play Store | MATCHED | public repo |
| Distribution | Release signing | Planned | Yes | UNVERIFIED | G11 |
| Distribution | Reproducible build | CI present | Unknown | UNVERIFIED | workflows |
| Distribution | Installability | PWA + APK | APK | MATCHED | manifest + sw |

All WON/MATCHED claims must be backed by a passing test or cited public source before final Phase 0 acceptance. UNVERIFIED cells will be closed or marked accurately.
