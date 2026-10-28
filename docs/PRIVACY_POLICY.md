# Privacy policy draft

**Status:** Product-development draft, not legal advice or a jurisdiction-reviewed public policy.

## Current web behavior
- Research projects and imported source records are stored in this browser. The web app does not provide accounts, cloud sync, analytics, advertisements, or cloud AI.
- Literature Radar sends the search query to the selected public metadata providers only after the user starts a search. Crossref and OpenAlex receive the optional contact email as `mailto`; PubMed receives it as `email`.
- If a contact email is saved, DOI results are also sent to Unpaywall for Open Access lookup. The email preference is stored in this browser and can be cleared in the Literature Radar email field. Search results are transient until individually imported; imported records stay local.
- PDFs opened in Citation Center are read from a local file in the browser. PDF bytes are not uploaded or stored by the app. Selected passages and notes are saved as local project records and can be restored after reselecting the same PDF.
- Provider services receive request metadata under their own terms. Availability and browser CORS restrictions can affect searches. Do not put private notes, manuscript text, or uploaded documents in search terms.

## Current Android behavior
- Android stores research projects in a local Room database. Its existing public-provider integrations make network requests when the researcher uses those search tools. This web phase does not change Android behavior.
- No cloud AI, analytics, advertising, account, or synchronization feature is described here as available.

## Planned behavior
Before adding any additional online provider, document what data is sent, the purpose, provider, known retention terms, and controls. Ask for explicit consent before sending manuscript text, private notes, or uploaded documents to cloud AI. Provide a local-only mode and consent revocation.

Do not commit API keys or embed shared provider secrets in a distributed APK. Minimize data collection. Make any future analytics transparent and appropriately opt-in.

## Controls to implement
- Export and delete projects and attachments.
- View and export the AI-use log.
- Review and revoke cloud-provider consent.
- Clear cached search results and downloaded metadata.
- Explain permissions and file access plainly.

Before public release, replace this draft with a reviewed policy identifying the responsible entity/contact, applicable jurisdictions, retention and deletion rules, processors, security practices, user rights, and effective date. Do not claim legal compliance without review.
