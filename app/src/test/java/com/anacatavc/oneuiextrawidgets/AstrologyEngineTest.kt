package com.anacatavc.oneuiextrawidgets

import com.anacatavc.oneuiextrawidgets.data.astrology.AstrologyEngine
import com.anacatavc.oneuiextrawidgets.data.astrology.PlanetaryMotionState
import com.anacatavc.oneuiextrawidgets.data.astrology.ZodiacSign
import io.github.cosinekitty.astronomy.Body
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AstrologyEngineTest {

    @Test
    fun `zodiac sign mapping from ecliptic longitude is accurate including wrap-around`() {
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromEclipticLongitude(0.0))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromEclipticLongitude(29.9))
        assertEquals(ZodiacSign.TAURUS, ZodiacSign.fromEclipticLongitude(30.0))
        assertEquals(ZodiacSign.SCORPIO, ZodiacSign.fromEclipticLongitude(234.0))
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromEclipticLongitude(359.5))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromEclipticLongitude(360.0))
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromEclipticLongitude(-1.0))
    }

    @Test
    fun `zodiac sign and motion state provide bilingual labels`() {
        assertEquals("Escorpio", ZodiacSign.SCORPIO.localizedName(isSpanish = true))
        assertEquals("Scorpio", ZodiacSign.SCORPIO.localizedName(isSpanish = false))
        assertEquals("Retrógrado", PlanetaryMotionState.RETROGRADE.localizedBadge(isSpanish = true))
        assertEquals("Retrograde", PlanetaryMotionState.RETROGRADE.localizedBadge(isSpanish = false))
    }

    @Test
    fun `computeSnapshot returns 5 tracked personal and social planets and valid lunar illumination`() {
        val sampleInstant = Instant.parse("2026-11-16T12:00:00Z")
        val snapshot = AstrologyEngine.computeSnapshot(sampleInstant)

        // Sun in mid-November is in Scorpio
        assertEquals(ZodiacSign.SCORPIO, snapshot.sunSign)
        assertTrue(snapshot.sunDegreeInSign in 0.0..30.0)
        assertEquals(5, snapshot.planets.size)
        assertTrue(snapshot.planets.any { it.body == Body.Mercury })
        assertTrue(snapshot.lunarStatus.illuminationPercent in 0..100)
        assertTrue(snapshot.lunarStatus.phaseAngleDeg in 0.0..360.0)
        assertTrue(snapshot.lunarStatus.localizedPhaseName(isSpanish = true).isNotBlank())
        assertTrue(snapshot.lunarStatus.localizedPhaseName(isSpanish = false).isNotBlank())
    }

    @Test
    fun `mercury retrograde is detected during known April 2024 retrograde window`() {
        // Mercury was retrograde from 2024-04-01 to 2024-04-25
        val retroInstant = Instant.parse("2024-04-10T12:00:00Z")
        val snapshot = AstrologyEngine.computeSnapshot(retroInstant)
        val mercury = snapshot.mercuryStatus

        assertEquals(PlanetaryMotionState.RETROGRADE, mercury.motionState)
        assertTrue(mercury.isRetrograde)
        assertTrue(mercury.dailySpeedDeg < 0.0)
    }

    @Test
    fun `mercury direct is detected outside retrograde and shadow windows`() {
        val directInstant = Instant.parse("2024-06-15T12:00:00Z")
        val snapshot = AstrologyEngine.computeSnapshot(directInstant)
        val mercury = snapshot.mercuryStatus

        assertEquals(PlanetaryMotionState.DIRECT, mercury.motionState)
        assertFalse(mercury.isRetrograde)
        assertTrue(mercury.dailySpeedDeg > 0.0)
    }

    @Test
    fun `lunarPhaseStatus computes scientific synodic age and waxing or waning cycle trend`() {
        val sampleInstant = Instant.parse("2026-11-16T12:00:00Z")
        val lunar = AstrologyEngine.computeSnapshot(sampleInstant).lunarStatus

        assertTrue(lunar.synodicAgeDays in 0.0..29.54)
        assertTrue(lunar.normalizedPhaseAngleDeg in 0.0..360.0)
        val expectedTrendEs = if (lunar.isWaxing) "Creciente" else "Menguante"
        val expectedTrendEn = if (lunar.isWaxing) "Waxing" else "Waning"
        assertEquals(expectedTrendEs, lunar.localizedCycleTrend(isSpanish = true))
        assertEquals(expectedTrendEn, lunar.localizedCycleTrend(isSpanish = false))
    }

    @Test
    fun `lunarPhaseStatus handles exact phase angle and illumination boundaries`() {
        val firstQuarter = com.anacatavc.oneuiextrawidgets.data.astrology.LunarPhaseStatus(
            phaseAngleDeg = 90.0,
            illuminationFraction = 0.5,
            phaseNameEs = "Cuarto Creciente",
            phaseNameEn = "First Quarter",
            moonSign = ZodiacSign.ARIES
        )
        assertEquals(90.0, firstQuarter.normalizedPhaseAngleDeg, 1e-6)
        assertEquals(29.530588 * 0.25, firstQuarter.synodicAgeDays, 1e-4)
        assertTrue(firstQuarter.isWaxing)
        assertEquals("Creciente", firstQuarter.localizedCycleTrend(isSpanish = true))
        assertEquals("Waxing", firstQuarter.localizedCycleTrend(isSpanish = false))
        assertEquals(50, firstQuarter.illuminationPercent)

        val fullMoon = firstQuarter.copy(phaseAngleDeg = 180.0, illuminationFraction = 1.2)
        assertEquals(180.0, fullMoon.normalizedPhaseAngleDeg, 1e-6)
        assertEquals(29.530588 * 0.5, fullMoon.synodicAgeDays, 1e-4)
        assertFalse(fullMoon.isWaxing)
        assertEquals("Menguante", fullMoon.localizedCycleTrend(isSpanish = true))
        assertEquals("Waning", fullMoon.localizedCycleTrend(isSpanish = false))
        assertEquals(100, fullMoon.illuminationPercent)

        val negativeAngle = firstQuarter.copy(phaseAngleDeg = -90.0, illuminationFraction = -0.1)
        assertEquals(270.0, negativeAngle.normalizedPhaseAngleDeg, 1e-6)
        assertFalse(negativeAngle.isWaxing)
        assertEquals(0, negativeAngle.illuminationPercent)
    }
}
