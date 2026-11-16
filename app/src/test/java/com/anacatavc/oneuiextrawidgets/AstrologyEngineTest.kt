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
}
