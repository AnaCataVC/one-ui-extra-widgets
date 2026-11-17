package com.anacatavc.oneuiextrawidgets.widgets.network

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import androidx.glance.unit.ColorProvider
import com.anacatavc.oneuiextrawidgets.MainActivity
import com.anacatavc.oneuiextrawidgets.data.network.NetworkQualityMonitor
import com.anacatavc.oneuiextrawidgets.data.network.NetworkQualitySnapshot
import com.anacatavc.oneuiextrawidgets.widgets.OneUiGlanceWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.WidgetInstanceHelper
import kotlinx.coroutines.flow.first
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens

internal enum class NetworkLayoutMode {
    COMPACT_PILL,
    SQUARE,
    EXPANDED
}

internal data class NetworkLayoutSpec(
    val mode: NetworkLayoutMode,
    val cardPaddingDp: Float,
    val innerPaddingDp: Float,
    val iconSizeDp: Float,
    val showHeaderTitle: Boolean,
    val showExtendedDetails: Boolean
)

internal fun calculateNetworkLayout(widthDp: Float, heightDp: Float): NetworkLayoutSpec {
    val safeW = widthDp.coerceAtLeast(110f)
    val safeH = heightDp.coerceAtLeast(64f)

    val mode = when {
        safeH < 125f -> NetworkLayoutMode.COMPACT_PILL
        safeW >= 240f -> NetworkLayoutMode.EXPANDED
        else -> NetworkLayoutMode.SQUARE
    }

    return when (mode) {
        NetworkLayoutMode.COMPACT_PILL -> {
            val padding = 10f
            val innerH = (safeH - padding * 2f).coerceAtLeast(44f)
            val iconSize = (innerH * 0.56f).coerceIn(24f, 36f)
            NetworkLayoutSpec(
                mode = mode,
                cardPaddingDp = padding,
                innerPaddingDp = 8f,
                iconSizeDp = iconSize,
                showHeaderTitle = false,
                showExtendedDetails = safeW >= 240f
            )
        }
        NetworkLayoutMode.SQUARE -> {
            val padding = 12f
            val innerMin = (minOf(safeW, safeH) - padding * 2f).coerceAtLeast(96f)
            val iconSize = (innerMin * 0.28f).coerceIn(32f, 40f)
            NetworkLayoutSpec(
                mode = mode,
                cardPaddingDp = padding,
                innerPaddingDp = 9f,
                iconSizeDp = iconSize,
                showHeaderTitle = true,
                showExtendedDetails = safeH >= 150f
            )
        }
        NetworkLayoutMode.EXPANDED -> {
            val padding = 14f
            val innerMin = (minOf(safeW * 0.5f, safeH) - padding * 2f).coerceAtLeast(100f)
            val iconSize = (innerMin * 0.42f).coerceIn(40f, 52f)
            NetworkLayoutSpec(
                mode = mode,
                cardPaddingDp = padding,
                innerPaddingDp = 12f,
                iconSizeDp = iconSize,
                showHeaderTitle = true,
                showExtendedDetails = true
            )
        }
    }
}

private fun signalAccentColor(bars: Int, isConnected: Boolean): ColorProvider {
    if (!isConnected || bars <= 0) return OneUiGlanceTokens.SecondaryText
    return when (bars.coerceIn(0, 4)) {
        4 -> OneUiGlanceTokens.WeatherGoodGreen
        3 -> OneUiGlanceTokens.OneUiAccentBlue
        2 -> OneUiGlanceTokens.WeatherModerateAmber
        else -> OneUiGlanceTokens.WeatherDangerRed
    }
}

class OneUiNetworkWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = NetworkQualityMonitor.readSnapshot(context)
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val isDark = OneUiGlanceTokens.isNightModeActive(context)

        provideContent {
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val isEs = LocaleHelper.isSpanishActive()
            val size = LocalSize.current
            val layout = calculateNetworkLayout(size.width.value, size.height.value)

            val mobileBitmap = WidgetCanvasRenderer.renderCellularSignalBitmap(
                bars = snapshot.mobileLevelBars,
                isConnected = snapshot.isMobileConnected,
                sizePx = 128,
                isDarkTheme = isDark
            )
            val wifiBitmap = WidgetCanvasRenderer.renderWifiSignalBitmap(
                bars = snapshot.wifiLevelBars,
                isConnected = snapshot.isWifiConnected,
                sizePx = 128,
                isDarkTheme = isDark
            )

            NetworkWidgetContent(
                context = context,
                snapshot = snapshot,
                layout = layout,
                mobileBitmap = mobileBitmap,
                wifiBitmap = wifiBitmap,
                isSpanish = isEs,
                opacityPct = opacityPct
            )
        }
    }

    @Composable
    private fun NetworkWidgetContent(
        context: Context,
        snapshot: NetworkQualitySnapshot,
        layout: NetworkLayoutSpec,
        mobileBitmap: Bitmap,
        wifiBitmap: Bitmap,
        isSpanish: Boolean,
        opacityPct: Int
    ) {
        val detailIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_NETWORK)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val surfaceBg = OneUiGlanceTokens.surfaceBackground(opacityPct)
        val innerBg = OneUiGlanceTokens.innerCardSurface(opacityPct)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(OneUiGlanceTokens.CornerRadius)
                .background(surfaceBg)
                .padding(layout.cardPaddingDp.dp)
                .clickable(actionStartActivity(detailIntent))
        ) {
            if (layout.showHeaderTitle) {
                HeaderBar(
                    isSpanish = isSpanish,
                    compact = layout.mode == NetworkLayoutMode.SQUARE
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
            }

            when (layout.mode) {
                NetworkLayoutMode.COMPACT_PILL -> {
                    CompactPillBody(
                        snapshot = snapshot,
                        layout = layout,
                        mobileBitmap = mobileBitmap,
                        wifiBitmap = wifiBitmap,
                        isSpanish = isSpanish,
                        innerSurface = innerBg
                    )
                }
                NetworkLayoutMode.SQUARE -> {
                    SquareBody(
                        snapshot = snapshot,
                        layout = layout,
                        mobileBitmap = mobileBitmap,
                        wifiBitmap = wifiBitmap,
                        isSpanish = isSpanish,
                        innerSurface = innerBg
                    )
                }
                NetworkLayoutMode.EXPANDED -> {
                    ExpandedBody(
                        snapshot = snapshot,
                        layout = layout,
                        mobileBitmap = mobileBitmap,
                        wifiBitmap = wifiBitmap,
                        isSpanish = isSpanish,
                        innerSurface = innerBg
                    )
                }
            }
        }
    }

    @Composable
    private fun HeaderBar(isSpanish: Boolean, compact: Boolean) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val title = when {
                compact && isSpanish -> "Señal • 5G y Wi-Fi"
                compact -> "Signal • 5G & Wi-Fi"
                isSpanish -> "Calidad de Red • One UI"
                else -> "Network Quality • One UI"
            }
            Text(
                text = title,
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
                style = TextStyle(
                    color = OneUiGlanceTokens.SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            RefreshHitButton(isSpanish = isSpanish)
        }
    }

    @Composable
    private fun RefreshHitButton(isSpanish: Boolean) {
        Box(
            modifier = GlanceModifier
                .size(26.dp)
                .cornerRadius(13.dp)
                .semantics {
                    contentDescription = if (isSpanish) "Actualizar red" else "Refresh network"
                }
                .clickable(actionRunCallback<RefreshNetworkAction>()),
            contentAlignment = Alignment.Center
        ) {
            Spacer(modifier = GlanceModifier.fillMaxSize())
            Text(
                text = "↻",
                style = TextStyle(
                    color = OneUiGlanceTokens.OneUiAccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun CompactPillBody(
        snapshot: NetworkQualitySnapshot,
        layout: NetworkLayoutSpec,
        mobileBitmap: Bitmap,
        wifiBitmap: Bitmap,
        isSpanish: Boolean,
        innerSurface: ColorProvider
    ) {
        val mobileColor = signalAccentColor(snapshot.mobileLevelBars, snapshot.isMobileConnected)
        val wifiColor = signalAccentColor(snapshot.wifiLevelBars, snapshot.isWifiConnected)
        val iconDp = layout.iconSizeDp.dp

        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .cornerRadius(OneUiGlanceTokens.InnerRadius)
                    .background(innerSurface)
                    .padding(layout.innerPaddingDp.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(mobileBitmap),
                    contentDescription = snapshot.mobileGeneration,
                    modifier = GlanceModifier.size(iconDp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = snapshot.mobileGeneration,
                        maxLines = 1,
                        style = TextStyle(
                            color = mobileColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (snapshot.isMobileConnected) {
                            "${snapshot.mobileDbm} dBm"
                        } else {
                            if (isSpanish) "Sin SIM" else "No SIM"
                        },
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .cornerRadius(OneUiGlanceTokens.InnerRadius)
                    .background(innerSurface)
                    .padding(layout.innerPaddingDp.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(wifiBitmap),
                    contentDescription = snapshot.wifiSsid,
                    modifier = GlanceModifier.size(iconDp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = snapshot.wifiBandLabel,
                        maxLines = 1,
                        style = TextStyle(
                            color = wifiColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (snapshot.isWifiConnected) {
                            "${snapshot.wifiLinkSpeedMbps} Mbps"
                        } else {
                            if (isSpanish) "Inactivo" else "Offline"
                        },
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(4.dp))
            RefreshHitButton(isSpanish = isSpanish)
        }
    }

    @Composable
    private fun SquareBody(
        snapshot: NetworkQualitySnapshot,
        layout: NetworkLayoutSpec,
        mobileBitmap: Bitmap,
        wifiBitmap: Bitmap,
        isSpanish: Boolean,
        innerSurface: ColorProvider
    ) {
        val mobileColor = signalAccentColor(snapshot.mobileLevelBars, snapshot.isMobileConnected)
        val wifiColor = signalAccentColor(snapshot.wifiLevelBars, snapshot.isWifiConnected)
        val iconDp = layout.iconSizeDp.dp
        val mobileQuality = if (snapshot.isMobileConnected) {
            snapshot.qualityLabel(snapshot.mobileLevelBars, isSpanish)
        } else {
            if (isSpanish) "Sin SIM" else "No SIM"
        }
        val wifiQuality = if (snapshot.isWifiConnected) {
            snapshot.qualityLabel(snapshot.wifiLevelBars, isSpanish)
        } else {
            if (isSpanish) "Inactivo" else "Offline"
        }

        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Cellular / 5G Row Card
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .cornerRadius(OneUiGlanceTokens.InnerRadius)
                    .background(innerSurface)
                    .padding(layout.innerPaddingDp.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(mobileBitmap),
                    contentDescription = snapshot.mobileGeneration,
                    modifier = GlanceModifier.size(iconDp)
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = snapshot.mobileGeneration,
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                color = mobileColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = mobileQuality,
                            maxLines = 1,
                            style = TextStyle(
                                color = mobileColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Text(
                        text = if (snapshot.isMobileConnected) {
                            "${snapshot.mobileDbm} dBm"
                        } else {
                            if (isSpanish) "Desconectado" else "Disconnected"
                        },
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (layout.showExtendedDetails) {
                        Text(
                            text = snapshot.carrierName,
                            maxLines = 1,
                            style = TextStyle(
                                color = OneUiGlanceTokens.SecondaryText,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Wi-Fi Row Card
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .cornerRadius(OneUiGlanceTokens.InnerRadius)
                    .background(innerSurface)
                    .padding(layout.innerPaddingDp.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(wifiBitmap),
                    contentDescription = snapshot.wifiSsid,
                    modifier = GlanceModifier.size(iconDp)
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = snapshot.wifiSsid,
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                color = wifiColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = wifiQuality,
                            maxLines = 1,
                            style = TextStyle(
                                color = wifiColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Text(
                        text = if (snapshot.isWifiConnected) {
                            "${snapshot.wifiLinkSpeedMbps} Mbps"
                        } else {
                            if (isSpanish) "Inactivo" else "Offline"
                        },
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (layout.showExtendedDetails) {
                        Text(
                            text = "${snapshot.wifiBandLabel} • ${snapshot.wifiRssiDbm} dBm",
                            maxLines = 1,
                            style = TextStyle(
                                color = OneUiGlanceTokens.SecondaryText,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ExpandedBody(
        snapshot: NetworkQualitySnapshot,
        layout: NetworkLayoutSpec,
        mobileBitmap: Bitmap,
        wifiBitmap: Bitmap,
        isSpanish: Boolean,
        innerSurface: ColorProvider
    ) {
        val mobileColor = signalAccentColor(snapshot.mobileLevelBars, snapshot.isMobileConnected)
        val wifiColor = signalAccentColor(snapshot.wifiLevelBars, snapshot.isWifiConnected)
        val iconDp = layout.iconSizeDp.dp
        val mobileQuality = if (snapshot.isMobileConnected) {
            snapshot.qualityLabel(snapshot.mobileLevelBars, isSpanish)
        } else {
            if (isSpanish) "Sin SIM" else "No SIM"
        }
        val wifiQuality = if (snapshot.isWifiConnected) {
            snapshot.qualityLabel(snapshot.wifiLevelBars, isSpanish)
        } else {
            if (isSpanish) "Inactivo" else "Offline"
        }

        Row(modifier = GlanceModifier.fillMaxSize()) {
            // 5G / Cellular Expanded Card
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .cornerRadius(OneUiGlanceTokens.InnerRadius)
                    .background(innerSurface)
                    .padding(layout.innerPaddingDp.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(mobileBitmap),
                        contentDescription = snapshot.mobileGeneration,
                        modifier = GlanceModifier.size(iconDp)
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = snapshot.mobileGeneration,
                            maxLines = 1,
                            style = TextStyle(
                                color = mobileColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = mobileQuality,
                            maxLines = 1,
                            style = TextStyle(
                                color = mobileColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = if (snapshot.isMobileConnected) {
                        "${snapshot.mobileDbm} dBm"
                    } else {
                        if (isSpanish) "Desconectado" else "Disconnected"
                    },
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.PrimaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = snapshot.carrierName,
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.SecondaryText,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Wi-Fi 6 Expanded Card
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .cornerRadius(OneUiGlanceTokens.InnerRadius)
                    .background(innerSurface)
                    .padding(layout.innerPaddingDp.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(wifiBitmap),
                        contentDescription = snapshot.wifiSsid,
                        modifier = GlanceModifier.size(iconDp)
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = snapshot.wifiSsid,
                            maxLines = 1,
                            style = TextStyle(
                                color = wifiColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = wifiQuality,
                            maxLines = 1,
                            style = TextStyle(
                                color = wifiColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = if (snapshot.isWifiConnected) {
                        "${snapshot.wifiLinkSpeedMbps} Mbps"
                    } else {
                        if (isSpanish) "Inactivo" else "Offline"
                    },
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.PrimaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${snapshot.wifiBandLabel} • ${snapshot.wifiRssiDbm} dBm",
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.SecondaryText,
                        fontSize = 10.sp
                    )
                )
            }
        }
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

class OneUiNetworkWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiNetworkWidget()
}
