# Architecture

## Current bootstrap
The project uses a single Android application module to keep the early build simple.

- UI: Jetpack Compose and Material 3.
- Navigation: Navigation Compose, with module metadata in ResearchModules.
- Persistence: Room database on-device.
- Concurrency: Kotlin coroutines; database writes run off the main thread.
- Preferences: DataStore dependency is available but not wired into a settings screen.
- Network: no research API or cloud AI calls are implemented in this bootstrap.

## Data flow
Compose observes the Room DAO Flow of ResearchProject records. Creating a paper inserts a record into the local database. Future features must distinguish source metadata, researcher notes, extracted evidence, and AI suggestions.

## Planned boundaries
When justified by implementation size, introduce clear boundaries for data sources, domain use cases, feature screens, AI providers, and export engines. Avoid premature multi-module complexity.

## AI and provider rules
1. Prefer local/offline workflows where practical.
2. Cloud AI must require explicit consent for each relevant operation.
3. Never silently send private manuscript text or uploaded documents to remote providers.
4. Source claims must trace to a DOI, URL, imported document, or researcher note.
5. Label unverified sources and uncertain claims clearly.
6. Never commit API keys or ship shared provider secrets in an APK.

## Reliability
Room schema changes require version increments and reviewed migrations. Do not use destructive migrations for research data. Add repository and migration tests as data workflows grow.
