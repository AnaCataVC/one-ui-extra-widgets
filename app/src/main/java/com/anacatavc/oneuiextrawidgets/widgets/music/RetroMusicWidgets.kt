package com.anacatavc.oneuiextrawidgets.widgets.music

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import com.anacatavc.oneuiextrawidgets.data.media.MediaNotificationListener
import com.anacatavc.oneuiextrawidgets.data.media.TransportCommand
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens

/**
 * 1. iPod Classic Widget (100% Jetpack Glance + Canvas Click Wheel)
 */
class IpodClassicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val media = MediaNotificationListener.latestState
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val isSpanish = LocaleHelper.isSpanishActive()
        val wheelBitmap = WidgetCanvasRenderer.renderIpodClickWheelBitmap(sizePx = 190, isDarkTheme = isDark)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(OneUiGlanceTokens.CornerRadius)
                    .background(OneUiGlanceTokens.IpodShellSurface)
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Retro LCD Display Screen
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(12.dp)
                        .background(OneUiGlanceTokens.IpodLcdSurface)
                        .padding(8.dp)
                ) {
                    val statusLabel = when {
                        media.isPlaying && isSpanish -> "▶ iPod  •  Reproduciendo"
                        media.isPlaying -> "▶ iPod  •  Now Playing"
                        else -> "⏸ iPod  •  One UI"
                    }
                    Text(
                        text = statusLabel,
                        style = TextStyle(
                            color = OneUiGlanceTokens.OneUiAccentBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = media.title,
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = media.artist,
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.SecondaryText,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Interactive Click Wheel with Previous / Play-Pause / Next zones
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier.size(96.dp)
                ) {
                    Image(
                        provider = ImageProvider(wheelBitmap),
                        contentDescription = "iPod Click Wheel",
                        modifier = GlanceModifier.fillMaxSize()
                    )
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .height(48.dp)
                                .semantics {
                                    contentDescription = if (isSpanish) "Pista anterior" else "Previous track"
                                }
                                .clickable(actionRunCallback<PrevTrackAction>())
                        ) {}
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .height(48.dp)
                                .semantics {
                                    contentDescription = if (media.isPlaying) {
                                        if (isSpanish) "Pausar" else "Pause"
                                    } else {
                                        if (isSpanish) "Reproducir" else "Play"
                                    }
                                }
                                .clickable(actionRunCallback<PlayPauseAction>())
                        ) {}
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .height(48.dp)
                                .semantics {
                                    contentDescription = if (isSpanish) "Siguiente pista" else "Next track"
                                }
                                .clickable(actionRunCallback<NextTrackAction>())
                        ) {}
                    }
                }
            }
        }
    }
}

/**
 * 2. 2000s Retro MP3 Backlit LCD Stick Widget (4x1 pill)
 */
class RetroMp3Widget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val media = MediaNotificationListener.latestState
        val isSpanish = LocaleHelper.isSpanishActive()

        provideContent {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(OneUiGlanceTokens.CornerRadius)
                    .background(OneUiGlanceTokens.RetroMp3ShellSurface)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Backlit LCD Screen
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(10.dp)
                        .background(OneUiGlanceTokens.RetroLcdBacklight)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "MP3 128KBPS • STEREO",
                        style = TextStyle(
                            color = OneUiGlanceTokens.RetroLcdSecondaryText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${media.title} — ${media.artist}",
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.RetroLcdPrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Tactile Silver Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RetroControlButton(
                        label = "⏮",
                        a11yLabel = if (isSpanish) "Pista anterior" else "Previous track",
                        action = actionRunCallback<PrevTrackAction>()
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    RetroControlButton(
                        label = if (media.isPlaying) "⏸" else "▶",
                        a11yLabel = if (media.isPlaying) {
                            if (isSpanish) "Pausar" else "Pause"
                        } else {
                            if (isSpanish) "Reproducir" else "Play"
                        },
                        action = actionRunCallback<PlayPauseAction>()
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    RetroControlButton(
                        label = "⏭",
                        a11yLabel = if (isSpanish) "Siguiente pista" else "Next track",
                        action = actionRunCallback<NextTrackAction>()
                    )
                }
            }
        }
    }
}

/**
 * 3. Portable CD Player (Discman) Widget with Spinning Album Art CD Window
 */
class DiscmanWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val media = MediaNotificationListener.latestState
        val isSpanish = LocaleHelper.isSpanishActive()
        val cdBitmap = WidgetCanvasRenderer.renderDiscmanCdBitmap(
            albumArt = media.albumArt,
            sizePx = 180
        )

        provideContent {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(OneUiGlanceTokens.CornerRadius)
                    .background(OneUiGlanceTokens.SurfaceBackground)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular CD Lid Window
                Image(
                    provider = ImageProvider(cdBitmap),
                    contentDescription = "Discman CD",
                    modifier = GlanceModifier
                        .size(86.dp)
                        .clickable(actionRunCallback<PlayPauseAction>())
                )

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Track Digital Readout + ESP Badge + Controls
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "DISCMAN • ESP ANTI-SHOCK",
                        style = TextStyle(
                            color = OneUiGlanceTokens.OneUiAccentBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = media.title,
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = media.artist,
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.SecondaryText,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RetroControlButton(
                            label = "⏮",
                            a11yLabel = if (isSpanish) "Pista anterior" else "Previous track",
                            action = actionRunCallback<PrevTrackAction>()
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        RetroControlButton(
                            label = if (media.isPlaying) "⏸" else "▶",
                            a11yLabel = if (media.isPlaying) {
                                if (isSpanish) "Pausar" else "Pause"
                            } else {
                                if (isSpanish) "Reproducir" else "Play"
                            },
                            action = actionRunCallback<PlayPauseAction>()
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        RetroControlButton(
                            label = "⏭",
                            a11yLabel = if (isSpanish) "Siguiente pista" else "Next track",
                            action = actionRunCallback<NextTrackAction>()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroControlButton(
    label: String,
    a11yLabel: String,
    action: androidx.glance.action.Action
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(34.dp)
            .cornerRadius(17.dp)
            .background(OneUiGlanceTokens.InnerCardSurface)
            .semantics { contentDescription = a11yLabel }
            .clickable(action)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = OneUiGlanceTokens.PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        MediaNotificationListener.sendTransportCommand(context, TransportCommand.PLAY_PAUSE)
    }
}

class NextTrackAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        MediaNotificationListener.sendTransportCommand(context, TransportCommand.NEXT)
    }
}

class PrevTrackAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        MediaNotificationListener.sendTransportCommand(context, TransportCommand.PREVIOUS)
    }
}

object RetroMusicWidgetUpdater {
    suspend fun updateAllMusicWidgets(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(IpodClassicWidget::class.java).forEach {
            IpodClassicWidget().update(context, it)
        }
        manager.getGlanceIds(RetroMp3Widget::class.java).forEach {
            RetroMp3Widget().update(context, it)
        }
        manager.getGlanceIds(DiscmanWidget::class.java).forEach {
            DiscmanWidget().update(context, it)
        }
    }
}

class IpodClassicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IpodClassicWidget()
}

class RetroMp3WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RetroMp3Widget()
}

class DiscmanWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DiscmanWidget()
}
