package com.anacatavc.oneuiextrawidgets.data.astrology

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.searchAltitude
import io.github.cosinekitty.astronomy.searchRiseSet
import io.github.cosinekitty.astronomy.sunPosition
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class ZodiacSign(
    val symbol: String,
    val nameEs: String,
    val nameEn: String
) {
    ARIES("♈", "Aries", "Aries"),
    TAURUS("♉", "Tauro", "Taurus"),
    GEMINI("♊", "Géminis", "Gemini"),
    CANCER("♋", "Cáncer", "Cancer"),
    LEO("♌", "Leo", "Leo"),
    VIRGO("♍", "Virgo", "Virgo"),
    LIBRA("♎", "Libra", "Libra"),
    SCORPIO("♏", "Escorpio", "Scorpio"),
    SAGITTARIUS("♐", "Sagitario", "Sagittarius"),
    CAPRICORN("♑", "Capricornio", "Capricorn"),
    AQUARIUS("♒", "Acuario", "Aquarius"),
    PISCES("♓", "Piscis", "Pisces");

    fun localizedName(isSpanish: Boolean): String = if (isSpanish) nameEs else nameEn

    companion object {
        fun fromEclipticLongitude(longitudeDeg: Double): ZodiacSign {
            val normalized = ((longitudeDeg % 360.0) + 360.0) % 360.0
            val index = (normalized / 30.0).toInt().coerceIn(0, 11)
            return entries[index]
        }
    }
}

enum class PlanetaryMotionState(
    val badgeEs: String,
    val badgeEn: String
) {
    DIRECT("Directo", "Direct"),
    RETROGRADE("Retrógrado", "Retrograde"),
    PRE_SHADOW("Sombra Pre-Retrógrada", "Pre-Retrograde Shadow"),
    POST_SHADOW("Sombra Post-Retrógrada", "Post-Retrograde Shadow");

    fun localizedBadge(isSpanish: Boolean): String = if (isSpanish) badgeEs else badgeEn
}

data class PlanetTransitStatus(
    val body: Body,
    val symbol: String,
    val nameEs: String,
    val nameEn: String,
    val zodiacSign: ZodiacSign,
    val degreeInSign: Double,
    val dailySpeedDeg: Double,
    val motionState: PlanetaryMotionState
) {
    val isRetrograde: Boolean get() = motionState == PlanetaryMotionState.RETROGRADE
    fun localizedName(isSpanish: Boolean): String = if (isSpanish) nameEs else nameEn
}

data class LunarPhaseStatus(
    val phaseAngleDeg: Double,
    val illuminationFraction: Double,
    val phaseNameEs: String,
    val phaseNameEn: String,
    val moonSign: ZodiacSign
) {
    val illuminationPercent: Int get() = (illuminationFraction * 100.0).toInt().coerceIn(0, 100)
    val normalizedPhaseAngleDeg: Double get() = ((phaseAngleDeg % 360.0) + 360.0) % 360.0
    val synodicAgeDays: Double get() = (normalizedPhaseAngleDeg / 360.0) * 29.530588
    val isWaxing: Boolean get() = normalizedPhaseAngleDeg < 180.0

    fun localizedPhaseName(isSpanish: Boolean): String = if (isSpanish) phaseNameEs else phaseNameEn
    fun localizedCycleTrend(isSpanish: Boolean): String = when {
        isWaxing -> if (isSpanish) "Creciente" else "Waxing"
        else -> if (isSpanish) "Menguante" else "Waning"
    }
}

data class AstrologySnapshot(
    val sunSign: ZodiacSign,
    val sunDegreeInSign: Double,
    val lunarStatus: LunarPhaseStatus,
    val planets: List<PlanetTransitStatus>
) {
    val mercuryStatus: PlanetTransitStatus
        get() = planets.first { it.body == Body.Mercury }
}

