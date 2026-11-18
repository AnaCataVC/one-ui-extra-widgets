package com.anacatavc.oneuiextrawidgets.widgets.music

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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
import com.anacatavc.oneuiextrawidgets.R
import com.anacatavc.oneuiextrawidgets.data.media.ActiveMediaState
import com.anacatavc.oneuiextrawidgets.data.media.MediaNotificationListener
import com.anacatavc.oneuiextrawidgets.data.media.formatMediaTime
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import com.anacatavc.oneuiextrawidgets.widgets.OneUiGlanceWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.WidgetInstanceHelper
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import kotlinx.coroutines.flow.first

private const val CASSETTE_ASPECT_RATIO = 7f / 12f

internal enum class CassetteLayoutMode {
    COMPACT,
    TALL,
    WIDE
}

internal data class CassetteLayoutSpec(
    val mode: CassetteLayoutMode,
    val shellPaddingDp: Float,
    val deckWidthDp: Float,
    val deckHeightDp: Float,
    val buttonSizeDp: Float,
    val sideButtonSizeDp: Float
)

internal fun calculateCassetteLayout(widthDp: Float, heightDp: Float): CassetteLayoutSpec {
    val safeW = widthDp.coerceAtLeast(140f)
    val safeH = heightDp.coerceAtLeast(140f)
    val isWide = safeW > safeH * 1.25f && safeW >= 220f
    val isTall = !isWide && safeH >= 200f
    val mode = when {
        isWide -> CassetteLayoutMode.WIDE
        isTall -> CassetteLayoutMode.TALL
        else -> CassetteLayoutMode.COMPACT
    }
    val padding = if (mode == CassetteLayoutMode.COMPACT) 10f else 14f
    val innerW = (safeW - padding * 2f).coerceAtLeast(100f)
    val innerH = (safeH - padding * 2f).coerceAtLeast(100f)

    return when (mode) {
        CassetteLayoutMode.WIDE -> {
            val maxDeckH = innerH * 0.92f
            val deckW = minOf(innerW * 0.50f, maxDeckH / CASSETTE_ASPECT_RATIO).coerceIn(108f, 210f)
            val deckH = deckW * CASSETTE_ASPECT_RATIO
            val rightColW = (innerW - deckW - 10f).coerceAtLeast(88f)
            val maxBtnForWidth = ((rightColW - 12f) / 3.4f).coerceAtLeast(22f)
            val btnSize = minOf((innerH * 0.27f).coerceIn(28f, 38f), maxBtnForWidth)
            val sideBtnSize = (btnSize * 0.80f).coerceAtLeast(18f)
            CassetteLayoutSpec(
                mode = mode,
                shellPaddingDp = padding,
                deckWidthDp = deckW,
                deckHeightDp = deckH,
                buttonSizeDp = btnSize,
                sideButtonSizeDp = sideBtnSize
            )
        }
        CassetteLayoutMode.TALL -> {
            val btnSize = (innerW * 0.21f).coerceIn(26f, 42f)
            val sideBtnSize = (btnSize * 0.78f).coerceAtLeast(20f)
            val maxDeckH = (innerH * 0.44f).coerceAtLeast(64f)
            val deckW = minOf(innerW, maxDeckH / CASSETTE_ASPECT_RATIO).coerceIn(110f, 220f)
            val deckH = deckW * CASSETTE_ASPECT_RATIO
            CassetteLayoutSpec(
                mode = mode,
                shellPaddingDp = padding,
                deckWidthDp = deckW,
                deckHeightDp = deckH,
                buttonSizeDp = btnSize,
                sideButtonSizeDp = sideBtnSize
            )
        }
        CassetteLayoutMode.COMPACT -> {
            val btnSize = (minOf(innerW, innerH) * 0.21f).coerceIn(26f, 34f)
            val sideBtnSize = (btnSize * 0.82f).coerceIn(21f, 28f)
            val maxDeckH = (innerH - btnSize - 48f).coerceAtLeast(44f)
            val deckW = minOf(innerW, maxDeckH / CASSETTE_ASPECT_RATIO).coerceIn(76f, 168f)
            val deckH = deckW * CASSETTE_ASPECT_RATIO
            CassetteLayoutSpec(
                mode = mode,
                shellPaddingDp = padding,
                deckWidthDp = deckW,
                deckHeightDp = deckH,
                buttonSizeDp = btnSize,
                sideButtonSizeDp = sideBtnSize
            )
        }
    }
}

internal enum class VinylLayoutMode {
    COMPACT,
    TALL,
    WIDE
}

internal data class VinylLayoutSpec(
    val mode: VinylLayoutMode,
    val shellPaddingDp: Float,
    val platterSizeDp: Float,
    val buttonSizeDp: Float,
    val sideButtonSizeDp: Float
)

