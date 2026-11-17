# `AstrologyEngine` — Offline Ephemeris & Transit Engine

`AstrologyEngine` (`app/src/main/java/com/anacatavc/oneuiextrawidgets/data/astrology/AstrologyEngine.kt`) is a 100% offline astronomical and astrological ephemeris calculator powered by **Astronomy Engine** (`io.github.cosinekitty:astronomy`). It performs zero network calls and serves as the shared calculation core for `OneUiAstrologyWidget`, `OneUiMoonPhaseWidget`, and `AstrologyDetailScreen`.

---

## Core Entry Point

```kotlin
val snapshot: AstrologySnapshot = AstrologyEngine.computeSnapshot(instant = Instant.now())
```

`computeSnapshot(instant)` converts the `Instant` into an `Astronomy Engine` `Time` and evaluates solar, lunar, and planetary positions in a single deterministic pass.

---

## Data Models

| Type | Role | Key Fields |
| :--- | :--- | :--- |
| `ZodiacSign` | 12 tropical zodiac signs (`ARIES`..`PISCES`) | `symbol`, `nameEs`, `nameEn`, `fromEclipticLongitude(longitudeDeg)` |
| `PlanetaryMotionState` | Orbital motion state relative to Earth | `DIRECT`, `RETROGRADE`, `PRE_SHADOW`, `POST_SHADOW` |
| `PlanetTransitStatus` | Snapshot for a single tracked planet | `body`, `zodiacSign`, `degreeInSign`, `dailySpeedDeg`, `motionState` |
| `LunarPhaseStatus` | Scientific & zodiacal lunar state | `phaseAngleDeg`, `illuminationFraction`, `illuminationPercent`, `synodicAgeDays`, `isWaxing`, `moonSign` |
| `AstrologySnapshot` | Aggregated snapshot returned by `computeSnapshot` | `sunSign`, `sunDegreeInSign`, `lunarStatus`, `planets`, `mercuryStatus` |

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

---

## Domain Separation & Consumers

- **`OneUiAstrologyWidget` & `AstrologyDetailScreen`** ([Astrology Widget Specs](astrology-widget.md)): Consume `sunSign`, `sunDegreeInSign`, `lunarStatus.moonSign`, `lunarStatus.illuminationPercent`, `lunarStatus.localizedPhaseName`, and `planets` (`Mercury`–`Saturn`).
- **`OneUiMoonPhaseWidget`** ([Weather & Lunar Widgets](weather-widgets.md)): Consumes strictly scientific lunar metrics from `lunarStatus` (`illuminationPercent`, `synodicAgeDays`, `normalizedPhaseAngleDeg`, `isWaxing`) without displaying zodiac or astrological data.

---

## Unit Tests

Pure-logic ephemeris behavior is verified under `app/src/test/` in `AstrologyEngineTest.kt` (`./gradlew testDebugUnitTest`).