enum class SolarLightPhase(
    val labelEs: String,
    val labelEn: String
) {
    DAYLIGHT("Día Solar", "Solar Day"),
    GOLDEN_HOUR("Hora Dorada", "Golden Hour"),
    BLUE_HOUR("Hora Azul", "Blue Hour"),
    TWILIGHT("Crepúsculo", "Twilight"),
    NIGHT("Noche", "Night");

    fun localizedLabel(isSpanish: Boolean): String = if (isSpanish) labelEs else labelEn
}

data class SolarArcSnapshot(
    val sunAltitudeDeg: Double,
    val dayProgressFraction: Float,
    val sunriseFormatted: String,
    val sunsetFormatted: String,
    val goldenHourFormatted: String,
    val daylightDurationFormatted: String,
    val lightPhase: SolarLightPhase,
    val daylightDeltaMinutes: Int
)

data class StargazingSnapshot(
    val scorePct: Int,
    val cloudCoverPct: Int,
    val moonIlluminationPct: Int,
    val visiblePlanetNamesEs: List<String>,
    val visiblePlanetNamesEn: List<String>
) {
    fun localizedVisiblePlanets(isSpanish: Boolean): List<String> =
        if (isSpanish) visiblePlanetNamesEs else visiblePlanetNamesEn
}

/**
 * 100% Offline Astronomical & Astrological Ephemeris Calculator powered by Astronomy Engine.
 */
object AstrologyEngine {

    private data class PlanetMeta(
        val body: Body,
        val symbol: String,
        val nameEs: String,
        val nameEn: String,
        val shadowWindowDays: Double
    )

    private val trackedPlanets = listOf(
        PlanetMeta(Body.Mercury, "☿", "Mercurio", "Mercury", shadowWindowDays = 14.0),
        PlanetMeta(Body.Venus, "♀", "Venus", "Venus", shadowWindowDays = 28.0),
        PlanetMeta(Body.Mars, "♂", "Marte", "Mars", shadowWindowDays = 35.0),
        PlanetMeta(Body.Jupiter, "♃", "Júpiter", "Jupiter", shadowWindowDays = 25.0),
        PlanetMeta(Body.Saturn, "♄", "Saturno", "Saturn", shadowWindowDays = 25.0)
    )

    fun computeSnapshot(instant: Instant = Instant.now()): AstrologySnapshot {
        val time = Time.fromMillisecondsSince1970(instant.toEpochMilli())

        // 1. Sun Sign & position
        val sunEcliptic = sunPosition(time)
        val sunSign = ZodiacSign.fromEclipticLongitude(sunEcliptic.elon)
        val sunDegInSign = ((sunEcliptic.elon % 360.0) + 360.0) % 30.0

        // 2. Moon Phase, Illumination & Moon Zodiac Transit
        val phaseAngle = moonPhase(time) // 0..360
        val moonIllum = illumination(Body.Moon, time).phaseFraction
        val moonEcliptic = eclipticGeoMoon(time)
        val moonSign = ZodiacSign.fromEclipticLongitude(moonEcliptic.lon)
        val (phaseEs, phaseEn) = classifyMoonPhaseName(phaseAngle)

        val lunarStatus = LunarPhaseStatus(
            phaseAngleDeg = phaseAngle,
            illuminationFraction = moonIllum,
            phaseNameEs = phaseEs,
            phaseNameEn = phaseEn,
            moonSign = moonSign
        )

        // 3. Planetary Positions & Retrograde / Shadow Detection
        val planetStatuses = trackedPlanets.map { meta ->
            val lonNow = geocentricEclipticLongitude(meta.body, time)
            val lonTomorrow = geocentricEclipticLongitude(meta.body, time.addDays(1.0))
            val dailySpeed = angularDeltaDegrees(lonNow, lonTomorrow)

            val motionState = when {
                dailySpeed < 0.0 -> PlanetaryMotionState.RETROGRADE
                else -> {
                    // Check if a retrograde station occurred recently (Post-Shadow) or is upcoming (Pre-Shadow)
                    val lonFuture = geocentricEclipticLongitude(meta.body, time.addDays(meta.shadowWindowDays))
                    val lonFutureNext = geocentricEclipticLongitude(meta.body, time.addDays(meta.shadowWindowDays + 1.0))
                    val futureSpeed = angularDeltaDegrees(lonFuture, lonFutureNext)

                    val lonPast = geocentricEclipticLongitude(meta.body, time.addDays(-meta.shadowWindowDays))
                    val lonPastNext = geocentricEclipticLongitude(meta.body, time.addDays(-meta.shadowWindowDays + 1.0))
                    val pastSpeed = angularDeltaDegrees(lonPast, lonPastNext)

                    when {
                        futureSpeed < 0.0 -> PlanetaryMotionState.PRE_SHADOW
                        pastSpeed < 0.0 -> PlanetaryMotionState.POST_SHADOW
                        else -> PlanetaryMotionState.DIRECT
                    }
                }
            }

            PlanetTransitStatus(
                body = meta.body,
                symbol = meta.symbol,
                nameEs = meta.nameEs,
                nameEn = meta.nameEn,
                zodiacSign = ZodiacSign.fromEclipticLongitude(lonNow),
                degreeInSign = ((lonNow % 360.0) + 360.0) % 30.0,
                dailySpeedDeg = dailySpeed,
                motionState = motionState
            )
        }

        return AstrologySnapshot(
            sunSign = sunSign,
            sunDegreeInSign = sunDegInSign,
            lunarStatus = lunarStatus,
            planets = planetStatuses
        )
    }

