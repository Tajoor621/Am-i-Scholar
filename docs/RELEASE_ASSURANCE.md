# Release assurance: security, accessibility, document exports

## Automated evidence
- Android CI compiles the debug variant, runs JVM tests and Android lint.
- Document conformance tests validate the DOCX ZIP container, the presence of required OpenXML parts, namespace-aware XML parsing, XML escaping, Unicode and paragraph preservation.
- These are **structural checks only**. They do not establish that Word or LibreOffice renders the document correctly; PDF has no renderer-level verification.

## Security review
- Android backup disabled; cleartext traffic disabled; exported launcher activity is intentional.
- Internet permission exists solely for user-triggered metadata discovery.
- No advertising, analytics SDK or paid inference dependency is declared.
- Research service responses are untrusted; check errors, timeouts and rate limits.
- Unresolved: enforce host allowlist at every request, dependency vulnerability scanning, encrypted backup option, privacy review of network requests, API response limits, Android device traffic capture and pen test.
- No confidential manuscript or draft should be sent to third-party providers without explicit opt-in.

## Accessibility review
- Material components and scalable vector launcher icon are present.
- Unresolved: TalkBack traversal and labeling, 200% font scaling, keyboard navigation, 48dp minimum touch targets, light/dark contrast, RTL layout, screen reader reading order and landscape/foldable testing.
- This document is a checklist, **not** an accessibility certification.

## Citation compliance
- The six bibliography formats are simplified templates; the export and audit do not implement CSL rules or the latest official style manuals.
- Full compliance requires a validated citation processor and fixtures covering author disambiguation, et al., locale, source type, page ranges, DOI formatting, italics, dates, and in-text citations.

## Release gate
Do not claim 10/10 or publish a production-ready signed release until the above is tested. A CI debug APK is for engineering review only. Actual screenshots must be captured from the running build, not generated concept art.
