package com.anacatavc.oneuiextrawidgets.widgets.battery

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import com.anacatavc.oneuiextrawidgets.MainActivity
import com.anacatavc.oneuiextrawidgets.data.battery.BatteryThermalLevel
import com.anacatavc.oneuiextrawidgets.data.battery.BatteryThermalMonitor
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import com.anacatavc.oneuiextrawidgets.widgets.OneUiGlanceWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.WidgetInstanceHelper
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import com.anacatavc.oneuiextrawidgets.widgets.weather.WeatherModularCard
import com.anacatavc.oneuiextrawidgets.widgets.weather.calculateWeatherCardLayout
import kotlinx.coroutines.flow.first
import java.util.Locale
import kotlin.math.roundToInt

/**
 * One UI Battery Thermal & Power Widget (2×1 Default, Resizable, Live Sticky Broadcast Readout).
 */
class OneUiBatteryThermalWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = BatteryThermalMonitor.readSnapshot(context)
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val fontScaleFlow = WidgetInstanceHelper.observeWidgetFontScale(context, id)
        val initialFontScale = fontScaleFlow.first()
        val isDark = OneUiGlanceTokens.isNightModeActive(context)

        provideContent {
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val fontScaleMode by fontScaleFlow.collectAsState(initial = initialFontScale)
            val isEs = LocaleHelper.isSpanishActive()
            val size = LocalSize.current
            val layout = calculateWeatherCardLayout(size.width.value, size.height.value)
            val bitmap = remember(snapshot.temperatureC, isDark) {
                WidgetCanvasRenderer.renderBatteryThermalGaugeBitmap(
                    temperatureC = snapshot.temperatureC,
                    sizePx = 160,
                    isDarkTheme = isDark
                )
            }

            val accentColor = when (snapshot.thermalLevel) {
                BatteryThermalLevel.COOL -> OneUiGlanceTokens.BatteryThermalCool
                BatteryThermalLevel.WARM -> OneUiGlanceTokens.BatteryThermalWarm
                BatteryThermalLevel.HOT -> OneUiGlanceTokens.BatteryThermalHot
            }

            val detailIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_NETWORK)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val voltageVolts = String.format(Locale.US, "%.2f V", snapshot.voltageMv / 1000.0)
            val powerFormatted = String.format(Locale.US, "%.1f W", snapshot.powerWatts)
            val thermalLabel = snapshot.localizedThermalLabel(isEs)
            val powerSource = snapshot.localizedPowerSource(isEs)
            val header = if (isEs) "Salud Térmica" else "Battery Thermal"

            WeatherModularCard(
                context = context,
                layout = layout,
                gaugeBitmap = bitmap,
                gaugeContentDescription = "$thermalLabel ${snapshot.temperatureC.roundToInt()}°C",
                headerLabel = header,
                primaryValue = "${snapshot.levelPct}%",
                statusBadge = thermalLabel,
                statusColor = accentColor,
                pillText = powerSource,
                compactLabel = "${snapshot.temperatureC.roundToInt()}°C",
                compactColor = accentColor,
                detailLeftLabel = if (isEs) "Voltaje / Corr." else "Voltage / Curr.",
                detailLeftValue = "$voltageVolts / ${snapshot.currentMa} mA",
                detailRightLabel = if (isEs) "Potencia" else "Power Draw",
                detailRightValue = powerFormatted,
                isSpanish = isEs,
                showRefreshAction = true,
                opacityPct = opacityPct,
                fontScaleMode = fontScaleMode,
                clickIntent = detailIntent,
                refreshAction = actionRunCallback<RefreshBatteryAction>()
            )
        }
    }
}

class RefreshBatteryAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        OneUiBatteryThermalWidget().update(context, glanceId)
    }
}

class OneUiBatteryThermalWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiBatteryThermalWidget()
}
