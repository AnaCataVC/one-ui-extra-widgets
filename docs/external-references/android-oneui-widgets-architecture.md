> **Created:** 2026-11-16
> **Last Updated:** 2026-11-16

# Android & Samsung One UI Extra Widgets — Technical Research & Architecture Analysis

## 1. Target Device & Cross-Device Compatibility

- **Primary Target:** Samsung Galaxy A55 5G (`SM-A556B` / `SM-A556E`)
  - **SoC:** Samsung Exynos 1480 (Octa-core, 64-bit `arm64-v8a`)
  - **Display:** 6.6" Super AMOLED, `1080 x 2340` px (19.5:9 ratio, ~390 ppi, `xxhdpi` / `sw360dp`–`sw411dp`), 120Hz.
  - **OS:** Shipped with Android 14 (One UI 6.1), upgradable through Android 15/16 (One UI 7/8).
- **Cross-Device Compatibility Strategy:**
  - Setting `minSdk = 26` (Android 8.0) or `minSdk = 31` (Android 12, where modern rounded widget corners `system_app_widget_background_radius` and Material You dynamic colors were introduced) and `targetSdk = 35` ensures 100% compatibility with the Galaxy A55 while working seamlessly on any modern Android phone (Pixel, Motorola, Xiaomi, Nothing, other Galaxy S/A/Z models).
  - Using standard Android Jetpack APIs (`AppWidgetProvider` / `Jetpack Glance`) instead of closed Samsung-only system signatures allows the APK to install on any Android device without root or system privileges.

---

## 2. Samsung One UI Widget Design System Guidelines

Samsung One UI 6 / 7 / 8 widgets follow distinct visual tokens compared to stock Pixel Material 3 widgets:
- **Corner Radius:** `22dp`–`26dp` squircle-like rounded corners on widget containers (`@android:dimen/system_app_widget_background_radius` on One UI).
- **Grid Cell Sizing:** Standard One UI home screen grids (4x5, 4x6, 5x5, 5x6) use widget templates:
  - `2x1` (Small pill/card)
  - `2x2` (Square card — ideal for Moon Phase, AQI, Retro iPod/Discman compact, or 5G/Wi-Fi gauge)
  - `4x1` (Horizontal bar — ideal for Music banner or Weather strip)
  - `4x2` (Medium card — ideal for detailed Weather + AQI + Rain Probability, or full Retro iPod/MP3/Discman player)
- **Surfaces & Transparency:** Semi-translucent dark/light cards (`#CC1C1C1E` in Dark Mode, `#CCF2F2F7` in Light Mode) or Dynamic Wallpaper tinting, with subtle inner padding (`12dp`–`16dp`) and high-contrast typography (SamsungOne / Roboto Medium & Bold).
- **Tap Behavior:** Tapping a widget either performs an immediate action (e.g., Play/Pause/Next on Music, Refresh on Network/Weather) or opens the native Samsung app (e.g., tapping the Weather widget can launch Samsung Weather `com.sec.android.daemonapp` if installed, or our companion detail screen as fallback).

---

## 3. Domain Research by Widget Category

### 3.1 Weather Widgets (Rain Probability, Air Quality / AQI, UV, Moon Phase)
- **How Samsung Weather Works (`com.sec.android.daemonapp`):**
  - Samsung's pre-installed Weather app fetches data from **The Weather Channel (TWC / weather.com)** using a proprietary corporate API key embedded in the system daemon.
  - On modern One UI versions (Android 14+), the internal ContentProvider of `com.sec.android.daemonapp` is protected by Samsung's `signatureOrSystem` permission, meaning third-party apps cannot directly query its SQLite database without root, and TWC's commercial API requires an expensive enterprise contract.
- **How to Match Samsung's Experience & Data Free of Charge:**
  1. **Open-Meteo API (Primary Zero-Key Provider):**
     - Completely free, open-source, no API key required.
     - Combines national weather services (NOAA GFS, ECMWF, DWD, UKMO) for high local accuracy.
     - **Forecast Endpoint (`https://api.open-meteo.com/v1/forecast`):** Provides hourly & daily `precipitation_probability`, `precipitation`, `temperature_2m`, `apparent_temperature`, `uv_index`, `wind_speed_10m`, `weather_code`.
     - **Air Quality Endpoint (`https://air-quality-api.open-meteo.com/v1/air-quality`):** Provides real-time `european_aqi`, `us_aqi`, `pm10`, `pm2_5`, `carbon_monoxide`, `nitrogen_dioxide`, `ozone`, and pollen data.
  2. **Optional Custom API Key Support (The Weather Channel / WeatherAPI / OpenWeatherMap):**
     - If desired, the app settings can allow configuring a personal API key or selecting the weather model, and **clicking the widget can deep-link directly into Samsung's native Weather app (`com.sec.android.daemonapp`)** so the user's workflow stays unified with One UI.

