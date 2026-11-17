# AGENTS.md — AI Coding Guidelines for One UI Extra Widgets

## Project Overview
**One UI Extra Widgets** is an Android home-screen widget suite styled after **Samsung One UI**, optimized for the **Samsung Galaxy A55 5G** (`One UI 8.5`, Android 16 QPR2; compatible with One UI 6.1–8.5) while remaining compatible with any Android 8.0+ (`minSdk = 26`, `targetSdk = 36`) device.

---

## Core Architecture & Stack
- **Language:** 100% Kotlin (`JVM 17`), Android Gradle Plugin `8.7.2`, version catalog in `gradle/libs.versions.toml`.
- **Widget Rendering:** 100% **Jetpack Glance** (`androidx.glance:glance-appwidget:1.1.1`) using `SizeMode.Exact` + `LocalSize.current` for resizable widgets, paired with deterministic `Bitmap` graphics rendered via `WidgetCanvasRenderer` (`utils/WidgetCanvasRenderer.kt`) for lunar phases, the iPod Click Wheel, and the circular Discman body / CD mask.
- **Companion App UI:** **Jetpack Compose Material 3** styled with Samsung One UI tokens (`26dp` rounded corners, generous top viewing headers, AMOLED dark & light surfaces in `ui/theme/OneUiTheme.kt`).
- **Weather & Air Quality:** Free, zero-key **Open-Meteo** Forecast & Air Quality REST APIs (`data/weather/OpenMeteoClient.kt`), cached in `DataStore` (`AppPreferences.kt`) and synced periodically via `WorkManager`.
- **Astrology & Moon Phases:** 100% offline ephemeris calculations via **`Astronomy Engine`** (`io.github.cosinekitty:astronomy:2.1.19`) in `data/astrology/AstrologyEngine.kt` (Sun sign, Moon transit sign, lunar phase, and planetary Direct/Retrograde/Shadow states).
- **Retro Music Players:** Universal `MediaSessionManager` + `NotificationListenerService` bridge (`data/media/MediaNotificationListener.kt`) with `AudioManager.dispatchMediaKeyEvent` fallback, supporting Spotify, YouTube Music, Apple Music, Samsung Music, etc.
- **5G & Wi-Fi Quality:** Real-time `TelephonyManager` (`CellSignalStrengthNr` / `CellSignalStrengthLte`) and `WifiManager` / `ConnectivityManager` monitoring (`data/network/NetworkQualityMonitor.kt`).

---

## Coding & Contribution Invariants
1. **No Code Duplication:** Keep shared Canvas rendering, permission checks, and locale helpers centralized in `utils/`.
2. **One UI Visual Consistency:** All widget containers and detail cards must use `OneUiGlanceTokens.CornerRadius` (`26.dp`) or `MaterialTheme.shapes.large` (`26.dp`) and respect adaptive Day/Night surfaces.
3. **Bilingual Support (ES / EN):** Every user-facing string in widgets and Compose screens must have both `res/values/strings.xml` (English) and `res/values-es/strings.xml` (Spanish) entries, or use `LocaleHelper.isSpanishActive()` for dynamic formatting.
4. **Offline-First & Battery-Safe:** Never introduce polling loops in widgets or UI. Use `WorkManager`, `NotificationListenerService` callbacks, and `ActionCallback` triggers.
5. **Testing:** Add JVM unit tests under `app/src/test/` for any pure-logic domain calculations (e.g., `AstrologyEngineTest.kt`) and run `./gradlew testDebugUnitTest` before committing.
6. **Documentation Standards:** Keep `README.md` (English), `README.es.md` (Spanish), and release notes bilingual and synchronized with any architectural or feature changes. Every widget or widget category must have its own dedicated documentation in `docs/` (e.g., `docs/retro-music-widgets.md`) linked from the READMEs (per-widget docs do not need to be bilingual). Never include country flag emojis or absolute local file paths in documentation.
7. **Widget Picker Previews (`previewLayout`):** Every `appwidget-provider` XML in `res/xml/` must define `android:previewLayout="@layout/widget_preview_*"` using `RemoteViews`-compatible (`@RemoteView`) XML views (`LinearLayout`, `FrameLayout`, `ImageView`, `TextView` — never `Space`, which is not `@RemoteView` and fails inflation) and vector/shape drawables in `res/drawable/` so the One UI Home widget picker renders an accurate preview instead of falling back to the app icon or showing an error.
8. **Responsive Resizing & Glance Touch Targets:** Resizable widgets (`resizeMode="horizontal|vertical"`) should use `SizeMode.Exact` with pure-logic layout calculators so buttons and screens redistribute proportionally. Always place a concrete child (such as `Spacer(GlanceModifier.fillMaxSize())`) inside clickable `Box` hit-zones so `RemoteViews` registers taps reliably.
9. **Adaptive Color Tokens in XML & Canvas:** Never hardcode raw hex colors in `res/layout/widget_preview_*.xml`, `res/drawable/*.xml`, or `WidgetCanvasRenderer.kt`; reference `@color/oneui_*` tokens in `res/values/colors.xml` / `res/values-night/colors.xml` and `OneUiCanvasPalette` in `OneUiGlanceTheme.kt`.

---

## CI, Detekt & Glance Pitfalls (Lessons Learned)
- **Glance `ColorProvider` Type vs Factory:** `androidx.glance.color.ColorProvider(day, night)` is a factory function; function parameters and properties typed as `ColorProvider` must import `androidx.glance.unit.ColorProvider`.
- **Centralized Night Mode Check:** Always call `OneUiGlanceTokens.isNightModeActive(context)` rather than inline `Configuration.UI_MODE_NIGHT_MASK` checks or unqualified `isNightModeActive(context)`. When refactoring imports, verify all call sites across the file.
- **Detekt Static Analysis (`config/detekt/detekt.yml`):** Keep all Kotlin lines (including KDoc comments and string templates) `<= 140` characters (`MaxLineLength`) and remove any unused variables/properties (`UnusedPrivateProperty`).
- **Careful Rebase Conflict Resolution:** Avoid blind `-X ours` / `-X theirs` flags when rebasing over upstream edits in the same file; resolve conflicts explicitly so updated domain logic and variable usages stay in sync.
- **Domain Separation (Weather vs Astrology):** Weather widgets (`OneUiRainWidget`, `OneUiAqiWidget`, `OneUiUvWidget`, `OneUiMoonPhaseWidget`) and `WeatherDetailScreen` must never display current temperature or astrological/zodiac data—lunar weather UI uses strictly scientific metrics (illumination %, synodic age, solar elongation, waxing/waning trend).
