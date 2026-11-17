package com.anacatavc.oneuiextrawidgets.widgets.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider as dayNightColorProvider
import androidx.glance.unit.ColorProvider
import com.anacatavc.oneuiextrawidgets.data.preferences.WidgetFontScaleMode
import kotlin.math.roundToInt

object OneUiGlanceTokens {
    val CornerRadius = 26.dp
    // Concentric law: R_inner <= R_outer (26.dp) - CardPadding (14.dp) = 12.dp
    val InnerRadius = 12.dp
    val CardPadding = 14.dp

    fun isNightModeActive(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    fun systemFontScale(context: Context): Float =
        context.resources.configuration.fontScale

    /**
     * Computes the bounded effective font scale factor so widgets respect One UI system
     * font scaling without overflowing tight RemoteViews bounds.
     */
    fun effectiveTextScale(
        systemFontScale: Float,
        mode: WidgetFontScaleMode,
        isCompactLayout: Boolean = false
    ): Float {
        val safeSystem = if (systemFontScale > 0.1f) systemFontScale else 1.0f
        val maxSystem = if (isCompactLayout) {
            minOf(mode.maxSystemScale, 1.10f)
        } else {
            mode.maxSystemScale
        }
        val clampedSystem = safeSystem.coerceIn(0.85f, maxSystem)
        return clampedSystem * mode.multiplier
    }

    /**
     * Returns an `sp` [TextUnit] compensated for `RemoteViews` automatic [systemFontScale]
     * multiplication so the rendered size matches [effectiveTextScale].
     */
    fun scaledSp(
        baseSp: Float,
        systemFontScale: Float,
        mode: WidgetFontScaleMode,
        isCompactLayout: Boolean = false
    ): TextUnit {
        val safeSystem = if (systemFontScale > 0.1f) systemFontScale else 1.0f
        val effective = effectiveTextScale(safeSystem, mode, isCompactLayout)
        return (baseSp * (effective / safeSystem)).sp
    }

    private val SurfaceDayBase = Color(0xF2F4F5F8)
    private val SurfaceNightBase = Color(0xEB1C1C1E)
    private val InnerCardDayBase = Color(0xFFFFFFFF)
    private val InnerCardNightBase = Color(0xFF2C2C2E)
    private val IpodShellDayBase = Color(0xFFEAECEF)
    private val IpodShellNightBase = Color(0xFF1F2024)
    private val RetroMp3ShellDayBase = Color(0xFF3F46A8)
    private val RetroMp3ShellNightBase = Color(0xFF2D3380)
    private val RetroMp3CapSeamDayBase = Color(0xFF252A6E)
    private val RetroMp3CapSeamNightBase = Color(0xFF191D4D)

    val SurfaceBackground: ColorProvider = dayNightColorProvider(
        day = SurfaceDayBase,
        night = SurfaceNightBase
    )

    val InnerCardSurface: ColorProvider = dayNightColorProvider(
        day = InnerCardDayBase,
        night = InnerCardNightBase
    )

    fun scaleColorAlpha(baseColor: Color, opacityPct: Int): Color {
        val clamped = opacityPct.coerceIn(0, 100)
        return baseColor.copy(alpha = baseColor.alpha * (clamped / 100f))
    }

    fun bitmapAlphaFromOpacity(opacityPct: Int): Int =
        (opacityPct.coerceIn(0, 100) * 255f / 100f).roundToInt().coerceIn(0, 255)

    fun surfaceBackground(opacityPct: Int): ColorProvider {
        if (opacityPct >= 100) return SurfaceBackground
        return dayNightColorProvider(
            day = scaleColorAlpha(SurfaceDayBase, opacityPct),
            night = scaleColorAlpha(SurfaceNightBase, opacityPct)
        )
    }

    fun innerCardSurface(opacityPct: Int): ColorProvider {
        if (opacityPct >= 100) return InnerCardSurface
        return dayNightColorProvider(
            day = scaleColorAlpha(InnerCardDayBase, opacityPct),
            night = scaleColorAlpha(InnerCardNightBase, opacityPct)
        )
    }

    fun ipodShellSurface(opacityPct: Int): ColorProvider {
        if (opacityPct >= 100) return IpodShellSurface
        return dayNightColorProvider(
            day = scaleColorAlpha(IpodShellDayBase, opacityPct),
            night = scaleColorAlpha(IpodShellNightBase, opacityPct)
        )
    }

    fun retroMp3ShellSurface(opacityPct: Int): ColorProvider {
        if (opacityPct >= 100) return RetroMp3ShellSurface
        return dayNightColorProvider(
            day = scaleColorAlpha(RetroMp3ShellDayBase, opacityPct),
            night = scaleColorAlpha(RetroMp3ShellNightBase, opacityPct)
        )
    }

    fun retroMp3CapSeam(opacityPct: Int): ColorProvider {
        if (opacityPct >= 100) return RetroMp3CapSeam
        return dayNightColorProvider(
            day = scaleColorAlpha(RetroMp3CapSeamDayBase, opacityPct),
            night = scaleColorAlpha(RetroMp3CapSeamNightBase, opacityPct)
        )
    }

    val PrimaryText = dayNightColorProvider(
        day = Color(0xFF121316),
        night = Color(0xFFF5F6F9)
    )

    val SecondaryText = dayNightColorProvider(
        day = Color(0xFF5D6068),
        night = Color(0xFFA0A4B0)
    )

    val OneUiAccentBlue = dayNightColorProvider(
        day = Color(0xFF0381FE),
        night = Color(0xFF3E9BFF)
    )

    val RetroLcdBacklight = dayNightColorProvider(
        day = Color(0xFF35E8F7),
        night = Color(0xFF1FD6E8)
    )

    val RetroAlertAmber = dayNightColorProvider(
        day = Color(0xFFD97706),
        night = Color(0xFFFBBF24)
    )

    val IpodShellSurface: ColorProvider = dayNightColorProvider(
        day = IpodShellDayBase,
        night = IpodShellNightBase
    )

    val IpodLcdSurface: ColorProvider = dayNightColorProvider(
        day = Color(0xFFD6E4E8),
        night = Color(0xFF121820)
    )

    val RetroMp3ShellSurface: ColorProvider = dayNightColorProvider(
        day = RetroMp3ShellDayBase,
        night = RetroMp3ShellNightBase
    )

    val RetroMp3CapSeam = dayNightColorProvider(
        day = RetroMp3CapSeamDayBase,
        night = RetroMp3CapSeamNightBase
    )

    val RetroMp3BezelSurface = dayNightColorProvider(
        day = Color(0xFF0E121B),
        night = Color(0xFF0A0D14)
    )

    val RetroMp3GoldLabel = dayNightColorProvider(
        day = Color(0xFFEBC94B),
        night = Color(0xFFF3D564)
    )

    val RetroMp3SilverSurface = dayNightColorProvider(
        day = Color(0xFFEDEFF8),
        night = Color(0xFFDCE1F0)
    )

    val RetroMp3SilverText = dayNightColorProvider(
        day = Color(0xFF232854),
        night = Color(0xFF1A1E40)
    )

    val RetroMp3ScreenSubtext = dayNightColorProvider(
        day = Color(0xFFA8B5C8),
        night = Color(0xFF94A3B8)
    )

    val RetroMp3TrackBg = dayNightColorProvider(
        day = Color(0xFF232C3D),
        night = Color(0xFF1E2636)
    )

    val RetroMp3EqIdle = dayNightColorProvider(
        day = Color(0xFF4F5E78),
        night = Color(0xFF435168)
    )

    val RetroLcdPrimaryText = dayNightColorProvider(
        day = Color(0xFF04262E),
        night = Color(0xFF031C22)
    )

    val RetroLcdSecondaryText = dayNightColorProvider(
        day = Color(0xFF0A4352),
        night = Color(0xFF073440)
    )

    // Semantic Weather & Environmental Tokens (Day / Night adaptive)
    val WeatherRainCyan = dayNightColorProvider(
        day = Color(0xFF0284C7),
        night = Color(0xFF38BDF8)
    )

    val WeatherGoodGreen = dayNightColorProvider(
        day = Color(0xFF059669),
        night = Color(0xFF34D399)
    )

    val WeatherModerateAmber = dayNightColorProvider(
        day = Color(0xFFD97706),
        night = Color(0xFFFBBF24)
    )

    val WeatherHighOrange = dayNightColorProvider(
        day = Color(0xFFEA580C),
        night = Color(0xFFFB923C)
    )

    val WeatherDangerRed = dayNightColorProvider(
        day = Color(0xFFDC2626),
        night = Color(0xFFF87171)
    )

    val WeatherExtremePurple = dayNightColorProvider(
        day = Color(0xFF7C3AED),
        night = Color(0xFFA78BFA)
    )

    val WeatherMoonSilver = dayNightColorProvider(
        day = Color(0xFF4F46E5),
        night = Color(0xFFC7D2FE)
    )

    // Semantic Astrology Tokens (Day / Night adaptive)
    val AstroShadowPurple = dayNightColorProvider(
        day = Color(0xFF7C3AED),
        night = Color(0xFFA78BFA)
    )

    val AstroDirectChipBg = dayNightColorProvider(
        day = Color(0x1A0381FE),
        night = Color(0x283E9BFF)
    )

    val AstroShadowChipBg = dayNightColorProvider(
        day = Color(0x1A7C3AED),
        night = Color(0x28A78BFA)
    )

    val AstroRetroChipBg = dayNightColorProvider(
        day = Color(0x1FD97706),
        night = Color(0x2AFBBF24)
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

    const val SIGNAL_EXCELLENT_LIGHT: Int = 0xFF059669.toInt()
    const val SIGNAL_EXCELLENT_DARK: Int = 0xFF34D399.toInt()
    const val SIGNAL_GOOD_LIGHT: Int = 0xFF0381FE.toInt()
    const val SIGNAL_GOOD_DARK: Int = 0xFF3E9BFF.toInt()
    const val SIGNAL_FAIR_LIGHT: Int = 0xFFD97706.toInt()
    const val SIGNAL_FAIR_DARK: Int = 0xFFFBBF24.toInt()
    const val SIGNAL_WEAK_LIGHT: Int = 0xFFDC2626.toInt()
    const val SIGNAL_WEAK_DARK: Int = 0xFFF87171.toInt()
    const val SIGNAL_MUTED_LIGHT: Int = 0xFF94A3B8.toInt()
    const val SIGNAL_MUTED_DARK: Int = 0xFF64748B.toInt()
    const val SIGNAL_BAR_EMPTY_LIGHT: Int = 0xFFE2E5EE.toInt()
    const val SIGNAL_BAR_EMPTY_DARK: Int = 0xFF3A3D47.toInt()
    const val SIGNAL_BADGE_BG_LIGHT: Int = 0x1A0381FE
    const val SIGNAL_BADGE_BG_DARK: Int = 0x243E9BFF
}



