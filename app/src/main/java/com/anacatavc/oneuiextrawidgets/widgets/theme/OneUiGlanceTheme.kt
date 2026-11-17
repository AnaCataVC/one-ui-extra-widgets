package com.anacatavc.oneuiextrawidgets.widgets.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.color.ColorProvider

object OneUiGlanceTokens {
    val CornerRadius = 26.dp
    // Concentric law: R_inner <= R_outer (26.dp) - CardPadding (14.dp) = 12.dp
    val InnerRadius = 12.dp
    val CardPadding = 14.dp

    fun isNightModeActive(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    val SurfaceBackground = ColorProvider(
        day = Color(0xF2F4F5F8),
        night = Color(0xEB1C1C1E)
    )

    val InnerCardSurface = ColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFF2C2C2E)
    )

    val PrimaryText = ColorProvider(
        day = Color(0xFF121316),
        night = Color(0xFFF5F6F9)
    )

    val SecondaryText = ColorProvider(
        day = Color(0xFF5D6068),
        night = Color(0xFFA0A4B0)
    )

    val OneUiAccentBlue = ColorProvider(
        day = Color(0xFF0381FE),
        night = Color(0xFF3E9BFF)
    )

    val RetroLcdBacklight = ColorProvider(
        day = Color(0xFF35E8F7),
        night = Color(0xFF1FD6E8)
    )

    val RetroAlertAmber = ColorProvider(
        day = Color(0xFFD97706),
        night = Color(0xFFFBBF24)
    )

    val IpodShellSurface = ColorProvider(
        day = Color(0xFFEAECEF),
        night = Color(0xFF1F2024)
    )

    val IpodLcdSurface = ColorProvider(
        day = Color(0xFFD6E4E8),
        night = Color(0xFF121820)
    )

    val RetroMp3ShellSurface = ColorProvider(
        day = Color(0xFF27359B),
        night = Color(0xFF1A236B)
    )

    val RetroMp3CapSeam = ColorProvider(
        day = Color(0xFF1A2473),
        night = Color(0xFF11174A)
    )

    val RetroMp3BezelSurface = ColorProvider(
        day = Color(0xFF0F1117),
        night = Color(0xFF090A0E)
    )

    val RetroMp3GoldLabel = ColorProvider(
        day = Color(0xFFEBC94B),
        night = Color(0xFFF3D564)
    )

    val RetroMp3SilverSurface = ColorProvider(
        day = Color(0xFFE4E7F0),
        night = Color(0xFFD0D5E2)
    )

    val RetroMp3SilverText = ColorProvider(
        day = Color(0xFF1B1F2A),
        night = Color(0xFF141821)
    )

    val RetroLcdPrimaryText = ColorProvider(
        day = Color(0xFF04262E),
        night = Color(0xFF031C22)
    )

    val RetroLcdSecondaryText = ColorProvider(
        day = Color(0xFF0A4352),
        night = Color(0xFF073440)
    )

    // Semantic Weather & Environmental Tokens (Day / Night adaptive)
    val WeatherRainCyan = ColorProvider(
        day = Color(0xFF0284C7),
        night = Color(0xFF38BDF8)
    )

    val WeatherGoodGreen = ColorProvider(
        day = Color(0xFF059669),
        night = Color(0xFF34D399)
    )

    val WeatherModerateAmber = ColorProvider(
        day = Color(0xFFD97706),
        night = Color(0xFFFBBF24)
    )

    val WeatherHighOrange = ColorProvider(
        day = Color(0xFFEA580C),
        night = Color(0xFFFB923C)
    )

    val WeatherDangerRed = ColorProvider(
        day = Color(0xFFDC2626),
        night = Color(0xFFF87171)
    )

    val WeatherExtremePurple = ColorProvider(
        day = Color(0xFF7C3AED),
        night = Color(0xFFA78BFA)
    )

    val WeatherMoonSilver = ColorProvider(
        day = Color(0xFF4F46E5),
        night = Color(0xFFC7D2FE)
    )
}

/**
 * Centralized ARGB integer color tokens for deterministic Android Canvas rendering
 * in [com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer].
 */
object OneUiCanvasPalette {
    const val GAUGE_TRACK_DARK: Int = 0xFF2C2E36.toInt()
    const val GAUGE_TRACK_LIGHT: Int = 0xFFE2E5EE.toInt()

    const val RAIN_ARC_START: Int = 0xFF0381FE.toInt()
    const val RAIN_ARC_END: Int = 0xFF35E8F7.toInt()
    const val RAIN_DROP_TOP: Int = 0xFF38BDF8.toInt()
    const val RAIN_DROP_BOTTOM: Int = 0xFF0284C7.toInt()
    const val RAIN_DROP_HIGHLIGHT: Int = 0x99FFFFFF.toInt()

    const val AQI_GOOD: Int = 0xFF10B981.toInt()
    const val AQI_FAIR: Int = 0xFF84CC16.toInt()
    const val AQI_MODERATE: Int = 0xFFF59E0B.toInt()
    const val AQI_POOR: Int = 0xFFEF4444.toInt()
    const val AQI_VERY_POOR: Int = 0xFF8B5CF6.toInt()

