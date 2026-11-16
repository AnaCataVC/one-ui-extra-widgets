package com.anacatavc.oneuiextrawidgets.widgets.weather

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anacatavc.oneuiextrawidgets.MainActivity
import com.anacatavc.oneuiextrawidgets.R
import com.anacatavc.oneuiextrawidgets.data.astrology.AstrologyEngine
import com.anacatavc.oneuiextrawidgets.data.preferences.AppPreferences
import com.anacatavc.oneuiextrawidgets.data.preferences.CachedWeatherState
import com.anacatavc.oneuiextrawidgets.data.weather.OpenMeteoClient
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

class OneUiWeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weatherFlow = AppPreferences.observeWeather(context)
        val initialWeather = weatherFlow.first()
        val astroSnapshot = AstrologyEngine.computeSnapshot()
        val moonBitmap = WidgetCanvasRenderer.renderMoonPhaseBitmap(
            phaseAngleDeg = astroSnapshot.lunarStatus.phaseAngleDeg,
            sizePx = 120
        )

        provideContent {
            val state by weatherFlow.collectAsState(initial = initialWeather)
            val isEs = LocaleHelper.isSpanishActive()

            WeatherWidgetContent(
                context = context,
                state = state,
                moonBitmap = moonBitmap,
                moonPhaseLabel = astroSnapshot.lunarStatus.localizedPhaseName(isEs),
                moonIllumPct = astroSnapshot.lunarStatus.illuminationPercent,
                isSpanish = isEs
            )
        }
    }

    @Composable
    private fun WeatherWidgetContent(
        context: Context,
        state: CachedWeatherState,
        moonBitmap: android.graphics.Bitmap,
        moonPhaseLabel: String,
        moonIllumPct: Int,
        isSpanish: Boolean
    ) {
        val detailIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_WEATHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(OneUiGlanceTokens.CornerRadius)
                .background(OneUiGlanceTokens.SurfaceBackground)
                .padding(OneUiGlanceTokens.CardPadding)
                .clickable(actionStartActivity(detailIntent))
        ) {
            // Top Row: City, Temperature & Refresh Button
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = state.cityName,
                        style = TextStyle(
                            color = OneUiGlanceTokens.SecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "${state.temperatureC.roundToInt()}°C",
                        style = TextStyle(
                            color = OneUiGlanceTokens.PrimaryText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Moon Phase Badge with custom Canvas sphere
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier
                        .cornerRadius(OneUiGlanceTokens.InnerRadius)
                        .background(OneUiGlanceTokens.InnerCardSurface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable(actionRunCallback<RefreshWeatherAction>())
                ) {
                    Image(
                        provider = ImageProvider(moonBitmap),
                        contentDescription = moonPhaseLabel,
                        modifier = GlanceModifier.size(28.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Column {
                        Text(
                            text = moonPhaseLabel,
                            style = TextStyle(
                                color = OneUiGlanceTokens.PrimaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "$moonIllumPct% ↻",
                            style = TextStyle(
                                color = OneUiGlanceTokens.SecondaryText,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Bottom Row: Rain Probability, Air Quality (AQI) & UV Index Pills
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                MetricPill(
                    title = if (isSpanish) "Lluvia" else "Rain",
                    value = "${state.rainProbabilityPct}%",
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                MetricPill(
                    title = "AQI (${aqiCategory(context, state.europeanAqi)})",
                    value = "${state.europeanAqi}",
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                MetricPill(
                    title = "UV / PM2.5",
                    value = "${state.uvIndex.roundToInt()} / ${state.pm25.roundToInt()}",
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }

    @Composable
    private fun MetricPill(
        title: String,
        value: String,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Column(
            modifier = modifier
                .cornerRadius(OneUiGlanceTokens.InnerRadius)
                .background(OneUiGlanceTokens.InnerCardSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = OneUiGlanceTokens.SecondaryText,
                    fontSize = 10.sp
                )
            )
            Text(
                text = value,
                style = TextStyle(
                    color = OneUiGlanceTokens.OneUiAccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    private fun aqiCategory(context: Context, aqi: Int): String = when {
        aqi <= 20 -> context.getString(R.string.aqi_good)
        aqi <= 40 -> context.getString(R.string.aqi_fair)
        aqi <= 60 -> context.getString(R.string.aqi_moderate)
        aqi <= 80 -> context.getString(R.string.aqi_poor)
        else -> context.getString(R.string.aqi_very_poor)
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
        OneUiWeatherWidget().update(context, glanceId)
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
            val manager = GlanceAppWidgetManager(appContext)
            val ids = manager.getGlanceIds(OneUiWeatherWidget::class.java)
            ids.forEach { OneUiWeatherWidget().update(appContext, it) }
        }
        return if (fetched.isSuccess) Result.success() else Result.retry()
    }
}

class OneUiWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiWeatherWidget()
}
