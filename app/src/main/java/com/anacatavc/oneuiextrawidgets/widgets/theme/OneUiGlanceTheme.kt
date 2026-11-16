package com.anacatavc.oneuiextrawidgets.widgets.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.color.ColorProvider

object OneUiGlanceTokens {
    val CornerRadius = 26.dp
    val InnerRadius = 18.dp
    val CardPadding = 14.dp

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
        day = Color(0xFF9BBC0F),
        night = Color(0xFF79E2B6)
    )

    val RetroAlertAmber = ColorProvider(
        day = Color(0xFFD97706),
        night = Color(0xFFFBBF24)
    )
}
