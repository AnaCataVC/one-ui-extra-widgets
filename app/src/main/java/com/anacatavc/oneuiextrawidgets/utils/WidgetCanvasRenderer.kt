package com.anacatavc.oneuiextrawidgets.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
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
}
