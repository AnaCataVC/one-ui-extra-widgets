# Modular One UI Weather, Solar & Astronomical Widgets — Architecture & Visual Specs

The Weather & Scientific Astronomy suite provides six independent, `SizeMode.Exact` home-screen widgets built with **Jetpack Glance**, deterministic **One UI Canvas** gauges (`WidgetCanvasRenderer`), **Open-Meteo** Forecast/Air Quality APIs, and offline **`Astronomy Engine`** calculations (targeting **Samsung One UI 8.5 / Android 16**, compatible with One UI 6.1–8.5).

By design, none of the six widgets display the current ambient temperature (`temperatureC`), avoiding visual redundancy with the pre-installed Samsung Weather widget and dedicating 100% of the card area to specialized environmental and astronomical gauges.

---

## 1. Shared Responsive Layout Calculator (`calculateWeatherCardLayout`)
All six widgets adapt dynamically across four `SizeMode.Exact` layout modes (`COMPACT_1X1`, `PILL`, `SQUARE`, and `EXPANDED`):
- **`COMPACT_1X1` (`1×1`, `widthDp < 130dp && heightDp < 130dp`):** Ultra-compact square with `8.dp` padding, a `34.dp–52.dp` `Canvas` gauge, and single concise value label.
- **`PILL` (`2×1` / `4×1`, `heightDp < 130dp`):** Horizontal pill with `12.dp` padding, a `42.dp–56.dp` `Canvas` gauge on the left, and the metric title, primary readout, status badge, and quick-refresh button (`↻`) on the right.
- **`SQUARE` (`2×2`, `heightDp >= 130dp && widthDp < 240dp`):** Vertical card with `14.dp` padding, a scaled `58.dp–82.dp` centered `Canvas` gauge, primary readout, status badge, and a bottom `12.dp` inner detail pill.
- **`EXPANDED` (`4×2`, `heightDp >= 130dp && widthDp >= 240dp`):** Wide horizontal dashboard with `14.dp` padding, a `64.dp–88.dp` `Canvas` gauge on the left, and two side-by-side `12.dp` inner metric pills on the right.

---

## 2. The Six Dedicated Widgets

### 2.1 Rain Probability (`OneUiRainWidget`)
- **Provider:** `app/src/main/res/xml/widget_weather_rain_info.xml` (`previewLayout="@layout/widget_preview_weather_rain"`)
- **Canvas Graphic (`renderRainGaugeBitmap`):** `270°` radial progress arc in cyan-blue gradient framing a sculpted center water droplet with specular highlight.
- **Metrics:** Maximum precipitation probability over the next 6 hours (`0%–100%`) and intensity tier (`Dry` / `Low Chance` / `Moderate` / `High Rain`).

### 2.2 European Air Quality (`OneUiAqiWidget`)
- **Provider:** `app/src/main/res/xml/widget_weather_aqi_info.xml` (`previewLayout="@layout/widget_preview_weather_aqi"`)
- **Canvas Graphic (`renderAqiGaugeBitmap`):** `270°` radial gauge color-coded by official European AQI thresholds (`<=20` Good, `<=40` Fair, `<=60` Moderate, `<=80` Poor, `>80` Very Poor) with an indicator dot and numeric center readout.
- **Metrics:** Localized air quality tier, European AQI score, and $\text{PM}_{2.5}$ / $\text{PM}_{10}$ particulate matter ($\mu\text{g/m}^3$).

### 2.3 Solar UV Index (`OneUiUvWidget`)
- **Provider:** `app/src/main/res/xml/widget_weather_uv_info.xml` (`previewLayout="@layout/widget_preview_weather_uv"`)
- **Canvas Graphic (`renderUvGaugeBitmap`):** `270°` solar radiation gauge mapped to the WHO `0–11+` UV scale with a radiant 8-ray sun core.
- **Metrics:** Numeric UV index (`UV 0–11+`), WHO risk category (`Low`, `Moderate`, `High`, `Very High`, `Extreme`), and sun protection guidance (`Safe`, `Use SPF 30+`, `SPF 50+ / Shades`, `Seek shade`).

### 2.4 Scientific Lunar Phase (`OneUiMoonPhaseWidget`)
- **Provider:** `app/src/main/res/xml/widget_weather_moon_info.xml` (`previewLayout="@layout/widget_preview_weather_moon"`)
- **Canvas Graphic (`renderMoonPhaseBitmap`):** High-contrast lunar sphere with accurate waxing/waning terminator geometry, crater shading, and outer rim highlight.
- **100% Scientific Astronomical Metrics (Zero Astrology):** Displays the localized astronomical lunar phase name, illuminated fraction (`0%–100%`), synodic lunar age in days (`0.0 d – 29.5 d`), solar elongation angle (`0°–360°`), and cycle direction (`Waxing` / `Waning`). Zodiac transit signs remain exclusively in `OneUiAstrologyWidget`.

### 2.5 Solar Arc & Golden Hour (`OneUiSunArcWidget`)
- **Provider:** `app/src/main/res/xml/widget_weather_sun_arc_info.xml` (`2×1` default, `previewLayout="@layout/widget_preview_weather_sun_arc"`)
- **Canvas Graphic (`renderSunArcGaugeBitmap`):** `270°` radial gauge framing an interior horizon line, a glowing golden solar parabola, a dashed twilight dip curve, and a live sun orb positioned from current solar altitude and day progress.
- **100% Offline Ephemeris Metrics (`AstrologyEngine.computeSolarArcSnapshot`):** Displays the active solar light phase (`Solar Day`, `Golden Hour`, `Blue Hour`, `Twilight`, `Night`), Golden Hour transition time, total daylight duration, daily daylight delta in minutes, and exact sunrise/sunset timestamps without network requests.

### 2.6 Night Sky & Stargazing Index (`OneUiStargazingWidget`)
- **Provider:** `app/src/main/res/xml/widget_weather_stargazing_info.xml` (`2×1` default, `previewLayout="@layout/widget_preview_weather_stargazing"`)
- **Canvas Graphic (`renderStargazingGaugeBitmap`):** `270°` indigo-to-cyan radial gauge enclosing a night-sky dome with 4-point sparkles, constellation lines, and percentage score.
- **Cross-Domain Metrics (`AstrologyEngine.computeStargazingSnapshot` + Open-Meteo `cloud_cover`):** Combines Open-Meteo cloud coverage (`70%` weight) with offline lunar darkness (`30%` weight) into a `0%–100%` clarity index alongside naked-eye planets currently above the local horizon (`> 5°` altitude).

---

## 3. Companion App Weather Tab (`WeatherDetailScreen`)
`WeatherDetailScreen` (`app/src/main/java/com/anacatavc/oneuiextrawidgets/ui/screens/WeatherDetailScreen.kt`) mirrors all six widgets in `OneUiCard` sections with a `64.dp` `Canvas` gauge alongside metric readouts:
- **2-Metric Cards (Rain, UV):** Use `Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween)`.
- **Multi-Metric Cards (European AQI, Solar Arc, Stargazing):** Use `FlowRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp))` so metrics wrap cleanly on narrow viewports or large font scales instead of squishing into vertical columns.
