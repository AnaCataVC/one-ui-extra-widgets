package com.anacatavc.oneuiextrawidgets.data.astrology

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.sunPosition
import java.time.Instant

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
