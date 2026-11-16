# UX Specification: Companion App & One UI Home Screen Widgets

> **Status:** Implemented  
> **Target Interface:** Mobile (Android Jetpack Compose Material 3 + Jetpack Glance)  
> **Scope:** 4-tab Companion App (`Home`, `Weather`, `Astrology`, `Network`) and 6 Home Screen Widgets

---

## 1. User Intent & Mental Model
- **Primary User Goal:** Configure permissions/language once in the companion app, inspect detailed weather, astrology, and 5G/Wi-Fi diagnostics on demand, and glance at or control widgets directly from the Samsung One UI home screen.
- **Mental Model (Jakob's Law):** Native Samsung One UI settings cards (`26dp` rounded blocks, generous top viewing header, bottom `NavigationBar`) paired with familiar tactile retro audio hardware (iPod Click Wheel, 2000s MP3 stick, Discman CD lid).
- **Progressive Disclosure Strategy:** Home-screen widgets surface only high-signal glanceable metrics (temperature, AQI, moon phase, Mercury retrograde badge, 5G/Wi-Fi bars, media transport); tapping any widget deep-links into its corresponding companion tab for full metrics and manual refresh.

---

## 2. The 5 Essential UX States

| State | Screen / Widget Behavior | Actionable Elements & Microcopy |
| :--- | :--- | :--- |
| **1. Ideal / Populated** | Displays live/cached Weather & AQI, offline Astrology ephemeris, 5G/Wi-Fi diagnostics, or active media playback metadata. | Single primary refresh CTA per detail screen (`Refresh Weather & AQI Now`, `Refresh Network Metrics`), `FilledTonalButton` for per-row permission grants. |
| **2. Empty** | Uninitialized permissions or idle media session. | Clear status indicator (`Grant` / `Permitir` tonal button on missing permissions; idle playback label `One UI` on retro music widgets). |
| **3. Loading & Transition** | Async Open-Meteo fetch or network metric read in progress. | Primary button disables (`enabled = false`), shows inline `CircularProgressIndicator`, and updates label to `Refreshing weather...` / `Measuring signal...`. |
| **4. Error & Recovery** | Open-Meteo network failure or timeout. | Empathetic inline error message without stack traces (`We couldn't refresh the weather. Check your connection and retry.`) paired with an `OutlinedButton` (`Retry` / `Reintentar`). |
| **5. Partial / Degraded** | Offline or location-denied state. | Falls back to last cached `DataStore` weather values (`CachedWeatherState`) and offline `Astronomy Engine` ephemeris without blocking the UI. |

---

## 3. Microcopy & Button Directory

| UI Element | Label (EN / ES) | Purpose & Feedback |
| :--- | :--- | :--- |
| Permission Row Action | `Grant` / `Permitir` (`FilledTonalButton`) | Requests runtime permission or opens Notification Listener settings without competing with a primary CTA. |
| Weather Refresh CTA | `Refresh Weather & AQI Now` / `Actualizar Clima y AQI Ahora` | Triggers fused location + Open-Meteo sync; shows spinner and disables while in flight. |
| Weather Retry CTA | `Retry` / `Reintentar` (`OutlinedButton`) | Shown below inline error message on failed fetch. |
| Network Refresh CTA | `Refresh Network Metrics` / `Actualizar Medición de Red` | Re-reads `TelephonyManager` / `WifiManager` snapshot. |

---

## 4. Interaction Ergonomics & Accessibility Guardrails
- **Concentric Border-Radius:** Glance widgets enforce `InnerRadius (12.dp) <= CornerRadius (26.dp) - CardPadding (14.dp)`.
- **Single Icon Family:** Bottom `NavigationBar` uses unified 24dp Material vector drawables (`ic_nav_home`, `ic_nav_weather`, `ic_nav_astrology`, `ic_nav_network`).
- **Screen Reader Semantics:** All icon-only widget controls (iPod Click Wheel zones, MP3/Discman transport buttons, Network refresh icon) declare localized `semantics { contentDescription = ... }`.
