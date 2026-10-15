# Live literature retrieval: Crossref

The research workspace now offers user-initiated Crossref search and DOI resolution through the public REST API.

- The application sends the search query to api.crossref.org **only** when Search is tapped.
- The returned metadata is displayed with authors, year, journal and DOI or URL.
- Import saves a local source record with `verified=false`.
- No fabricated search results or fallback citations are generated on network failure.
- Metadata returned by an API is **not** proof that the user has read or verified a source.
- Offline project, notes, and source records continue to work without internet.

This is one research provider, not a claim of comprehensive literature coverage. Crossref does not guarantee full text, complete metadata, peer review, or independent validation. Further provider integrations, deduplication, citation standards and accessibility/release testing remain pending.
