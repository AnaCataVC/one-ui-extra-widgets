package com.anacatavc.oneuiextrawidgets

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression guard for [com.anacatavc.oneuiextrawidgets.ui.screens.WeatherDetailScreen]
 * ensuring multi-metric cards next to a 64.dp gauge wrap cleanly instead of squishing
 * labels/values into vertical single-character columns on narrow phone screens.
 */
class WeatherDetailScreenLayoutTest {

    @Test
    fun `AQI card uses FlowRow with spacedBy instead of Row SpaceBetween for 3 metrics`() {
        val screenFile = resolveWeatherDetailScreenFile()
        val source = screenFile.readText()

        val aqiSectionStart = source.indexOf("// 2. Air Quality (AQI) Card")
        val aqiSectionEnd = source.indexOf("// 3. Solar UV Index Card")
        assertTrue("Expected AQI card section in WeatherDetailScreen.kt", aqiSectionStart >= 0)
        assertTrue("Expected UV card section after AQI card", aqiSectionEnd > aqiSectionStart)

        val aqiSection = source.substring(aqiSectionStart, aqiSectionEnd)
        assertTrue(
            "AQI card must use FlowRow so 3 MetricBlocks wrap cleanly on narrow screens",
            aqiSection.contains("FlowRow(")
        )
        assertTrue(
            "AQI FlowRow must use spacedBy horizontal arrangement",
            aqiSection.contains("horizontalArrangement = Arrangement.spacedBy(")
        )
        assertFalse(
            "AQI card must not use Arrangement.SpaceBetween across 3 MetricBlocks next to the gauge",
            aqiSection.contains("Arrangement.SpaceBetween")
        )
    }

    private fun resolveWeatherDetailScreenFile(): File {
        val relative = "java/com/anacatavc/oneuiextrawidgets/ui/screens/WeatherDetailScreen.kt"
        val candidates = listOf(
            File("src/main/$relative"),
            File("app/src/main/$relative")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Could not locate WeatherDetailScreen.kt from ${File(".").absolutePath}")
    }
}