internal fun calculateVinylLayout(widthDp: Float, heightDp: Float): VinylLayoutSpec {
    val safeW = widthDp.coerceAtLeast(140f)
    val safeH = heightDp.coerceAtLeast(140f)
    val isWide = safeW > safeH * 1.25f && safeW >= 220f
    val isTall = !isWide && safeH >= 200f
    val mode = when {
        isWide -> VinylLayoutMode.WIDE
        isTall -> VinylLayoutMode.TALL
        else -> VinylLayoutMode.COMPACT
    }
    val padding = if (mode == VinylLayoutMode.COMPACT) 10f else 14f
    val innerW = (safeW - padding * 2f).coerceAtLeast(100f)
    val innerH = (safeH - padding * 2f).coerceAtLeast(100f)

    return when (mode) {
        VinylLayoutMode.WIDE -> {
            val platterSize = minOf(innerH * 0.94f, innerW * 0.46f).coerceIn(88f, 184f)
            val rightColW = (innerW - platterSize - 12f).coerceAtLeast(88f)
            val maxBtnForWidth = ((rightColW - 12f) / 3.4f).coerceAtLeast(22f)
            val btnSize = minOf((innerH * 0.25f).coerceIn(26f, 36f), maxBtnForWidth)
            val sideBtnSize = (btnSize * 0.80f).coerceAtLeast(18f)
            VinylLayoutSpec(
                mode = mode,
                shellPaddingDp = padding,
                platterSizeDp = platterSize,
                buttonSizeDp = btnSize,
                sideButtonSizeDp = sideBtnSize
            )
        }
        VinylLayoutMode.TALL -> {
            val platterSize = minOf(innerW * 0.86f, innerH * 0.44f).coerceIn(96f, 196f)
            val btnSize = (innerW * 0.21f).coerceIn(26f, 42f)
            val sideBtnSize = (btnSize * 0.78f).coerceAtLeast(20f)
            VinylLayoutSpec(
                mode = mode,
                shellPaddingDp = padding,
                platterSizeDp = platterSize,
                buttonSizeDp = btnSize,
                sideButtonSizeDp = sideBtnSize
            )
        }
        VinylLayoutMode.COMPACT -> {
            val btnSize = (minOf(innerW, innerH) * 0.21f).coerceIn(26f, 34f)
            val sideBtnSize = (btnSize * 0.82f).coerceIn(21f, 28f)
            val platterSize = minOf(innerW * 0.54f, innerH - btnSize - 48f).coerceIn(44f, 82f)
            VinylLayoutSpec(
                mode = mode,
                shellPaddingDp = padding,
                platterSizeDp = platterSize,
                buttonSizeDp = btnSize,
                sideButtonSizeDp = sideBtnSize
            )
        }
    }
}

internal enum class MiniDiscLayoutMode {
    PILL,
    STACKED
}

internal data class MiniDiscLayoutSpec(
    val mode: MiniDiscLayoutMode,
    val buttonSizeDp: Float,
    val sideButtonSizeDp: Float,
    val discBadgeSizeDp: Float,
    val showDiscBadge: Boolean,
    val showSpectrum: Boolean
)

internal fun calculateMiniDiscLayout(widthDp: Float, heightDp: Float): MiniDiscLayoutSpec {
    val safeW = widthDp.coerceAtLeast(140f)
    val safeH = heightDp.coerceAtLeast(70f)
    val isStacked = safeH >= 120f && safeW < safeH * 2.2f

    return if (isStacked) {
        val btnSize = (safeW * 0.18f).coerceIn(32f, 44f)
        val sideBtnSize = (btnSize - 8f).coerceIn(24f, 36f)
        val showBadge = safeW >= 160f && safeH >= 130f
        val badgeSize = if (showBadge) {
            minOf(safeW * 0.24f, safeH * 0.28f).coerceIn(36f, 56f)
        } else {
            0f
        }
        MiniDiscLayoutSpec(
            mode = MiniDiscLayoutMode.STACKED,
            buttonSizeDp = btnSize,
            sideButtonSizeDp = sideBtnSize,
            discBadgeSizeDp = badgeSize,
            showDiscBadge = showBadge,
            showSpectrum = safeW >= 170f
        )
    } else {
        val btnSize = if (safeW < 220f) 30f else 36f
        val sideBtnSize = if (safeW < 220f) 22f else 26f
        val showBadge = safeW >= 240f
        val badgeSize = if (showBadge) (safeH * 0.62f).coerceIn(34f, 48f) else 0f
        MiniDiscLayoutSpec(
            mode = MiniDiscLayoutMode.PILL,
            buttonSizeDp = btnSize,
            sideButtonSizeDp = sideBtnSize,
            discBadgeSizeDp = badgeSize,
            showDiscBadge = showBadge,
            showSpectrum = safeW >= 220f
        )
    }
}

