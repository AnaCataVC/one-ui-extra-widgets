---
name: oneui-render-canvas-preview
description: Designs deterministic Canvas Bitmap graphics in WidgetCanvasRenderer and builds matching RemoteViews XML preview layouts and vector drawables for the One UI widget picker.
tools:
  - view_file
  - grep_search
  - find_by_name
  - list_dir
  - write_to_file
  - replace_file_content
  - multi_replace_file_content
  - run_command
---
# Skill: Render Widget Canvas & Picker Preview Layout

When invoked, act as an **Android Canvas Graphics & RemoteViews Preview Engineer** for `one-ui-extra-widgets`.

Your objective is to create or refine deterministic `Bitmap` graphics in `WidgetCanvasRenderer.kt` and build matching static XML `previewLayout` files so the Samsung One UI Home widget picker displays an accurate visual preview for every widget.

---

## Technical Rules & Constraints

1. **Centralized Deterministic Canvas Rendering (`utils/WidgetCanvasRenderer.kt`):**
   - All custom `Bitmap` drawing (lunar phase spheres, iPod Click Wheel, Discman metallic body and CD mask, gauges, or indicators) must reside in `WidgetCanvasRenderer`.
   - Functions must be deterministic, allocation-safe, use `Bitmap.Config.ARGB_8888` with `Paint.ANTI_ALIAS_FLAG`, and scale proportionally to `sizePx`.
   - Bound `sizePx` when invoked from Glance widgets to avoid exceeding Android `RemoteViews` Binder transaction memory limits.
2. **One UI Widget Picker Previews (`android:previewLayout`):**
   - Every `app/src/main/res/xml/widget_*_info.xml` file must declare `android:previewLayout="@layout/widget_preview_*"`.
   - Preview layouts in `app/src/main/res/layout/widget_preview_*.xml` must only use `RemoteViews`-supported views: `FrameLayout`, `LinearLayout`, `RelativeLayout`, `ImageView`, `TextView`, and `Space`.
   - Never use Jetpack Compose views, custom `View` subclasses, or unsupported `ConstraintLayout` attributes inside `widget_preview_*.xml`.
3. **Preview Drawables (`app/src/main/res/drawable/`):**
   - Name preview drawables with the prefix `bg_preview_*` (shapes/containers) or `ic_preview_*` (vector graphics).
   - Match the `26dp` One UI corner radius on outer widget shells (`<corners android:radius="26dp" />`).

---

## Step-by-Step Execution Workflow

1. **Inspect Target Widget & Provider XML:**
   - Check the target widget composable in `app/src/main/java/com/anacatavc/oneuiextrawidgets/widgets/` and its provider configuration in `app/src/main/res/xml/widget_*_info.xml`.
2. **Implement or Update Canvas Renderer (If Applicable):**
   - Add or update the bitmap rendering method in `app/src/main/java/com/anacatavc/oneuiextrawidgets/utils/WidgetCanvasRenderer.kt`.
3. **Create RemoteViews Preview Drawables & Layout:**
   - Create shape/vector drawables in `app/src/main/res/drawable/bg_preview_*.xml` and `ic_preview_*.xml`.
   - Create `app/src/main/res/layout/widget_preview_<name>.xml` mirroring the widget's default target cell proportions and visual hierarchy.
   - Link `android:previewLayout="@layout/widget_preview_<name>"` in `app/src/main/res/xml/widget_<name>_info.xml`.
4. **Verify Resource Compilation:**
   - Run `./gradlew testDebugUnitTest` or resource verification tasks to confirm all XML drawables, layouts, and string references compile cleanly.
