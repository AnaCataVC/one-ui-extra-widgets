package com.anacatavc.oneuiextrawidgets.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anacatavc.oneuiextrawidgets.data.astrology.LunarPhaseStatus
import com.anacatavc.oneuiextrawidgets.data.preferences.CachedWeatherState
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiCard
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiHeader
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WeatherDetailScreen(
    weather: CachedWeatherState,
    lunarStatus: LunarPhaseStatus,
    isSpanish: Boolean,
    isRefreshing: Boolean = false,
    errorMessage: String? = null,
    onRefreshWeather: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val rainBitmap = remember(weather.rainProbabilityPct, isDark) {
        WidgetCanvasRenderer.renderRainGaugeBitmap(weather.rainProbabilityPct, sizePx = 180, isDarkTheme = isDark)
    }
    val aqiBitmap = remember(weather.europeanAqi, isDark) {
        WidgetCanvasRenderer.renderAqiGaugeBitmap(weather.europeanAqi, sizePx = 180, isDarkTheme = isDark)
    }
    val uvBitmap = remember(weather.uvIndex, isDark) {
        WidgetCanvasRenderer.renderUvGaugeBitmap(weather.uvIndex, sizePx = 180, isDarkTheme = isDark)
    }
    val moonBitmap = remember(lunarStatus.phaseAngleDeg) {
        WidgetCanvasRenderer.renderMoonPhaseBitmap(lunarStatus.phaseAngleDeg, sizePx = 180)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OneUiHeader(
            title = weather.cityName,
            subtitle = if (isSpanish) {
                "4 Widgets Ambientales y Fase Lunar Científica • Fuente Open-Meteo"
            } else {
                "4 Environmental & Scientific Lunar Widgets • Open-Meteo Source"
            }
        )

        // 1. Rain Probability Card
        OneUiCard(title = if (isSpanish) "1. Probabilidad de Lluvia (Próx. 6h)" else "1. Rain Probability (Next 6h)") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    bitmap = rainBitmap.asImageBitmap(),
                    contentDescription = if (isSpanish) "Medidor de lluvia" else "Rain gauge",
                    modifier = Modifier.size(64.dp)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBlock(
                        label = if (isSpanish) "Máx. Próx. 6h" else "Next 6h Max",
                        value = "${weather.rainProbabilityPct}%"
                    )
                    MetricBlock(
                        label = if (isSpanish) "Intensidad" else "Intensity",
                        value = when {
                            weather.rainProbabilityPct <= 10 -> if (isSpanish) "Sin lluvia" else "Dry"
                            weather.rainProbabilityPct <= 35 -> if (isSpanish) "Baja" else "Low"
                            weather.rainProbabilityPct <= 65 -> if (isSpanish) "Moderada" else "Moderate"
                            else -> if (isSpanish) "Alta" else "High"
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRefreshWeather,
                enabled = !isRefreshing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isSpanish) "Actualizando clima..." else "Refreshing weather...")
                } else {
                    Text(text = if (isSpanish) "Actualizar Clima y AQI Ahora" else "Refresh Weather & AQI Now")
                }
            }
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onRefreshWeather,
                    enabled = !isRefreshing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isSpanish) "Reintentar" else "Retry")
                }
            }
        }

        // 2. Air Quality (AQI) Card
        OneUiCard(title = if (isSpanish) "2. Calidad del Aire (AQI Europeo)" else "2. Air Quality Index (European AQI)") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    bitmap = aqiBitmap.asImageBitmap(),
                    contentDescription = "European AQI ${weather.europeanAqi}",
                    modifier = Modifier.size(64.dp)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBlock(
                        label = "European AQI",
                        value = "${weather.europeanAqi}"
                    )
                    MetricBlock(
                        label = "PM2.5",
                        value = "${weather.pm25.roundToInt()} µg/m³"
                    )
                    MetricBlock(
                        label = "PM10",
                        value = "${weather.pm10.roundToInt()} µg/m³"
                    )
                }
            }
        }

        // 3. Solar UV Index Card
        OneUiCard(title = if (isSpanish) "3. Índice UV Solar (Escala OMS)" else "3. Solar UV Index (WHO Scale)") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    bitmap = uvBitmap.asImageBitmap(),
                    contentDescription = "UV ${weather.uvIndex.roundToInt()}",
                    modifier = Modifier.size(64.dp)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBlock(
                        label = if (isSpanish) "Índice UV" else "UV Index",
                        value = "UV ${weather.uvIndex.roundToInt()}"
                    )
                    MetricBlock(
                        label = if (isSpanish) "Recomendación" else "Guidance",
                        value = when {
                            weather.uvIndex.roundToInt() <= 2 -> if (isSpanish) "Sin riesgo" else "Safe"
                            weather.uvIndex.roundToInt() <= 5 -> if (isSpanish) "FPS 30+" else "SPF 30+"
                            weather.uvIndex.roundToInt() <= 7 -> if (isSpanish) "FPS 50+" else "SPF 50+"
                            else -> if (isSpanish) "Evita sol" else "Seek shade"
                        }
                    )
                }
            }
        }

        // 4. Scientific Moon Phase Card (Zero Astrology/Zodiac)
        OneUiCard(title = if (isSpanish) "4. Fase Lunar (Astronomía Científica)" else "4. Lunar Phase (Scientific Astronomy)") {
            val ageDays = String.format(Locale.US, "%.1f d", lunarStatus.synodicAgeDays)
            val elongationDeg = "${lunarStatus.normalizedPhaseAngleDeg.roundToInt() % 360}°"
            val cycleTrend = lunarStatus.localizedCycleTrend(isSpanish)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    bitmap = moonBitmap.asImageBitmap(),
                    contentDescription = lunarStatus.localizedPhaseName(isSpanish),
                    modifier = Modifier.size(64.dp)
                )
                Column {
                    Text(
                        text = lunarStatus.localizedPhaseName(isSpanish),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSpanish) {
                            "Iluminación: ${lunarStatus.illuminationPercent}% • " +
                                "Luna en ${lunarStatus.moonSign.symbol} ${lunarStatus.moonSign.nameEs}"
                        } else {
                            "Illumination: ${lunarStatus.illuminationPercent}% • " +
                                "Moon in ${lunarStatus.moonSign.symbol} ${lunarStatus.moonSign.nameEn}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
