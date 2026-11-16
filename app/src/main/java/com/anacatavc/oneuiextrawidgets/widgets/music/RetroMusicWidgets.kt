package com.anacatavc.oneuiextrawidgets.widgets.music

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.color.ColorProvider
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.anacatavc.oneuiextrawidgets.data.media.MediaNotificationListener
import com.anacatavc.oneuiextrawidgets.data.media.TransportCommand
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens

/**
 * 1. iPod Classic Widget (100% Jetpack Glance + Canvas Click Wheel)
 */
class IpodClassicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val media = MediaNotificationListener.latestState
        val wheelBitmap = WidgetCanvasRenderer.renderIpodClickWheelBitmap(sizePx = 190, isDarkTheme = true)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(OneUiGlanceTokens.CornerRadius)
                    .background(ColorProvider(day = Color(0xFFEAECEF), night = Color(0xFF1F2024)))
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Retro LCD Display Screen
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(12.dp)
                        .background(ColorProvider(day = Color(0xFFD6E4E8), night = Color(0xFF121820)))
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (media.isPlaying) "▶ iPod  •  Now Playing" else "⏸ iPod  •  One UI",
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
                                .clickable(actionRunCallback<PrevTrackAction>())
                        ) {}
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .height(48.dp)
                                .clickable(actionRunCallback<PlayPauseAction>())
                        ) {}
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .height(48.dp)
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

        provideContent {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(OneUiGlanceTokens.CornerRadius)
                    .background(ColorProvider(day = Color(0xFF282C34), night = Color(0xFF15171C)))
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
                            color = ColorProvider(day = Color(0xFF0F290F), night = Color(0xFF092218)),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${media.title} — ${media.artist}",
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(day = Color(0xFF081C08), night = Color(0xFF04140E)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Tactile Silver Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RetroControlButton("⏮", actionRunCallback<PrevTrackAction>())
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    RetroControlButton(if (media.isPlaying) "⏸" else "▶", actionRunCallback<PlayPauseAction>())
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    RetroControlButton("⏭", actionRunCallback<NextTrackAction>())
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
                        RetroControlButton("⏮", actionRunCallback<PrevTrackAction>())
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        RetroControlButton(if (media.isPlaying) "⏸" else "▶", actionRunCallback<PlayPauseAction>())
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        RetroControlButton("⏭", actionRunCallback<NextTrackAction>())
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroControlButton(label: String, action: androidx.glance.action.Action) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(34.dp)
            .cornerRadius(17.dp)
            .background(OneUiGlanceTokens.InnerCardSurface)
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