/**
 * 4. Retro Music Widget: 80s/90s Cassette Walkman with Type II CrO2 label & dynamic tape spools.
 */
class CassetteWalkmanWidget : GlanceAppWidget() {

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
            val progressFraction = mediaProgressFraction(media)
            val deckBitmap = remember(isDark, media.isPlaying, opacityPct, progressFraction) {
                WidgetCanvasRenderer.renderCassetteDeckBitmap(
                    progressFraction = progressFraction,
                    sizePx = 360,
                    isDarkTheme = isDark,
                    isPlaying = media.isPlaying,
                    opacityPct = opacityPct
                )
            }
            val shellBg = OneUiGlanceTokens.cassetteShellSurface(opacityPct)
            val size = LocalSize.current
            val layout = calculateCassetteLayout(size.width.value, size.height.value)
            val systemScale = OneUiGlanceTokens.systemFontScale(context)
            val sp: (Float) -> TextUnit = { base ->
                OneUiGlanceTokens.scaledSp(
                    baseSp = base,
                    systemFontScale = systemScale,
                    mode = fontScaleMode,
                    isCompactLayout = layout.mode == CassetteLayoutMode.COMPACT
                )
            }
            val infoClickIntent = if (media.hasNotificationAccess) menuIntent else notifSettingsIntent