### 3.2 Astrology & Astronomy Widgets (Moon Phases, Sun Sign, Mercury & Planetary Retrogrades)
- **100% Offline, Zero-Latency Calculation:**
  - Astronomical and astrological phenomena do **not** require an external API; they can be computed deterministically on-device with sub-arcminute precision using **`cosinekitty/astronomy` (Astronomy Engine for Kotlin/Java, Maven Central: `io.github.cosinekitty:astronomy:2.1.19`)**.
- **Capabilities Calculated Locally:**
  - **Moon Phase & Illumination:** Exact lunar phase angle (`0°–360°`), illumination percentage, waxing/waning status, and next New/Full Moon dates.
  - **Current Tropical Sun Sign (& Moon Sign):** Computed from the apparent geocentric ecliptic longitude ($\lambda$) of the Sun (and Moon) divided into the 12 tropical zodiac sectors ($30^\circ$ each: Aries $0^\circ\text{–}30^\circ$, Taurus $30^\circ\text{–}60^\circ$, etc.).
  - **Mercury Retrograde (and other planets: Venus, Mars, Jupiter, Saturn):** A planet is in **apparent retrograde motion** from Earth when its apparent geocentric ecliptic longitude decreases over time ($d\lambda / dt < 0$). Using `Astronomy Engine`, we compare the geocentric ecliptic longitude at $t$ and $t + \Delta t$ (e.g., $\pm 1$ hour) to detect whether Mercury (or any planet) is Direct, Retrograde, or in Pre/Post-Retrograde Shadow ("Sombra pre/post-retrógrada"), and find the exact start/end dates of the current or next retrograde cycle.

### 3.3 Retro Music Player Widgets (iPod Classic, Retro MP3 Stick, Sony Discman)
- **Universal Compatibility with Any Music App (Spotify, YouTube Music, Apple Music, Samsung Music, Tidal):**
  - Android provides `NotificationListenerService` + `MediaSessionManager.getActiveSessions()`.
  - Once the user grants Notification Access to our app once, our background service listens in real time to whichever media player is active on the phone:
    - Reads **Track Title, Artist, Album Name, Playback State (Playing/Paused), Track Duration/Position, and Album Art `Bitmap`**.
    - Sends real transport controls (`mediaController.transportControls.play()`, `pause()`, `skipToNext()`, `skipToPrevious()`) directly from the widget buttons!
