# Modular One UI Weather & Lunar Widgets — Architecture & Visual Specs

The Weather & Scientific Lunar suite provides four independent, `SizeMode.Exact` home-screen widgets built with **Jetpack Glance**, deterministic **One UI Canvas** gauges (`WidgetCanvasRenderer`), **Open-Meteo** Forecast/Air Quality APIs, and offline **`Astronomy Engine`** calculations (targeting **Samsung One UI 8.5 / Android 16**, compatible with One UI 6.1–8.5).

By design, none of the four widgets display the current ambient temperature (`temperatureC`), avoiding visual redundancy with the pre-installed Samsung Weather widget and dedicating 100% of the card area to specialized environmental and astronomical gauges.

---

## 1. Shared Responsive Layout Calculator (`calculateWeatherCardLayout`)
All four widgets default to a compact **`2×1` pill** (`targetCellWidth="2"`, `targetCellHeight="1"`, `resizeMode="horizontal|vertical"`) and adapt dynamically across three layout modes:
- **`PILL` (`2×1` / `4×1`, `heightDp < 130dp`):** Horizontal pill with `12.dp` padding, a `42.dp–56.dp` `Canvas` gauge on the left, and the metric title, primary readout, status badge, and quick-refresh button (`↻`) on the right.
- **`SQUARE` (`2×2`, `heightDp >= 130dp && widthDp < 240dp`):** Vertical card with `14.dp` padding, a scaled `58.dp–82.dp` centered `Canvas` gauge, primary readout, status badge, and a bottom `12.dp` inner detail pill.
- **`EXPANDED` (`4×2`, `heightDp >= 130dp && widthDp >= 240dp`):** Wide horizontal dashboard with `14.dp` padding, a `64.dp–88.dp` `Canvas` gauge on the left, and two side-by-side `12.dp` inner metric pills on the right.

---

## 2. The Four Dedicated Widgets

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
