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
    fun renderDiscmanBodyBitmap(sizePx: Int = 420, isDarkTheme: Boolean = true): Bitmap {
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

        return bitmap
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
    ): Bitmap {
        val safeSize = sizePx.coerceIn(64, 256)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = safeSize / 2f
        val cy = safeSize / 2f

        val accentColor = when {
            isRetrogradeActive -> if (isDarkTheme) OneUiCanvasPalette.ASTRO_RETRO_DARK else OneUiCanvasPalette.ASTRO_RETRO_LIGHT
            isShadowActive -> if (isDarkTheme) OneUiCanvasPalette.ASTRO_SHADOW_DARK else OneUiCanvasPalette.ASTRO_SHADOW_LIGHT
            else -> if (isDarkTheme) OneUiCanvasPalette.ASTRO_DIRECT_DARK else OneUiCanvasPalette.ASTRO_DIRECT_LIGHT
        }
        val glowColor = when {
            isRetrogradeActive -> OneUiCanvasPalette.ASTRO_GLOW_RETRO
            isShadowActive -> OneUiCanvasPalette.ASTRO_GLOW_SHADOW
            else -> OneUiCanvasPalette.ASTRO_GLOW_DIRECT
        }
        val trackColor = if (isDarkTheme) OneUiCanvasPalette.GAUGE_TRACK_DARK else OneUiCanvasPalette.GAUGE_TRACK_LIGHT

        // Soft tinted backdrop circle
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glowColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, safeSize * 0.45f, glowPaint)

        // Outer orbital track
        val orbitR = safeSize * 0.39f
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.STROKE
            strokeWidth = safeSize * 0.055f
        }
        canvas.drawCircle(cx, cy, orbitR, trackPaint)

        // Active orbital sweep arc
        val arcRect = RectF(cx - orbitR, cy - orbitR, cx + orbitR, cy + orbitR)
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = safeSize * 0.06f
            strokeCap = Paint.Cap.ROUND
        }
        val startDeg = if (isRetrogradeActive) 210f else -65f
        val sweepDeg = if (isRetrogradeActive) -215f else 235f
        canvas.drawArc(arcRect, startDeg, sweepDeg, false, arcPaint)

        // Orbiting planet dot at end of arc
        val endRad = Math.toRadians((startDeg + sweepDeg).toDouble())
        val dotX = cx + (orbitR * cos(endRad)).toFloat()
        val dotY = cy + (orbitR * sin(endRad)).toFloat()
        val dotRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.INDICATOR_RING_DARK else OneUiCanvasPalette.INDICATOR_RING_LIGHT
            style = Paint.Style.FILL
        }
        val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(dotX, dotY, safeSize * 0.068f, dotRingPaint)
        canvas.drawCircle(dotX, dotY, safeSize * 0.045f, dotFillPaint)

        // Vector Mercury glyph (☿) in the center
        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.052f).coerceAtLeast(2.2f)
            strokeCap = Paint.Cap.ROUND
        }
        val circleR = safeSize * 0.115f
        val circleCy = cy - safeSize * 0.01f

        // Central circle
        canvas.drawCircle(cx, circleCy, circleR, glyphPaint)

        // Top horns arc
        val hornRect = RectF(
            cx - circleR * 1.1f,
            circleCy - circleR * 2.15f,
            cx + circleR * 1.1f,
            circleCy - circleR * 0.65f
        )
        canvas.drawArc(hornRect, 15f, 150f, false, glyphPaint)

        // Bottom cross vertical stem & horizontal bar
        val stemTop = circleCy + circleR
        val stemBottom = circleCy + circleR * 2.25f
        val crossY = circleCy + circleR * 1.65f
        val crossHalfW = circleR * 0.78f
        canvas.drawLine(cx, stemTop, cx, stemBottom, glyphPaint)
        canvas.drawLine(cx - crossHalfW, crossY, cx + crossHalfW, crossY, glyphPaint)

        return bitmap
    }

    /**
     * Renders a crisp, emoji-free One UI circular zodiac badge with element-tinted backdrop
     * and custom vector line-art for each of the 12 [ZodiacSign] values.
     */
    fun renderZodiacSignBitmap(
        sign: ZodiacSign,
        sizePx: Int = 64,
        isDarkTheme: Boolean = true,
        isRetrogradeTint: Boolean = false
    ): Bitmap {
        val safeSize = sizePx.coerceIn(36, 160)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = safeSize / 2f
        val cy = safeSize / 2f

        val (accentColor, glowColor) = if (isRetrogradeTint) {
            val retro = if (isDarkTheme) OneUiCanvasPalette.ASTRO_RETRO_DARK else OneUiCanvasPalette.ASTRO_RETRO_LIGHT
            retro to OneUiCanvasPalette.ASTRO_GLOW_RETRO
        } else {
            when (sign) {
                ZodiacSign.ARIES, ZodiacSign.LEO, ZodiacSign.SAGITTARIUS ->
                    (if (isDarkTheme) OneUiCanvasPalette.ZODIAC_FIRE_DARK else OneUiCanvasPalette.ZODIAC_FIRE_LIGHT) to
                        OneUiCanvasPalette.ZODIAC_FIRE_GLOW
                ZodiacSign.TAURUS, ZodiacSign.VIRGO, ZodiacSign.CAPRICORN ->
                    (if (isDarkTheme) OneUiCanvasPalette.ZODIAC_EARTH_DARK else OneUiCanvasPalette.ZODIAC_EARTH_LIGHT) to
                        OneUiCanvasPalette.ZODIAC_EARTH_GLOW
                ZodiacSign.GEMINI, ZodiacSign.LIBRA, ZodiacSign.AQUARIUS ->
                    (if (isDarkTheme) OneUiCanvasPalette.ZODIAC_AIR_DARK else OneUiCanvasPalette.ZODIAC_AIR_LIGHT) to
                        OneUiCanvasPalette.ZODIAC_AIR_GLOW
                ZodiacSign.CANCER, ZodiacSign.SCORPIO, ZodiacSign.PISCES ->
                    (if (isDarkTheme) OneUiCanvasPalette.ZODIAC_WATER_DARK else OneUiCanvasPalette.ZODIAC_WATER_LIGHT) to
                        OneUiCanvasPalette.ZODIAC_WATER_GLOW
            }
        }

        // Soft element-tinted circular badge
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glowColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, safeSize * 0.46f, bgPaint)

        // Subtle outer ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            alpha = 72
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.032f).coerceAtLeast(1.2f)
        }
        canvas.drawCircle(cx, cy, safeSize * 0.44f, ringPaint)

        // Vector zodiac glyph paint
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.068f).coerceAtLeast(2.0f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val r = safeSize * 0.22f
        when (sign) {
            ZodiacSign.ARIES -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.95f, cy - r * 0.25f)
                    cubicTo(cx - r * 0.95f, cy - r * 1.05f, cx - r * 0.1f, cy - r * 0.95f, cx, cy + r * 0.85f)
                    moveTo(cx + r * 0.95f, cy - r * 0.25f)
                    cubicTo(cx + r * 0.95f, cy - r * 1.05f, cx + r * 0.1f, cy - r * 0.95f, cx, cy + r * 0.85f)
                }
                canvas.drawPath(path, p)
            }
            ZodiacSign.TAURUS -> {
                canvas.drawCircle(cx, cy + r * 0.28f, r * 0.52f, p)
                val horns = RectF(cx - r * 0.8f, cy - r * 0.95f, cx + r * 0.8f, cy - r * 0.15f)
                canvas.drawArc(horns, 10f, 160f, false, p)
            }
            ZodiacSign.GEMINI -> {
                val topArc = RectF(cx - r * 0.95f, cy - r * 1.15f, cx + r * 0.95f, cy - r * 0.55f)
                val botArc = RectF(cx - r * 0.95f, cy + r * 0.55f, cx + r * 0.95f, cy + r * 1.15f)
                canvas.drawArc(topArc, 15f, 150f, false, p)
                canvas.drawArc(botArc, 195f, 150f, false, p)
                canvas.drawLine(cx - r * 0.38f, cy - r * 0.62f, cx - r * 0.38f, cy + r * 0.62f, p)
                canvas.drawLine(cx + r * 0.38f, cy - r * 0.62f, cx + r * 0.38f, cy + r * 0.62f, p)
            }
            ZodiacSign.CANCER -> {
                val cR = r * 0.34f
                canvas.drawCircle(cx - r * 0.45f, cy - r * 0.22f, cR, p)
                canvas.drawCircle(cx + r * 0.45f, cy + r * 0.22f, cR, p)
                val topTail = RectF(cx - r * 0.78f, cy - r * 0.62f, cx + r * 0.82f, cy + r * 0.08f)
                val botTail = RectF(cx - r * 0.82f, cy - r * 0.08f, cx + r * 0.78f, cy + r * 0.62f)
                canvas.drawArc(topTail, 200f, 140f, false, p)
                canvas.drawArc(botTail, 20f, 140f, false, p)
            }
            ZodiacSign.LEO -> {
                canvas.drawCircle(cx - r * 0.48f, cy + r * 0.35f, r * 0.32f, p)
                val path = Path().apply {
                    moveTo(cx - r * 0.26f, cy + r * 0.12f)
                    cubicTo(cx - r * 0.45f, cy - r * 0.95f, cx + r * 0.68f, cy - r * 0.95f, cx + r * 0.38f, cy + r * 0.28f)
                    cubicTo(cx + r * 0.24f, cy + r * 0.82f, cx + r * 0.78f, cy + r * 0.88f, cx + r * 0.88f, cy + r * 0.48f)
                }
                canvas.drawPath(path, p)
            }
            ZodiacSign.VIRGO -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.85f, cy - r * 0.55f)
                    lineTo(cx - r * 0.85f, cy + r * 0.48f)
                    moveTo(cx - r * 0.85f, cy - r * 0.25f)
                    cubicTo(cx - r * 0.65f, cy - r * 0.7f, cx - r * 0.25f, cy - r * 0.7f, cx - r * 0.25f, cy + r * 0.48f)
                    moveTo(cx - r * 0.25f, cy - r * 0.25f)
                    cubicTo(cx - r * 0.05f, cy - r * 0.7f, cx + r * 0.35f, cy - r * 0.7f, cx + r * 0.35f, cy + r * 0.52f)
                    cubicTo(cx + r * 0.35f, cy + r * 0.92f, cx + r * 0.72f, cy + r * 0.92f, cx + r * 0.85f, cy + r * 0.65f)
                    moveTo(cx + r * 0.35f, cy - r * 0.05f)
                    cubicTo(cx + r * 0.88f, cy - r * 0.48f, cx + r * 0.92f, cy + r * 0.35f, cx + r * 0.1f, cy + r * 0.82f)
                }
                canvas.drawPath(path, p)
            }
            ZodiacSign.LIBRA -> {
                canvas.drawLine(cx - r * 0.88f, cy + r * 0.62f, cx + r * 0.88f, cy + r * 0.62f, p)
                canvas.drawLine(cx - r * 0.88f, cy + r * 0.18f, cx - r * 0.42f, cy + r * 0.18f, p)
                canvas.drawLine(cx + r * 0.42f, cy + r * 0.18f, cx + r * 0.88f, cy + r * 0.18f, p)
                val omega = RectF(cx - r * 0.48f, cy - r * 0.62f, cx + r * 0.48f, cy + r * 0.28f)
                canvas.drawArc(omega, 145f, 250f, false, p)
            }
            ZodiacSign.SCORPIO -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.88f, cy - r * 0.55f)
                    lineTo(cx - r * 0.88f, cy + r * 0.45f)
                    moveTo(cx - r * 0.88f, cy - r * 0.25f)
                    cubicTo(cx - r * 0.68f, cy - r * 0.7f, cx - r * 0.28f, cy - r * 0.7f, cx - r * 0.28f, cy + r * 0.45f)
                    moveTo(cx - r * 0.28f, cy - r * 0.25f)
                    cubicTo(cx - r * 0.08f, cy - r * 0.7f, cx + r * 0.32f, cy - r * 0.7f, cx + r * 0.32f, cy + r * 0.38f)
                    cubicTo(cx + r * 0.32f, cy + r * 0.85f, cx + r * 0.7f, cy + r * 0.85f, cx + r * 0.88f, cy + r * 0.55f)
                    // Arrow tip
                    moveTo(cx + r * 0.62f, cy + r * 0.58f)
                    lineTo(cx + r * 0.88f, cy + r * 0.55f)
                    lineTo(cx + r * 0.82f, cy + r * 0.82f)
                }
                canvas.drawPath(path, p)
            }
            ZodiacSign.SAGITTARIUS -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.75f, cy + r * 0.75f)
                    lineTo(cx + r * 0.75f, cy - r * 0.75f)
                    // Arrowhead
                    moveTo(cx + r * 0.22f, cy - r * 0.75f)
                    lineTo(cx + r * 0.75f, cy - r * 0.75f)
                    lineTo(cx + r * 0.75f, cy - r * 0.22f)
                    // Crossbar
                    moveTo(cx - r * 0.48f, cy - r * 0.02f)
                    lineTo(cx + r * 0.02f, cy + r * 0.48f)
                }
                canvas.drawPath(path, p)
            }
            ZodiacSign.CAPRICORN -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.85f, cy - r * 0.42f)
                    lineTo(cx - r * 0.35f, cy + r * 0.48f)
                    lineTo(cx + r * 0.15f, cy - r * 0.45f)
                    lineTo(cx + r * 0.15f, cy + r * 0.42f)
                    cubicTo(cx + r * 0.15f, cy + r * 0.88f, cx + r * 0.78f, cy + r * 0.88f, cx + r * 0.78f, cy + r * 0.38f)
                    cubicTo(cx + r * 0.78f, cy - r * 0.05f, cx + r * 0.15f, cy + r * 0.1f, cx + r * 0.48f, cy + r * 0.65f)
                }
                canvas.drawPath(path, p)
            }
            ZodiacSign.AQUARIUS -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.85f, cy - r * 0.18f)
                    lineTo(cx - r * 0.42f, cy - r * 0.52f)
                    lineTo(cx, cy - r * 0.18f)
                    lineTo(cx + r * 0.42f, cy - r * 0.52f)
                    lineTo(cx + r * 0.85f, cy - r * 0.18f)

                    moveTo(cx - r * 0.85f, cy + r * 0.42f)
                    lineTo(cx - r * 0.42f, cy + r * 0.08f)
                    lineTo(cx, cy + r * 0.42f)
                    lineTo(cx + r * 0.42f, cy + r * 0.08f)
                    lineTo(cx + r * 0.85f, cy + r * 0.42f)
                }
                canvas.drawPath(path, p)
            }
            ZodiacSign.PISCES -> {
                val leftArc = RectF(cx - r * 1.15f, cy - r * 0.82f, cx - r * 0.25f, cy + r * 0.82f)
                val rightArc = RectF(cx + r * 0.25f, cy - r * 0.82f, cx + r * 1.15f, cy + r * 0.82f)
                canvas.drawArc(leftArc, -65f, 130f, false, p)
                canvas.drawArc(rightArc, 115f, 130f, false, p)
                canvas.drawLine(cx - r * 0.85f, cy, cx + r * 0.85f, cy, p)
            }
        }

        return bitmap
    }
}


