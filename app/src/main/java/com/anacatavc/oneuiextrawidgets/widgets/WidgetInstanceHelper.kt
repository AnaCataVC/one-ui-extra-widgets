package com.anacatavc.oneuiextrawidgets.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.anacatavc.oneuiextrawidgets.data.preferences.AppPreferences
import com.anacatavc.oneuiextrawidgets.widgets.astrology.OneUiAstrologyWidget
import com.anacatavc.oneuiextrawidgets.widgets.astrology.OneUiAstrologyWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.DiscmanWidget
import com.anacatavc.oneuiextrawidgets.widgets.music.DiscmanWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.IpodClassicWidget
import com.anacatavc.oneuiextrawidgets.widgets.music.IpodClassicWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.music.RetroMp3Widget
import com.anacatavc.oneuiextrawidgets.widgets.music.RetroMp3WidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.network.OneUiNetworkWidget
import com.anacatavc.oneuiextrawidgets.widgets.network.OneUiNetworkWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiAqiWidget
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiAqiWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiMoonPhaseWidget
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiMoonPhaseWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiRainWidget
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiRainWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiUvWidget
import com.anacatavc.oneuiextrawidgets.widgets.weather.OneUiUvWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Shared helpers for per-instance Glance widget identification, opacity observation,
 * targeted widget updates, and preference cleanup on deletion.
 */
object WidgetInstanceHelper {

    fun resolveAppWidgetId(context: Context, glanceId: GlanceId): Int =
        runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)

    fun observeWidgetOpacity(context: Context, glanceId: GlanceId): Flow<Int> {
        val appWidgetId = resolveAppWidgetId(context, glanceId)
        return AppPreferences.observeWidgetOpacity(context, appWidgetId)
    }

    fun resolveWidgetForProvider(providerClassName: String?): GlanceAppWidget? = when (providerClassName) {
        OneUiRainWidgetReceiver::class.java.name -> OneUiRainWidget()
        OneUiAqiWidgetReceiver::class.java.name -> OneUiAqiWidget()
        OneUiUvWidgetReceiver::class.java.name -> OneUiUvWidget()
        OneUiMoonPhaseWidgetReceiver::class.java.name -> OneUiMoonPhaseWidget()
        OneUiAstrologyWidgetReceiver::class.java.name -> OneUiAstrologyWidget()
        IpodClassicWidgetReceiver::class.java.name -> IpodClassicWidget()
        RetroMp3WidgetReceiver::class.java.name -> RetroMp3Widget()
        DiscmanWidgetReceiver::class.java.name -> DiscmanWidget()
        OneUiNetworkWidgetReceiver::class.java.name -> OneUiNetworkWidget()
        else -> null
    }

    suspend fun updateWidgetInstance(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val providerName = AppWidgetManager.getInstance(context)
            .getAppWidgetInfo(appWidgetId)
            ?.provider
            ?.className
        val widget = resolveWidgetForProvider(providerName)
        val glanceId = if (widget != null) {
            runCatching { GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId) }.getOrNull()
        } else {
            null
        }
        if (widget != null && glanceId != null) {
            widget.update(context, glanceId)
        }
    }
}

/**
 * Base [GlanceAppWidgetReceiver] that cleans up per-instance opacity preferences
 * when a widget instance is removed from the home screen.
 */
abstract class OneUiGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            AppPreferences.clearWidgetOpacities(appContext, appWidgetIds)
        }
    }
}
