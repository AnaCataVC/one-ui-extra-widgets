# `AstrologyEngine` — Offline Ephemeris & Transit Engine

`AstrologyEngine` (`app/src/main/java/com/anacatavc/oneuiextrawidgets/data/astrology/AstrologyEngine.kt`) is a 100% offline astronomical and astrological ephemeris calculator powered by **Astronomy Engine** (`io.github.cosinekitty:astronomy`). It performs zero network calls and serves as the shared calculation core for `OneUiAstrologyWidget`, `OneUiMoonPhaseWidget`, `OneUiSunArcWidget`, `OneUiStargazingWidget`, `WeatherDetailScreen`, and `AstrologyDetailScreen`.

---

## Core Entry Points

```kotlin
val snapshot: AstrologySnapshot = AstrologyEngine.computeSnapshot(instant = Instant.now())
val solarArc: SolarArcSnapshot = AstrologyEngine.computeSolarArcSnapshot(latitude, longitude)
val stargazing: StargazingSnapshot = AstrologyEngine.computeStargazingSnapshot(cloudCoverPct, latitude, longitude)
```

`computeSnapshot(instant)`, `computeSolarArcSnapshot(...)`, and `computeStargazingSnapshot(...)` convert `Instant` into an `Astronomy Engine` `Time` and evaluate solar, lunar, horizontal, and planetary ephemerides deterministically.

---

## Data Models

| Type | Role | Key Fields |
| :--- | :--- | :--- |
| `ZodiacSign` | 12 tropical zodiac signs (`ARIES`..`PISCES`) | `symbol`, `nameEs`, `nameEn`, `fromEclipticLongitude(longitudeDeg)` |
| `PlanetaryMotionState` | Orbital motion state relative to Earth | `DIRECT`, `RETROGRADE`, `PRE_SHADOW`, `POST_SHADOW` |
| `PlanetTransitStatus` | Snapshot for a single tracked planet | `body`, `zodiacSign`, `degreeInSign`, `dailySpeedDeg`, `motionState` |
| `LunarPhaseStatus` | Scientific & zodiacal lunar state | `phaseAngleDeg`, `illuminationFraction`, `illuminationPercent`, `synodicAgeDays`, `isWaxing`, `moonSign` |
| `AstrologySnapshot` | Aggregated snapshot returned by `computeSnapshot` | `sunSign`, `sunDegreeInSign`, `lunarStatus`, `planets`, `mercuryStatus` |
| `SolarLightPhase` | Solar elevation light classification | `DAYLIGHT`, `GOLDEN_HOUR`, `BLUE_HOUR`, `TWILIGHT`, `NIGHT`, `localizedLabel(isSpanish)` |
| `SolarArcSnapshot` | Solar arc, Golden Hour & daylight duration state | `sunAltitudeDeg`, `dayProgressFraction`, `sunriseFormatted`, `sunsetFormatted`, `goldenHourFormatted`, `daylightDurationFormatted`, `lightPhase`, `daylightDeltaMinutes` |
| `StargazingSnapshot` | Night-sky clarity & visible naked-eye planets | `scorePct`, `cloudCoverPct`, `moonIlluminationPct`, `visiblePlanetNamesEs`, `visiblePlanetNamesEn`, `localizedVisiblePlanets(isSpanish)` |

---

## Ephemeris Calculations

### 1. Tropical Sun Sign & Degree
- Calls `sunPosition(time)` to obtain the apparent ecliptic longitude (`elon`).
- Maps `elon` to a `ZodiacSign` in `30°` segments via `ZodiacSign.fromEclipticLongitude` and computes `sunDegreeInSign` in `[0°, 30°)`.