            if (layout.mode == CassetteLayoutMode.WIDE) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(OneUiGlanceTokens.CornerRadius)
                        .background(shellBg)
                        .padding(layout.shellPaddingDp.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CassetteDeckImage(
                        deckBitmap = deckBitmap,
                        widthDp = layout.deckWidthDp,
                        heightDp = layout.deckHeightDp,
                        isSpanish = isSpanish,
                        clickIntent = infoClickIntent
                    )
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CassetteTrackLabelCard(
                            media = media,
                            isCompact = false,
                            sp = sp,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity(infoClickIntent))
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        RetroFourButtonRow(
                            isPlaying = media.isPlaying,
                            isSpanish = isSpanish,
                            menuIntent = menuIntent,
                            buttonSizeDp = layout.buttonSizeDp,
                            sideButtonSizeDp = layout.sideButtonSizeDp,
                            surfaceColor = OneUiGlanceTokens.CassetteButtonSurface,
                            iconTint = OneUiGlanceTokens.CassetteButtonText,
                            primaryCornerRadiusDp = (layout.buttonSizeDp * 0.28f),
                            sideCornerRadiusDp = (layout.sideButtonSizeDp * 0.28f)
                        )
                    }
                }
            } else {
                val isCompact = layout.mode == CassetteLayoutMode.COMPACT
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(OneUiGlanceTokens.CornerRadius)
                        .background(shellBg)
                        .padding(layout.shellPaddingDp.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CassetteDeckImage(
                        deckBitmap = deckBitmap,
                        widthDp = layout.deckWidthDp,
                        heightDp = layout.deckHeightDp,
                        isSpanish = isSpanish,
                        clickIntent = infoClickIntent
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    CassetteTrackLabelCard(
                        media = media,
                        isCompact = isCompact,
                        sp = sp,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity(infoClickIntent))
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    RetroFourButtonRow(
                        isPlaying = media.isPlaying,
                        isSpanish = isSpanish,
                        menuIntent = menuIntent,
                        buttonSizeDp = layout.buttonSizeDp,
                        sideButtonSizeDp = layout.sideButtonSizeDp,
                        surfaceColor = OneUiGlanceTokens.CassetteButtonSurface,
                        iconTint = OneUiGlanceTokens.CassetteButtonText,
                        primaryCornerRadiusDp = (layout.buttonSizeDp * 0.28f),
                        sideCornerRadiusDp = (layout.sideButtonSizeDp * 0.28f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CassetteDeckImage(
    deckBitmap: Bitmap,
    widthDp: Float,
    heightDp: Float,
    isSpanish: Boolean,
    clickIntent: Intent
) {
    Image(
        provider = ImageProvider(deckBitmap),
        contentDescription = if (isSpanish) "Casetera Walkman" else "Cassette Walkman Deck",
        modifier = GlanceModifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clickable(actionStartActivity(clickIntent))
    )
}

@Composable
private fun CassetteTrackLabelCard(
    media: ActiveMediaState,
    isCompact: Boolean,
    sp: (Float) -> TextUnit,
    modifier: GlanceModifier
) {
    val timeSummary = "${formatMediaTime(media.positionMs)} / ${formatMediaTime(media.durationMs)}"
    Column(
        modifier = modifier
            .cornerRadius(if (isCompact) 10.dp else 12.dp)
            .background(OneUiGlanceTokens.CassetteLabelSurface)
            .padding(
                horizontal = if (isCompact) 8.dp else 10.dp,
                vertical = if (isCompact) 3.dp else 7.dp
            )
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TYPE II CrO2",
                maxLines = 1,
                style = TextStyle(
                    color = OneUiGlanceTokens.CassetteAmberAccent,
                    fontSize = sp(if (isCompact) 7.5f else 9f),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = timeSummary,
                maxLines = 1,
                style = TextStyle(
                    color = OneUiGlanceTokens.CassetteLabelSubtext,
                    fontSize = sp(if (isCompact) 7.5f else 8.5f)
                )
            )
        }
        Text(
            text = media.title,
            maxLines = 1,
            style = TextStyle(
                color = OneUiGlanceTokens.CassetteLabelText,
                fontSize = sp(if (isCompact) 10.5f else 12.5f),
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = media.artist,
            maxLines = 1,
            style = TextStyle(
                color = OneUiGlanceTokens.CassetteLabelSubtext,
                fontSize = sp(if (isCompact) 8.5f else 10f)
            )
        )
    }
}

/**
 * 5. Retro Music Widget: Braun / Dieter Rams Vinyl Turntable with LP grooves & tonearm.
 */
class VinylTurntableWidget : GlanceAppWidget() {

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
            val progressFraction = mediaProgressFraction(media)
            val turntableBitmap = remember(media.albumArt, isDark, media.isPlaying, progressFraction) {
                WidgetCanvasRenderer.renderVinylTurntableBitmap(
                    albumArt = media.albumArt,
                    progressFraction = progressFraction,
                    sizePx = 320,
                    isDarkTheme = isDark,
                    isPlaying = media.isPlaying
                )
            }
            val plinthBg = OneUiGlanceTokens.vinylPlinthSurface(opacityPct)
            val size = LocalSize.current
            val layout = calculateVinylLayout(size.width.value, size.height.value)
            val systemScale = OneUiGlanceTokens.systemFontScale(context)
            val isCompact = layout.mode == VinylLayoutMode.COMPACT
            val sp: (Float) -> TextUnit = { base ->
                OneUiGlanceTokens.scaledSp(
                    baseSp = base,
                    systemFontScale = systemScale,
                    mode = fontScaleMode,
                    isCompactLayout = isCompact
                )
            }
            val infoClickIntent = if (media.hasNotificationAccess) menuIntent else notifSettingsIntent
            val barMaxWidthDp = resolveVinylBarMaxWidth(size.width.value, layout)

            when (layout.mode) {
                VinylLayoutMode.WIDE -> {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(OneUiGlanceTokens.CornerRadius)
                            .background(plinthBg)
                            .padding(layout.shellPaddingDp.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VinylPlatterImage(
                            bitmap = turntableBitmap,
                            sizeDp = layout.platterSizeDp,
                            isSpanish = isSpanish,
                            clickIntent = infoClickIntent
                        )
                        Spacer(modifier = GlanceModifier.width(12.dp))
                        Column(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            VinylSpeedBadge(isCompact = false, sp = sp)
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            VinylReadoutCard(
                                media = media,
                                progressFraction = progressFraction,
                                barMaxWidthDp = barMaxWidthDp,
                                isCompact = false,
                                showTimestamps = true,
                                sp = sp,
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .clickable(actionStartActivity(infoClickIntent))
                            )
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            RetroFourButtonRow(
                                isPlaying = media.isPlaying,
                                isSpanish = isSpanish,
                                menuIntent = menuIntent,
                                buttonSizeDp = layout.buttonSizeDp,
                                sideButtonSizeDp = layout.sideButtonSizeDp,
                                surfaceColor = OneUiGlanceTokens.VinylControlSurface,
                                iconTint = OneUiGlanceTokens.PrimaryText,
                                primaryCornerRadiusDp = layout.buttonSizeDp / 2f,
                                sideCornerRadiusDp = layout.sideButtonSizeDp / 2f
                            )
                        }
                    }
                }
                VinylLayoutMode.TALL -> {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(OneUiGlanceTokens.CornerRadius)
                            .background(plinthBg)
                            .padding(layout.shellPaddingDp.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VinylSpeedBadge(isCompact = false, sp = sp)
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        VinylPlatterImage(
                            bitmap = turntableBitmap,
                            sizeDp = layout.platterSizeDp,
                            isSpanish = isSpanish,
                            clickIntent = infoClickIntent
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        VinylReadoutCard(
                            media = media,
                            progressFraction = progressFraction,
                            barMaxWidthDp = barMaxWidthDp,
                            isCompact = false,
                            showTimestamps = true,
                            sp = sp,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity(infoClickIntent))
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        RetroFourButtonRow(
                            isPlaying = media.isPlaying,
                            isSpanish = isSpanish,
                            menuIntent = menuIntent,
                            buttonSizeDp = layout.buttonSizeDp,
                            sideButtonSizeDp = layout.sideButtonSizeDp,
                            surfaceColor = OneUiGlanceTokens.VinylControlSurface,
                            iconTint = OneUiGlanceTokens.PrimaryText,
                            primaryCornerRadiusDp = layout.buttonSizeDp / 2f,
                            sideCornerRadiusDp = layout.sideButtonSizeDp / 2f
                        )
                    }
                }
                VinylLayoutMode.COMPACT -> {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(OneUiGlanceTokens.CornerRadius)
                            .background(plinthBg)
                            .padding(layout.shellPaddingDp.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VinylPlatterImage(
                                bitmap = turntableBitmap,
                                sizeDp = layout.platterSizeDp,
                                isSpanish = isSpanish,
                                clickIntent = infoClickIntent
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .clickable(actionStartActivity(infoClickIntent))
                            ) {
                                VinylSpeedBadge(isCompact = true, sp = sp)
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = "${formatMediaTime(media.positionMs)} / ${formatMediaTime(media.durationMs)}",
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = OneUiGlanceTokens.SecondaryText,
                                        fontSize = sp(7.5f)
                                    )
                                )
                            }
                        }
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        VinylReadoutCard(
                            media = media,
                            progressFraction = progressFraction,
                            barMaxWidthDp = barMaxWidthDp,
                            isCompact = true,
                            showTimestamps = false,
                            sp = sp,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity(infoClickIntent))
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        RetroFourButtonRow(
                            isPlaying = media.isPlaying,
                            isSpanish = isSpanish,
                            menuIntent = menuIntent,
                            buttonSizeDp = layout.buttonSizeDp,
                            sideButtonSizeDp = layout.sideButtonSizeDp,
                            surfaceColor = OneUiGlanceTokens.VinylControlSurface,
                            iconTint = OneUiGlanceTokens.PrimaryText,
                            primaryCornerRadiusDp = layout.buttonSizeDp / 2f,
                            sideCornerRadiusDp = layout.sideButtonSizeDp / 2f
                        )
                    }
                }
            }
        }
    }
}

private fun resolveVinylBarMaxWidth(widthDp: Float, layout: VinylLayoutSpec): Float {
    val safeW = widthDp.coerceAtLeast(140f)
    val innerW = (safeW - layout.shellPaddingDp * 2f).coerceAtLeast(100f)
    return when (layout.mode) {
        VinylLayoutMode.WIDE -> (innerW - layout.platterSizeDp - 32f).coerceIn(64f, 160f)
        VinylLayoutMode.TALL -> (innerW - 20f).coerceIn(80f, 200f)
        VinylLayoutMode.COMPACT -> (innerW - 18f).coerceIn(72f, 140f)
    }
}

@Composable
private fun VinylPlatterImage(
    bitmap: Bitmap,
    sizeDp: Float,
    isSpanish: Boolean,
    clickIntent: Intent
) {
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = if (isSpanish) "Tocadiscos de vinilo" else "Vinyl Turntable",
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .clickable(actionStartActivity(clickIntent))
    )
}

