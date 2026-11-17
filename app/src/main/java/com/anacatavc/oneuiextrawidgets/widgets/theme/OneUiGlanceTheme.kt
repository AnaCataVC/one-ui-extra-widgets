package com.anacatavc.oneuiextrawidgets.widgets.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.color.ColorProvider

object OneUiGlanceTokens {
    val CornerRadius = 26.dp
    // Concentric law: R_inner <= R_outer (26.dp) - CardPadding (14.dp) = 12.dp
    val InnerRadius = 12.dp
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
}
