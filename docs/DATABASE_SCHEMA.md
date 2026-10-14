# Database schema — initial

The bootstrap currently persists one table: research_projects.

| Column | Type | Purpose |
| --- | --- | --- |
| id | Long / INTEGER | Auto-generated primary key |
| title | String / TEXT | Required working title |
| researchQuestion | String / TEXT | Optional initial question |
| discipline | String / TEXT | Discipline or “Not set” |
| citationStyle | String / TEXT | APA, MLA, Chicago, IEEE, Harvard, or Vancouver |
| createdAtEpochMillis | Long / INTEGER | Creation timestamp |
| updatedAtEpochMillis | Long / INTEGER | Last-update timestamp |

## Planned, not yet implemented
- sources: title, authors, year, DOI, URL, provider, verification state, license/access notes.
- evidence_entries: source link, method, sample, findings, limitations, relevance, quote, page/locator, researcher-confirmed flag.
- outline_nodes and draft_sections: structured outline, order, versioning, evidence links, support warnings.
- ai_use_events: consented operation and disclosure record; do not store secrets.
- revision_items: reviewer comment, clearly labeled as simulated or real, response and status.
- attachments: local URI, metadata, content hash, and deletion state.

Quotes need source identity and page/locator. Extracted claims remain unconfirmed until the researcher checks them. A DOI-shaped string alone is not proof of verification.
