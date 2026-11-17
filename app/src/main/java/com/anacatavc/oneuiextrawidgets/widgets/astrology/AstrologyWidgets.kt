package com.anacatavc.oneuiextrawidgets.widgets.astrology

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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.anacatavc.oneuiextrawidgets.MainActivity
import com.anacatavc.oneuiextrawidgets.data.astrology.AstrologyEngine
import com.anacatavc.oneuiextrawidgets.data.astrology.AstrologySnapshot
import com.anacatavc.oneuiextrawidgets.data.astrology.PlanetaryMotionState
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.utils.WidgetCanvasRenderer
import com.anacatavc.oneuiextrawidgets.widgets.OneUiGlanceWidgetReceiver
import com.anacatavc.oneuiextrawidgets.widgets.WidgetInstanceHelper
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import io.github.cosinekitty.astronomy.Body
import kotlinx.coroutines.flow.first
import java.util.Locale

internal enum class AstrologyLayoutMode {
    COMPACT,
    STANDARD,
    EXPANDED
}

internal data class AstrologyLayoutSpec(
    val mode: AstrologyLayoutMode,
    val cardPaddingDp: Float,
    val emblemSizeDp: Float,
    val signBadgeSizeDp: Float,
    val showDegrees: Boolean,
    val showPlanetStrip: Boolean,
    val showPlanetMotionBadge: Boolean
)

internal fun calculateAstrologyLayout(widthDp: Float, heightDp: Float): AstrologyLayoutSpec {
    val safeW = if (widthDp.isNaN()) 130f else widthDp.coerceAtLeast(130f)
    val safeH = if (heightDp.isNaN()) 100f else heightDp.coerceAtLeast(100f)

    val mode = when {
        safeW < 220f || safeH < 120f -> AstrologyLayoutMode.COMPACT
        safeH >= 195f -> AstrologyLayoutMode.EXPANDED
        else -> AstrologyLayoutMode.STANDARD
    }

    return when (mode) {
        AstrologyLayoutMode.COMPACT -> AstrologyLayoutSpec(
            mode = mode,
            cardPaddingDp = 12f,
            emblemSizeDp = 34f,
            signBadgeSizeDp = 24f,
            showDegrees = false,
            showPlanetStrip = false,
            showPlanetMotionBadge = false
        )
        AstrologyLayoutMode.STANDARD -> AstrologyLayoutSpec(
            mode = mode,
            cardPaddingDp = 14f,
            emblemSizeDp = 42f,
            signBadgeSizeDp = 30f,
            showDegrees = true,
            showPlanetStrip = safeH >= 145f,
            showPlanetMotionBadge = false
        )
        AstrologyLayoutMode.EXPANDED -> AstrologyLayoutSpec(
            mode = mode,
            cardPaddingDp = 16f,
            emblemSizeDp = 48f,
            signBadgeSizeDp = 34f,
            showDegrees = true,
            showPlanetStrip = true,
            showPlanetMotionBadge = true
        )
    }
}

class OneUiAstrologyWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = AstrologyEngine.computeSnapshot()
        val opacityFlow = WidgetInstanceHelper.observeWidgetOpacity(context, id)
        val initialOpacity = opacityFlow.first()
        val isDark = OneUiGlanceTokens.isNightModeActive(context)

        provideContent {
            val opacityPct by opacityFlow.collectAsState(initial = initialOpacity)
            val isEs = LocaleHelper.isSpanishActive()
            val size = LocalSize.current
            val layout = calculateAstrologyLayout(size.width.value, size.height.value)
            AstrologyWidgetContent(
                context = context,
                snapshot = snapshot,
                layout = layout,
                isDarkTheme = isDark,
                isSpanish = isEs,
                opacityPct = opacityPct
            )
        }
    }

    @Composable
    private fun AstrologyWidgetContent(
        context: Context,
        snapshot: AstrologySnapshot,
        layout: AstrologyLayoutSpec,
        isDarkTheme: Boolean,
        isSpanish: Boolean,
        opacityPct: Int
    ) {
        val detailIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_ASTROLOGY)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val mercury = snapshot.mercuryStatus
        val retrogradePlanets = snapshot.planets.filter { it.isRetrograde }
        val isAnyRetrograde = retrogradePlanets.isNotEmpty()
        val isMercuryShadow = !mercury.isRetrograde &&
            (mercury.motionState == PlanetaryMotionState.PRE_SHADOW ||
                mercury.motionState == PlanetaryMotionState.POST_SHADOW)

        val emblemBitmap = WidgetCanvasRenderer.renderAstrologyEmblemBitmap(
            isRetrogradeActive = mercury.isRetrograde || isAnyRetrograde,
            isShadowActive = isMercuryShadow,
            sizePx = 96,
            isDarkTheme = isDarkTheme
        )
        val sunBadgeBitmap = WidgetCanvasRenderer.renderZodiacSignBitmap(
            sign = snapshot.sunSign,
            sizePx = 64,
            isDarkTheme = isDarkTheme
        )
        val moonBadgeBitmap = WidgetCanvasRenderer.renderZodiacSignBitmap(
            sign = snapshot.lunarStatus.moonSign,
            sizePx = 64,
            isDarkTheme = isDarkTheme
        )

        val paddingDp = layout.cardPaddingDp.dp
        val surfaceBg = OneUiGlanceTokens.surfaceBackground(opacityPct)
        val innerBg = OneUiGlanceTokens.innerCardSurface(opacityPct)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(OneUiGlanceTokens.CornerRadius)
                .background(surfaceBg)
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(paddingDp)
            ) {
                // 1. Hero Header: Orbital Mercury Emblem + Retrograde Summary (Emoji-free)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(emblemBitmap),
                        contentDescription = mercury.localizedName(isSpanish),
                        modifier = GlanceModifier.size(layout.emblemSizeDp.dp)
                    )

                    Spacer(modifier = GlanceModifier.width(10.dp))

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = if (isSpanish) "ASTROLOGÍA • ONE UI" else "ASTROLOGY • ONE UI",
                            maxLines = 1,
                            style = TextStyle(
                                color = OneUiGlanceTokens.SecondaryText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        val mercuryHeadline = if (mercury.isRetrograde) {
                            if (isSpanish) "Mercurio Retrógrado" else "Mercury Retrograde"
                        } else {
                            "${mercury.localizedName(isSpanish)}: ${mercury.motionState.localizedBadge(isSpanish)}"
                        }
                        Text(
                            text = mercuryHeadline,
                            maxLines = 1,
                            style = TextStyle(
                                color = if (mercury.isRetrograde) {
                                    OneUiGlanceTokens.RetroAlertAmber
                                } else {
                                    OneUiGlanceTokens.PrimaryText
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        val mercurySignText = mercury.zodiacSign.localizedName(isSpanish)
                        val retroSummary = if (retrogradePlanets.isEmpty()) {
                            val allDirect = if (isSpanish) "Todos directos" else "All planets direct"
                            "$mercurySignText • $allDirect"
                        } else {
                            val names = retrogradePlanets.joinToString(", ") { it.localizedName(isSpanish) }
                            val label = if (isSpanish) "Retrógrados" else "Retrograde"
                            "$mercurySignText • $label: $names"
                        }
                        Text(
                            text = retroSummary,
                            maxLines = 1,
                            style = TextStyle(
                                color = if (isAnyRetrograde) {
                                    OneUiGlanceTokens.RetroAlertAmber
                                } else {
                                    OneUiGlanceTokens.SecondaryText
                                },
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // 2. Middle Row: Sun Sign & Daily Moon Zodiac Transit with Canvas Zodiac Art
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    val sunDeg = String.format(Locale.US, "%.0f°", snapshot.sunDegreeInSign)
                    val sunValue = if (layout.showDegrees) {
                        "${snapshot.sunSign.localizedName(isSpanish)} • $sunDeg"
                    } else {
                        snapshot.sunSign.localizedName(isSpanish)
                    }
                    AstroSignCard(
                        badgeBitmap = sunBadgeBitmap,
                        badgeSizeDp = layout.signBadgeSizeDp,
                        label = if (isSpanish) "Signo Solar" else "Sun Sign",
                        value = sunValue,
                        subLabel = if (layout.mode == AstrologyLayoutMode.EXPANDED) {
                            if (isSpanish) "Tránsito actual" else "Current transit"
                        } else {
                            null
                        },
                        background = innerBg,
                        modifier = GlanceModifier.defaultWeight()
                    )

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    val moonValue = snapshot.lunarStatus.moonSign.localizedName(isSpanish)
                    AstroSignCard(
                        badgeBitmap = moonBadgeBitmap,
                        badgeSizeDp = layout.signBadgeSizeDp,
                        label = if (isSpanish) "Luna Hoy" else "Moon Transit",
                        value = moonValue,
                        subLabel = if (layout.mode == AstrologyLayoutMode.EXPANDED) {
                            if (isSpanish) "Signo zodiacal" else "Zodiac sign"
                        } else {
                            null
                        },
                        background = innerBg,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }

                // 3. Bottom Strip: 5 Tracked Planets with Canvas Zodiac Icons (Emoji-free)
                if (layout.showPlanetStrip) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(OneUiGlanceTokens.InnerRadius)
                            .background(innerBg)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        snapshot.planets.forEach { planet ->
                            val planetBadge = WidgetCanvasRenderer.renderZodiacSignBitmap(
                                sign = planet.zodiacSign,
                                sizePx = 44,
                                isDarkTheme = isDarkTheme,
                                isRetrogradeTint = planet.isRetrograde
                            )
                            val statusColor = when (planet.motionState) {
                                PlanetaryMotionState.RETROGRADE -> OneUiGlanceTokens.RetroAlertAmber
                                PlanetaryMotionState.PRE_SHADOW,
                                PlanetaryMotionState.POST_SHADOW -> OneUiGlanceTokens.OneUiAccentBlue
                                PlanetaryMotionState.DIRECT -> OneUiGlanceTokens.SecondaryText
                            }
                            val motionTag = when {
                                !layout.showPlanetMotionBadge -> ""
                                planet.motionState == PlanetaryMotionState.RETROGRADE -> " R"
                                planet.motionState == PlanetaryMotionState.PRE_SHADOW ||
                                    planet.motionState == PlanetaryMotionState.POST_SHADOW -> " S"
                                else -> ""
                            }
                            Row(
                                modifier = GlanceModifier.defaultWeight(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    provider = ImageProvider(planetBadge),
                                    contentDescription = planet.zodiacSign.localizedName(isSpanish),
                                    modifier = GlanceModifier.size(18.dp)
                                )
                                Spacer(modifier = GlanceModifier.width(3.dp))
                                Text(
                                    text = "${shortPlanetCode(planet.body, isSpanish)}$motionTag",
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = if (planet.isRetrograde) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Full-widget reliable tap target for One UI Home RemoteViews
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(detailIntent))
            ) {
                Spacer(modifier = GlanceModifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun AstroSignCard(
        badgeBitmap: Bitmap,
        badgeSizeDp: Float,
        label: String,
        value: String,
        subLabel: String?,
        background: ColorProvider,
        modifier: GlanceModifier = GlanceModifier
    ) {
        Row(
            modifier = modifier
                .cornerRadius(OneUiGlanceTokens.InnerRadius)
                .background(background)
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(badgeBitmap),
                contentDescription = value,
                modifier = GlanceModifier.size(badgeSizeDp.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = label,
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.SecondaryText,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = value,
                    maxLines = 1,
                    style = TextStyle(
                        color = OneUiGlanceTokens.PrimaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (subLabel != null) {
                    Text(
                        text = subLabel,
                        maxLines = 1,
                        style = TextStyle(
                            color = OneUiGlanceTokens.SecondaryText,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }
    }

    private fun shortPlanetCode(body: Body, isSpanish: Boolean): String = when (body) {
        Body.Mercury -> "Mer"
        Body.Venus -> "Ven"
        Body.Mars -> "Mar"
        Body.Jupiter -> if (isSpanish) "Júp" else "Jup"
        Body.Saturn -> "Sat"
        else -> body.name.take(3)
    }
}

class OneUiAstrologyWidgetReceiver : OneUiGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiAstrologyWidget()
}
