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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anacatavc.oneuiextrawidgets.data.battery.BatteryThermalSnapshot
import com.anacatavc.oneuiextrawidgets.data.network.NetworkQualitySnapshot
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiCard
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiHeader
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import java.util.Locale

@Composable
fun NetworkDetailScreen(
    snapshot: NetworkQualitySnapshot,
    isSpanish: Boolean,
    isRefreshing: Boolean = false,
    batterySnapshot: BatteryThermalSnapshot = BatteryThermalSnapshot(),
    onRefresh: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val batteryBitmap = remember(batterySnapshot.temperatureC, isDark) {
        WidgetCanvasRenderer.renderBatteryThermalGaugeBitmap(
            temperatureC = batterySnapshot.temperatureC,
            sizePx = 180,
            isDarkTheme = isDark
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OneUiHeader(
            title = if (isSpanish) "Diagnóstico 5G, Wi-Fi y Batería" else "5G, Wi-Fi & Battery Diagnostics",
            subtitle = "${snapshot.mobileGeneration} (${snapshot.mobileDbm} dBm) • ${snapshot.wifiSsid}"
        )

        OneUiCard(title = if (isSpanish) "Red Móvil 5G NR / LTE" else "5G NR / LTE Mobile Network") {
            NetworkInfoRow(
                label = if (isSpanish) "Operador y Tecnología" else "Carrier & Generation",
                value = "${snapshot.carrierName} (${snapshot.mobileGeneration})"
            )
            NetworkInfoRow(
                label = if (isSpanish) "Potencia RSRP / Señal" else "RSRP Signal Strength",
                value = "${snapshot.mobileDbm} dBm (${snapshot.qualityLabel(snapshot.mobileLevelBars, isSpanish)})"
            )
            NetworkInfoRow(
                label = if (isSpanish) "Nivel de Barras (0–4)" else "Signal Bars (0–4)",
                value = "${snapshot.mobileLevelBars} / 4"
            )
        }

        OneUiCard(title = if (isSpanish) "Red Inalámbrica Wi-Fi 6 / 5" else "Wi-Fi 6 / 5 Wireless Network") {
            NetworkInfoRow(
                label = "SSID",
                value = snapshot.wifiSsid
            )
            NetworkInfoRow(
                label = if (isSpanish) "Velocidad de Enlace" else "Link Speed",
                value = "${snapshot.wifiLinkSpeedMbps} Mbps (${snapshot.wifiBandLabel})"
            )
            NetworkInfoRow(
                label = if (isSpanish) "Potencia RSSI" else "RSSI Power",
                value = "${snapshot.wifiRssiDbm} dBm (${snapshot.qualityLabel(snapshot.wifiLevelBars, isSpanish)})"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRefresh,
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
                    Text(text = if (isSpanish) "Midiendo señal..." else "Measuring signal...")
                } else {
                    Text(text = if (isSpanish) "Actualizar Diagnóstico" else "Refresh Diagnostics")
                }
            }
        }

        val batteryCardTitle = if (isSpanish) {
            "Salud Térmica y Eléctrica de Batería"
        } else {
            "Battery Thermal & Electrical Health"
        }
        OneUiCard(title = batteryCardTitle) {
            val tempStr = String.format(Locale.US, "%.1f °C", batterySnapshot.temperatureC)
            val wattsStr = String.format(Locale.US, "%.1f W", batterySnapshot.powerWatts)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    bitmap = batteryBitmap.asImageBitmap(),
                    contentDescription = tempStr,
                    modifier = Modifier.size(64.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$tempStr (${batterySnapshot.localizedThermalLabel(isSpanish)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${batterySnapshot.levelPct}% • ${batterySnapshot.localizedPowerSource(isSpanish)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRow(
                label = if (isSpanish) "Voltaje y Corriente" else "Voltage & Current",
                value = "${batterySnapshot.voltageMv} mV • ${batterySnapshot.currentMa} mA"
            )
            NetworkInfoRow(
                label = if (isSpanish) "Potencia Instantánea" else "Instant Power",
                value = "$wattsStr (${batterySnapshot.localizedPowerSource(isSpanish)})"
            )
        }
    }
}

@Composable
private fun NetworkInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
