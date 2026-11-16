package com.anacatavc.oneuiextrawidgets

import com.anacatavc.oneuiextrawidgets.data.astrology.AstrologyEngine
import com.anacatavc.oneuiextrawidgets.data.astrology.ZodiacSign
import io.github.cosinekitty.astronomy.Body
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AstrologyEngineTest {

    @Test
    fun `zodiac sign mapping from ecliptic longitude is accurate`() {
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromEclipticLongitude(0.0))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromEclipticLongitude(29.9))
        assertEquals(ZodiacSign.TAURUS, ZodiacSign.fromEclipticLongitude(30.0))
        assertEquals(ZodiacSign.SCORPIO, ZodiacSign.fromEclipticLongitude(234.0))
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromEclipticLongitude(359.5))
    }

    @Test
    fun `computeSnapshot returns 5 tracked personal and social planets and valid lunar illumination`() {
        val sampleInstant = Instant.parse("2026-11-16T12:00:00Z")
        val snapshot = AstrologyEngine.computeSnapshot(sampleInstant)

        // Sun in mid-November is in Scorpio
        assertEquals(ZodiacSign.SCORPIO, snapshot.sunSign)
        assertEquals(5, snapshot.planets.size)
        assertTrue(snapshot.planets.any { it.body == Body.Mercury })
        assertTrue(snapshot.lunarStatus.illuminationPercent in 0..100)
    }
}
