# UX Specification: 4 Modular One UI Weather & Scientific Lunar Widgets

> **Status:** Implemented  
> **Target Interface:** Mobile (Android Jetpack Glance `SizeMode.Exact` + Jetpack Compose Material 3)  
> **Related Plan:** `implementation_plan.md`

---

## 1. User Intent & Mental Model
- **Primary User Goal:** Glance at specific environmental and astronomical metrics (**Rain Probability**, **Air Quality / AQI**, **Solar UV Index**, and **Scientific Moon Phase**) via independent, visually rich `2×1` compact pills on the Samsung One UI home screen—without redundant current temperature readouts (already covered by the native Samsung Weather widget) and without mixing astrological signs into the scientific Moon widget.
- **Mental Model (Jakob's Law):** Samsung One UI 6.1 – 8.5 modular health & weather tiles (`26dp` squircle corners, high-contrast `270°` radial arc gauges, adaptive Day/Night translucent surfaces, and effortless resizing from `2×1` compact pills to `2×2` squares or `4×2` wide cards).
- **Progressive Disclosure Strategy:**
  - **`2×1` Compact Pill (`PILL` mode, default):** Left-hand `Canvas` gauge/sphere + primary numerical value + short localized status label (`Buena`, `Moderado`, `Sin lluvia`, `Edad 10.2 d`) + tap-to-refresh hit-zone.
  - **`2×2` Square Card (`SQUARE` mode):** Larger centered `Canvas` graphic + primary readout + secondary metric pill ($\text{PM}_{2.5}$, UV protection tip, synodic lunar age `10.2 d / 29.5 d`, or 6h rain window).
  - **`4×2` Wide Card (`EXPANDED` mode):** Side-by-side dashboard with large `Canvas` graphic on the left and two metric pills ($\text{PM}_{2.5}$ + $\text{PM}_{10}$, UV risk + advice, synodic age + solar elongation angle) on the right.

---

## 2. The 5 Essential UX States

| State | Widget / Screen Behavior | Actionable Elements & Microcopy |
| :--- | :--- | :--- |
| **1. Ideal / Populated** | Displays live/cached Open-Meteo metric and offline `Astronomy Engine` scientific lunar phase with deterministic `Canvas` arc gauges/sphere. | Tap widget body to open `WeatherDetailScreen`; tap `↻` badge to trigger `RefreshWeatherAction` (`updateAllWeatherWidgets`). |
| **2. Empty** | Location permission not yet granted; uses default city fallback (`Santiago`) with clear prompt in companion screen. | Companion app shows `Grant` / `Permitir` (`FilledTonalButton`) in the Permissions card. |
| **3. Loading & Transition** | Manual or background Open-Meteo sync in flight. | Companion CTA disables (`enabled = false`), displays inline `CircularProgressIndicator`, and updates label to `Refreshing weather...` / `Actualizando clima...`. |
| **4. Error & Recovery** | Network timeout or Open-Meteo HTTP error. | Zero stack-trace leakage: displays empathetic inline message with an `OutlinedButton` (`Retry` / `Reintentar`) while keeping widgets populated with last cached values. |
| **5. Partial / Degraded** | Device offline. | `OneUiMoonPhaseWidget` computes 100% offline via `Astronomy Engine`; Rain, AQI, and UV widgets render last persisted `DataStore` snapshot (`CachedWeatherState`). |

---

## 3. Microcopy & Widget Directory

| Widget | Title (EN / ES) | Primary Readout | Secondary Badge / Scientific Detail |
| :--- | :--- | :--- | :--- |
| **`OneUiRainWidget`** | `One UI Weather: Rain Probability` / `Clima One UI: Probabilidad de Lluvia` | `0%–100%` | `Dry` / `Low Chance` / `Moderate` / `High Rain` (`Sin lluvia` / `Prob. Baja` / `Moderada` / `Lluvia Alta`) • Next 6h |
| **`OneUiAqiWidget`** | `One UI Weather: Air Quality (AQI)` / `Clima One UI: Calidad del Aire (AQI)` | `Good`..`Very Poor` | `AQI <N>` + `PM2.5` & `PM10` ($\mu\text{g/m}^3$) |
| **`OneUiUvWidget`** | `One UI Weather: Solar UV Index` / `Clima One UI: Índice UV Solar` | `UV <N>` | `Low`..`Extreme` + SPF guidance (`Use SPF 30+` / `Usa FPS 30+`) |
| **`OneUiMoonPhaseWidget`** | `One UI Weather: Lunar Phase` / `Clima One UI: Fase Lunar` | `<Phase Name>` | `<Illumination>%` + Synodic Age (`10.2 d / 29.5 d`) + Solar Elongation (`124° Waxing`) |

---

## 4. Interaction Ergonomics & Safety Guardrails
- **Zero Temperature & Zero Astrology in Weather Widgets:** None of the 4 weather widgets render `temperatureC` or zodiac signs (`moonSign`).
- **Concentric Border-Radius:** Root widget containers use `OneUiGlanceTokens.CornerRadius` (`26.dp`) and inner pills use `OneUiGlanceTokens.InnerRadius` (`12.dp`).
- **Reliable Glance Touch Targets:** Refresh hit-zones wrap a concrete `Spacer(GlanceModifier.fillMaxSize())` and declare localized `contentDescription` semantics.
