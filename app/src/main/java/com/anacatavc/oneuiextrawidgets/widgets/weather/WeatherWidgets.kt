package com.anacatavc.oneuiextrawidgets.widgets.weather

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anacatavc.oneuiextrawidgets.MainActivity
import com.anacatavc.oneuiextrawidgets.R
import com.anacatavc.oneuiextrawidgets.data.astrology.AstrologyEngine
import com.anacatavc.oneuiextrawidgets.data.astrology.LunarPhaseStatus
import com.anacatavc.oneuiextrawidgets.data.astrology.SolarLightPhase
import com.anacatavc.oneuiextrawidgets.data.preferences.AppPreferences
import com.anacatavc.oneuiextrawidgets.data.preferences.CachedWeatherState
import com.anacatavc.oneuiextrawidgets.data.preferences.WidgetFontScaleMode
import com.anacatavc.oneuiextrawidgets.data.weather.OpenMeteoClient
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import com.anacatavc.oneuiextrawidgets.widgets.OneUiGlanceWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.WidgetInstanceHelper
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import kotlinx.coroutines.flow.first
import java.util.Locale
import kotlin.math.roundToInt

internal enum class WeatherCardLayoutMode {
    COMPACT_1X1,
    PILL,
    SQUARE,
    EXPANDED
}

internal data class WeatherCardLayoutSpec(
    val mode: WeatherCardLayoutMode,
    val cardPaddingDp: Float,
    val gaugeSizeDp: Float,
    val showExtendedDetails: Boolean
)

internal fun calculateWeatherCardLayout(widthDp: Float, heightDp: Float): WeatherCardLayoutSpec {
    val safeW = widthDp.coerceAtLeast(70f)
    val safeH = heightDp.coerceAtLeast(64f)

    val mode = when {
        safeW < 130f && safeH < 130f -> WeatherCardLayoutMode.COMPACT_1X1
        safeH < 130f -> WeatherCardLayoutMode.PILL
        safeW >= 240f -> WeatherCardLayoutMode.EXPANDED
        else -> WeatherCardLayoutMode.SQUARE
    }

    return when (mode) {
        WeatherCardLayoutMode.COMPACT_1X1 -> {
            val padding = 8f
            val innerMin = minOf(safeW, safeH) - padding * 2f
            val gaugeSize = (innerMin * 0.64f).coerceIn(34f, 52f)
            WeatherCardLayoutSpec(
                mode = mode,
                cardPaddingDp = padding,
                gaugeSizeDp = gaugeSize,
                showExtendedDetails = false
            )
        }
        WeatherCardLayoutMode.PILL -> {
            val padding = 12f
            val innerH = (safeH - padding * 2f).coerceAtLeast(40f)
            val gaugeSize = (innerH * 0.88f).coerceIn(42f, 56f)
            WeatherCardLayoutSpec(
                mode = mode,
                cardPaddingDp = padding,
                gaugeSizeDp = gaugeSize,
                showExtendedDetails = safeW >= 240f
            )
        }
        WeatherCardLayoutMode.SQUARE -> {
            val padding = 14f
            val innerMin = minOf(safeW, safeH) - padding * 2f
            val gaugeSize = (innerMin * 0.46f).coerceIn(58f, 82f)
            WeatherCardLayoutSpec(
                mode = mode,
                cardPaddingDp = padding,
                gaugeSizeDp = gaugeSize,
                showExtendedDetails = true
            )
        }
        WeatherCardLayoutMode.EXPANDED -> {
            val padding = 14f
            val innerH = (safeH - padding * 2f).coerceAtLeast(90f)
            val gaugeSize = (innerH * 0.68f).coerceIn(64f, 88f)
            WeatherCardLayoutSpec(
                mode = mode,
                cardPaddingDp = padding,
                gaugeSizeDp = gaugeSize,
                showExtendedDetails = true
            )
        }
    }
}

internal enum class RainIntensityLevel {
    DRY,
    LOW,
    MODERATE,
    HIGH
}

