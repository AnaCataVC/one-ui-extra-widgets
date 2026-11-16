package com.anacatavc.oneuiextrawidgets.widgets.astrology

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.anacatavc.oneuiextrawidgets.MainActivity
import com.anacatavc.oneuiextrawidgets.data.astrology.AstrologyEngine
import com.anacatavc.oneuiextrawidgets.data.astrology.AstrologySnapshot
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens

class OneUiAstrologyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = AstrologyEngine.computeSnapshot()
        provideContent {
            val isEs = LocaleHelper.isSpanishActive()
            AstrologyWidgetContent(context = context, snapshot = snapshot, isSpanish = isEs)
        }
    }

    @Composable
    private fun AstrologyWidgetContent(
        context: Context,
        snapshot: AstrologySnapshot,
        isSpanish: Boolean
    ) {
        val detailIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_ASTROLOGY)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val mercury = snapshot.mercuryStatus
        val retrogradePlanets = snapshot.planets.filter { it.isRetrograde }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(OneUiGlanceTokens.CornerRadius)
                .background(OneUiGlanceTokens.SurfaceBackground)
                .padding(OneUiGlanceTokens.CardPadding)
                .clickable(actionStartActivity(detailIntent))
        ) {
            // Header: Mercury Status Banner
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(OneUiGlanceTokens.InnerRadius)
                    .background(OneUiGlanceTokens.InnerCardSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    val mercuryHeadline = if (mercury.isRetrograde) {
                        if (isSpanish) "☿ Mercurio Retrógrado Activo" else "☿ Mercury Retrograde Active"
                    } else {
                        "☿ ${mercury.localizedName(isEs = isSpanish)}: ${mercury.motionState.localizedBadge(isSpanish)}"
                    }
                    Text(
                        text = mercuryHeadline,
                        style = TextStyle(
                            color = if (mercury.isRetrograde) {
                                OneUiGlanceTokens.RetroAlertAmber
                            } else {
                                OneUiGlanceTokens.PrimaryText
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val retroSummary = if (retrogradePlanets.isEmpty()) {
                        if (isSpanish) "Todos los planetas personales directos" else "All personal planets direct"
                    } else {
                        val names = retrogradePlanets.joinToString(", ") { "${it.symbol} ${it.localizedName(isSpanish)}" }
                        if (isSpanish) "Retrógrados hoy: $names" else "Retrograde today: $names"
                    }
                    Text(
                        text = retroSummary,
                        style = TextStyle(
                            color = OneUiGlanceTokens.SecondaryText,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Bottom Row: Sun Sign, Moon Transit Sign & Lunar Phase
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                AstroPill(
                    label = if (isSpanish) "Sol en" else "Sun in",
                    value = "${snapshot.sunSign.symbol} ${snapshot.sunSign.localizedName(isSpanish)}",
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                AstroPill(
                    label = if (isSpanish) "Luna hoy en" else "Moon in",
                    value = "${snapshot.lunarStatus.moonSign.symbol} ${snapshot.lunarStatus.moonSign.localizedName(isSpanish)}",
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                AstroPill(
                    label = if (isSpanish) "Fase Lunar" else "Moon Phase",
                    value = "${snapshot.lunarStatus.illuminationPercent}%",
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }

    @Composable
    private fun AstroPill(
        label: String,
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
                text = label,
                style = TextStyle(
                    color = OneUiGlanceTokens.SecondaryText,
                    fontSize = 10.sp
                )
            )
            Text(
                text = value,
                style = TextStyle(
                    color = OneUiGlanceTokens.PrimaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class OneUiAstrologyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiAstrologyWidget()
}
