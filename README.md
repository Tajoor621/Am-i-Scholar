# Am I Scholar

**An offline-first, Android-first research companion that helps you move from an idea to a submission-ready research workflow—without inventing evidence.**

> Status: **Bootstrap / MVP foundation.** This repository is the starting point, not a finished research-writing product. Network integrations, export engines, local-model runtimes, and advanced integrity checks are planned and must be implemented and verified before being described as available.

## Product principles

- **Evidence before prose:** draft support should be grounded in user-provided notes and traceable sources.
- **No fabricated scholarship:** never invent citations, DOIs, research data, statistics, peer reviews, or source claims.
- **Local-first privacy:** core project notes are stored on-device. Cloud AI or sync must be opt-in, with clear disclosure of what leaves the device.
- **Researcher remains the author:** support planning, explanation, editing, and critique—not undisclosed ghostwriting.
- **Integrity by design:** encourage source verification, quotation attribution, AI-use disclosure, and compliance with institutional and journal policies.
- **Accessible and low-resource friendly:** readable type, scalable fonts, accessible targets, and graceful offline states.

## Initial technology baseline

- Kotlin, Jetpack Compose, Material 3
- Android minimum SDK 26 (Android 8.0)
- Room for local project persistence
- Navigation Compose
- DataStore for preferences
- WorkManager for deferred work (to be integrated as features need it)
- Retrofit/OkHttp and Kotlin Serialization for future online research providers
- GitHub Actions Android build/test workflow

## First foundation included

- Material 3 light/dark theme
- Dashboard and New Paper form
- Navigation entry points for the planned research workflow
- Room entities/DAO/database foundation for research projects
- Unit tests for core navigation/module metadata
- Documentation for architecture, schema, privacy, integrity, and roadmap
- CI workflow that assembles and runs unit tests

## Planned research workflow

1. Onboarding & Integrity
2. Topic Lab
3. Literature Radar
4. Evidence Matrix
5. Outline Architect
6. Draft Studio
7. Humaniser & Style Coach
8. Citation Center
9. Originality & Integrity
10. Review Simulator (clearly simulated, never real peer review)
11. Journal Match & Submission Kit
12. Revision & Response
13. Archive & Reproducibility

These are product areas, not a claim that all are implemented in this bootstrap.

## Build locally

1. Install Android Studio with a compatible Android SDK (API 35).
2. Open this repository as a project in Android Studio.
3. Allow Gradle to sync and download dependencies.
4. Run the `app` configuration on an Android 8.0+ device or emulator.

This repository intentionally does not commit generated build output or local machine settings. Android Studio can generate a Gradle wrapper if needed. The CI workflow provisions Gradle and the Android SDK.

## Integrity and privacy

Read:
- [Architecture](docs/ARCHITECTURE.md)
- [Database schema](docs/DATABASE_SCHEMA.md)
- [Privacy policy draft](docs/PRIVACY_POLICY.md)
- [Academic integrity policy](docs/ACADEMIC_INTEGRITY.md)
- [MVP roadmap](docs/ROADMAP.md)

This project is not affiliated with a university, publisher, or research database. API availability, access limits, journal policies, fees, and licensing must be verified at the time of use.

## License

No open-source license has been selected yet. Until a license is added, the repository contents remain subject to the default copyright rules.
## Official logo and app icons (621/24)

The user-approved black-and-gold academic emblem replaces the previous launcher artwork in the web PWA and Android. The cropped emblem is used for small app icons; the complete logo is available in the downloadable brand-assets package. The repository includes the approved compressed WebP emblem, generated PWA PNG icons (16–512 px), iOS touch icon, Android density-specific launcher PNGs, and adaptive-icon resources. Regenerate sizes with `python brand/generate_assets.py` (Pillow required). Web and `docs/` icon assets are mirrored for GitHub Pages. See [branding documentation](docs/BRANDING_AND_SCREENSHOTS.md).
