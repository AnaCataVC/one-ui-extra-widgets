package com.anacatavc.oneuiextrawidgets.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anacatavc.oneuiextrawidgets.data.astrology.AstrologySnapshot
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiCard
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiHeader
import java.util.Locale

@Composable
fun AstrologyDetailScreen(
    snapshot: AstrologySnapshot,
    isSpanish: Boolean
) {
    val mercury = snapshot.mercuryStatus

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OneUiHeader(
            title = if (mercury.isRetrograde) {
                if (isSpanish) "☿ Mercurio está Retrógrado" else "☿ Mercury is Retrograde"
            } else {
                if (isSpanish) "☿ Mercurio está Directo" else "☿ Mercury is Direct"
            },
            subtitle = "${mercury.motionState.localizedBadge(isSpanish)} • ${mercury.zodiacSign.symbol} ${mercury.zodiacSign.localizedName(isSpanish)}"
        )

        OneUiCard(title = if (isSpanish) "Sol y Luna Hoy" else "Sun & Moon Today") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isSpanish) "Signo Solar Actual" else "Current Sun Sign",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${snapshot.sunSign.symbol} ${snapshot.sunSign.localizedName(isSpanish)} (${String.format(Locale.US, "%.1f", snapshot.sunDegreeInSign)}°)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isSpanish) "Tránsito Lunar" else "Moon Transit",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${snapshot.lunarStatus.moonSign.symbol} ${snapshot.lunarStatus.moonSign.localizedName(isSpanish)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        OneUiCard(
            title = if (isSpanish) {
                "Estado Planetario (Mercurio, Venus, Marte, Júpiter, Saturno)"
            } else {
                "Planetary Status (Mercury, Venus, Mars, Jupiter, Saturn)"
            }
        ) {
            snapshot.planets.forEach { planet ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${planet.symbol} ${planet.localizedName(isSpanish)} — ${planet.zodiacSign.symbol} ${planet.zodiacSign.localizedName(isSpanish)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", planet.degreeInSign)}° • Δ ${String.format(Locale.US, "%+.2f", planet.dailySpeedDeg)}°/día",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = planet.motionState.localizedBadge(isSpanish),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (planet.isRetrograde) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    }
}
