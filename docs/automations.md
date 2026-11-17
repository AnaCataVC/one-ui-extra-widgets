# Project Automations

This document inventories the CI/CD workflows, developer scripts, git hooks, static analysis tools, and runtime background jobs configured in **One UI Extra Widgets**.

---

## 1. CI/CD Workflows

| Workflow | Trigger | Purpose & Quality Gates |
| :--- | :--- | :--- |
| `.github/workflows/android-ci.yml` | `push` / `pull_request` to `main` | Runs with least-privilege `contents: read` permissions and `concurrency` cancellation. Executes `scripts/check_invariants.py`, `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew detekt`, and `./gradlew assembleDebug`. |
| `.github/workflows/release.yml` | `push` tag `v*` | Verifies that the git tag matches `versionName` in `app/build.gradle.kts`, runs `scripts/check_invariants.py` and `testDebugUnitTest`, enforces all release signing secrets (`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`), builds `assembleRelease`, and publishes `OneUiExtraWidgets-<tag>.apk` to GitHub Releases. |

---

## 2. Developer Scripts, Git Hooks & Static Analysis

| Component | Path | Usage & Behavior |
| :--- | :--- | :--- |
| **Invariants Checker** | `scripts/check_invariants.py` | Verifies ES/EN parity between `res/values/strings.xml` and `res/values-es/strings.xml`, enforces documentation hygiene (no country flag emojis or absolute local paths), and warns on missing `android:previewLayout` attributes in `res/xml/widget_*.xml` (blocking with `--strict`). |
| **Local Verification Script** | `scripts/verify.sh` | Runs `scripts/check_invariants.py`, `testDebugUnitTest`, `lintDebug`, `detekt`, and `assembleDebug` in a single command. |
| **Pre-Push Git Hook** | `.githooks/pre-push` | Delegates to `scripts/verify.sh` before pushing. Enable locally with `git config core.hooksPath .githooks`. |
| **Kotlin Static Analysis** | `config/detekt/detekt.yml` | Configures Detekt (`1.23.7`) for Kotlin, Jetpack Compose, and Jetpack Glance (allows PascalCase `@Composable` functions, flags unused private members, and enforces a 140-character line limit). |

---

## 3. Domain & Runtime Automations

| Component | Path | Mechanism |
| :--- | :--- | :--- |
| **Weather & AQI Sync** | `OneUiWidgetsApp.kt` / `WeatherWidgets.kt` | `WorkManager` periodic job (`WeatherRefreshWorker`) running every 30 minutes with `NetworkType.CONNECTED` constraint. |
| **Media Session Bridge** | `MediaNotificationListener.kt` | Event-driven `NotificationListenerService` + `MediaSessionManager` callback listener updating retro music widgets without polling. |