@Composable
private fun VinylSpeedBadge(
    isCompact: Boolean,
    sp: (Float) -> TextUnit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = GlanceModifier
                .size(if (isCompact) 5.dp else 6.dp)
                .cornerRadius(3.dp)
                .background(OneUiGlanceTokens.VinylBraunAccent)
        ) {
            Spacer(modifier = GlanceModifier.fillMaxSize())
        }
        Spacer(modifier = GlanceModifier.width(5.dp))
        Text(
            text = "33⅓ RPM • HI-FI",
            maxLines = 1,
            style = TextStyle(
                color = OneUiGlanceTokens.SecondaryText,
                fontSize = sp(if (isCompact) 7.5f else 9f),
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun VinylReadoutCard(
    media: ActiveMediaState,
    progressFraction: Float,
    barMaxWidthDp: Float,
    isCompact: Boolean,
    showTimestamps: Boolean,
    sp: (Float) -> TextUnit,
    modifier: GlanceModifier
) {
    Column(
        modifier = modifier
            .cornerRadius(if (isCompact) 10.dp else 12.dp)
            .background(OneUiGlanceTokens.VinylReadoutSurface)
            .padding(
                horizontal = if (isCompact) 8.dp else 10.dp,
                vertical = if (isCompact) 4.dp else 7.dp
            )
    ) {
        Text(
            text = media.title,
            maxLines = 1,
            style = TextStyle(
                color = OneUiGlanceTokens.PrimaryText,
                fontSize = sp(if (isCompact) 10.5f else 12.5f),
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = media.artist,
            maxLines = 1,
            style = TextStyle(
                color = OneUiGlanceTokens.SecondaryText,
                fontSize = sp(if (isCompact) 8.5f else 10f)
            )
        )
        Spacer(modifier = GlanceModifier.height(if (isCompact) 3.dp else 5.dp))
        RetroProgressBar(
            progressFraction = progressFraction,
            maxWidthDp = barMaxWidthDp,
            trackColor = OneUiGlanceTokens.VinylProgressTrack,
            fillColor = OneUiGlanceTokens.VinylBraunAccent
        )
        if (showTimestamps) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = formatMediaTime(media.positionMs),
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.SecondaryText,
                        fontSize = sp(8.5f)
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = formatMediaTime(media.durationMs),
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.SecondaryText,
                        fontSize = sp(8.5f)
                    )
                )
            }
        }
    }
}