internal fun classifyRainLevel(probabilityPct: Int): RainIntensityLevel = when {
    probabilityPct <= 10 -> RainIntensityLevel.DRY
    probabilityPct <= 35 -> RainIntensityLevel.LOW
    probabilityPct <= 65 -> RainIntensityLevel.MODERATE
    else -> RainIntensityLevel.HIGH
}

internal enum class AqiQualityLevel {
    GOOD,
    FAIR,
    MODERATE,
    POOR,
    VERY_POOR
}

internal fun classifyAqiLevel(europeanAqi: Int): AqiQualityLevel = when {
    europeanAqi <= 20 -> AqiQualityLevel.GOOD
    europeanAqi <= 40 -> AqiQualityLevel.FAIR
    europeanAqi <= 60 -> AqiQualityLevel.MODERATE
    europeanAqi <= 80 -> AqiQualityLevel.POOR
    else -> AqiQualityLevel.VERY_POOR
}

internal enum class UvRiskLevel {
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH,
    EXTREME
}

internal fun classifyUvLevel(uvIndex: Double): UvRiskLevel {
    if (uvIndex.isNaN()) return UvRiskLevel.LOW
    val uv = uvIndex.roundToInt()
    return when {
        uv <= 2 -> UvRiskLevel.LOW
        uv <= 5 -> UvRiskLevel.MODERATE
        uv <= 7 -> UvRiskLevel.HIGH
        uv <= 10 -> UvRiskLevel.VERY_HIGH
        else -> UvRiskLevel.EXTREME
    }
}

internal fun createWeatherDetailIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_WEATHER)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

/**
 * 1. One UI Rain Probability Widget (2×1 Default, Resizable, Zero Temperature)
 */
class OneUiRainWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weatherFlow = AppPreferences.observeWeather(context)
        val initialWeather = weatherFlow.first()
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val fontScaleFlow = WidgetInstanceHelper.observeWidgetFontScale(context, id)
        val initialFontScale = fontScaleFlow.first()
        val isDark = OneUiGlanceTokens.isNightModeActive(context)

        provideContent {
            val state by weatherFlow.collectAsState(initial = initialWeather)
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val fontScaleMode by fontScaleFlow.collectAsState(initial = initialFontScale)
            val isEs = LocaleHelper.isSpanishActive()
            val size = LocalSize.current
            val layout = calculateWeatherCardLayout(size.width.value, size.height.value)
            val bitmap = WidgetCanvasRenderer.renderRainGaugeBitmap(
                probabilityPct = state.rainProbabilityPct,
                sizePx = 160,
                isDarkTheme = isDark
            )

            val rainLevel = classifyRainLevel(state.rainProbabilityPct)
            val statusBadge = when (rainLevel) {
                RainIntensityLevel.DRY -> if (isEs) "Sin lluvia" else "Dry"
                RainIntensityLevel.LOW -> if (isEs) "Prob. Baja" else "Low Chance"
                RainIntensityLevel.MODERATE -> if (isEs) "Moderada" else "Moderate"
                RainIntensityLevel.HIGH -> if (isEs) "Lluvia Alta" else "High Rain"
            }
            val accentColor = when (rainLevel) {
                RainIntensityLevel.DRY -> OneUiGlanceTokens.WeatherGoodGreen
                RainIntensityLevel.LOW -> OneUiGlanceTokens.WeatherRainCyan
                RainIntensityLevel.MODERATE -> OneUiGlanceTokens.OneUiAccentBlue
                RainIntensityLevel.HIGH -> OneUiGlanceTokens.WeatherModerateAmber
            }

            WeatherModularCard(
                context = context,
                layout = layout,
                gaugeBitmap = bitmap,
                gaugeContentDescription = statusBadge,
                headerLabel = if (isEs) "Prob. Lluvia" else "Rain Prob.",
                primaryValue = "${state.rainProbabilityPct}%",
                statusBadge = statusBadge,
                statusColor = accentColor,
                pillText = state.cityName,
                compactLabel = "${state.rainProbabilityPct}%",
                compactColor = accentColor,
                detailLeftLabel = if (isEs) "Ventana" else "Window",
                detailLeftValue = if (isEs) "Próx. 6h" else "Next 6h",
                detailRightLabel = if (isEs) "Nubosidad" else "Cloud Cover",
                detailRightValue = "${state.cloudCoverPct}%",
                isSpanish = isEs,
                showRefreshAction = true,
                opacityPct = opacityPct,
                fontScaleMode = fontScaleMode
            )
        }
    }
}

