---
name: oneui-scaffold-widget
description: Scaffolds or extends Jetpack Glance widgets following Samsung One UI 6.1 and 7 design tokens, responsive SizeMode.Exact layout calculators, RemoteViews preview layouts, and bilingual resources.
tools:
  - view_file
  - grep_search
  - find_by_name
  - list_dir
  - write_to_file
  - replace_file_content
  - multi_replace_file_content
  - run_command
  - manage_task
---
# Skill: Scaffold One UI Glance Widget

When invoked, act as an **Android Jetpack Glance & One UI Widget Architect**.

Your goal is to scaffold a new widget or extend an existing widget in `one-ui-extra-widgets` while strictly enforcing all architectural invariants defined in `AGENTS.md`.

---

## Architectural Checklist

Every widget in this repository must satisfy the following requirements:

1. **Jetpack Glance & Adaptive Sizing (`SizeMode.Exact`):**
   - Implement a `GlanceAppWidget` subclass and a corresponding `GlanceAppWidgetReceiver`.
   - Override `val sizeMode: SizeMode = SizeMode.Exact`.
   - Read `LocalSize.current` inside `provideContent` and delegate dimension math to an `internal` pure-logic layout calculator function (e.g., `calculate<Widget>Layout(widthDp: Float, heightDp: Float)`).
2. **One UI Visual Tokens (`OneUiGlanceTokens`):**
   - Apply `OneUiGlanceTokens.CornerRadius` (`26.dp`) to root widget containers and inner cards.
   - Respect adaptive Day/Night surfaces (`isSystemInDarkTheme` via `Configuration.UI_MODE_NIGHT_MASK`).
3. **Reliable Glance Touch Targets:**
   - Every clickable `Box` hit-zone must contain a concrete child such as `Spacer(GlanceModifier.fillMaxSize())` so `RemoteViews` registers tap events reliably on One UI Home.
   - Use `ActionCallback` with `actionRunCallback` or `actionStartActivity` instead of polling loops.
4. **Widget Provider XML & Picker Preview (`previewLayout`):**
   - Create `app/src/main/res/xml/widget_<name>_info.xml` with `resizeMode="horizontal|vertical"` and `android:previewLayout="@layout/widget_preview_<name>"`.
   - Create `app/src/main/res/layout/widget_preview_<name>.xml` using `RemoteViews`-compatible XML views (`FrameLayout`, `LinearLayout`, `ImageView`, `TextView`, `Space`) and vector/shape drawables in `app/src/main/res/drawable/`.
5. **AndroidManifest Registration:**
   - Register the `GlanceAppWidgetReceiver` in `app/src/main/AndroidManifest.xml` with `android.appwidget.action.APPWIDGET_UPDATE` and `@xml/widget_<name>_info`.
6. **Bilingual Strings & Documentation:**
   - Add widget title, description, and UI labels to both `app/src/main/res/values/strings.xml` (English) and `app/src/main/res/values-es/strings.xml` (Spanish), or use `LocaleHelper.isSpanishActive()` for dynamic formatting.
   - Update `README.md`, `README.es.md`, and the corresponding `docs/<category>.md` guide (without country flag emojis or absolute local paths).
7. **Unit Testing:**
   - Create a JVM unit test under `app/src/test/java/com/anacatavc/oneuiextrawidgets/` verifying the pure-logic layout calculator across Compact, Tall, and Wide dimensions.

---

## Step-by-Step Execution Workflow

1. **Requirement Clarification:**
   - Confirm the widget category (`astrology`, `music`, `network`, `weather`, or a new package under `widgets/`), target cell size (`targetCellWidth`, `targetCellHeight`), and interactive actions.
2. **Pure-Logic Layout Spec & Unit Test:**
   - Define the layout mode enum, spec data class, and `calculate<Widget>Layout(widthDp: Float, heightDp: Float)` function.
   - Write unit tests in `app/src/test/` covering minimum bounds, aspect-ratio transitions, and padding invariants.
3. **Glance Composable & Receiver Implementation:**
   - Implement the `GlanceAppWidget` and `GlanceAppWidgetReceiver` using `OneUiGlanceTokens` and shared helpers in `utils/` (`WidgetCanvasRenderer`, `LocaleHelper`, `PermissionHelper`).
4. **XML Provider, Preview Layout & Manifest Wiring:**
   - Create `res/drawable/` preview shapes, `res/layout/widget_preview_<name>.xml`, `res/xml/widget_<name>_info.xml`, and register the receiver in `AndroidManifest.xml`.
5. **Bilingual Resources & Verification:**
   - Update `values/strings.xml`, `values-es/strings.xml`, and documentation.
   - Run `./gradlew testDebugUnitTest` to verify all unit tests pass.
