package com.anacatavc.oneuiextrawidgets.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.anacatavc.oneuiextrawidgets.data.astrology.ZodiacSign
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiCanvasPalette
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders custom Canvas Bitmaps for Jetpack Glance widgets:
 * - Accurate Lunar Phase sphere with waxing/waning terminator curve
 * - One UI 7 Rain Probability ring gauge with sculpted water droplet
 * - One UI 7 European AQI 270° radial arc gauge with indicator dot
 * - One UI 7 Solar UV Index 270° radial arc gauge with sun rays
 * - iPod Classic Click Wheel graphic
 * - Circular portable CD Discman body with metallic rim & speaker grille
 */
object WidgetCanvasRenderer {

    /**
     * Draws a realistic Moon Phase sphere given phase angle in degrees [0..360).
     * 0 = New Moon, 90 = First Quarter, 180 = Full Moon, 270 = Third Quarter.
     */
    fun renderMoonPhaseBitmap(phaseAngleDeg: Double, sizePx: Int = 160): Bitmap {
        val safeSize = sizePx.coerceIn(64, 360)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = safeSize * 0.44f
        val cx = safeSize / 2f
        val cy = safeSize / 2f

        val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MOON_DARK_DISK
            style = Paint.Style.FILL
        }
        val litPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MOON_LIT_DISK
            style = Paint.Style.FILL
        }
        val craterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MOON_CRATER_SHADOW
            style = Paint.Style.FILL
        }
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MOON_RIM_STROKE
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.025f).coerceAtLeast(1.5f)
        }

        // Base unlit lunar disk
        canvas.drawCircle(cx, cy, radius, darkPaint)

        // Terminator geometry
        val normalized = ((phaseAngleDeg % 360.0) + 360.0) % 360.0
        val isWaxing = normalized < 180.0
        val phaseRad = Math.toRadians(normalized)
        val terminatorScale = cos(phaseRad).toFloat() // +1 at 0 deg, 0 at 90 deg, -1 at 180 deg

        val path = Path()
        val outerOval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val innerWidth = kotlin.math.abs(terminatorScale) * radius
        val innerOval = RectF(cx - innerWidth, cy - radius, cx + innerWidth, cy + radius)

        if (isWaxing) {
            path.arcTo(outerOval, -90f, 180f)
            if (terminatorScale >= 0f) {
                path.arcTo(innerOval, 90f, -180f)
            } else {
                path.arcTo(innerOval, 90f, 180f)
            }
        } else {
            path.arcTo(outerOval, 90f, 180f)
            if (terminatorScale >= 0f) {
                path.arcTo(innerOval, -90f, -180f)
            } else {
                path.arcTo(innerOval, -90f, 180f)
            }
        }
        path.close()
        canvas.drawPath(path, litPaint)

        // Subtle craters & outer rim
        canvas.drawCircle(cx - radius * 0.28f, cy - radius * 0.2f, radius * 0.14f, craterPaint)
        canvas.drawCircle(cx + radius * 0.22f, cy + radius * 0.18f, radius * 0.18f, craterPaint)
        canvas.drawCircle(cx + radius * 0.1f, cy - radius * 0.35f, radius * 0.1f, craterPaint)
        canvas.drawCircle(cx - radius * 0.12f, cy + radius * 0.32f, radius * 0.11f, craterPaint)
        canvas.drawCircle(cx, cy, radius, rimPaint)

        return bitmap
    }

    /**
     * Renders a One UI 7 Rain Probability gauge with a circular progress arc and sculpted center droplet.
     */
    fun renderRainGaugeBitmap(
        probabilityPct: Int,
        sizePx: Int = 160,
        isDarkTheme: Boolean = true
    ): Bitmap {
        val safeSize = sizePx.coerceIn(64, 360)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = safeSize / 2f
        val cy = safeSize / 2f
        val strokeW = safeSize * 0.105f
        val radius = safeSize * 0.41f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.GAUGE_TRACK_DARK else OneUiCanvasPalette.GAUGE_TRACK_LIGHT
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)

        val clampedPct = probabilityPct.coerceIn(0, 100)
        val sweep = (clampedPct / 100f) * 270f
        if (sweep > 1f) {
            val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    cx - radius, cy - radius, cx + radius, cy + radius,
                    OneUiCanvasPalette.RAIN_ARC_START,
                    OneUiCanvasPalette.RAIN_ARC_END,
                    Shader.TileMode.CLAMP
                )
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(arcRect, 135f, sweep.coerceAtLeast(8f), false, activePaint)
        }

        // Sculpted water droplet in center
        val dropRadius = safeSize * 0.145f
        val dropCenterY = cy + safeSize * 0.04f
        val dropTopY = cy - safeSize * 0.19f
        val dropPath = Path().apply {
            moveTo(cx, dropTopY)
            cubicTo(
                cx - dropRadius * 1.15f, dropCenterY - dropRadius * 0.35f,
                cx - dropRadius, dropCenterY + dropRadius * 0.85f,
                cx, dropCenterY + dropRadius
            )
            cubicTo(
                cx + dropRadius, dropCenterY + dropRadius * 0.85f,
                cx + dropRadius * 1.15f, dropCenterY - dropRadius * 0.35f,
                cx, dropTopY
            )
            close()
        }
        val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, dropTopY, cx, dropCenterY + dropRadius,
                OneUiCanvasPalette.RAIN_DROP_TOP,
                OneUiCanvasPalette.RAIN_DROP_BOTTOM,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawPath(dropPath, dropPaint)

        // Specular highlight inside droplet
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.RAIN_DROP_HIGHLIGHT
            style = Paint.Style.FILL
        }
        canvas.drawCircle(
            cx - dropRadius * 0.28f,
            dropCenterY + dropRadius * 0.1f,
            dropRadius * 0.18f,
            highlightPaint
        )

        return bitmap
    }

    /**
     * Renders a One UI 7 European Air Quality (AQI) 270° radial gauge with color-coded arc and indicator dot.
     */
    fun renderAqiGaugeBitmap(
        europeanAqi: Int,
        sizePx: Int = 160,
        isDarkTheme: Boolean = true
    ): Bitmap {
        val safeSize = sizePx.coerceIn(64, 360)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = safeSize / 2f
        val cy = safeSize / 2f
        val strokeW = safeSize * 0.105f
        val radius = safeSize * 0.41f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.GAUGE_TRACK_DARK else OneUiCanvasPalette.GAUGE_TRACK_LIGHT
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)

        val aqiColor = when {
            europeanAqi <= 20 -> OneUiCanvasPalette.AQI_GOOD
            europeanAqi <= 40 -> OneUiCanvasPalette.AQI_FAIR
            europeanAqi <= 60 -> OneUiCanvasPalette.AQI_MODERATE
            europeanAqi <= 80 -> OneUiCanvasPalette.AQI_POOR
            else -> OneUiCanvasPalette.AQI_VERY_POOR
        }

        val normalized = (europeanAqi.coerceIn(0, 100) / 100f).coerceAtLeast(0.06f)
        val sweep = normalized * 270f
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = aqiColor
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, sweep, false, activePaint)

        // Indicator dot on the arc tip
        val endAngleRad = Math.toRadians((135f + sweep).toDouble())
        val dotX = cx + (radius * cos(endAngleRad)).toFloat()
        val dotY = cy + (radius * sin(endAngleRad)).toFloat()
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.INDICATOR_RING_DARK else OneUiCanvasPalette.INDICATOR_RING_LIGHT
            style = Paint.Style.FILL
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = aqiColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(dotX, dotY, strokeW * 0.65f, ringPaint)
        canvas.drawCircle(dotX, dotY, strokeW * 0.42f, dotPaint)

        // Center AQI numeric readout
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.TEXT_PRIMARY_DARK else OneUiCanvasPalette.TEXT_PRIMARY_LIGHT
            textSize = safeSize * 0.25f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = aqiColor
            textSize = safeSize * 0.125f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(europeanAqi.coerceAtLeast(0).toString(), cx, cy + safeSize * 0.05f, valuePaint)
        canvas.drawText("AQI", cx, cy + safeSize * 0.24f, labelPaint)

        return bitmap
    }

    /**
     * Renders a One UI 7 Solar UV Index 270° radial gauge with sun core, rays, and scale indicator.
     */
    fun renderUvGaugeBitmap(
        uvIndex: Double,
        sizePx: Int = 160,
        isDarkTheme: Boolean = true
    ): Bitmap {
        val safeSize = sizePx.coerceIn(64, 360)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = safeSize / 2f
        val cy = safeSize / 2f
        val strokeW = safeSize * 0.105f
        val radius = safeSize * 0.41f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.GAUGE_TRACK_DARK else OneUiCanvasPalette.GAUGE_TRACK_LIGHT
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)

        val uvRounded = uvIndex.roundToInt().coerceAtLeast(0)
        val uvColor = when {
            uvRounded <= 2 -> OneUiCanvasPalette.UV_LOW
            uvRounded <= 5 -> OneUiCanvasPalette.UV_MODERATE
            uvRounded <= 7 -> OneUiCanvasPalette.UV_HIGH
            uvRounded <= 10 -> OneUiCanvasPalette.UV_VERY_HIGH
            else -> OneUiCanvasPalette.UV_EXTREME
        }

        val normalized = (uvIndex.coerceIn(0.0, 11.0) / 11.0).toFloat().coerceAtLeast(0.06f)
        val sweep = normalized * 270f
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = uvColor
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, sweep, false, activePaint)

        // Radiant sun core & rays in the center
        val sunRadius = safeSize * 0.14f
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, cy - sunRadius, cx, cy + sunRadius,
                OneUiCanvasPalette.UV_SUN_CORE_TOP,
                OneUiCanvasPalette.UV_SUN_CORE_BOTTOM,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, sunRadius, sunPaint)

        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = uvColor
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.032f).coerceAtLeast(2f)
            strokeCap = Paint.Cap.ROUND
        }
        val rayInner = sunRadius * 1.28f
        val rayOuter = sunRadius * 1.68f
        for (i in 0 until 8) {
            val angleRad = Math.toRadians(i * 45.0)
            val x1 = cx + (rayInner * cos(angleRad)).toFloat()
            val y1 = cy + (rayInner * sin(angleRad)).toFloat()
            val x2 = cx + (rayOuter * cos(angleRad)).toFloat()
            val y2 = cy + (rayOuter * sin(angleRad)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, rayPaint)
        }

        return bitmap
    }

    /**
     * Renders the iPod Classic Click Wheel ring with MENU, |<<, >>|, >|| labels and center select button.
     */
    fun renderIpodClickWheelBitmap(sizePx: Int = 220, isDarkTheme: Boolean = true): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val outerR = sizePx * 0.46f
        val innerR = sizePx * 0.18f

        val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.IPOD_WHEEL_DARK else OneUiCanvasPalette.IPOD_WHEEL_LIGHT
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.IPOD_STROKE_DARK else OneUiCanvasPalette.IPOD_STROKE_LIGHT
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, cy - innerR, cx, cy + innerR,
                if (isDarkTheme) OneUiCanvasPalette.IPOD_CENTER_TOP_DARK else OneUiCanvasPalette.IPOD_CENTER_TOP_LIGHT,
                if (isDarkTheme) OneUiCanvasPalette.IPOD_CENTER_BOTTOM_DARK else OneUiCanvasPalette.IPOD_CENTER_BOTTOM_LIGHT,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.IPOD_LABEL_DARK else OneUiCanvasPalette.IPOD_LABEL_LIGHT
            textSize = sizePx * 0.085f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        canvas.drawCircle(cx, cy, outerR, wheelPaint)
        canvas.drawCircle(cx, cy, outerR, strokePaint)
        canvas.drawCircle(cx, cy, innerR, centerPaint)
        canvas.drawCircle(cx, cy, innerR, strokePaint)

        // Wheel labels
        canvas.drawText("MENU", cx, cy - outerR * 0.65f, labelPaint)
        canvas.drawText("▶‖", cx, cy + outerR * 0.75f, labelPaint)
        canvas.drawText("⏮", cx - outerR * 0.68f, cy + sizePx * 0.03f, labelPaint)
        canvas.drawText("⏭", cx + outerR * 0.68f, cy + sizePx * 0.03f, labelPaint)

        return bitmap
    }

    /**
     * Renders a circular portable CD player (Discman) shell with metallic bevel rim,
     * COMPACT DISC / ESP ANTI-SHOCK branding, central pill module recess, and bottom speaker grille.
     */
    fun renderDiscmanBodyBitmap(
        sizePx: Int = 420,
        isDarkTheme: Boolean = true,
        opacityPct: Int = 100
    ): Bitmap {
        val alphaInt = OneUiGlanceTokens.bitmapAlphaFromOpacity(opacityPct)
        if (alphaInt == 0) {
            return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val outerR = sizePx * 0.485f
        val bodyR = sizePx * 0.46f

        // Outer metallic bevel rim
        val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, cy - outerR, 0f, cy + outerR,
                intArrayOf(
                    OneUiCanvasPalette.DISCMAN_BEVEL_TOP,
                    OneUiCanvasPalette.DISCMAN_BEVEL_MID,
                    OneUiCanvasPalette.DISCMAN_BEVEL_BOTTOM
                ),
                null,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, outerR, bevelPaint)

        // Matte dark graphite Discman body
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, cy - bodyR, 0f, cy + bodyR,
                if (isDarkTheme) OneUiCanvasPalette.DISCMAN_BODY_TOP_DARK else OneUiCanvasPalette.DISCMAN_BODY_TOP_LIGHT,
                if (isDarkTheme) OneUiCanvasPalette.DISCMAN_BODY_BOTTOM_DARK else OneUiCanvasPalette.DISCMAN_BODY_BOTTOM_LIGHT,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, bodyR, bodyPaint)

        val bodyRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_RIM_HIGHLIGHT
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.005f
        }
        canvas.drawCircle(cx, cy, bodyR, bodyRimPaint)

        // Top branding: COMPACT DISC DIGITAL AUDIO + ESP ANTI-SHOCK
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_TITLE_TEXT
            textSize = sizePx * 0.048f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_SUBTITLE_TEXT
            textSize = sizePx * 0.026f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("COMPACT DISC", cx, cy - bodyR * 0.56f, titlePaint)
        canvas.drawText("DIGITAL AUDIO  •  ESP ANTI-SHOCK", cx, cy - bodyR * 0.42f, subtitlePaint)

        // Center pill-shaped control module recess
        val pillW = bodyR * 1.66f
        val pillH = bodyR * 0.48f
        val pillRect = RectF(cx - pillW / 2f, cy - pillH / 2f, cx + pillW / 2f, cy + pillH / 2f)
        val pillRadius = pillH / 2f
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_PILL_BG
            style = Paint.Style.FILL
        }
        val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_PILL_STROKE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.006f
        }
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillPaint)
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillStrokePaint)

        // Bottom perforated speaker grille dots arranged in concentric arcs
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_SPEAKER_DOT
            style = Paint.Style.FILL
        }
        val dotRadius = sizePx * 0.0085f
        val arcRadii = floatArrayOf(bodyR * 0.56f, bodyR * 0.67f, bodyR * 0.78f)
        val dotCounts = intArrayOf(11, 13, 15)
        for (row in arcRadii.indices) {
            val r = arcRadii[row]
            val count = dotCounts[row]
            val startAngle = 42.0
            val endAngle = 138.0
            for (i in 0 until count) {
                val t = if (count == 1) 0.5 else i.toDouble() / (count - 1)
                val angleRad = Math.toRadians(startAngle + t * (endAngle - startAngle))
                val dx = cx + (r * cos(angleRad)).toFloat()
                val dy = cy + (r * kotlin.math.sin(angleRad)).toFloat()
                canvas.drawCircle(dx, dy, dotRadius, dotPaint)
            }
        }

        return if (alphaInt >= 255) {
            bitmap
        } else {
            // Composite the opaque body as a single layer to avoid alpha-stacking seams
            val fadedBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val fadedCanvas = Canvas(fadedBitmap)
            val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = alphaInt }
            fadedCanvas.drawBitmap(bitmap, 0f, 0f, layerPaint)
            bitmap.recycle()
            fadedBitmap
        }
    }

    /**
     * Renders a crisp One UI celestial orbital emblem with a vector Mercury glyph (☿)
     * and status-colored orbital arc (amber for Retrograde, purple for Shadow, blue for Direct).
     */
    fun renderAstrologyEmblemBitmap(
        isRetrogradeActive: Boolean,
        isShadowActive: Boolean = false,
        sizePx: Int = 128,
        isDarkTheme: Boolean = true
    ): Bitmap = AstrologyCanvasRenderer.renderAstrologyEmblemBitmap(
        isRetrogradeActive = isRetrogradeActive,
        isShadowActive = isShadowActive,
        sizePx = sizePx,
        isDarkTheme = isDarkTheme
    )

    /**
     * Renders a crisp, emoji-free One UI circular zodiac badge with element-tinted backdrop
     * and custom vector line-art for each of the 12 [ZodiacSign] values.
     */
    fun renderZodiacSignBitmap(
        sign: ZodiacSign,
        sizePx: Int = 64,
        isDarkTheme: Boolean = true,
        isRetrogradeTint: Boolean = false
    ): Bitmap = AstrologyCanvasRenderer.renderZodiacSignBitmap(
        sign = sign,
        sizePx = sizePx,
        isDarkTheme = isDarkTheme,
        isRetrogradeTint = isRetrogradeTint
    )

    private fun resolveSignalAccentArgb(
        bars: Int,
        isConnected: Boolean,
        isDarkTheme: Boolean
    ): Int {
        if (!isConnected || bars <= 0) {
            return if (isDarkTheme) {
                OneUiCanvasPalette.SIGNAL_MUTED_DARK
            } else {
                OneUiCanvasPalette.SIGNAL_MUTED_LIGHT
            }
        }
        return when (bars.coerceIn(0, 4)) {
            4 -> if (isDarkTheme) {
                OneUiCanvasPalette.SIGNAL_EXCELLENT_DARK
            } else {
                OneUiCanvasPalette.SIGNAL_EXCELLENT_LIGHT
            }
            3 -> if (isDarkTheme) {
                OneUiCanvasPalette.SIGNAL_GOOD_DARK
            } else {
                OneUiCanvasPalette.SIGNAL_GOOD_LIGHT
            }
            2 -> if (isDarkTheme) {
                OneUiCanvasPalette.SIGNAL_FAIR_DARK
            } else {
                OneUiCanvasPalette.SIGNAL_FAIR_LIGHT
            }
            else -> if (isDarkTheme) {
                OneUiCanvasPalette.SIGNAL_WEAK_DARK
            } else {
                OneUiCanvasPalette.SIGNAL_WEAK_LIGHT
            }
        }
    }

    /**
     * Renders a One UI circular badge with 4 stepped cellular signal bars
     * color-coded by signal quality (0..4 bars).
     */
    fun renderCellularSignalBitmap(
        bars: Int,
        isConnected: Boolean = true,
        sizePx: Int = 120,
        isDarkTheme: Boolean = true
    ): Bitmap {
        val safeSize = sizePx.coerceIn(48, 240)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = safeSize / 2f
        val cy = safeSize / 2f
        val activeBars = if (isConnected) bars.coerceIn(0, 4) else 0
        val accent = resolveSignalAccentArgb(bars, isConnected, isDarkTheme)
        val emptyColor = if (isDarkTheme) {
            OneUiCanvasPalette.SIGNAL_BAR_EMPTY_DARK
        } else {
            OneUiCanvasPalette.SIGNAL_BAR_EMPTY_LIGHT
        }

        // Soft tinted One UI circular backdrop + subtle ring
        val badgeRadius = safeSize * 0.46f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (accent and 0x00FFFFFF) or 0x24000000
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, badgeRadius, bgPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (accent and 0x00FFFFFF) or 0x44000000
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.025f).coerceAtLeast(1.5f)
        }
        canvas.drawCircle(cx, cy, badgeRadius - ringPaint.strokeWidth * 0.5f, ringPaint)

        // 4 stepped rounded bars
        val barWidth = safeSize * 0.095f
        val barGap = safeSize * 0.048f
        val totalW = barWidth * 4f + barGap * 3f
        val startX = cx - totalW / 2f
        val baselineY = cy + safeSize * 0.21f
        val heights = floatArrayOf(0.14f, 0.23f, 0.32f, 0.42f)
        val corner = barWidth * 0.45f

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (i in 0 until 4) {
            val left = startX + i * (barWidth + barGap)
            val barH = safeSize * heights[i]
            val rect = RectF(left, baselineY - barH, left + barWidth, baselineY)
            barPaint.color = if (i < activeBars) accent else emptyColor
            canvas.drawRoundRect(rect, corner, corner, barPaint)
        }

        if (!isConnected) {
            val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                style = Paint.Style.STROKE
                strokeWidth = (safeSize * 0.045f).coerceAtLeast(2f)
                strokeCap = Paint.Cap.ROUND
            }
            val off = safeSize * 0.20f
            canvas.drawLine(cx - off, cy + off, cx + off, cy - off, slashPaint)
        }

        return bitmap
    }

    /**
     * Renders a One UI circular badge with concentric Wi-Fi signal arcs
     * color-coded by RSSI bar strength (0..4 bars).
     */
    fun renderWifiSignalBitmap(
        bars: Int,
        isConnected: Boolean = true,
        sizePx: Int = 120,
        isDarkTheme: Boolean = true
    ): Bitmap {
        val safeSize = sizePx.coerceIn(48, 240)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = safeSize / 2f
        val cy = safeSize / 2f
        val activeBars = if (isConnected) bars.coerceIn(0, 4) else 0
        val accent = resolveSignalAccentArgb(bars, isConnected, isDarkTheme)
        val emptyColor = if (isDarkTheme) {
            OneUiCanvasPalette.SIGNAL_BAR_EMPTY_DARK
        } else {
            OneUiCanvasPalette.SIGNAL_BAR_EMPTY_LIGHT
        }

        val badgeRadius = safeSize * 0.46f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (accent and 0x00FFFFFF) or 0x24000000
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, badgeRadius, bgPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (accent and 0x00FFFFFF) or 0x44000000
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.025f).coerceAtLeast(1.5f)
        }
        canvas.drawCircle(cx, cy, badgeRadius - ringPaint.strokeWidth * 0.5f, ringPaint)

        val originY = cy + safeSize * 0.20f
        val strokeW = (safeSize * 0.068f).coerceAtLeast(2.2f)
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }

        // 3 concentric arcs radiating upward (-135 deg to -45 deg)
        val arcRadii = floatArrayOf(safeSize * 0.17f, safeSize * 0.28f, safeSize * 0.39f)
        for (i in arcRadii.indices) {
            val r = arcRadii[i]
            val rect = RectF(cx - r, originY - r, cx + r, originY + r)
            val isLit = activeBars >= i + 2
            arcPaint.color = if (isLit) accent else emptyColor
            canvas.drawArc(rect, -135f, 90f, false, arcPaint)
        }

        // Base dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (activeBars >= 1) accent else emptyColor
        }
        canvas.drawCircle(cx, originY, safeSize * 0.052f, dotPaint)

        if (!isConnected) {
            val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                style = Paint.Style.STROKE
                strokeWidth = (safeSize * 0.045f).coerceAtLeast(2f)
                strokeCap = Paint.Cap.ROUND
            }
            val off = safeSize * 0.20f
            canvas.drawLine(cx - off, cy + off, cx + off, cy - off, slashPaint)
        }

        return bitmap
    }
}