/**
 * 2. One UI Air Quality (AQI) Widget (2×1 Default, Resizable, Zero Temperature)
 */
class OneUiAqiWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weatherFlow = AppPreferences.observeWeather(context)
        val initialWeather = weatherFlow.first()
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val fontScaleFlow = WidgetInstanceHelper.observeWidgetFontScale(context, id)
        val initialFontScale = fontScaleFlow.first()
        val isDark = OneUiGlanceTokens.isNightModeActive(context)

        provideContent {
            val state by weatherFlow.collectAsState(initial = initialWeather)
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val fontScaleMode by fontScaleFlow.collectAsState(initial = initialFontScale)
            val isEs = LocaleHelper.isSpanishActive()
            val size = LocalSize.current
            val layout = calculateWeatherCardLayout(size.width.value, size.height.value)
            val bitmap = WidgetCanvasRenderer.renderAqiGaugeBitmap(
                europeanAqi = state.europeanAqi,
                sizePx = 160,
                isDarkTheme = isDark
            )

            val level = classifyAqiLevel(state.europeanAqi)
            val statusBadge = when (level) {
                AqiQualityLevel.GOOD -> context.getString(R.string.aqi_good)
                AqiQualityLevel.FAIR -> context.getString(R.string.aqi_fair)
                AqiQualityLevel.MODERATE -> context.getString(R.string.aqi_moderate)
                AqiQualityLevel.POOR -> context.getString(R.string.aqi_poor)
                AqiQualityLevel.VERY_POOR -> context.getString(R.string.aqi_very_poor)
            }
            val accentColor = when (level) {
                AqiQualityLevel.GOOD -> OneUiGlanceTokens.WeatherGoodGreen
                AqiQualityLevel.FAIR -> OneUiGlanceTokens.WeatherGoodGreen
                AqiQualityLevel.MODERATE -> OneUiGlanceTokens.WeatherModerateAmber
                AqiQualityLevel.POOR -> OneUiGlanceTokens.WeatherDangerRed
                AqiQualityLevel.VERY_POOR -> OneUiGlanceTokens.WeatherExtremePurple
            }

            WeatherModularCard(
                context = context,
                layout = layout,
                gaugeBitmap = bitmap,
                gaugeContentDescription = "AQI ${state.europeanAqi} $statusBadge",
                headerLabel = if (isEs) "Calidad del Aire" else "Air Quality",
                primaryValue = statusBadge,
                statusBadge = if (isEs) "Índice Europeo" else "European Index",
                statusColor = accentColor,
                pillText = state.cityName,
                compactLabel = statusBadge,
                compactColor = accentColor,
                detailLeftLabel = "PM2.5",
                detailLeftValue = "${state.pm25.roundToInt()} µg/m³",
                detailRightLabel = "PM10",
                detailRightValue = "${state.pm10.roundToInt()} µg/m³",
                isSpanish = isEs,
                showRefreshAction = true,
                opacityPct = opacityPct,
                fontScaleMode = fontScaleMode
            )
        }
    }
}

/**
 * 3. One UI Solar UV Index Widget (2×1 Default, Resizable, Zero Temperature)
 */
class OneUiUvWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weatherFlow = AppPreferences.observeWeather(context)
        val initialWeather = weatherFlow.first()
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val fontScaleFlow = WidgetInstanceHelper.observeWidgetFontScale(context, id)
        val initialFontScale = fontScaleFlow.first()
        val isDark = OneUiGlanceTokens.isNightModeActive(context)

        provideContent {
            val state by weatherFlow.collectAsState(initial = initialWeather)
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val fontScaleMode by fontScaleFlow.collectAsState(initial = initialFontScale)
            val isEs = LocaleHelper.isSpanishActive()
            val size = LocalSize.current
            val layout = calculateWeatherCardLayout(size.width.value, size.height.value)
            val bitmap = WidgetCanvasRenderer.renderUvGaugeBitmap(
                uvIndex = state.uvIndex,
                sizePx = 160,
                isDarkTheme = isDark
            )

            val level = classifyUvLevel(state.uvIndex)
            val statusBadge = when (level) {
                UvRiskLevel.LOW -> if (isEs) "Bajo" else "Low"
                UvRiskLevel.MODERATE -> if (isEs) "Moderado" else "Moderate"
                UvRiskLevel.HIGH -> if (isEs) "Alto" else "High"
                UvRiskLevel.VERY_HIGH -> if (isEs) "Muy Alto" else "Very High"
                UvRiskLevel.EXTREME -> if (isEs) "Extremo" else "Extreme"
            }
            val protectionTip = when (level) {
                UvRiskLevel.LOW -> if (isEs) "Sin riesgo" else "Safe"
                UvRiskLevel.MODERATE -> if (isEs) "Usa FPS 30+" else "Use SPF 30+"
                UvRiskLevel.HIGH -> if (isEs) "FPS 50+ / Gafas" else "SPF 50+ / Shades"
                UvRiskLevel.VERY_HIGH -> if (isEs) "Evita sol directo" else "Seek shade"
                UvRiskLevel.EXTREME -> if (isEs) "Riesgo máximo" else "Max UV alert"
            }
            val accentColor = when (level) {
                UvRiskLevel.LOW -> OneUiGlanceTokens.WeatherGoodGreen
                UvRiskLevel.MODERATE -> OneUiGlanceTokens.WeatherModerateAmber
                UvRiskLevel.HIGH -> OneUiGlanceTokens.WeatherHighOrange
                UvRiskLevel.VERY_HIGH -> OneUiGlanceTokens.WeatherDangerRed
                UvRiskLevel.EXTREME -> OneUiGlanceTokens.WeatherExtremePurple
            }

            WeatherModularCard(
                context = context,
                layout = layout,
                gaugeBitmap = bitmap,
                gaugeContentDescription = "UV ${state.uvIndex.roundToInt()} $statusBadge",
                headerLabel = if (isEs) "Índice UV Solar" else "Solar UV Index",
                primaryValue = "UV ${state.uvIndex.roundToInt()}",
                statusBadge = statusBadge,
                statusColor = accentColor,
                pillText = state.cityName,
                compactLabel = statusBadge,
                compactColor = accentColor,
                detailLeftLabel = if (isEs) "Protección" else "Protection",
                detailLeftValue = protectionTip,
                detailRightLabel = if (isEs) "Nubosidad" else "Cloud Cover",
                detailRightValue = "${state.cloudCoverPct}%",
                isSpanish = isEs,
                showRefreshAction = true,
                opacityPct = opacityPct,
                fontScaleMode = fontScaleMode
            )
        }
    }
}

/**
 * 4. One UI Scientific Moon Phase Widget (2×1 Default, Resizable, 100% Astronomical & Zero Astrology/Temperature)
 */
class OneUiMoonPhaseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val astroSnapshot = AstrologyEngine.computeSnapshot()
        val lunar: LunarPhaseStatus = astroSnapshot.lunarStatus
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val fontScaleFlow = WidgetInstanceHelper.observeWidgetFontScale(context, id)
        val initialFontScale = fontScaleFlow.first()
        val moonBitmap = WidgetCanvasRenderer.renderMoonPhaseBitmap(
            phaseAngleDeg = lunar.phaseAngleDeg,
            sizePx = 160
        )

        provideContent {
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val fontScaleMode by fontScaleFlow.collectAsState(initial = initialFontScale)
            val isEs = LocaleHelper.isSpanishActive()
            val size = LocalSize.current
            val layout = calculateWeatherCardLayout(size.width.value, size.height.value)

            val phaseName = lunar.localizedPhaseName(isEs)
            val ageFormatted = String.format(Locale.US, "%.1f d", lunar.synodicAgeDays)
            val elongationFormatted = "${lunar.normalizedPhaseAngleDeg.roundToInt() % 360}°"
            val litBadge = if (isEs) {
                "${lunar.illuminationPercent}% ilum."
            } else {
                "${lunar.illuminationPercent}% lit"
            }

            WeatherModularCard(
                context = context,
                layout = layout,
                gaugeBitmap = moonBitmap,
                gaugeContentDescription = "$phaseName ${lunar.illuminationPercent}%",
                headerLabel = if (isEs) "Fase Lunar" else "Moon Phase",
                primaryValue = phaseName,
                statusBadge = litBadge,
                statusColor = OneUiGlanceTokens.WeatherMoonSilver,
                pillText = elongationFormatted,
                compactLabel = "${lunar.illuminationPercent}%",
                compactColor = OneUiGlanceTokens.WeatherMoonSilver,
                detailLeftLabel = if (isEs) "Edad Sinódica" else "Synodic Age",
                detailLeftValue = "$ageFormatted / 29.5 d",
                detailRightLabel = if (isEs) "Elongación" else "Elongation",
                detailRightValue = elongationFormatted,
                isSpanish = isEs,
                showRefreshAction = false,
                opacityPct = opacityPct,
                fontScaleMode = fontScaleMode
            )
        }
    }
}

/**
 * 5. One UI Sun Arc & Golden/Blue Hour Widget (2×1 Default, Resizable, 100% Offline)
 */
class OneUiSunArcWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weatherFlow = AppPreferences.observeWeather(context)
        val initialWeather = weatherFlow.first()
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val fontScaleFlow = WidgetInstanceHelper.observeWidgetFontScale(context, id)
        val initialFontScale = fontScaleFlow.first()
        val isDark = OneUiGlanceTokens.isNightModeActive(context)

        provideContent {
            val state by weatherFlow.collectAsState(initial = initialWeather)
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val fontScaleMode by fontScaleFlow.collectAsState(initial = initialFontScale)
            val isEs = LocaleHelper.isSpanishActive()
            val size = LocalSize.current
            val layout = calculateWeatherCardLayout(size.width.value, size.height.value)
            val solar = AstrologyEngine.computeSolarArcSnapshot(state.latitude, state.longitude)
            val bitmap = WidgetCanvasRenderer.renderSunArcGaugeBitmap(
                sunAltitudeDeg = solar.sunAltitudeDeg,
                dayProgressFraction = solar.dayProgressFraction,
                sizePx = 160,
                isDarkTheme = isDark
            )

            val accentColor = when (solar.lightPhase) {
                SolarLightPhase.GOLDEN_HOUR -> OneUiGlanceTokens.SunGoldenAmber
                SolarLightPhase.BLUE_HOUR,
                SolarLightPhase.TWILIGHT -> OneUiGlanceTokens.SunBlueHourIndigo
                SolarLightPhase.DAYLIGHT -> OneUiGlanceTokens.WeatherModerateAmber
                SolarLightPhase.NIGHT -> OneUiGlanceTokens.StargazingIndigo
            }

            val phaseLabel = solar.lightPhase.localizedLabel(isEs)
            val goldenBadge = if (isEs) {
                "Dorada ${solar.goldenHourFormatted}"
            } else {
                "Golden ${solar.goldenHourFormatted}"
            }

            WeatherModularCard(
                context = context,
                layout = layout,
                gaugeBitmap = bitmap,
                gaugeContentDescription = "$phaseLabel ${solar.sunriseFormatted}–${solar.sunsetFormatted}",
                headerLabel = if (isEs) "Sol • Hora Dorada" else "Sun • Golden Hour",
                primaryValue = phaseLabel,
                statusBadge = goldenBadge,
                statusColor = accentColor,
                pillText = solar.daylightDurationFormatted,
                compactLabel = solar.sunriseFormatted,
                compactColor = accentColor,
                detailLeftLabel = if (isEs) "Amanecer" else "Sunrise",
                detailLeftValue = solar.sunriseFormatted,
                detailRightLabel = if (isEs) "Atardecer" else "Sunset",
                detailRightValue = solar.sunsetFormatted,
                isSpanish = isEs,
                showRefreshAction = false,
                opacityPct = opacityPct,
                fontScaleMode = fontScaleMode
            )
        }
    }
}