/**
 * 6. Retro Music Widget: 2000s Sony MiniDisc Hi-MD Stick with dark EL bezel & ATRAC spectrum.
 */
class MiniDiscWidget : GlanceAppWidget() {

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
            val size = LocalSize.current
            val layout = calculateMiniDiscLayout(size.width.value, size.height.value)
            val discBitmap = remember(isDark, media.isPlaying, layout.showDiscBadge) {
                if (layout.showDiscBadge) {
                    WidgetCanvasRenderer.renderMiniDiscBadgeBitmap(
                        sizePx = 128,
                        isDarkTheme = isDark,
                        isPlaying = media.isPlaying
                    )
                } else {
                    null
                }
            }
            val shellBg = OneUiGlanceTokens.miniDiscShellSurface(opacityPct)
            val systemScale = OneUiGlanceTokens.systemFontScale(context)
            val isPill = layout.mode == MiniDiscLayoutMode.PILL
            val sp: (Float) -> TextUnit = { base ->
                OneUiGlanceTokens.scaledSp(
                    baseSp = base,
                    systemFontScale = systemScale,
                    mode = fontScaleMode,
                    isCompactLayout = isPill
                )
            }
            val infoClickIntent = if (media.hasNotificationAccess) menuIntent else notifSettingsIntent
            val progressFraction = mediaProgressFraction(media)
            val barMaxWidthDp = resolveMiniDiscBarMaxWidth(size.width.value, layout)

            if (layout.mode == MiniDiscLayoutMode.STACKED) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(OneUiGlanceTokens.CornerRadius)
                        .background(shellBg)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (discBitmap != null) {
                            Image(
                                provider = ImageProvider(discBitmap),
                                contentDescription = if (isSpanish) "Cartucho MiniDisc" else "MiniDisc Cartridge",
                                modifier = GlanceModifier
                                    .size(layout.discBadgeSizeDp.dp)
                                    .clickable(actionStartActivity(infoClickIntent))
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                        }
                        MiniDiscElBezelDisplay(
                            media = media,
                            isStacked = true,
                            showSpectrum = layout.showSpectrum,
                            progressFraction = progressFraction,
                            barMaxWidthDp = barMaxWidthDp,
                            sp = sp,
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .clickable(actionStartActivity(infoClickIntent))
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    MiniDiscTransportRow(
                        isPlaying = media.isPlaying,
                        isSpanish = isSpanish,
                        buttonSizeDp = layout.buttonSizeDp,
                        sideButtonSizeDp = layout.sideButtonSizeDp,
                        spreadEvenly = true
                    )
                }
            } else {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(OneUiGlanceTokens.CornerRadius)
                        .background(shellBg)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (discBitmap != null) {
                        Image(
                            provider = ImageProvider(discBitmap),
                            contentDescription = if (isSpanish) "Cartucho MiniDisc" else "MiniDisc Cartridge",
                            modifier = GlanceModifier
                                .size(layout.discBadgeSizeDp.dp)
                                .clickable(actionStartActivity(infoClickIntent))
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                    }
                    MiniDiscElBezelDisplay(
                        media = media,
                        isStacked = false,
                        showSpectrum = layout.showSpectrum,
                        progressFraction = progressFraction,
                        barMaxWidthDp = barMaxWidthDp,
                        sp = sp,
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .clickable(actionStartActivity(infoClickIntent))
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    MiniDiscTransportRow(
                        isPlaying = media.isPlaying,
                        isSpanish = isSpanish,
                        buttonSizeDp = layout.buttonSizeDp,
                        sideButtonSizeDp = layout.sideButtonSizeDp,
                        spreadEvenly = false
                    )
                }
            }
        }
    }
}

private fun resolveMiniDiscBarMaxWidth(widthDp: Float, layout: MiniDiscLayoutSpec): Float {
    val safeW = widthDp.coerceAtLeast(140f)
    val badgeConsume = if (layout.showDiscBadge) layout.discBadgeSizeDp + 8f else 0f
    val eqConsume = if (layout.showSpectrum) 28f else 0f
    return if (layout.mode == MiniDiscLayoutMode.STACKED) {
        (safeW - 44f - badgeConsume - eqConsume).coerceIn(40f, 160f)
    } else {
        val controlsW = layout.buttonSizeDp + layout.sideButtonSizeDp * 2f + 16f
        (safeW - 40f - badgeConsume - controlsW - eqConsume).coerceIn(40f, 160f)
    }
}