    const val UV_LOW: Int = 0xFF22C55E.toInt()
    const val UV_MODERATE: Int = 0xFFEAB308.toInt()
    const val UV_HIGH: Int = 0xFFF97316.toInt()
    const val UV_VERY_HIGH: Int = 0xFFEF4444.toInt()
    const val UV_EXTREME: Int = 0xFFA855F7.toInt()
    const val UV_SUN_CORE_TOP: Int = 0xFFFDE047.toInt()
    const val UV_SUN_CORE_BOTTOM: Int = 0xFFF59E0B.toInt()

    const val MOON_DARK_DISK: Int = 0xFF232631.toInt()
    const val MOON_LIT_DISK: Int = 0xFFF5F3CE.toInt()
    const val MOON_RIM_STROKE: Int = 0x5594A3B8
    const val MOON_CRATER_SHADOW: Int = 0x24000000

    const val TEXT_PRIMARY_DARK: Int = 0xFFF5F6F9.toInt()
    const val TEXT_PRIMARY_LIGHT: Int = 0xFF121316.toInt()
    const val INDICATOR_RING_DARK: Int = 0xFF1C1C1E.toInt()
    const val INDICATOR_RING_LIGHT: Int = 0xFFFFFFFF.toInt()

    const val IPOD_WHEEL_DARK: Int = 0xFF2B2C30.toInt()
    const val IPOD_WHEEL_LIGHT: Int = 0xFFE5E6EB.toInt()
    const val IPOD_STROKE_DARK: Int = 0xFF44464E.toInt()
    const val IPOD_STROKE_LIGHT: Int = 0xFFC6C8D0.toInt()
    const val IPOD_CENTER_TOP_DARK: Int = 0xFF3E4047.toInt()
    const val IPOD_CENTER_TOP_LIGHT: Int = 0xFFFFFFFF.toInt()
    const val IPOD_CENTER_BOTTOM_DARK: Int = 0xFF1D1E22.toInt()
    const val IPOD_CENTER_BOTTOM_LIGHT: Int = 0xFFD4D6DE.toInt()
    const val IPOD_LABEL_DARK: Int = 0xFFD8DAE2.toInt()
    const val IPOD_LABEL_LIGHT: Int = 0xFF5A5D66.toInt()

    const val DISCMAN_BEVEL_TOP: Int = 0xFF6A6F7D.toInt()
    const val DISCMAN_BEVEL_MID: Int = 0xFF2A2D35.toInt()
    const val DISCMAN_BEVEL_BOTTOM: Int = 0xFF545966.toInt()
    const val DISCMAN_BODY_TOP_DARK: Int = 0xFF323640.toInt()
    const val DISCMAN_BODY_TOP_LIGHT: Int = 0xFF3C404B.toInt()
    const val DISCMAN_BODY_BOTTOM_DARK: Int = 0xFF1B1D23.toInt()
    const val DISCMAN_BODY_BOTTOM_LIGHT: Int = 0xFF23262E.toInt()
    const val DISCMAN_RIM_HIGHLIGHT: Int = 0x44FFFFFF
    const val DISCMAN_TITLE_TEXT: Int = 0xFFE6E8EE.toInt()
    const val DISCMAN_SUBTITLE_TEXT: Int = 0xFFB5BAC7.toInt()
    const val DISCMAN_PILL_BG: Int = 0xFF13151A.toInt()
    const val DISCMAN_PILL_STROKE: Int = 0xFF4B505C.toInt()
    const val DISCMAN_SPEAKER_DOT: Int = 0xFF101216.toInt()

    const val ASTRO_DIRECT_LIGHT: Int = 0xFF0381FE.toInt()
    const val ASTRO_DIRECT_DARK: Int = 0xFF3E9BFF.toInt()
    const val ASTRO_RETRO_LIGHT: Int = 0xFFD97706.toInt()
    const val ASTRO_RETRO_DARK: Int = 0xFFFBBF24.toInt()
    const val ASTRO_SHADOW_LIGHT: Int = 0xFF7C3AED.toInt()
    const val ASTRO_SHADOW_DARK: Int = 0xFFA78BFA.toInt()
    const val ASTRO_GLOW_DIRECT: Int = 0x220381FE
    const val ASTRO_GLOW_RETRO: Int = 0x26D97706
    const val ASTRO_GLOW_SHADOW: Int = 0x227C3AED

    const val ZODIAC_FIRE_LIGHT: Int = 0xFFEA580C.toInt()
    const val ZODIAC_FIRE_DARK: Int = 0xFFFB923C.toInt()
    const val ZODIAC_FIRE_GLOW: Int = 0x24EA580C
    const val ZODIAC_EARTH_LIGHT: Int = 0xFF059669.toInt()
    const val ZODIAC_EARTH_DARK: Int = 0xFF34D399.toInt()
    const val ZODIAC_EARTH_GLOW: Int = 0x24059669
    const val ZODIAC_AIR_LIGHT: Int = 0xFF0284C7.toInt()
    const val ZODIAC_AIR_DARK: Int = 0xFF38BDF8.toInt()
    const val ZODIAC_AIR_GLOW: Int = 0x240284C7
    const val ZODIAC_WATER_LIGHT: Int = 0xFF4F46E5.toInt()
    const val ZODIAC_WATER_DARK: Int = 0xFFA5B4FC.toInt()
    const val ZODIAC_WATER_GLOW: Int = 0x244F46E5
}