/**
 * 6. One UI Night Sky: Stargazing Index Widget (2×1 Default, Resizable, Zero Temperature/Zodiac)
 */
class OneUiStargazingWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weatherFlow = AppPreferences.observeWeather(context)
        val initialWeather = weatherFlow.first()
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val fontScaleFlow = WidgetInstanceHelper.observeWidgetFontScale(context, id)
        val initialFontScale = fontScaleFlow.first()
        val isDark = OneUiGlanceTokens.isNightModeActive(context)

        provideContent {
            val state by weatherFlow.collectAsState(initial = initialWeather)
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val fontScaleMode by fontScaleFlow.collectAsState(initial = initialFontScale)
            val isEs = LocaleHelper.isSpanishActive()
            val size = LocalSize.current
            val layout = calculateWeatherCardLayout(size.width.value, size.height.value)
            val star = AstrologyEngine.computeStargazingSnapshot(
                cloudCoverPct = state.cloudCoverPct,
                latitude = state.latitude,
                longitude = state.longitude
            )
            val bitmap = WidgetCanvasRenderer.renderStargazingGaugeBitmap(
                stargazingIndexPct = star.scorePct,
                sizePx = 160,
                isDarkTheme = isDark
            )

            val (tierLabel, accentColor) = when {
                star.scorePct >= 75 -> {
                    val label = if (isEs) "Cielo Óptimo" else "Clear Sky"
                    label to OneUiGlanceTokens.StargazingCyan
                }
                star.scorePct >= 45 -> {
                    val label = if (isEs) "Visibilidad Media" else "Fair Sky"
                    label to OneUiGlanceTokens.StargazingIndigo
                }
                else -> {
                    val label = if (isEs) "Nublado / Brillo" else "Cloudy / Glare"
                    label to OneUiGlanceTokens.WeatherModerateAmber
                }
            }

            val planets = star.localizedVisiblePlanets(isEs).take(2).joinToString(", ")

            WeatherModularCard(
                context = context,
                layout = layout,
                gaugeBitmap = bitmap,
                gaugeContentDescription = "$tierLabel ${star.scorePct}%",
                headerLabel = if (isEs) "Cielo Nocturno" else "Night Sky",
                primaryValue = "${star.scorePct}%",
                statusBadge = tierLabel,
                statusColor = accentColor,
                pillText = state.cityName,
                compactLabel = "${star.scorePct}%",
                compactColor = accentColor,
                detailLeftLabel = if (isEs) "Planetas Visibles" else "Visible Planets",
                detailLeftValue = planets,
                detailRightLabel = if (isEs) "Nubes / Luna" else "Clouds / Moon",
                detailRightValue = "${star.cloudCoverPct}% / ${star.moonIlluminationPct}%",
                isSpanish = isEs,
                showRefreshAction = true,
                opacityPct = opacityPct,
                fontScaleMode = fontScaleMode
            )
        }
    }
}

