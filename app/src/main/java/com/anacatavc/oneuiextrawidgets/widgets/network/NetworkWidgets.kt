package com.anacatavc.oneuiextrawidgets.widgets.network

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.anacatavc.oneuiextrawidgets.MainActivity
import com.anacatavc.oneuiextrawidgets.data.network.NetworkQualityMonitor
import com.anacatavc.oneuiextrawidgets.data.network.NetworkQualitySnapshot
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens

class OneUiNetworkWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = NetworkQualityMonitor.readSnapshot(context)
        provideContent {
            val isEs = LocaleHelper.isSpanishActive()
            NetworkWidgetContent(context = context, snapshot = snapshot, isSpanish = isEs)
        }
    }

    @Composable
    private fun NetworkWidgetContent(
        context: Context,
        snapshot: NetworkQualitySnapshot,
        isSpanish: Boolean
    ) {
        val detailIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_NETWORK)
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
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSpanish) "Calidad de Red • One UI" else "Network Quality • One UI",
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = OneUiGlanceTokens.SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "↻",
                    modifier = GlanceModifier
                        .semantics {
                            contentDescription = if (isSpanish) "Actualizar red" else "Refresh network"
                        }
                        .clickable(actionRunCallback<RefreshNetworkAction>())
                        .padding(4.dp),
                    style = TextStyle(
                        color = OneUiGlanceTokens.OneUiAccentBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                // 5G / Cellular Card
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(OneUiGlanceTokens.InnerRadius)
                        .background(OneUiGlanceTokens.InnerCardSurface)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${snapshot.mobileGeneration} • ${snapshot.carrierName}",
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.OneUiAccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${snapshot.mobileDbm} dBm",
                        style = TextStyle(
                            color = OneUiGlanceTokens.PrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${barsVisual(snapshot.mobileLevelBars)} ${snapshot.qualityLabel(snapshot.mobileLevelBars, isSpanish)}",
                        style = TextStyle(
                            color = OneUiGlanceTokens.SecondaryText,
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Wi-Fi 6 Card
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(OneUiGlanceTokens.InnerRadius)
                        .background(OneUiGlanceTokens.InnerCardSurface)
                        .padding(10.dp)
                ) {
                    Text(
                        text = snapshot.wifiSsid,
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.OneUiAccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val speedOrState = if (snapshot.isWifiConnected) {
                        "${snapshot.wifiLinkSpeedMbps} Mbps"
                    } else {
                        if (isSpanish) "Inactivo" else "Offline"
                    }
                    Text(
                        text = speedOrState,
                        style = TextStyle(
                            color = OneUiGlanceTokens.PrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${snapshot.wifiBandLabel} • ${snapshot.wifiRssiDbm} dBm",
                        style = TextStyle(
                            color = OneUiGlanceTokens.SecondaryText,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }

    private fun barsVisual(bars: Int): String {
        val filled = "▂▄▆█".take(bars.coerceIn(0, 4))
        val empty = "····".take((4 - bars).coerceIn(0, 4))
        return filled + empty
    }
}

class RefreshNetworkAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        OneUiNetworkWidget().update(context, glanceId)
    }
}

class OneUiNetworkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiNetworkWidget()
}
