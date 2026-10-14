# Phase 1–2 Repair Notes

Date: 2026-10-15

## Scope

This change addresses repository/build reliability and a lifecycle issue in the Android starter app. It does not implement the planned research modules.

## Confirmed issues addressed

- The previous Android CI run failed inside `android-actions/setup-android` while accepting SDK licenses. The action attempted to prompt for multiple SDK licenses interactively and exited before the build started.
- CI now limits setup-android to platform-tools, disables its interactive license step, and explicitly accepts SDK licenses non-interactively before installing the required API 35 platform/build tools.
- CI now creates a Gradle 8.9 wrapper on the clean runner and uses that wrapper for build/test commands, rather than invoking the runner's ambient Gradle directly.
- `MainActivity` now launches the database insert in `lifecycleScope`, moving only the insert operation to `Dispatchers.IO`. The unstructured `MainScope()` is removed.

## Important wrapper limitation

The repository currently does not contain a checked-in Gradle Wrapper JAR or wrapper scripts. The workflow-generated wrapper makes CI's Gradle invocation pinned and repeatable for that job, but it does **not** replace committing the standard wrapper files for developer machines. To complete that repository-level requirement, generate the wrapper with a compatible local Gradle installation and commit all four standard files: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, and `gradle/wrapper/gradle-wrapper.jar`. Do not create a placeholder JAR.

## Validation status

These edits are committed for CI validation. They have not been independently compiled in this editing session. The new GitHub Actions run must finish successfully before the project is described as build-verified. A successful CI build also does not replace installing and testing the APK on a physical Samsung device.