@Composable
private fun MiniDiscElBezelDisplay(
    media: ActiveMediaState,
    isStacked: Boolean,
    showSpectrum: Boolean,
    progressFraction: Float,
    barMaxWidthDp: Float,
    sp: (Float) -> TextUnit,
    modifier: GlanceModifier
) {
    Column(
        modifier = modifier
            .cornerRadius(14.dp)
            .background(OneUiGlanceTokens.MiniDiscElBezelSurface)
            .padding(horizontal = 10.dp, vertical = if (isStacked) 8.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HI-MD • ATRAC DSP",
                maxLines = 1,
                style = TextStyle(
                    color = OneUiGlanceTokens.MiniDiscHiMdAmber,
                    fontSize = sp(if (isStacked) 8.5f else 7f),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = formatMediaTime(media.positionMs, padMinutes = true),
                maxLines = 1,
                style = TextStyle(
                    color = OneUiGlanceTokens.MiniDiscElSubtext,
                    fontSize = sp(if (isStacked) 8.5f else 7f),
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(if (isStacked) 4.dp else 2.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = media.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.MiniDiscElText,
                        fontSize = sp(if (isStacked) 13f else 11.5f),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = media.artist,
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.MiniDiscElSubtext,
                        fontSize = sp(if (isStacked) 10.5f else 9f)
                    )
                )
                Spacer(modifier = GlanceModifier.height(if (isStacked) 5.dp else 3.dp))
                RetroProgressBar(
                    progressFraction = progressFraction,
                    maxWidthDp = barMaxWidthDp,
                    trackColor = OneUiGlanceTokens.MiniDiscTrackBg,
                    fillColor = OneUiGlanceTokens.MiniDiscElText
                )
            }
            if (showSpectrum) {
                Spacer(modifier = GlanceModifier.width(8.dp))
                MiniDiscSpectrumGraphic(
                    isPlaying = media.isPlaying,
                    positionMs = media.positionMs,
                    isStacked = isStacked
                )
            }
        }
    }
}

