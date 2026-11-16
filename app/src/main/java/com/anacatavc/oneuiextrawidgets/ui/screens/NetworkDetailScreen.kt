package com.anacatavc.oneuiextrawidgets.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anacatavc.oneuiextrawidgets.data.network.NetworkQualitySnapshot
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiCard
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiHeader

@Composable
fun NetworkDetailScreen(
    snapshot: NetworkQualitySnapshot,
    isSpanish: Boolean,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OneUiHeader(
            title = if (isSpanish) "Diagnóstico 5G y Wi-Fi" else "5G & Wi-Fi Diagnostics",
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
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text(text = if (isSpanish) "Actualizar Medición de Red" else "Refresh Network Metrics")
            }
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
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