@Composable
internal fun WeatherModularCard(
    context: Context,
    layout: WeatherCardLayoutSpec,
    gaugeBitmap: Bitmap,
    gaugeContentDescription: String,
    headerLabel: String,
    primaryValue: String,
    statusBadge: String,
    statusColor: ColorProvider,
    pillText: String,
    compactLabel: String,
    compactColor: ColorProvider,
    detailLeftLabel: String,
    detailLeftValue: String,
    detailRightLabel: String,
    detailRightValue: String,
    isSpanish: Boolean,
    showRefreshAction: Boolean,
    opacityPct: Int,
    fontScaleMode: WidgetFontScaleMode,
    clickIntent: Intent = createWeatherDetailIntent(context),
    refreshAction: Action = actionRunCallback<RefreshWeatherAction>()
) {
    val refreshDescription = if (isSpanish) "Actualizar datos del clima" else "Refresh weather data"
    val surfaceBg = OneUiGlanceTokens.surfaceBackground(opacityPct)
    val innerBg = OneUiGlanceTokens.innerCardSurface(opacityPct)
    val systemScale = OneUiGlanceTokens.systemFontScale(context)
    val isCompact = layout.mode == WeatherCardLayoutMode.COMPACT_1X1 ||
        layout.mode == WeatherCardLayoutMode.PILL
    val sp: (Float) -> TextUnit = { base ->
        OneUiGlanceTokens.scaledSp(base, systemScale, fontScaleMode, isCompact)
    }

    when (layout.mode) {
        WeatherCardLayoutMode.COMPACT_1X1 -> {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(OneUiGlanceTokens.CornerRadius)
                    .background(surfaceBg)
                    .padding(layout.cardPaddingDp.dp)
                    .clickable(actionStartActivity(clickIntent)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(gaugeBitmap),
                    contentDescription = gaugeContentDescription,
                    modifier = GlanceModifier.size(layout.gaugeSizeDp.dp)
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = compactLabel,
                    style = TextStyle(
                        color = compactColor,
                        fontSize = sp(11f),
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
        }

        WeatherCardLayoutMode.PILL -> {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(OneUiGlanceTokens.CornerRadius)
                    .background(surfaceBg)
                    .padding(layout.cardPaddingDp.dp)
                    .clickable(actionStartActivity(clickIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(gaugeBitmap),
                    contentDescription = gaugeContentDescription,
                    modifier = GlanceModifier.size(layout.gaugeSizeDp.dp)
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = headerLabel,
                            style = TextStyle(
                                color = OneUiGlanceTokens.SecondaryText,
                                fontSize = sp(11f),
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (showRefreshAction) {
                            RefreshPillBadge(
                                label = if (layout.showExtendedDetails) "$pillText ↻" else "↻",
                                contentDesc = refreshDescription,
                                background = innerBg,
                                fontSize = sp(10f),
                                refreshAction = refreshAction
                            )
                        }
                    }
                    Text(
                        text = primaryValue,
                        style = TextStyle(
                            color = OneUiGlanceTokens.PrimaryText,
                            fontSize = sp(if (primaryValue.length > 10) 14f else 18f),
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = statusBadge,
                        style = TextStyle(
                            color = statusColor,
                            fontSize = sp(11f),
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        WeatherCardLayoutMode.SQUARE -> {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(OneUiGlanceTokens.CornerRadius)
                    .background(surfaceBg)
                    .padding(layout.cardPaddingDp.dp)
                    .clickable(actionStartActivity(clickIntent)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = headerLabel,
                        style = TextStyle(
                            color = OneUiGlanceTokens.SecondaryText,
                            fontSize = sp(11f),
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    if (showRefreshAction) {
                        RefreshPillBadge(
                            label = "↻",
                            contentDesc = refreshDescription,
                            background = innerBg,
                            fontSize = sp(10f),
                            refreshAction = refreshAction
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.height(6.dp))
                Image(
                    provider = ImageProvider(gaugeBitmap),
                    contentDescription = gaugeContentDescription,
                    modifier = GlanceModifier.size(layout.gaugeSizeDp.dp)
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = primaryValue,
                    style = TextStyle(
                        color = OneUiGlanceTokens.PrimaryText,
                        fontSize = sp(if (primaryValue.length > 10) 15f else 20f),
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = statusBadge,
                    style = TextStyle(
                        color = statusColor,
                        fontSize = sp(11f),
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                DetailSubPill(
                    label = detailLeftLabel,
                    value = detailLeftValue,
                    background = innerBg,
                    labelFontSize = sp(10f),
                    valueFontSize = sp(12f),
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }

        WeatherCardLayoutMode.EXPANDED -> {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(OneUiGlanceTokens.CornerRadius)
                    .background(surfaceBg)
                    .padding(layout.cardPaddingDp.dp)
                    .clickable(actionStartActivity(clickIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(gaugeBitmap),
                    contentDescription = gaugeContentDescription,
                    modifier = GlanceModifier.size(layout.gaugeSizeDp.dp)
                )
                Spacer(modifier = GlanceModifier.width(14.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = headerLabel,
                            style = TextStyle(
                                color = OneUiGlanceTokens.SecondaryText,
                                fontSize = sp(12f),
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (showRefreshAction) {
                            RefreshPillBadge(
                                label = "$pillText ↻",
                                contentDesc = refreshDescription,
                                background = innerBg,
                                fontSize = sp(10f),
                                refreshAction = refreshAction
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = primaryValue,
                            style = TextStyle(
                                color = OneUiGlanceTokens.PrimaryText,
                                fontSize = sp(20f),
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = statusBadge,
                            style = TextStyle(
                                color = statusColor,
                                fontSize = sp(12f),
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        DetailSubPill(
                            label = detailLeftLabel,
                            value = detailLeftValue,
                            background = innerBg,
                            labelFontSize = sp(10f),
                            valueFontSize = sp(12f),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        DetailSubPill(
                            label = detailRightLabel,
                            value = detailRightValue,
                            background = innerBg,
                            labelFontSize = sp(10f),
                            valueFontSize = sp(12f),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun RefreshPillBadge(
    label: String,
    contentDesc: String,
    background: ColorProvider,
    fontSize: TextUnit,
    refreshAction: Action = actionRunCallback<RefreshWeatherAction>()
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .cornerRadius(OneUiGlanceTokens.InnerRadius)
            .background(background)
            .padding(horizontal = 7.dp, vertical = 3.dp)
            .semantics { contentDescription = contentDesc }
            .clickable(refreshAction)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = OneUiGlanceTokens.SecondaryText,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

@Composable
internal fun DetailSubPill(
    label: String,
    value: String,
    background: ColorProvider,
    labelFontSize: TextUnit,
    valueFontSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier
            .cornerRadius(OneUiGlanceTokens.InnerRadius)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = OneUiGlanceTokens.SecondaryText,
                fontSize = labelFontSize
            ),
            maxLines = 1
        )
        Text(
            text = value,
            style = TextStyle(
                color = OneUiGlanceTokens.PrimaryText,
                fontSize = valueFontSize,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

suspend fun updateAllWeatherWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    val widgets = listOf(
        OneUiRainWidget(),
        OneUiAqiWidget(),
        OneUiUvWidget(),
        OneUiMoonPhaseWidget(),
        OneUiSunArcWidget(),
        OneUiStargazingWidget()
    )
    for (widget in widgets) {
        val ids = manager.getGlanceIds(widget.javaClass)
        ids.forEach { widget.update(context, it) }
    }
}

class RefreshWeatherAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val current = AppPreferences.observeWeather(context).first()
        OpenMeteoClient.fetchWeatherAndAirQuality(
            latitude = current.latitude,
            longitude = current.longitude,
            cityName = current.cityName
        ).onSuccess { updated ->
            AppPreferences.saveWeather(context, updated)
        }
        updateAllWeatherWidgets(context)
    }
}

class WeatherRefreshWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val current = AppPreferences.observeWeather(appContext).first()
        val fetched = OpenMeteoClient.fetchWeatherAndAirQuality(
            latitude = current.latitude,
            longitude = current.longitude,
            cityName = current.cityName
        )
        fetched.onSuccess { updated ->
            AppPreferences.saveWeather(appContext, updated)
            updateAllWeatherWidgets(appContext)
        }
        return if (fetched.isSuccess) Result.success() else Result.retry()
    }
}

class OneUiRainWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiRainWidget()
}

class OneUiAqiWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiAqiWidget()
}

class OneUiUvWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiUvWidget()
}

class OneUiMoonPhaseWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiMoonPhaseWidget()
}

class OneUiSunArcWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiSunArcWidget()
}

class OneUiStargazingWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiStargazingWidget()
}
