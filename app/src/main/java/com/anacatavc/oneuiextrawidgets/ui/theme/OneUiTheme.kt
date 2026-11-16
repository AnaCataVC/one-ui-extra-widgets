package com.anacatavc.oneuiextrawidgets.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val OneUiBlue = Color(0xFF0381FE)
val OneUiDarkBackground = Color(0xFF000000)
val OneUiDarkSurface = Color(0xFF17171A)
val OneUiLightBackground = Color(0xFFF2F2F7)
val OneUiLightSurface = Color(0xFFFFFFFF)

private val OneUiDarkColorScheme = darkColorScheme(
    primary = Color(0xFF3E9BFF),
    secondary = Color(0xFF68B5FF),
    background = OneUiDarkBackground,
    surface = OneUiDarkSurface,
    surfaceVariant = Color(0xFF242429)
)

private val OneUiLightColorScheme = lightColorScheme(
    primary = OneUiBlue,
    secondary = Color(0xFF0066CC),
    background = OneUiLightBackground,
    surface = OneUiLightSurface,
    surfaceVariant = Color(0xFFE5E5EA)
)

val OneUiShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun OneUiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> OneUiDarkColorScheme
        else -> OneUiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = OneUiShapes,
        content = content
    )
}
