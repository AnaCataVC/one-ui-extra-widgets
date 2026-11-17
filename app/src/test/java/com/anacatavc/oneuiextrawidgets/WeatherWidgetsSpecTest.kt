package com.anacatavc.oneuiextrawidgets

import com.anacatavc.oneuiextrawidgets.widgets.weather.AqiQualityLevel
import com.anacatavc.oneuiextrawidgets.widgets.weather.RainIntensityLevel
import com.anacatavc.oneuiextrawidgets.widgets.weather.UvRiskLevel
import com.anacatavc.oneuiextrawidgets.widgets.weather.WeatherCardLayoutMode
import com.anacatavc.oneuiextrawidgets.widgets.weather.calculateWeatherCardLayout
import com.anacatavc.oneuiextrawidgets.widgets.weather.classifyAqiLevel
import com.anacatavc.oneuiextrawidgets.widgets.weather.classifyRainLevel
import com.anacatavc.oneuiextrawidgets.widgets.weather.classifyUvLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherWidgetsSpecTest {

    @Test
    fun `default 1x1 compact cell selects COMPACT_1X1 layout mode and fits gauge`() {
        val spec = calculateWeatherCardLayout(widthDp = 82f, heightDp = 82f)

        assertEquals(WeatherCardLayoutMode.COMPACT_1X1, spec.mode)
        assertEquals(8f, spec.cardPaddingDp, 0.01f)
        assertTrue(spec.gaugeSizeDp in 34f..52f)
        assertFalse(spec.showExtendedDetails)
    }

    @Test
    fun `default 2x1 compact cell selects PILL layout mode and fits gauge`() {
        val spec = calculateWeatherCardLayout(widthDp = 155f, heightDp = 82f)

        assertEquals(WeatherCardLayoutMode.PILL, spec.mode)
        assertEquals(12f, spec.cardPaddingDp, 0.01f)
        assertTrue(spec.gaugeSizeDp in 42f..56f)
        assertFalse(spec.showExtendedDetails)
    }

    @Test
    fun `wide 4x1 cell stays in PILL mode and enables extended details`() {
        val spec = calculateWeatherCardLayout(widthDp = 300f, heightDp = 82f)

        assertEquals(WeatherCardLayoutMode.PILL, spec.mode)
        assertTrue(spec.showExtendedDetails)
    }

    @Test
    fun `degenerate zero or negative dimensions coerce safely without invalid sizes`() {
        val zeroSpec = calculateWeatherCardLayout(widthDp = 0f, heightDp = 0f)
        val negSpec = calculateWeatherCardLayout(widthDp = -20f, heightDp = -10f)

        assertEquals(WeatherCardLayoutMode.COMPACT_1X1, zeroSpec.mode)
        assertEquals(34f, zeroSpec.gaugeSizeDp, 0.01f)
        assertEquals(WeatherCardLayoutMode.COMPACT_1X1, negSpec.mode)
        assertEquals(34f, negSpec.gaugeSizeDp, 0.01f)
    }

    @Test
    fun `2x2 cell selects SQUARE layout mode and scales gauge up`() {
        val pillSpec = calculateWeatherCardLayout(widthDp = 155f, heightDp = 129.9f)
        val squareSpec = calculateWeatherCardLayout(widthDp = 239.9f, heightDp = 130f)

        assertEquals(WeatherCardLayoutMode.PILL, pillSpec.mode)
        assertEquals(WeatherCardLayoutMode.SQUARE, squareSpec.mode)
        assertEquals(14f, squareSpec.cardPaddingDp, 0.01f)
        assertTrue(squareSpec.gaugeSizeDp > pillSpec.gaugeSizeDp)
        assertTrue(squareSpec.gaugeSizeDp in 58f..82f)
        assertTrue(squareSpec.showExtendedDetails)
    }

    @Test
    fun `4x2 wide cell selects EXPANDED layout mode with full detail pills`() {
        val expandedSpec = calculateWeatherCardLayout(widthDp = 240f, heightDp = 130f)

        assertEquals(WeatherCardLayoutMode.EXPANDED, expandedSpec.mode)
        assertEquals(14f, expandedSpec.cardPaddingDp, 0.01f)
        assertTrue(expandedSpec.gaugeSizeDp in 64f..88f)
        assertTrue(expandedSpec.showExtendedDetails)
    }

    @Test
    fun `rain probability classifier maps percentages and exact boundaries to intensity tiers`() {
        assertEquals(RainIntensityLevel.DRY, classifyRainLevel(0))
        assertEquals(RainIntensityLevel.DRY, classifyRainLevel(10))
        assertEquals(RainIntensityLevel.LOW, classifyRainLevel(11))
        assertEquals(RainIntensityLevel.LOW, classifyRainLevel(35))
        assertEquals(RainIntensityLevel.MODERATE, classifyRainLevel(36))
        assertEquals(RainIntensityLevel.MODERATE, classifyRainLevel(65))
        assertEquals(RainIntensityLevel.HIGH, classifyRainLevel(66))
        assertEquals(RainIntensityLevel.HIGH, classifyRainLevel(100))
    }

    @Test
    fun `european AQI classifier maps index and exact step boundaries to official quality tiers`() {
        assertEquals(AqiQualityLevel.GOOD, classifyAqiLevel(20))
        assertEquals(AqiQualityLevel.FAIR, classifyAqiLevel(21))
        assertEquals(AqiQualityLevel.FAIR, classifyAqiLevel(40))
        assertEquals(AqiQualityLevel.MODERATE, classifyAqiLevel(41))
        assertEquals(AqiQualityLevel.MODERATE, classifyAqiLevel(60))
        assertEquals(AqiQualityLevel.POOR, classifyAqiLevel(61))
        assertEquals(AqiQualityLevel.POOR, classifyAqiLevel(80))
        assertEquals(AqiQualityLevel.VERY_POOR, classifyAqiLevel(81))
    }

    @Test
    fun `UV index classifier maps radiation values and rounding boundaries to WHO risk categories`() {
        assertEquals(UvRiskLevel.LOW, classifyUvLevel(Double.NaN))
        assertEquals(UvRiskLevel.LOW, classifyUvLevel(2.4))
        assertEquals(UvRiskLevel.MODERATE, classifyUvLevel(2.5))
        assertEquals(UvRiskLevel.MODERATE, classifyUvLevel(5.4))
        assertEquals(UvRiskLevel.HIGH, classifyUvLevel(5.5))
        assertEquals(UvRiskLevel.HIGH, classifyUvLevel(7.4))
        assertEquals(UvRiskLevel.VERY_HIGH, classifyUvLevel(7.5))
        assertEquals(UvRiskLevel.VERY_HIGH, classifyUvLevel(10.4))
        assertEquals(UvRiskLevel.EXTREME, classifyUvLevel(10.5))
    }
}