@Composable
private fun MiniDiscSpectrumGraphic(
    isPlaying: Boolean,
    positionMs: Long,
    isStacked: Boolean
) {
    val scale = if (isStacked) 1.2f else 1.0f
    val barHeights = computeEqualizerBarHeights(
        positionMs = positionMs,
        isPlaying = isPlaying,
        minHeightDp = 4f,
        maxHeightDp = 17f
    )
    val barColor = if (isPlaying) OneUiGlanceTokens.MiniDiscElText else OneUiGlanceTokens.MiniDiscTrackBg
    Row(verticalAlignment = Alignment.Bottom) {
        barHeights.forEachIndexed { index, rawHeight ->
            if (index > 0) {
                Spacer(modifier = GlanceModifier.width(2.dp))
            }
            Box(
                modifier = GlanceModifier
                    .width(2.5.dp)
                    .height((rawHeight * scale).dp)
                    .cornerRadius(1.5.dp)
                    .background(barColor)
            ) {
                Spacer(modifier = GlanceModifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun MiniDiscTransportRow(
    isPlaying: Boolean,
    isSpanish: Boolean,
    buttonSizeDp: Float,
    sideButtonSizeDp: Float,
    spreadEvenly: Boolean
) {
    val playIcon = if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
    val modifier = if (spreadEvenly) GlanceModifier.fillMaxWidth() else GlanceModifier
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (spreadEvenly) Spacer(modifier = GlanceModifier.defaultWeight())
        RetroTransportButton(
            iconRes = R.drawable.ic_media_prev,
            a11yLabel = prevTrackA11yLabel(isSpanish),
            action = actionRunCallback<PrevTrackAction>(),
            sizeDp = sideButtonSizeDp,
            surfaceColor = OneUiGlanceTokens.MiniDiscButtonSurface,
            iconTint = OneUiGlanceTokens.MiniDiscButtonText
        )
        if (spreadEvenly) {
            Spacer(modifier = GlanceModifier.defaultWeight())
        } else {
            Spacer(modifier = GlanceModifier.width(4.dp))
        }
        RetroTransportButton(
            iconRes = playIcon,
            a11yLabel = playPauseA11yLabel(isPlaying, isSpanish),
            action = actionRunCallback<PlayPauseAction>(),
            sizeDp = buttonSizeDp,
            surfaceColor = OneUiGlanceTokens.MiniDiscButtonSurface,
            iconTint = OneUiGlanceTokens.MiniDiscButtonText,
            isPrimary = true
        )
        if (spreadEvenly) {
            Spacer(modifier = GlanceModifier.defaultWeight())
        } else {
            Spacer(modifier = GlanceModifier.width(4.dp))
        }
        RetroTransportButton(
            iconRes = R.drawable.ic_media_next,
            a11yLabel = nextTrackA11yLabel(isSpanish),
            action = actionRunCallback<NextTrackAction>(),
            sizeDp = sideButtonSizeDp,
            surfaceColor = OneUiGlanceTokens.MiniDiscButtonSurface,
            iconTint = OneUiGlanceTokens.MiniDiscButtonText
        )
        if (spreadEvenly) Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun RetroFourButtonRow(
    isPlaying: Boolean,
    isSpanish: Boolean,
    menuIntent: Intent,
    buttonSizeDp: Float,
    sideButtonSizeDp: Float,
    surfaceColor: ColorProvider,
    iconTint: ColorProvider,
    primaryCornerRadiusDp: Float,
    sideCornerRadiusDp: Float
) {
    val playIcon = if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
    Row(verticalAlignment = Alignment.CenterVertically) {
        RetroTransportButton(
            iconRes = R.drawable.ic_media_prev,
            a11yLabel = prevTrackA11yLabel(isSpanish),
            action = actionRunCallback<PrevTrackAction>(),
            sizeDp = sideButtonSizeDp,
            surfaceColor = surfaceColor,
            iconTint = iconTint,
            cornerRadiusDp = sideCornerRadiusDp
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        RetroTransportButton(
            iconRes = playIcon,
            a11yLabel = playPauseA11yLabel(isPlaying, isSpanish),
            action = actionRunCallback<PlayPauseAction>(),
            sizeDp = buttonSizeDp,
            surfaceColor = surfaceColor,
            iconTint = iconTint,
            cornerRadiusDp = primaryCornerRadiusDp,
            isPrimary = true
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        RetroTransportButton(
            iconRes = R.drawable.ic_media_next,
            a11yLabel = nextTrackA11yLabel(isSpanish),
            action = actionRunCallback<NextTrackAction>(),
            sizeDp = sideButtonSizeDp,
            surfaceColor = surfaceColor,
            iconTint = iconTint,
            cornerRadiusDp = sideCornerRadiusDp
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        RetroTransportButton(
            iconRes = R.drawable.ic_media_menu,
            a11yLabel = menuA11yLabel(isSpanish),
            action = actionStartActivity(menuIntent),
            sizeDp = sideButtonSizeDp,
            surfaceColor = surfaceColor,
            iconTint = iconTint,
            cornerRadiusDp = sideCornerRadiusDp
        )
    }
}

@Composable
private fun RetroTransportButton(
    @DrawableRes iconRes: Int,
    a11yLabel: String,
    action: Action,
    sizeDp: Float,
    surfaceColor: ColorProvider,
    iconTint: ColorProvider,
    cornerRadiusDp: Float = sizeDp / 2f,
    isPrimary: Boolean = false
) {
    val iconSize = if (isPrimary) (sizeDp * 0.52f).dp else (sizeDp * 0.48f).dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .cornerRadius(cornerRadiusDp.dp)
            .background(surfaceColor)
            .semantics { contentDescription = a11yLabel }
            .clickable(action)
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = a11yLabel,
            colorFilter = ColorFilter.tint(iconTint),
            modifier = GlanceModifier.size(iconSize)
        )
    }
}

@Composable
private fun RetroProgressBar(
    progressFraction: Float,
    maxWidthDp: Float,
    trackColor: ColorProvider,
    fillColor: ColorProvider
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(3.dp)
            .cornerRadius(2.dp)
            .background(trackColor)
    ) {
        Box(
            modifier = GlanceModifier
                .width((maxWidthDp * progressFraction.coerceIn(0.05f, 1f)).dp)
                .fillMaxHeight()
                .cornerRadius(2.dp)
                .background(fillColor)
        ) {
            Spacer(modifier = GlanceModifier.fillMaxSize())
        }
    }
}

private fun mediaProgressFraction(media: ActiveMediaState, fallback: Float = 0.22f): Float =
    if (media.durationMs > 0L) {
        (media.positionMs.toFloat() / media.durationMs.toFloat()).coerceIn(0.05f, 1f)
    } else {
        fallback
    }

private fun playPauseA11yLabel(isPlaying: Boolean, isSpanish: Boolean): String =
    if (isPlaying) {
        if (isSpanish) "Pausar" else "Pause"
    } else {
        if (isSpanish) "Reproducir" else "Play"
    }

private fun prevTrackA11yLabel(isSpanish: Boolean): String =
    if (isSpanish) "Pista anterior" else "Previous track"

private fun nextTrackA11yLabel(isSpanish: Boolean): String =
    if (isSpanish) "Siguiente pista" else "Next track"

private fun menuA11yLabel(isSpanish: Boolean): String =
    if (isSpanish) "Menú" else "Menu"

class CassetteWalkmanWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CassetteWalkmanWidget()
}

class VinylTurntableWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VinylTurntableWidget()
}

class MiniDiscWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MiniDiscWidget()
}