    /**
     * Computes apparent geocentric ecliptic longitude in degrees [0, 360) for any planet.
     */
    fun geocentricEclipticLongitude(body: Body, time: Time): Double {
        val vec = geoVector(body, time, Aberration.Corrected)
        val ecl = equatorialToEcliptic(vec)
        return ((ecl.elon % 360.0) + 360.0) % 360.0
    }

    fun classifySolarLightPhase(sunAltitudeDeg: Double): SolarLightPhase {
        if (sunAltitudeDeg.isNaN()) return SolarLightPhase.NIGHT
        return when {
            sunAltitudeDeg > 6.0 -> SolarLightPhase.DAYLIGHT
            sunAltitudeDeg >= -4.0 -> SolarLightPhase.GOLDEN_HOUR
            sunAltitudeDeg >= -8.0 -> SolarLightPhase.BLUE_HOUR
            sunAltitudeDeg >= -18.0 -> SolarLightPhase.TWILIGHT
            else -> SolarLightPhase.NIGHT
        }
    }

    fun computeSolarArcSnapshot(
        latitude: Double,
        longitude: Double,
        instant: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SolarArcSnapshot {
        val safeLat = if (latitude.isNaN()) -33.4489 else latitude.coerceIn(-89.9, 89.9)
        val safeLon = if (longitude.isNaN()) -70.6693 else longitude.coerceIn(-180.0, 180.0)
        val observer = Observer(safeLat, safeLon, 0.0)
        val nowTime = Time.fromMillisecondsSince1970(instant.toEpochMilli())
        val sunAltitudeDeg = bodyAltitudeDegrees(Body.Sun, nowTime, observer)
        val lightPhase = classifySolarLightPhase(sunAltitudeDeg)

        val localDate = instant.atZone(zoneId).toLocalDate()
        val dayStartInstant = localDate.atStartOfDay(zoneId).toInstant()
        val dayStartTime = Time.fromMillisecondsSince1970(dayStartInstant.toEpochMilli())
        val yesterdayStartInstant = localDate.minusDays(1).atStartOfDay(zoneId).toInstant()
        val yesterdayStartTime = Time.fromMillisecondsSince1970(yesterdayStartInstant.toEpochMilli())

        var sunrise = searchRiseSet(Body.Sun, observer, Direction.Rise, dayStartTime, 1.0)
        var sunset = if (sunrise != null) {
            searchRiseSet(Body.Sun, observer, Direction.Set, sunrise, 1.0)
        } else {
            searchRiseSet(Body.Sun, observer, Direction.Set, dayStartTime, 1.0)
        }

        if (sunrise != null &&
            instant.toEpochMilli() < sunrise.toMillisecondsSince1970() &&
            sunAltitudeDeg > -0.833
        ) {
            val activeRise = searchRiseSet(Body.Sun, observer, Direction.Rise, nowTime, -1.0)
            val activeSet = searchRiseSet(Body.Sun, observer, Direction.Set, nowTime, 1.0)
            if (activeRise != null && activeSet != null) {
                sunrise = activeRise
                sunset = activeSet
            }
        }

        val ySunrise = searchRiseSet(Body.Sun, observer, Direction.Rise, yesterdayStartTime, 1.0)
        val ySunset = ySunrise?.let { searchRiseSet(Body.Sun, observer, Direction.Set, it, 1.0) }

        val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(zoneId)
        val nowMs = instant.toEpochMilli()

        val (dayProgressFraction, daylightDurationFormatted, daylightDeltaMinutes) =
            if (sunrise != null && sunset != null) {
                val riseMs = sunrise.toMillisecondsSince1970()
                val setMs = sunset.toMillisecondsSince1970()
                val spanMs = (setMs - riseMs).coerceAtLeast(1L)
                val progress = ((nowMs - riseMs).toDouble() / spanMs.toDouble())
                    .toFloat().coerceIn(0f, 1f)
                val todayMinutes = (spanMs / 60_000.0).roundToInt().coerceAtLeast(0)
                val delta = if (ySunrise != null && ySunset != null) {
                    val yesterdayMs = (ySunset.toMillisecondsSince1970() - ySunrise.toMillisecondsSince1970())
                        .coerceAtLeast(0L)
                    ((spanMs - yesterdayMs) / 60_000.0).roundToInt()
                } else {
                    0
                }
                Triple(progress, formatDurationMinutes(todayMinutes), delta)
            } else if (sunAltitudeDeg > -0.833) {
                val dayElapsed = ((nowMs - dayStartInstant.toEpochMilli()).toDouble() / 86_400_000.0)
                    .toFloat().coerceIn(0f, 1f)
                Triple(dayElapsed, "24h 00m", 0)
            } else {
                Triple(0f, "0h 00m", 0)
            }

        val goldenHourTime = findNextGoldenHourTransition(
            observer = observer,
            nowTime = nowTime,
            sunAltitudeDeg = sunAltitudeDeg,
            lightPhase = lightPhase
        )

        return SolarArcSnapshot(
            sunAltitudeDeg = sunAltitudeDeg,
            dayProgressFraction = dayProgressFraction,
            sunriseFormatted = sunrise?.let { formatTime(it, formatter) } ?: "--:--",
            sunsetFormatted = sunset?.let { formatTime(it, formatter) } ?: "--:--",
            goldenHourFormatted = goldenHourTime?.let { formatTime(it, formatter) } ?: "--:--",
            daylightDurationFormatted = daylightDurationFormatted,
            lightPhase = lightPhase,
            daylightDeltaMinutes = daylightDeltaMinutes
        )
    }

    fun computeStargazingScore(cloudCoverPct: Int, moonIlluminationPct: Int): Int {
        val cloudScore = (100 - cloudCoverPct.coerceIn(0, 100)) * 0.70
        val darkScore = (100 - moonIlluminationPct.coerceIn(0, 100)) * 0.30
        return (cloudScore + darkScore).roundToInt().coerceIn(0, 100)
    }

    fun computeStargazingSnapshot(
        cloudCoverPct: Int,
        latitude: Double,
        longitude: Double,
        instant: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StargazingSnapshot {
        val safeCloudCover = cloudCoverPct.coerceIn(0, 100)
        val moonPct = computeSnapshot(instant).lunarStatus.illuminationPercent
        val score = computeStargazingScore(safeCloudCover, moonPct)

        val safeLat = if (latitude.isNaN()) -33.4489 else latitude.coerceIn(-89.9, 89.9)
        val safeLon = if (longitude.isNaN()) -70.6693 else longitude.coerceIn(-180.0, 180.0)
        val observer = Observer(safeLat, safeLon, 0.0)
        val nowTime = Time.fromMillisecondsSince1970(instant.toEpochMilli())
        val currentSunAlt = bodyAltitudeDegrees(Body.Sun, nowTime, observer)
        val obsTime = if (currentSunAlt < -6.0) {
            nowTime
        } else {
            searchAltitude(Body.Sun, observer, Direction.Set, nowTime, 1.0, -6.0)
                ?: Time.fromMillisecondsSince1970(
                    instant.atZone(zoneId).toLocalDate()
                        .atTime(21, 30)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli()
                )
        }

        val planetAltitudes = trackedPlanets
            .map { meta -> meta to bodyAltitudeDegrees(meta.body, obsTime, observer) }
            .sortedByDescending { it.second }

        val visible = planetAltitudes.filter { it.second > 5.0 }
            .ifEmpty { listOf(planetAltitudes.first()) }

        return StargazingSnapshot(
            scorePct = score,
            cloudCoverPct = safeCloudCover,
            moonIlluminationPct = moonPct,
            visiblePlanetNamesEs = visible.map { it.first.nameEs },
            visiblePlanetNamesEn = visible.map { it.first.nameEn }
        )
    }

    private fun bodyAltitudeDegrees(body: Body, time: Time, observer: Observer): Double {
        val eq = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        return horizon(time, observer, eq.ra, eq.dec, Refraction.Normal).altitude
    }

    private fun findNextGoldenHourTransition(
        observer: Observer,
        nowTime: Time,
        sunAltitudeDeg: Double,
        lightPhase: SolarLightPhase
    ): Time? = when {
        lightPhase == SolarLightPhase.DAYLIGHT ->
            searchAltitude(Body.Sun, observer, Direction.Set, nowTime, 1.05, 6.0)
        lightPhase == SolarLightPhase.GOLDEN_HOUR -> {
            val morningEnd = searchAltitude(Body.Sun, observer, Direction.Rise, nowTime, 1.05, 6.0)
            val eveningEnd = searchAltitude(Body.Sun, observer, Direction.Set, nowTime, 1.05, -4.0)
            listOfNotNull(morningEnd, eveningEnd).minByOrNull { it.toMillisecondsSince1970() }
        }
        sunAltitudeDeg < -4.0 ->
            searchAltitude(Body.Sun, observer, Direction.Rise, nowTime, 1.05, -4.0)
        else -> null
    }

    private fun formatTime(time: Time, formatter: DateTimeFormatter): String =
        formatter.format(Instant.ofEpochMilli(time.toMillisecondsSince1970()))

    private fun formatDurationMinutes(totalMinutes: Int): String {
        val safeMinutes = totalMinutes.coerceAtLeast(0)
        val hours = safeMinutes / 60
        val minutes = safeMinutes % 60
        return String.format(Locale.US, "%dh %02dm", hours, minutes)
    }

    private fun angularDeltaDegrees(fromDeg: Double, toDeg: Double): Double {
        var diff = (toDeg - fromDeg) % 360.0
        if (diff > 180.0) diff -= 360.0
        if (diff < -180.0) diff += 360.0
        return diff
    }

    private fun classifyMoonPhaseName(angleDeg: Double): Pair<String, String> {
        val a = ((angleDeg % 360.0) + 360.0) % 360.0
        return when {
            a < 22.5 || a >= 337.5 -> "Luna Nueva" to "New Moon"
            a < 67.5 -> "Luna Creciente" to "Waxing Crescent"
            a < 112.5 -> "Cuarto Creciente" to "First Quarter"
            a < 157.5 -> "Gibosa Creciente" to "Waxing Gibbous"
            a < 202.5 -> "Luna Llena" to "Full Moon"
            a < 247.5 -> "Gibosa Menguante" to "Waning Gibbous"
            a < 292.5 -> "Cuarto Menguante" to "Last Quarter"
            else -> "Luna Menguante" to "Waning Crescent"
        }
    }
}
