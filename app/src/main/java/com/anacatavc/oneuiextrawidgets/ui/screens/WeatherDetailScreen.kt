package com.anacatavc.oneuiextrawidgets.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anacatavc.oneuiextrawidgets.data.astrology.LunarPhaseStatus
import com.anacatavc.oneuiextrawidgets.data.preferences.CachedWeatherState
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiCard
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiHeader
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import kotlin.math.roundToInt

@Composable
fun WeatherDetailScreen(
    weather: CachedWeatherState,
    lunarStatus: LunarPhaseStatus,
    isSpanish: Boolean,
    onRefreshWeather: () -> Unit
) {
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
            title = "${weather.cityName} • ${weather.temperatureC.roundToInt()}°C",
            subtitle = if (isSpanish) {
                "Sensación térmica ${weather.feelsLikeC.roundToInt()}°C • Fuente Open-Meteo"
            } else {
                "Feels like ${weather.feelsLikeC.roundToInt()}°C • Open-Meteo Source"
            }
        )

        OneUiCard(title = if (isSpanish) "Probabilidad de Lluvia y UV" else "Rain Probability & UV") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricBlock(
                    label = if (isSpanish) "Probabilidad de Lluvia" else "Rain Probability",
                    value = "${weather.rainProbabilityPct}%"
                )
                MetricBlock(
                    label = if (isSpanish) "Índice UV" else "UV Index",
                    value = "${weather.uvIndex}"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRefreshWeather, modifier = Modifier.fillMaxWidth()) {
                Text(text = if (isSpanish) "Actualizar Clima y AQI Ahora" else "Refresh Weather & AQI Now")
            }
        }

        OneUiCard(title = if (isSpanish) "Calidad del Aire (AQI)" else "Air Quality Index (AQI)") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricBlock(
                    label = "European AQI",
                    value = "${weather.europeanAqi}"
                )
                MetricBlock(
                    label = "PM2.5",
                    value = "${weather.pm25} µg/m³"
                )
                MetricBlock(
                    label = "PM10",
                    value = "${weather.pm10} µg/m³"
                )
            }
        }

        OneUiCard(title = if (isSpanish) "Fase de la Luna Hoy" else "Today's Moon Phase") {
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSpanish) {
                            "Iluminación: ${lunarStatus.illuminationPercent}% • Luna en ${lunarStatus.moonSign.symbol} ${lunarStatus.moonSign.nameEs}"
                        } else {
                            "Illumination: ${lunarStatus.illuminationPercent}% • Moon in ${lunarStatus.moonSign.symbol} ${lunarStatus.moonSign.nameEn}"
                        },
                        fontSize = 13.sp,
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
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