- **Retro Skins with One UI Polish:**
  1. **iPod Classic Widget:** Metallic/white or dark One UI body, classic LCD screen showing song/artist/album art and battery icon, and an interactive **Click Wheel** where tapping `MENU` opens the active music app, `|<<` / `>>|` changes tracks, and `>|` toggles play/pause.
  2. **Retro 2000s MP3 Player (Backlit LCD Stick):** Compact `4x1` or `4x2` pill/stick design with cyan/amber/green backlit dot-matrix LCD aesthetic, bitrate badge (`128 kbps MP3`), scrolling-style track title, and tactile silver buttons.
  3. **Portable CD Player (Discman):** Circular/square window showing the CD (either metallic reflective disc or the current song's Album Art masked as a CD disc with a center spindle hole), "ESP Anti-Shock" badge, track number (`TRK 01`), and physical-style transport buttons.

### 3.4 5G & Wi-Fi Network Quality Widgets
- **Android 14–16 APIs (`TelephonyManager`, `WifiManager`, `ConnectivityManager`):**
  - **Wi-Fi Quality:**
    - `ConnectivityManager.getNetworkCapabilities()` + `WifiInfo` (via `NetworkCapabilities.transportInfo` on Android 12+):
    - Signal level (`0–4` bars), RSSI in `dBm` (e.g., `-52 dBm` = Excelente), Link Speed in `Mbps` (e.g., `866 Mbps`), Wi-Fi Standard (Wi-Fi 6 `802.11ax` on Galaxy A55, Wi-Fi 5, etc.), Frequency band (`5 GHz` / `2.4 GHz`), and SSID (if `ACCESS_FINE_LOCATION` is granted).
  - **5G / Cellular Quality:**
    - `TelephonyManager` + `SignalStrength` / `CellSignalStrengthNr` (5G NR) & `CellSignalStrengthLte` (4G LTE):
    - Detects exact network generation (`5G NSA/SA`, `4G+ LTE-A`, `4G`, `3G`), carrier name, signal level (`0–4` bars), and RSRP/SINR in `dBm` (with `READ_PHONE_STATE` / `ACCESS_COARSE_LOCATION`).
    - Includes a quick-refresh button and shortcut to One UI's Connections / Wi-Fi settings panel.

### 3.5 Localization (Spanish Default + English Selector)
- **Native Android Resource System + Per-App Language API:**
  - Using standard `res/values-es/strings.xml` and `res/values/strings.xml` (with default fallback to Spanish or automatic system language detection).
  - Because the Galaxy A55 runs Android 14+, it natively supports `LocaleConfig` and `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es" | "en"))`.
  - This means:
    1. By default, it automatically uses the phone's configured language (Spanish on your Galaxy A55).
    2. Inside the app's One UI Settings screen, you also have an explicit selector (**Automático del sistema / Español / English**) that updates both the app and all widgets immediately.

---

## 4. Architectural Options for Widget Rendering

### Option A: Jetpack Glance (`androidx.glance:glance-appwidget`) + Custom Canvas Graphics (Modern Declarative Stack)
- **How it works:** Uses Google's modern Jetpack Compose-style framework for widgets (`glance-appwidget:1.1.1`), combined with `Canvas`-rendered `Bitmap` overlays for complex visual gauges (Moon illumination rendering, CD/Discman album-art masking, signal arcs) and `WorkManager` + `NotificationListenerService`.
- **Pros:**
  - 100% Kotlin declarative code (no verbose XML files for every small state change).
  - Shares UI mental model with Jetpack Compose used in the configuration app.
  - Built-in support for responsive widget sizes (`SizeMode.Exact` / `SizeMode.Responsive`) for One UI grid resizing.
- **Cons:**
  - Glance translates to `RemoteViews` under the hood, so highly skeuomorphic layouts (like the iPod Click Wheel with 5 directional hit zones installed around a circle) require careful `Box` alignment + custom `Canvas` background drawing.

### Option B: Hybrid Architecture — Classic XML `RemoteViews` for Skeuomorphic Widgets (Music Players) + Jetpack Glance for Data Card Widgets (Weather, Astrology, Network)
- **How it works:** Uses Jetpack Glance for the One UI data cards (Weather, AQI, Moon Phase, Astrology, 5G/Wi-Fi) and classic Android XML `RemoteViews` (`RelativeLayout`/`ConstraintLayout` + Vector Drawables) for the retro Music widgets (iPod, MP3, Discman) where pixel-exact button hitboxes over circular wheels/chassis are critical.
- **Pros:**
  - Best of both worlds: rapid Compose-like development for One UI cards, and rock-solid, zero-flicker pixel-precise hitboxes and custom fonts/progress bars on the retro music players.
  - Zero Samsung launcher bitmap-caching bugs when album art changes rapidly on track skip.
- **Cons:**
  - Maintains two widget rendering paradigms (Glance Kotlin + `res/layout/` XML for music widgets).

---

## 5. Sources & References
- [Android Developers — Jetpack Glance AppWidget Overview](https://developer.android.com/develop/ui/compose/glance/create-app-widget)
- [Android Developers — App Widgets Overview & Enhancements](https://developer.android.com/develop/ui/views/appwidgets/overview)
- [Android Developers — NotificationListenerService API Reference](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
- [Android Developers — Request Permission to Access Nearby Wi-Fi Devices & SignalStrength](https://developer.android.com/develop/connectivity/wifi/wifi-permissions)
- [Open-Meteo Weather Forecast & Air Quality API Documentation](https://open-meteo.com/en/docs)
- [Astronomy Engine (`io.github.cosinekitty:astronomy`) Repository & Maven Central](https://github.com/cosinekitty/astronomy)
- [Samsung Galaxy A55 5G Technical Specifications](https://en.wikipedia.org/wiki/Samsung_Galaxy_A55_5G)
