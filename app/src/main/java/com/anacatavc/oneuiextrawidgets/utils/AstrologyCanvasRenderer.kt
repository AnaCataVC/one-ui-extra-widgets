package com.anacatavc.oneuiextrawidgets.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.anacatavc.oneuiextrawidgets.data.astrology.ZodiacSign
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiCanvasPalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders astrology-specific Canvas Bitmaps for Jetpack Glance widgets:
 * - Celestial Mercury orbital emblem with status-colored sweep arc
 * - Minimalist geometric vector badges for all 12 [ZodiacSign] values
 */
internal object AstrologyCanvasRenderer {

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

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glowColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, safeSize * 0.45f, glowPaint)

        val orbitR = safeSize * 0.39f
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.STROKE
            strokeWidth = safeSize * 0.055f
        }
        canvas.drawCircle(cx, cy, orbitR, trackPaint)

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

        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.052f).coerceAtLeast(2.2f)
            strokeCap = Paint.Cap.ROUND
        }
        val circleR = safeSize * 0.115f
        val circleCy = cy - safeSize * 0.01f

        canvas.drawCircle(cx, circleCy, circleR, glyphPaint)

        val hornRect = RectF(
            cx - circleR * 1.1f,
            circleCy - circleR * 2.15f,
            cx + circleR * 1.1f,
            circleCy - circleR * 0.65f
        )
        canvas.drawArc(hornRect, 15f, 150f, false, glyphPaint)

        val stemTop = circleCy + circleR
        val stemBottom = circleCy + circleR * 2.25f
        val crossY = circleCy + circleR * 1.65f
        val crossHalfW = circleR * 0.78f
        canvas.drawLine(cx, stemTop, cx, stemBottom, glyphPaint)
        canvas.drawLine(cx - crossHalfW, crossY, cx + crossHalfW, crossY, glyphPaint)

        return bitmap
    }

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

        val (accentColor, glowColor) = resolveElementColors(sign, isDarkTheme, isRetrogradeTint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glowColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, safeSize * 0.46f, bgPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            alpha = 72
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.032f).coerceAtLeast(1.2f)
        }
        canvas.drawCircle(cx, cy, safeSize * 0.44f, ringPaint)

        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.068f).coerceAtLeast(2.0f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        drawZodiacGlyph(canvas, sign, cx, cy, safeSize * 0.22f, p)
        return bitmap
    }

    private fun resolveElementColors(
        sign: ZodiacSign,
        isDarkTheme: Boolean,
        isRetrogradeTint: Boolean
    ): Pair<Int, Int> {
        if (isRetrogradeTint) {
            val retro = if (isDarkTheme) OneUiCanvasPalette.ASTRO_RETRO_DARK else OneUiCanvasPalette.ASTRO_RETRO_LIGHT
            return retro to OneUiCanvasPalette.ASTRO_GLOW_RETRO
        }
        return when (sign) {
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

    private fun drawZodiacGlyph(
        canvas: Canvas,
        sign: ZodiacSign,
        cx: Float,
        cy: Float,
        r: Float,
        p: Paint
    ) {
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
                    moveTo(cx + r * 0.22f, cy - r * 0.75f)
                    lineTo(cx + r * 0.75f, cy - r * 0.75f)
                    lineTo(cx + r * 0.75f, cy - r * 0.22f)
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
    }
}