### 2. Lunar Phase, Illumination & Zodiac Transit
- **Phase Angle:** `moonPhase(time)` in `[0°, 360°)` (`0°` = New Moon, `180°` = Full Moon).
- **Illumination:** `illumination(Body.Moon, time).phaseFraction` exposed as `illuminationPercent` (`0–100`).
- **Synodic Age:** `(normalizedPhaseAngleDeg / 360.0) * 29.530588` days.
- **Moon Sign:** `eclipticGeoMoon(time).lon` mapped to `ZodiacSign`.
- **Phase Classification:** `classifyMoonPhaseName(angleDeg)` maps the phase angle across 8 bilingual lunar phases (`New Moon` / `Luna Nueva`, `Waxing Crescent` / `Luna Creciente`, `First Quarter` / `Cuarto Creciente`, `Waxing Gibbous` / `Gibosa Creciente`, `Full Moon` / `Luna Llena`, `Waning Gibbous` / `Gibosa Menguante`, `Last Quarter` / `Cuarto Menguante`, `Waning Crescent` / `Luna Menguante`).

### 3. Tracked Planets & Retrograde / Shadow Detection
`AstrologyEngine` tracks five planets using `geocentricEclipticLongitude(body, time)` (`geoVector(body, time, Aberration.Corrected)` converted via `equatorialToEcliptic`):

| Planet | `Body` | `shadowWindowDays` |
| :--- | :--- | :--- |
| Mercury | `Body.Mercury` | `14.0` |
| Venus | `Body.Venus` | `28.0` |
| Mars | `Body.Mars` | `35.0` |
| Jupiter | `Body.Jupiter` | `25.0` |
| Saturn | `Body.Saturn` | `25.0` |

Daily angular velocity is computed as `angularDeltaDegrees(lonNow, lonTomorrow)` wrapped to `(-180°, 180°]`:
- **`RETROGRADE`:** `dailySpeed < 0.0` (apparent backward motion).
- **`PRE_SHADOW`:** `dailySpeed >= 0.0` and the planet's daily speed at `time + shadowWindowDays` is negative.
- **`POST_SHADOW`:** `dailySpeed >= 0.0`, not in pre-shadow, and the planet's daily speed at `time - shadowWindowDays` was negative.
- **`DIRECT`:** Otherwise.

### 4. Solar Arc, Golden Hour & Blue Hour (`computeSolarArcSnapshot`)
- Computes local solar elevation using `Observer(latitude, longitude, 0.0)`, `equator(Body.Sun, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)`, and `horizon(..., Refraction.Normal).altitude`.
- Classifies the active light window (`classifySolarLightPhase`) across `DAYLIGHT` (`> 6.0°`), `GOLDEN_HOUR` (`-4.0°..6.0°`), `BLUE_HOUR` (`-8.0°..-4.0°`), `TWILIGHT` (`-18.0°..-8.0°`), and `NIGHT` (`< -18.0°`).
- Resolves sunrise, sunset, daylight duration, and daily daylight change (`daylightDeltaMinutes`) via `searchRiseSet`, plus the next Golden Hour transition (`6.0°` / `-4.0°`) via `searchAltitude`.

### 5. Night Sky Clarity & Visible Planets (`computeStargazingSnapshot`)
- Computes the `0%–100%` stargazing clarity score (`computeStargazingScore`) by combining Open-Meteo `cloud_cover` (`70%` weight) with offline lunar darkness (`30%` weight).
- Evaluates the horizontal altitude (`horizon(..., Refraction.Normal).altitude`) of Mercury, Venus, Mars, Jupiter, and Saturn at current night or evening civil dusk (`-6.0°` sun altitude), ranking planets visible above `5.0°`.

---

## Domain Separation & Consumers

- **`OneUiAstrologyWidget` & `AstrologyDetailScreen`** ([Astrology Widget Specs](astrology-widget.md)): Consume `sunSign`, `sunDegreeInSign`, `lunarStatus.moonSign`, `lunarStatus.illuminationPercent`, `lunarStatus.localizedPhaseName`, and `planets` (`Mercury`–`Saturn`).
- **`OneUiMoonPhaseWidget`, `OneUiSunArcWidget`, `OneUiStargazingWidget` & `WeatherDetailScreen`** ([Weather, Solar & Astronomical Widgets](weather-widgets.md)): Consume strictly scientific lunar, solar elevation, and night-sky visibility metrics (`SolarArcSnapshot`, `StargazingSnapshot`, `lunarStatus`) without displaying zodiac or astrological sign data.

---

## Unit Tests

Pure-logic ephemeris behavior is verified under `app/src/test/` in `AstrologyEngineTest.kt` (`./gradlew testDebugUnitTest`).
