package com.anacatavc.oneuiextrawidgets

import androidx.compose.ui.graphics.Color
import com.anacatavc.oneuiextrawidgets.data.preferences.AppPreferences
import com.anacatavc.oneuiextrawidgets.data.preferences.WidgetFontScaleMode
import com.anacatavc.oneuiextrawidgets.widgets.WidgetInstanceHelper
import com.anacatavc.oneuiextrawidgets.widgets.astrology.OneUiAstrologyWidget
import com.anacatavc.oneuiextrawidgets.widgets.astrology.OneUiAstrologyWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.battery.OneUiBatteryThermalWidget
import com.anacatavc.oneuiextrawidgets.widgets.battery.OneUiBatteryThermalWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.CassetteWalkmanWidget
import com.anacatavc.oneuiextrawidgets.widgets.music.CassetteWalkmanWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.DiscmanWidget
import com.anacatavc.oneuiextrawidgets.widgets.music.DiscmanWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.IpodClassicWidget
import com.anacatavc.oneuiextrawidgets.widgets.music.IpodClassicWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.MiniDiscWidget
import com.anacatavc.oneuiextrawidgets.widgets.music.MiniDiscWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.RetroMp3Widget
import com.anacatavc.oneuiextrawidgets.widgets.music.RetroMp3WidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.VinylTurntableWidget
import com.anacatavc.oneuiextrawidgets.widgets.music.VinylTurntableWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.network.OneUiNetworkWidget
import com.anacatavc.oneuiextrawidgets.widgets.network.OneUiNetworkWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiAqiWidget
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiAqiWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiMoonPhaseWidget
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiMoonPhaseWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiRainWidget
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiRainWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiStargazingWidget
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiStargazingWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiSunArcWidget
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiSunArcWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiUvWidget
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiUvWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetOpacityTest {

    @Test
    fun `clampWidgetOpacity clamps out-of-range values to 0-100`() {
        assertEquals(0, AppPreferences.clampWidgetOpacity(-25))
        assertEquals(0, AppPreferences.clampWidgetOpacity(0))
        assertEquals(65, AppPreferences.clampWidgetOpacity(65))
        assertEquals(100, AppPreferences.clampWidgetOpacity(100))
        assertEquals(100, AppPreferences.clampWidgetOpacity(150))
    }

    @Test
    fun `scaleColorAlpha preserves base alpha at 100 percent and scales linearly`() {
        val base = Color(0xF2F4F5F8) // alpha ~= 0.949
        val full = OneUiGlanceTokens.scaleColorAlpha(base, 100)
        assertEquals(base.alpha, full.alpha, 0.001f)

        val half = OneUiGlanceTokens.scaleColorAlpha(base, 50)
        assertEquals(base.alpha * 0.5f, half.alpha, 0.001f)

        val zero = OneUiGlanceTokens.scaleColorAlpha(base, 0)
        assertEquals(0f, zero.alpha, 0.001f)
    }

    @Test
    fun `bitmapAlphaFromOpacity maps percentages to 0-255`() {
        assertEquals(0, OneUiGlanceTokens.bitmapAlphaFromOpacity(-10))
        assertEquals(0, OneUiGlanceTokens.bitmapAlphaFromOpacity(0))
        assertEquals(128, OneUiGlanceTokens.bitmapAlphaFromOpacity(50))
        assertEquals(255, OneUiGlanceTokens.bitmapAlphaFromOpacity(100))
        assertEquals(255, OneUiGlanceTokens.bitmapAlphaFromOpacity(200))
    }

    @Test
    fun `resolveWidgetForProvider maps all 15 receivers and rejects unknown`() {
        val expectedMappings = listOf(
            OneUiRainWidgetReceiver::class.java.name to OneUiRainWidget::class.java,
            OneUiAqiWidgetReceiver::class.java.name to OneUiAqiWidget::class.java,
            OneUiUvWidgetReceiver::class.java.name to OneUiUvWidget::class.java,
            OneUiMoonPhaseWidgetReceiver::class.java.name to OneUiMoonPhaseWidget::class.java,
            OneUiSunArcWidgetReceiver::class.java.name to OneUiSunArcWidget::class.java,
            OneUiStargazingWidgetReceiver::class.java.name to OneUiStargazingWidget::class.java,
            OneUiAstrologyWidgetReceiver::class.java.name to OneUiAstrologyWidget::class.java,
            IpodClassicWidgetReceiver::class.java.name to IpodClassicWidget::class.java,
            RetroMp3WidgetReceiver::class.java.name to RetroMp3Widget::class.java,
            DiscmanWidgetReceiver::class.java.name to DiscmanWidget::class.java,
            CassetteWalkmanWidgetReceiver::class.java.name to CassetteWalkmanWidget::class.java,
            VinylTurntableWidgetReceiver::class.java.name to VinylTurntableWidget::class.java,
            MiniDiscWidgetReceiver::class.java.name to MiniDiscWidget::class.java,
            OneUiNetworkWidgetReceiver::class.java.name to OneUiNetworkWidget::class.java,
            OneUiBatteryThermalWidgetReceiver::class.java.name to OneUiBatteryThermalWidget::class.java
        )
        assertEquals(15, expectedMappings.size)
        expectedMappings.forEach { (className, widgetClass) ->
            val resolved = WidgetInstanceHelper.resolveWidgetForProvider(className)
            assertNotNull("Expected widget for $className", resolved)
            assertTrue("Expected ${widgetClass.simpleName} for $className", widgetClass.isInstance(resolved))
        }
        assertNull(WidgetInstanceHelper.resolveWidgetForProvider(null))
        assertNull(WidgetInstanceHelper.resolveWidgetForProvider("com.example.UnknownReceiver"))
    }

    @Test
    fun `effectiveTextScale bounds extreme system fontScale and applies mode multiplier`() {
        assertEquals(1.0f, OneUiGlanceTokens.effectiveTextScale(1.0f, WidgetFontScaleMode.SYSTEM), 0.001f)
        assertEquals(1.20f, OneUiGlanceTokens.effectiveTextScale(1.6f, WidgetFontScaleMode.SYSTEM), 0.001f)
        assertEquals(
            1.10f,
            OneUiGlanceTokens.effectiveTextScale(1.6f, WidgetFontScaleMode.SYSTEM, isCompactLayout = true),
            0.001f
        )
        assertEquals(0.88f, OneUiGlanceTokens.effectiveTextScale(1.5f, WidgetFontScaleMode.COMPACT), 0.001f)
        assertEquals(
            1.35f * 1.12f,
            OneUiGlanceTokens.effectiveTextScale(1.8f, WidgetFontScaleMode.LARGE),
            0.001f
        )
    }

    @Test
    fun `scaledSp compensates RemoteViews systemFontScale multiplication`() {
        val spAt150 = OneUiGlanceTokens.scaledSp(
            baseSp = 12f,
            systemFontScale = 1.5f,
            mode = WidgetFontScaleMode.SYSTEM
        )
        // Rendered size = spAt150.value * 1.5f == 12f * 1.20f
        assertEquals(12f * 1.20f, spAt150.value * 1.5f, 0.01f)
        assertEquals(WidgetFontScaleMode.SYSTEM, WidgetFontScaleMode.fromStorageKey(null))
        assertEquals(WidgetFontScaleMode.COMPACT, WidgetFontScaleMode.fromStorageKey("compact"))
    }
}
