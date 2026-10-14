# Privacy policy draft

**Status:** Product-development draft, not legal advice or a jurisdiction-reviewed public policy.

## Current bootstrap
- Research projects are stored in a local Room database on the Android device.
- This bootstrap does not implement remote research search, cloud AI, analytics, advertising, accounts, or synchronization.
- The app does not currently request network permission.

## Planned behavior
Before adding any online provider, document what data is sent, the purpose, provider, known retention terms, and controls. Ask for explicit consent before sending manuscript text, private notes, or uploaded documents to cloud AI. Provide a local-only mode and consent revocation.

Do not commit API keys or embed shared provider secrets in a distributed APK. Minimize data collection. Make any future analytics transparent and appropriately opt-in.

## Controls to implement
- Export and delete projects and attachments.
- View and export the AI-use log.
- Review and revoke cloud-provider consent.
- Clear cached search results and downloaded metadata.
- Explain permissions and file access plainly.

Before public release, replace this draft with a reviewed policy identifying the responsible entity/contact, applicable jurisdictions, retention and deletion rules, processors, security practices, user rights, and effective date. Do not claim legal compliance without review.
