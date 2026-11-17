package com.anacatavc.oneuiextrawidgets

import com.anacatavc.oneuiextrawidgets.widgets.astrology.AstrologyLayoutMode
import com.anacatavc.oneuiextrawidgets.widgets.astrology.calculateAstrologyLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AstrologyLayoutSpecTest {

    @Test
    fun `narrow or short bounds select COMPACT mode without planet strip`() {
        val narrow = calculateAstrologyLayout(widthDp = 180f, heightDp = 160f)
        assertEquals(AstrologyLayoutMode.COMPACT, narrow.mode)
        assertEquals(12f, narrow.cardPaddingDp, 0.01f)
        assertEquals(34f, narrow.emblemSizeDp, 0.01f)
        assertFalse(narrow.showDegrees)
        assertFalse(narrow.showPlanetStrip)
        assertFalse(narrow.showPlanetMotionBadge)

        val short = calculateAstrologyLayout(widthDp = 280f, heightDp = 110f)
        assertEquals(AstrologyLayoutMode.COMPACT, short.mode)
        assertFalse(short.showPlanetStrip)
    }

    @Test
    fun `default 4x2 cell selects STANDARD mode and enables planet strip at 145dp+`() {
        val standardShort = calculateAstrologyLayout(widthDp = 280f, heightDp = 135f)
        assertEquals(AstrologyLayoutMode.STANDARD, standardShort.mode)
        assertEquals(14f, standardShort.cardPaddingDp, 0.01f)
        assertEquals(42f, standardShort.emblemSizeDp, 0.01f)
        assertTrue(standardShort.showDegrees)
        assertFalse(standardShort.showPlanetStrip)

        val standardTall = calculateAstrologyLayout(widthDp = 300f, heightDp = 165f)
        assertEquals(AstrologyLayoutMode.STANDARD, standardTall.mode)
        assertTrue(standardTall.showDegrees)
        assertTrue(standardTall.showPlanetStrip)
        assertFalse(standardTall.showPlanetMotionBadge)
    }

    @Test
    fun `tall 4x3+ cell selects EXPANDED mode with full motion badges`() {
        val expanded = calculateAstrologyLayout(widthDp = 310f, heightDp = 220f)
        assertEquals(AstrologyLayoutMode.EXPANDED, expanded.mode)
        assertEquals(16f, expanded.cardPaddingDp, 0.01f)
        assertEquals(48f, expanded.emblemSizeDp, 0.01f)
        assertTrue(expanded.showDegrees)
        assertTrue(expanded.showPlanetStrip)
        assertTrue(expanded.showPlanetMotionBadge)
    }

    @Test
    fun `degenerate zero, negative, and NaN dimensions coerce safely to COMPACT`() {
        val zeroSpec = calculateAstrologyLayout(widthDp = 0f, heightDp = 0f)
        val negSpec = calculateAstrologyLayout(widthDp = -40f, heightDp = -20f)
        val nanSpec = calculateAstrologyLayout(widthDp = Float.NaN, heightDp = Float.NaN)
        val nanHeightSpec = calculateAstrologyLayout(widthDp = 300f, heightDp = Float.NaN)

        assertEquals(AstrologyLayoutMode.COMPACT, zeroSpec.mode)
        assertEquals(34f, zeroSpec.emblemSizeDp, 0.01f)
        assertEquals(AstrologyLayoutMode.COMPACT, negSpec.mode)
        assertFalse(negSpec.showPlanetStrip)
        assertEquals(AstrologyLayoutMode.COMPACT, nanSpec.mode)
        assertEquals(AstrologyLayoutMode.COMPACT, nanHeightSpec.mode)
    }

    @Test
    fun `exact boundary thresholds transition cleanly across COMPACT, STANDARD, and EXPANDED`() {
        assertEquals(AstrologyLayoutMode.COMPACT, calculateAstrologyLayout(219.9f, 150f).mode)
        assertEquals(AstrologyLayoutMode.STANDARD, calculateAstrologyLayout(220.0f, 150f).mode)

        assertEquals(AstrologyLayoutMode.COMPACT, calculateAstrologyLayout(250f, 119.9f).mode)
        val std120 = calculateAstrologyLayout(250f, 120.0f)
        assertEquals(AstrologyLayoutMode.STANDARD, std120.mode)
        assertFalse(std120.showPlanetStrip)

        assertFalse(calculateAstrologyLayout(250f, 144.9f).showPlanetStrip)
        assertTrue(calculateAstrologyLayout(250f, 145.0f).showPlanetStrip)

        assertEquals(AstrologyLayoutMode.STANDARD, calculateAstrologyLayout(250f, 194.9f).mode)
        val exp195 = calculateAstrologyLayout(250f, 195.0f)
        assertEquals(AstrologyLayoutMode.EXPANDED, exp195.mode)
        assertTrue(exp195.showPlanetMotionBadge)

        // Narrow + tall stays COMPACT rather than overflowing horizontally in EXPANDED
        assertEquals(AstrologyLayoutMode.COMPACT, calculateAstrologyLayout(180f, 220f).mode)
    }
}

