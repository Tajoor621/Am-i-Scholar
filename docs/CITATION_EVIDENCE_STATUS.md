# Citation correctness, evidence persistence, and brand assets — October 2026

## Implemented in this incremental release

- Research records remain in the original `amischolar.web.v2` localStorage key. A backwards-compatible optional `evidenceLinks` array now stores durable links between evidence claims and source records, including researcher-assessed support status, rationale, and creation timestamp.
- Web JSON backup version 2 remains unchanged and now optionally includes `evidenceLinks`. Older backups without this property still restore.
- Deleting a linked record removes dangling relationships.
- CSL-JSON metadata exports now use stable source record IDs, preserve available metadata, and omit invalid DOIs rather than inventing identifiers.
- Citation rendering is **NOT yet CSL compliant**. The existing Android templates are still draft-only. A genuine offline CSL processor and golden fixtures remain mandatory before claiming style correctness.
- A new simplified vector identity inspired by the black/gold/white logo concept is integrated in web favicon/PWA SVG and Android adaptive-icon foreground. This is a small-size vector adaptation, not the full rendered brand illustration.
- The existing research and student data models, Room database, and original project backup versions remain unchanged.

## Remaining acceptance gates

1. Bundle an audited, appropriately licensed offline CSL processor and the official six/seven style XML files; verify APA 7, MLA 9, Chicago author-date and notes, IEEE, Harvard (chosen variant), Vancouver against authoritative fixtures.
2. Persist claim-to-source relationships on Android without destructive Room migrations; validate web/Android round-trip.
3. Test PWA icon install and Android launcher appearance on real devices.
4. Pass browser E2E, Android CI, emulator, and cross-platform restore tests before release.
