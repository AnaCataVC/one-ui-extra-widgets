package com.anacatavc.oneuiextrawidgets.widgets.music

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import androidx.glance.layout.ContentScale
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
import com.anacatavc.oneuiextrawidgets.R
import com.anacatavc.oneuiextrawidgets.data.media.ActiveMediaState
import com.anacatavc.oneuiextrawidgets.data.media.MediaNotificationListener
import com.anacatavc.oneuiextrawidgets.data.media.TransportCommand
import com.anacatavc.oneuiextrawidgets.data.preferences.WidgetFontScaleMode
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import com.anacatavc.oneuiextrawidgets.widgets.OneUiGlanceWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.WidgetInstanceHelper
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import kotlinx.coroutines.flow.first

internal enum class IpodLayoutMode {
    COMPACT,
    TALL,
    WIDE
}

internal data class IpodLayoutSpec(
    val mode: IpodLayoutMode,
    val shellPaddingDp: Float,
    val lcdHeightDp: Float,
    val wheelSizeDp: Float
)

internal fun calculateIpodLayout(widthDp: Float, heightDp: Float): IpodLayoutSpec {
    val safeW = widthDp.coerceAtLeast(140f)
    val safeH = heightDp.coerceAtLeast(140f)

    val isWide = safeW > safeH * 1.25f && safeW >= 220f
    val isTall = !isWide && safeH >= 200f

    val mode = when {
        isWide -> IpodLayoutMode.WIDE
        isTall -> IpodLayoutMode.TALL
        else -> IpodLayoutMode.COMPACT
    }

    val padding = if (mode == IpodLayoutMode.COMPACT) 10f else 14f
    val innerW = (safeW - padding * 2f).coerceAtLeast(100f)
    val innerH = (safeH - padding * 2f).coerceAtLeast(100f)

    return when (mode) {
        IpodLayoutMode.WIDE -> {
            val wheelSize = minOf(innerH * 0.92f, innerW * 0.48f).coerceIn(88f, 180f)
            IpodLayoutSpec(
                mode = mode,
                shellPaddingDp = padding,
                lcdHeightDp = innerH,
                wheelSizeDp = wheelSize
            )
        }
        IpodLayoutMode.TALL -> {
            val lcdHeight = (innerH * 0.34f).coerceIn(68f, 136f)
            val lowerSpace = (innerH - lcdHeight - 12f).coerceAtLeast(96f)
            val wheelSize = minOf(innerW * 0.88f, lowerSpace * 0.84f).coerceIn(96f, 190f)
            IpodLayoutSpec(
                mode = mode,
                shellPaddingDp = padding,
                lcdHeightDp = lcdHeight,
                wheelSizeDp = wheelSize
            )
        }
        IpodLayoutMode.COMPACT -> {
            val lowerSpace = (innerH - 64f).coerceAtLeast(72f)
            val wheelSize = minOf(innerW * 0.82f, lowerSpace * 0.94f).coerceIn(72f, 128f)
            IpodLayoutSpec(
                mode = mode,
                shellPaddingDp = padding,
                lcdHeightDp = 0f,
                wheelSizeDp = wheelSize
            )
        }
    }
}

private fun buildMenuIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

private fun buildNotificationAccessIntent(): Intent =
    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

/**
 * 1. iPod Classic Widget (100% Jetpack Glance + Canvas Click Wheel + Album Art)
 */
class IpodClassicWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        MediaNotificationListener.refreshActiveMediaSession(context)
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val fontScaleFlow = WidgetInstanceHelper.observeWidgetFontScale(context, id)
        val initialFontScale = fontScaleFlow.first()
        val isDark = OneUiGlanceTokens.isNightModeActive(context)
        val isSpanish = LocaleHelper.isSpanishActive()
        val wheelBitmap = WidgetCanvasRenderer.renderIpodClickWheelBitmap(
            sizePx = 360,
            isDarkTheme = isDark
        )
        val menuIntent = buildMenuIntent(context)
        val notifSettingsIntent = buildNotificationAccessIntent()

        provideContent {
            val media by MediaNotificationListener.mediaState.collectAsState()
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val fontScaleMode by fontScaleFlow.collectAsState(initial = initialFontScale)
            val shellBg = OneUiGlanceTokens.ipodShellSurface(opacityPct)
            val size = LocalSize.current
            val layout = calculateIpodLayout(size.width.value, size.height.value)
            val systemScale = OneUiGlanceTokens.systemFontScale(context)
            val sp: (Float) -> TextUnit = { base ->
                OneUiGlanceTokens.scaledSp(
                    baseSp = base,
                    systemFontScale = systemScale,
                    mode = fontScaleMode,
                    isCompactLayout = layout.mode == IpodLayoutMode.COMPACT
                )
            }
            val lcdClickIntent = if (media.hasNotificationAccess) menuIntent else notifSettingsIntent

            if (layout.mode == IpodLayoutMode.WIDE) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(OneUiGlanceTokens.CornerRadius)
                        .background(shellBg)
                        .padding(layout.shellPaddingDp.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IpodLcdScreen(
                        media = media,
                        isSpanish = isSpanish,
                        mode = layout.mode,
                        sp = sp,
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .clickable(actionStartActivity(lcdClickIntent))
                    )
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    IpodClickWheel(
                        wheelBitmap = wheelBitmap,
                        wheelSizeDp = layout.wheelSizeDp,
                        isPlaying = media.isPlaying,
                        isSpanish = isSpanish,
                        menuIntent = menuIntent
                    )
                }
            } else {
                val lcdModifier = if (layout.mode == IpodLayoutMode.TALL) {
                    GlanceModifier.fillMaxWidth().height(layout.lcdHeightDp.dp)
                } else {
                    GlanceModifier.fillMaxWidth()
                }
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(OneUiGlanceTokens.CornerRadius)
                        .background(shellBg)
                        .padding(layout.shellPaddingDp.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IpodLcdScreen(
                        media = media,
                        isSpanish = isSpanish,
                        mode = layout.mode,
                        sp = sp,
                        modifier = lcdModifier.clickable(actionStartActivity(lcdClickIntent))
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    IpodClickWheel(
                        wheelBitmap = wheelBitmap,
                        wheelSizeDp = layout.wheelSizeDp,
                        isPlaying = media.isPlaying,
                        isSpanish = isSpanish,
                        menuIntent = menuIntent
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun IpodLcdScreen(
    media: ActiveMediaState,
    isSpanish: Boolean,
    mode: IpodLayoutMode,
    sp: (Float) -> TextUnit,
    modifier: GlanceModifier
) {
    val isExpanded = mode != IpodLayoutMode.COMPACT
    val artSizeDp = when (mode) {
        IpodLayoutMode.WIDE -> 58.dp
        IpodLayoutMode.TALL -> 44.dp
        IpodLayoutMode.COMPACT -> 32.dp
    }

    Column(
        modifier = modifier
            .cornerRadius(12.dp)
            .background(OneUiGlanceTokens.IpodLcdSurface)
            .padding(if (isExpanded) 10.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val statusLabel = when {
            !media.hasNotificationAccess && isSpanish -> "iPod  •  Sin permiso"
            !media.hasNotificationAccess -> "iPod  •  Sync needed"
            media.isPlaying && isSpanish -> "iPod  •  Reproduciendo"
            media.isPlaying -> "iPod  •  Now Playing"
            isSpanish -> "iPod  •  En pausa"
            else -> "iPod  •  Standby"
        }
        Text(
            text = statusLabel,
            maxLines = 1,
            style = TextStyle(
                color = OneUiGlanceTokens.OneUiAccentBlue,
                fontSize = sp(if (isExpanded) 11f else 9f),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(if (isExpanded) 6.dp else 4.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val artBitmap = media.albumArt
            if (artBitmap != null && !artBitmap.isRecycled) {
                Image(
                    provider = ImageProvider(artBitmap),
                    contentDescription = media.album.ifBlank { media.title },
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier
                        .size(artSizeDp)
                        .cornerRadius(6.dp)
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(artSizeDp)
                        .cornerRadius(6.dp)
                        .background(OneUiGlanceTokens.InnerCardSurface)
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_note),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(OneUiGlanceTokens.SecondaryText),
                        modifier = GlanceModifier.size(artSizeDp * 0.55f)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(if (isExpanded) 9.dp else 7.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = media.title,
                    maxLines = if (mode == IpodLayoutMode.WIDE) 2 else 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.PrimaryText,
                        fontSize = sp(if (isExpanded) 14f else 12f),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = media.artist,
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.SecondaryText,
                        fontSize = sp(if (isExpanded) 12f else 10f)
                    )
                )
                if (mode == IpodLayoutMode.WIDE && media.album.isNotBlank()) {
                    Text(
                        text = media.album,
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.SecondaryText,
                            fontSize = sp(10f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun IpodClickWheel(
    wheelBitmap: Bitmap,
    wheelSizeDp: Float,
    isPlaying: Boolean,
    isSpanish: Boolean,
    menuIntent: Intent
) {
    val playPauseLabel = if (isPlaying) {
        if (isSpanish) "Pausar" else "Pause"
    } else {
        if (isSpanish) "Reproducir" else "Play"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.size(wheelSizeDp.dp)
    ) {
        Image(
            provider = ImageProvider(wheelBitmap),
            contentDescription = "iPod Click Wheel",
            modifier = GlanceModifier.fillMaxSize()
        )
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Top zone: MENU
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .semantics {
                        contentDescription = if (isSpanish) "Menú" else "Menu"
                    }
                    .clickable(actionStartActivity(menuIntent))
            ) {
                Spacer(modifier = GlanceModifier.fillMaxSize())
            }

            // Middle row: Previous | Center Select / Play-Pause | Next
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .semantics {
                            contentDescription = if (isSpanish) "Pista anterior" else "Previous track"
                        }
                        .clickable(actionRunCallback<PrevTrackAction>())
                ) {
                    Spacer(modifier = GlanceModifier.fillMaxSize())
                }
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .semantics {
                            contentDescription = playPauseLabel
                        }
                        .clickable(actionRunCallback<PlayPauseAction>())
                ) {
                    Spacer(modifier = GlanceModifier.fillMaxSize())
                }
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .semantics {
                            contentDescription = if (isSpanish) "Siguiente pista" else "Next track"
                        }
                        .clickable(actionRunCallback<NextTrackAction>())
                ) {
                    Spacer(modifier = GlanceModifier.fillMaxSize())
                }
            }

            // Bottom zone: Play / Pause
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .semantics {
                        contentDescription = playPauseLabel
                    }
                    .clickable(actionRunCallback<PlayPauseAction>())
            ) {
                Spacer(modifier = GlanceModifier.fillMaxSize())
            }
        }
    }
}

internal enum class RetroMp3LayoutMode {
    PILL,
    STACKED
}

internal data class RetroMp3LayoutSpec(
    val mode: RetroMp3LayoutMode,
    val buttonSizeDp: Float,
    val sideButtonSizeDp: Float,
    val showUsbCap: Boolean,
    val usbCapWidthDp: Float,
    val showEqualizer: Boolean
)

internal fun calculateMp3Layout(widthDp: Float, heightDp: Float): RetroMp3LayoutSpec {
    val safeW = widthDp.coerceAtLeast(140f)
    val safeH = heightDp.coerceAtLeast(70f)
    val isStacked = safeH >= 120f && safeW < safeH * 2.2f
    return if (isStacked) {
        val btnSize = (safeW * 0.18f).coerceIn(36f, 46f)
        val sideBtnSize = (btnSize - 8f).coerceAtLeast(28f)
        RetroMp3LayoutSpec(
            mode = RetroMp3LayoutMode.STACKED,
            buttonSizeDp = btnSize,
            sideButtonSizeDp = sideBtnSize,
            showUsbCap = false,
            usbCapWidthDp = 0f,
            showEqualizer = safeW >= 170f
        )
    } else {
        val btnSize = if (safeW < 220f) 32f else 38f
        val sideBtnSize = if (safeW < 220f) 24f else 28f
        val showCap = safeW >= 240f
        val capWidth = if (safeW >= 300f) 38f else if (showCap) 30f else 0f
        val showEq = safeW >= 220f
        RetroMp3LayoutSpec(
            mode = RetroMp3LayoutMode.PILL,
            buttonSizeDp = btnSize,
            sideButtonSizeDp = sideBtnSize,
            showUsbCap = showCap,
            usbCapWidthDp = capWidth,
            showEqualizer = showEq
        )
    }
}

internal data class DiscmanLayoutSpec(
    val discSizeDp: Float,
    val pillWidthDp: Float,
    val pillHeightDp: Float,
    val buttonSizeDp: Float
)

internal fun calculateDiscmanLayout(widthDp: Float, heightDp: Float): DiscmanLayoutSpec {
    val safeW = widthDp.coerceAtLeast(140f)
    val safeH = heightDp.coerceAtLeast(140f)
    val discSize = minOf(safeW, safeH).coerceIn(140f, 320f)
    val pillWidth = discSize * 0.74f
    val pillHeight = (discSize * 0.21f).coerceIn(36f, 72f)
    val btnSize = (pillWidth * 0.135f).coerceIn(22f, 36f)
    return DiscmanLayoutSpec(
        discSizeDp = discSize,
        pillWidthDp = pillWidth,
        pillHeightDp = pillHeight,
        buttonSizeDp = btnSize
    )
}

/**
 * 2. 2000s Retro MP3 Backlit Cyan LCD USB Stick Widget (4x1 pill, responsive when stretched)
 */
class RetroMp3Widget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        MediaNotificationListener.refreshActiveMediaSession(context)
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val fontScaleFlow = WidgetInstanceHelper.observeWidgetFontScale(context, id)
        val initialFontScale = fontScaleFlow.first()
        val isSpanish = LocaleHelper.isSpanishActive()
        val menuIntent = buildMenuIntent(context)
        val notifSettingsIntent = buildNotificationAccessIntent()

        provideContent {
            val media by MediaNotificationListener.mediaState.collectAsState()
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val fontScaleMode by fontScaleFlow.collectAsState(initial = initialFontScale)
            val shellBg = OneUiGlanceTokens.retroMp3ShellSurface(opacityPct)
            val seamColor = OneUiGlanceTokens.retroMp3CapSeam(opacityPct)
            val size = LocalSize.current
            val layout = calculateMp3Layout(size.width.value, size.height.value)
            val systemScale = OneUiGlanceTokens.systemFontScale(context)
            val sp: (Float) -> TextUnit = { base ->
                OneUiGlanceTokens.scaledSp(
                    baseSp = base,
                    systemFontScale = systemScale,
                    mode = fontScaleMode,
                    isCompactLayout = layout.mode == RetroMp3LayoutMode.PILL
                )
            }
            val playPauseA11y = if (media.isPlaying) {
                if (isSpanish) "Pausar" else "Pause"
            } else {
                if (isSpanish) "Reproducir" else "Play"
            }
            val playPauseIcon = if (media.isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
            val lcdClickIntent = if (media.hasNotificationAccess) menuIntent else notifSettingsIntent

            if (layout.mode == RetroMp3LayoutMode.STACKED) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(OneUiGlanceTokens.CornerRadius)
                        .background(shellBg)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Mp3BezelDisplay(
                        media = media,
                        isStacked = true,
                        showEqualizer = layout.showEqualizer,
                        sp = sp,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .clickable(actionStartActivity(lcdClickIntent))
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Mp3SilverButton(
                            iconRes = R.drawable.ic_media_prev,
                            a11yLabel = if (isSpanish) "Pista anterior" else "Previous track",
                            action = actionRunCallback<PrevTrackAction>(),
                            sizeDp = layout.sideButtonSizeDp
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Mp3SilverButton(
                            iconRes = playPauseIcon,
                            a11yLabel = playPauseA11y,
                            action = actionRunCallback<PlayPauseAction>(),
                            sizeDp = layout.buttonSizeDp,
                            isPrimary = true
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Mp3SilverButton(
                            iconRes = R.drawable.ic_media_next,
                            a11yLabel = if (isSpanish) "Siguiente pista" else "Next track",
                            action = actionRunCallback<NextTrackAction>(),
                            sizeDp = layout.sideButtonSizeDp
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                    }
                }
            } else {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(OneUiGlanceTokens.CornerRadius)
                        .background(shellBg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .padding(
                                start = 12.dp,
                                top = 8.dp,
                                bottom = 8.dp,
                                end = if (layout.showUsbCap) 10.dp else 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Mp3SilverButton(
                                iconRes = R.drawable.ic_media_prev,
                                a11yLabel = if (isSpanish) "Pista anterior" else "Previous track",
                                action = actionRunCallback<PrevTrackAction>(),
                                sizeDp = layout.sideButtonSizeDp
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Mp3SilverButton(
                                iconRes = playPauseIcon,
                                a11yLabel = playPauseA11y,
                                action = actionRunCallback<PlayPauseAction>(),
                                sizeDp = layout.buttonSizeDp,
                                isPrimary = true
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Mp3SilverButton(
                                iconRes = R.drawable.ic_media_next,
                                a11yLabel = if (isSpanish) "Siguiente pista" else "Next track",
                                action = actionRunCallback<NextTrackAction>(),
                                sizeDp = layout.sideButtonSizeDp
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(10.dp))

                        Mp3BezelDisplay(
                            media = media,
                            isStacked = false,
                            showEqualizer = layout.showEqualizer,
                            sp = sp,
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .clickable(actionStartActivity(lcdClickIntent))
                        )
                    }

                    if (layout.showUsbCap) {
                        Mp3UsbCapSection(
                            capWidthDp = layout.usbCapWidthDp,
                            seamColor = seamColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Mp3BezelDisplay(
    media: ActiveMediaState,
    isStacked: Boolean,
    showEqualizer: Boolean,
    sp: (Float) -> TextUnit,
    modifier: GlanceModifier
) {
    Column(
        modifier = modifier
            .cornerRadius(14.dp)
            .background(OneUiGlanceTokens.RetroMp3BezelSurface)
            .padding(horizontal = 10.dp, vertical = if (isStacked) 8.dp else 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DIGITAL MP3",
                maxLines = 1,
                style = TextStyle(
                    color = OneUiGlanceTokens.RetroMp3GoldLabel,
                    fontSize = sp(if (isStacked) 9f else 7.5f),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "USB",
                maxLines = 1,
                style = TextStyle(
                    color = OneUiGlanceTokens.RetroMp3GoldLabel,
                    fontSize = sp(if (isStacked) 9f else 7.5f),
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(if (isStacked) 5.dp else 2.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = media.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.RetroLcdBacklight,
                        fontSize = sp(if (isStacked) 14f else 12f),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = media.artist,
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.RetroMp3ScreenSubtext,
                        fontSize = sp(if (isStacked) 11f else 9.5f)
                    )
                )
                Spacer(modifier = GlanceModifier.height(if (isStacked) 6.dp else 4.dp))
                Mp3ScreenProgressBar(isPlaying = media.isPlaying)
            }

            if (showEqualizer) {
                Spacer(modifier = GlanceModifier.width(8.dp))
                Mp3EqualizerGraphic(
                    isPlaying = media.isPlaying,
                    isStacked = isStacked
                )
            }
        }
    }
}

@Composable
private fun Mp3ScreenProgressBar(isPlaying: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(3.dp)
            .cornerRadius(2.dp)
            .background(OneUiGlanceTokens.RetroMp3TrackBg)
    ) {
        Box(
            modifier = GlanceModifier
                .width(if (isPlaying) 56.dp else 28.dp)
                .fillMaxHeight()
                .cornerRadius(2.dp)
                .background(OneUiGlanceTokens.RetroLcdBacklight)
        ) {}
    }
}

@Composable
private fun Mp3EqualizerGraphic(
    isPlaying: Boolean,
    isStacked: Boolean
) {
    val scale = if (isStacked) 1.2f else 1.0f
    val barHeights = listOf(8f, 15f, 10f, 18f, 11f)
    Row(verticalAlignment = Alignment.Bottom) {
        barHeights.forEachIndexed { index, rawHeight ->
            if (index > 0) {
                Spacer(modifier = GlanceModifier.width(2.dp))
            }
            val barColor = if (isPlaying) {
                OneUiGlanceTokens.RetroLcdBacklight
            } else {
                OneUiGlanceTokens.RetroMp3EqIdle
            }
            Box(
                modifier = GlanceModifier
                    .width(2.5.dp)
                    .height((rawHeight * scale).dp)
                    .cornerRadius(1.5.dp)
                    .background(barColor)
            ) {}
        }
    }
}

@Composable
private fun Mp3UsbCapSection(
    capWidthDp: Float,
    seamColor: ColorProvider
) {
    Row(
        modifier = GlanceModifier
            .width(capWidthDp.dp)
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(2.dp)
                .fillMaxHeight()
                .background(seamColor)
        ) {}
        Row(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(end = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(18.dp)
                    .cornerRadius(2.dp)
                    .background(seamColor)
            ) {}
        }
    }
}

@Composable
private fun Mp3SilverButton(
    @DrawableRes iconRes: Int,
    a11yLabel: String,
    action: androidx.glance.action.Action,
    sizeDp: Float,
    isPrimary: Boolean = false
) {
    val iconSize = if (isPrimary) (sizeDp * 0.52f).dp else (sizeDp * 0.48f).dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .cornerRadius((sizeDp / 2f).dp)
            .background(OneUiGlanceTokens.RetroMp3SilverSurface)
            .semantics { contentDescription = a11yLabel }
            .clickable(action)
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = a11yLabel,
            colorFilter = ColorFilter.tint(OneUiGlanceTokens.RetroMp3SilverText),
            modifier = GlanceModifier.size(iconSize)
        )
    }
}

/**
 * 3. Portable CD Player (Discman) Circular Widget with Cyan LCD & Central Pill Controls
 */
class DiscmanWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        MediaNotificationListener.refreshActiveMediaSession(context)
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val fontScaleFlow = WidgetInstanceHelper.observeWidgetFontScale(context, id)
        val initialFontScale = fontScaleFlow.first()
        val isDark = OneUiGlanceTokens.isNightModeActive(context)
        val isSpanish = LocaleHelper.isSpanishActive()
        val menuIntent = buildMenuIntent(context)
        val notifSettingsIntent = buildNotificationAccessIntent()

        provideContent {
            val media by MediaNotificationListener.mediaState.collectAsState()
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val fontScaleMode by fontScaleFlow.collectAsState(initial = initialFontScale)
            val discmanBitmap = remember(isDark, opacityPct) {
                WidgetCanvasRenderer.renderDiscmanBodyBitmap(
                    sizePx = 440,
                    isDarkTheme = isDark,
                    opacityPct = opacityPct
                )
            }
            val size = LocalSize.current
            val layout = calculateDiscmanLayout(size.width.value, size.height.value)
            val playPauseA11y = if (media.isPlaying) {
                if (isSpanish) "Pausar" else "Pause"
            } else {
                if (isSpanish) "Reproducir" else "Play"
            }
            val playPauseIcon = if (media.isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
            val compactMode = layout.discSizeDp < 190f
            val lcdClickIntent = if (media.hasNotificationAccess) menuIntent else notifSettingsIntent
            val systemScale = OneUiGlanceTokens.systemFontScale(context)
            val sp: (Float) -> TextUnit = { base ->
                OneUiGlanceTokens.scaledSp(
                    baseSp = base,
                    systemFontScale = systemScale,
                    mode = fontScaleMode,
                    isCompactLayout = compactMode
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier.size(layout.discSizeDp.dp)
                ) {
                    Image(
                        provider = ImageProvider(discmanBitmap),
                        contentDescription = "Discman CD Player",
                        modifier = GlanceModifier.fillMaxSize()
                    )

                    // Central Pill Control Module
                    Row(
                        modifier = GlanceModifier
                            .width(layout.pillWidthDp.dp)
                            .height(layout.pillHeightDp.dp)
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DiscmanPillButton(
                            iconRes = R.drawable.ic_media_prev,
                            a11yLabel = if (isSpanish) "Pista anterior" else "Previous track",
                            action = actionRunCallback<PrevTrackAction>(),
                            sizeDp = layout.buttonSizeDp
                        )
                        if (!compactMode) {
                            Spacer(modifier = GlanceModifier.width(3.dp))
                            DiscmanPillButton(
                                iconRes = R.drawable.ic_media_menu,
                                a11yLabel = if (isSpanish) "Menú" else "Menu",
                                action = actionStartActivity(menuIntent),
                                sizeDp = layout.buttonSizeDp
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(4.dp))

                        // Cyan Backlit LCD Display
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .cornerRadius(6.dp)
                                .background(OneUiGlanceTokens.RetroLcdBacklight)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                .clickable(actionStartActivity(lcdClickIntent)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (media.isPlaying) "CD • PLAY" else "CD • PAUSE",
                                maxLines = 1,
                                style = TextStyle(
                                    color = OneUiGlanceTokens.RetroLcdSecondaryText,
                                    fontSize = sp(if (compactMode) 7f else 8f),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = media.title,
                                maxLines = 1,
                                style = TextStyle(
                                    color = OneUiGlanceTokens.RetroLcdPrimaryText,
                                    fontSize = sp(if (compactMode) 9f else 11f),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (!compactMode) {
                                Text(
                                    text = media.artist,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = OneUiGlanceTokens.RetroLcdSecondaryText,
                                        fontSize = sp(9f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(4.dp))
                        DiscmanPillButton(
                            iconRes = playPauseIcon,
                            a11yLabel = playPauseA11y,
                            action = actionRunCallback<PlayPauseAction>(),
                            sizeDp = layout.buttonSizeDp
                        )
                        Spacer(modifier = GlanceModifier.width(3.dp))
                        DiscmanPillButton(
                            iconRes = R.drawable.ic_media_next,
                            a11yLabel = if (isSpanish) "Siguiente pista" else "Next track",
                            action = actionRunCallback<NextTrackAction>(),
                            sizeDp = layout.buttonSizeDp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscmanPillButton(
    @DrawableRes iconRes: Int,
    a11yLabel: String,
    action: androidx.glance.action.Action,
    sizeDp: Float
) {
    val iconSize = (sizeDp * 0.54f).dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .cornerRadius((sizeDp / 2f).dp)
            .background(OneUiGlanceTokens.InnerCardSurface)
            .semantics { contentDescription = a11yLabel }
            .clickable(action)
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = a11yLabel,
            colorFilter = ColorFilter.tint(OneUiGlanceTokens.PrimaryText),
            modifier = GlanceModifier.size(iconSize)
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

class IpodClassicWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IpodClassicWidget()
}

class RetroMp3WidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RetroMp3Widget()
}

class DiscmanWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DiscmanWidget()
}
