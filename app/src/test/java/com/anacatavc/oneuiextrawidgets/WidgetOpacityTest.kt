package com.anacatavc.oneuiextrawidgets

import androidx.compose.ui.graphics.Color
import com.anacatavc.oneuiextrawidgets.data.preferences.AppPreferences
import com.anacatavc.oneuiextrawidgets.widgets.WidgetInstanceHelper
import com.anacatavc.oneuiextrawidgets.widgets.astrology.OneUiAstrologyWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.DiscmanWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.IpodClassicWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.RetroMp3WidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.network.OneUiNetworkWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiAqiWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiMoonPhaseWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiRainWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiUvWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun `resolveWidgetForProvider maps all 9 receivers and rejects unknown`() {
        val receivers = listOf(
            OneUiRainWidgetReceiver::class.java.name,
            OneUiAqiWidgetReceiver::class.java.name,
            OneUiUvWidgetReceiver::class.java.name,
            OneUiMoonPhaseWidgetReceiver::class.java.name,
            OneUiAstrologyWidgetReceiver::class.java.name,
            IpodClassicWidgetReceiver::class.java.name,
            RetroMp3WidgetReceiver::class.java.name,
            DiscmanWidgetReceiver::class.java.name,
            OneUiNetworkWidgetReceiver::class.java.name
        )
        receivers.forEach { className ->
            assertNotNull("Expected widget for $className", WidgetInstanceHelper.resolveWidgetForProvider(className))
        }
        assertNull(WidgetInstanceHelper.resolveWidgetForProvider(null))
        assertNull(WidgetInstanceHelper.resolveWidgetForProvider("com.example.UnknownReceiver"))
    }
}
