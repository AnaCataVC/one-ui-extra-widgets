package com.anacatavc.oneuiextrawidgets.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiCanvasPalette
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deterministic Canvas bitmap renderers for the iPod Classic Click Wheel, Discman body,
 * Cassette Walkman deck, Braun Vinyl turntable platter, and MiniDisc Hi-MD cartridge.
 */
object MusicCanvasRenderer {

    /**
     * Renders the iPod Classic Click Wheel ring with MENU text, geometric media icons, and center button.
     */
    fun renderIpodClickWheelBitmap(
        sizePx: Int = 220,
        isDarkTheme: Boolean = true,
        isPlaying: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val outerR = sizePx * 0.46f
        val innerR = sizePx * 0.185f

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
        val labelColor = if (isDarkTheme) OneUiCanvasPalette.IPOD_LABEL_DARK else OneUiCanvasPalette.IPOD_LABEL_LIGHT
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = sizePx * 0.085f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        canvas.drawCircle(cx, cy, outerR, wheelPaint)
        canvas.drawCircle(cx, cy, outerR, strokePaint)
        canvas.drawCircle(cx, cy, innerR, centerPaint)
        canvas.drawCircle(cx, cy, innerR, strokePaint)

        canvas.drawText("MENU", cx, cy - outerR * 0.65f, labelPaint)
        drawWheelMediaIcons(canvas, cx, cy, outerR, sizePx * 0.038f, labelColor)
        drawCenterPlayPauseIcon(canvas, cx, cy, innerR, isPlaying, labelColor)

        return bitmap
    }

    private fun drawCenterPlayPauseIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        innerR: Float,
        isPlaying: Boolean,
        color: Int
    ) {
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        if (isPlaying) {
            val barW = innerR * 0.22f
            val barH = innerR * 0.72f
            val gap = innerR * 0.16f
            canvas.drawRoundRect(
                RectF(cx - gap / 2f - barW, cy - barH / 2f, cx - gap / 2f, cy + barH / 2f),
                barW * 0.3f,
                barW * 0.3f,
                iconPaint
            )
            canvas.drawRoundRect(
                RectF(cx + gap / 2f, cy - barH / 2f, cx + gap / 2f + barW, cy + barH / 2f),
                barW * 0.3f,
                barW * 0.3f,
                iconPaint
            )
        } else {
            val halfH = innerR * 0.42f
            val halfW = innerR * 0.38f
            val shiftX = innerR * 0.06f
            val path = Path().apply {
                moveTo(cx - halfW + shiftX, cy - halfH)
                lineTo(cx + halfW + shiftX, cy)
                lineTo(cx - halfW + shiftX, cy + halfH)
                close()
            }
            canvas.drawPath(path, iconPaint)
        }
    }

    private fun drawWheelMediaIcons(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        outerR: Float,
        iconR: Float,
        color: Int
    ) {
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        // Bottom: Pause double bar indicator
        val bottomY = cy + outerR * 0.71f
        val barW = iconR * 0.42f
        val barH = iconR * 1.45f
        val gap = iconR * 0.36f
        canvas.drawRect(cx - gap / 2f - barW, bottomY - barH / 2f, cx - gap / 2f, bottomY + barH / 2f, iconPaint)
        canvas.drawRect(cx + gap / 2f, bottomY - barH / 2f, cx + gap / 2f + barW, bottomY + barH / 2f, iconPaint)

        // Left: Previous track
        val prevX = cx - outerR * 0.68f
        canvas.drawRect(prevX - iconR * 1.15f, cy - iconR * 0.68f, prevX - iconR * 0.85f, cy + iconR * 0.68f, iconPaint)
        canvas.drawPath(horizontalTriangle(prevX - iconR * 0.8f, prevX + iconR * 0.15f, cy, iconR * 0.68f), iconPaint)
        canvas.drawPath(horizontalTriangle(prevX + iconR * 0.15f, prevX + iconR * 1.1f, cy, iconR * 0.68f), iconPaint)

        // Right: Next track
        val nextX = cx + outerR * 0.68f
        canvas.drawPath(horizontalTriangle(nextX - iconR * 0.15f, nextX - iconR * 1.1f, cy, iconR * 0.68f), iconPaint)
        canvas.drawPath(horizontalTriangle(nextX + iconR * 0.8f, nextX - iconR * 0.15f, cy, iconR * 0.68f), iconPaint)
        canvas.drawRect(nextX + iconR * 0.85f, cy - iconR * 0.68f, nextX + iconR * 1.15f, cy + iconR * 0.68f, iconPaint)
    }

    private fun horizontalTriangle(tipX: Float, baseX: Float, cy: Float, halfH: Float): Path =
        Path().apply {
            moveTo(tipX, cy)
            lineTo(baseX, cy - halfH)
            lineTo(baseX, cy + halfH)
            close()
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

        // Top-right lid-release latch accent on the outer bevel
        val latchRect = RectF(cx - outerR * 0.97f, cy - outerR * 0.97f, cx + outerR * 0.97f, cy + outerR * 0.97f)
        val latchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_LATCH_ACCENT
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.024f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(latchRect, -64f, 26f, false, latchPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_TITLE_TEXT
            textSize = sizePx * 0.052f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_SUBTITLE_TEXT
            textSize = sizePx * 0.025f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("COMPACT DISC", cx, cy - bodyR * 0.62f, titlePaint)
        canvas.drawText("DIGITAL AUDIO  •  ESP ANTI-SHOCK", cx, cy - bodyR * 0.49f, subtitlePaint)

        val lcdW = bodyR * 1.48f
        val lcdH = bodyR * 0.54f
        val lcdCenterY = cy - bodyR * 0.12f
        val lcdRect = RectF(cx - lcdW / 2f, lcdCenterY - lcdH / 2f, cx + lcdW / 2f, lcdCenterY + lcdH / 2f)
        val lcdRadius = sizePx * 0.045f
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_PILL_BG
            style = Paint.Style.FILL
        }
        val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_PILL_STROKE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.006f
        }
        canvas.drawRoundRect(lcdRect, lcdRadius, lcdRadius, pillPaint)
        canvas.drawRoundRect(lcdRect, lcdRadius, lcdRadius, pillStrokePaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.DISCMAN_SPEAKER_DOT
            style = Paint.Style.FILL
        }
        val dotRadius = sizePx * 0.0085f
        val arcRadii = floatArrayOf(bodyR * 0.68f, bodyR * 0.78f, bodyR * 0.87f)
        val dotCounts = intArrayOf(9, 11, 13)
        for (row in arcRadii.indices) {
            val r = arcRadii[row]
            val count = dotCounts[row]
            val startAngle = 52.0
            val endAngle = 128.0
            for (i in 0 until count) {
                val t = if (count == 1) 0.5 else i.toDouble() / (count - 1)
                val angleRad = Math.toRadians(startAngle + t * (endAngle - startAngle))
                val dx = cx + (r * cos(angleRad)).toFloat()
                val dy = cy + (r * sin(angleRad)).toFloat()
                canvas.drawCircle(dx, dy, dotRadius, dotPaint)
            }
        }

        return applyBitmapOpacity(bitmap, alphaInt)
    }

    /**
     * Renders a retro Walkman compact cassette shell with corner micro-screws, creamy Type II label,
     * smoky center window with dynamically sized 6-tooth tape spools, 3-digit counter, and head bridge.
     */
    fun renderCassetteDeckBitmap(
        progressFraction: Float,
        sizePx: Int = 360,
        isDarkTheme: Boolean = true,
        isPlaying: Boolean = false,
        opacityPct: Int = 100
    ): Bitmap = CassetteDeckPainter.render(
        progressFraction = progressFraction,
        sizePx = sizePx,
        isDarkTheme = isDarkTheme,
        isPlaying = isPlaying,
        opacityPct = opacityPct
    )

    /**
     * Renders a Braun / Dieter Rams-inspired vinyl turntable platter, stroboscopic speed rim,
     * micro-grooved LP disc with anisotropic sheen, circular-clipped album art (or fallback label),
     * and an articulated S-shaped metallic tonearm tracking [progressFraction].
     */
    fun renderVinylTurntableBitmap(
        albumArt: Bitmap?,
        progressFraction: Float,
        sizePx: Int = 320,
        isDarkTheme: Boolean = true,
        isPlaying: Boolean = false
    ): Bitmap = VinylTurntablePainter.render(
        albumArt = albumArt,
        progressFraction = progressFraction,
        sizePx = sizePx,
        isDarkTheme = isDarkTheme,
        isPlaying = isPlaying
    )

    /**
     * Renders an authentic square Sony-style MiniDisc cartridge badge with metallic slate shell,
     * smoky window revealing the iridescent magneto-optical disc & clamping hub, and silver shutter.
     */
    fun renderMiniDiscBadgeBitmap(
        sizePx: Int = 128,
        isDarkTheme: Boolean = true,
        isPlaying: Boolean = false
    ): Bitmap = MiniDiscBadgePainter.render(
        sizePx = sizePx,
        isDarkTheme = isDarkTheme,
        isPlaying = isPlaying
    )
}

private fun applyBitmapOpacity(bitmap: Bitmap, alphaInt: Int): Bitmap {
    if (alphaInt >= 255) return bitmap
    val fadedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val fadedCanvas = Canvas(fadedBitmap)
    val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = alphaInt }
    fadedCanvas.drawBitmap(bitmap, 0f, 0f, layerPaint)
    bitmap.recycle()
    return fadedBitmap
}

private object CassetteDeckPainter {

    fun render(
        progressFraction: Float,
        sizePx: Int,
        isDarkTheme: Boolean,
        isPlaying: Boolean,
        opacityPct: Int
    ): Bitmap {
        val safeSize = sizePx.coerceIn(120, 640)
        val widthPx = safeSize
        val heightPx = (safeSize * 7f / 12f).roundToInt().coerceAtLeast(70)
        val alphaInt = OneUiGlanceTokens.bitmapAlphaFromOpacity(opacityPct)
        if (alphaInt == 0) {
            return Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        }

        val progress = if (progressFraction.isNaN()) 0f else progressFraction.coerceIn(0f, 1f)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val pad = w * 0.018f
        val shellRect = RectF(pad, pad, w - pad, h - pad)
        val shellCorner = w * 0.048f

        val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, shellRect.top, 0f, shellRect.bottom,
                if (isDarkTheme) OneUiCanvasPalette.CASSETTE_BODY_TOP_DARK else OneUiCanvasPalette.CASSETTE_BODY_TOP_LIGHT,
                if (isDarkTheme) {
                    OneUiCanvasPalette.CASSETTE_BODY_BOTTOM_DARK
                } else {
                    OneUiCanvasPalette.CASSETTE_BODY_BOTTOM_LIGHT
                },
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(shellRect, shellCorner, shellCorner, shellPaint)

        val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.CASSETTE_BEVEL_DARK else OneUiCanvasPalette.CASSETTE_BEVEL_LIGHT
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.006f).coerceAtLeast(1.5f)
        }
        canvas.drawRoundRect(shellRect, shellCorner, shellCorner, bevelPaint)

        val recessInset = w * 0.032f
        val recessRect = RectF(
            shellRect.left + recessInset,
            shellRect.top + recessInset,
            shellRect.right - recessInset,
            shellRect.bottom - recessInset
        )
        val recessPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) {
                OneUiCanvasPalette.CASSETTE_RECESS_STROKE_DARK
            } else {
                OneUiCanvasPalette.CASSETTE_RECESS_STROKE_LIGHT
            }
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.003f).coerceAtLeast(1f)
        }
        canvas.drawRoundRect(recessRect, shellCorner * 0.65f, shellCorner * 0.65f, recessPaint)

        drawCassetteScrews(canvas, shellRect, w, isDarkTheme)
        drawCassetteLabelAndWindow(canvas, w, h, progress, isDarkTheme)
        drawCassetteBottomBridge(canvas, shellRect, w, h, isDarkTheme, isPlaying)

        return applyBitmapOpacity(bitmap, alphaInt)
    }

    private fun drawCassetteScrews(
        canvas: Canvas,
        shellRect: RectF,
        w: Float,
        isDarkTheme: Boolean
    ) {
        val screwR = (w * 0.014f).coerceAtLeast(2.2f)
        val inset = w * 0.048f
        val screwPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) {
                OneUiCanvasPalette.CASSETTE_SCREW_HEAD_DARK
            } else {
                OneUiCanvasPalette.CASSETTE_SCREW_HEAD_LIGHT
            }
            style = Paint.Style.FILL
        }
        val slotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_SCREW_SLOT
            style = Paint.Style.STROKE
            strokeWidth = (screwR * 0.36f).coerceAtLeast(1f)
            strokeCap = Paint.Cap.ROUND
        }
        val corners = arrayOf(
            shellRect.left + inset to shellRect.top + inset,
            shellRect.right - inset to shellRect.top + inset,
            shellRect.left + inset to shellRect.bottom - inset,
            shellRect.right - inset to shellRect.bottom - inset
        )
        for ((sx, sy) in corners) {
            canvas.drawCircle(sx, sy, screwR, screwPaint)
            val d = screwR * 0.56f
            canvas.drawLine(sx - d, sy - d, sx + d, sy + d, slotPaint)
            canvas.drawLine(sx - d, sy + d, sx + d, sy - d, slotPaint)
        }
    }

    private fun drawCassetteLabelAndWindow(
        canvas: Canvas,
        w: Float,
        h: Float,
        progress: Float,
        isDarkTheme: Boolean
    ) {
        val labelRect = RectF(w * 0.088f, h * 0.095f, w * 0.912f, h * 0.725f)
        val labelRadius = w * 0.022f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.CASSETTE_LABEL_DARK else OneUiCanvasPalette.CASSETTE_LABEL_LIGHT
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(labelRect, labelRadius, labelRadius, labelPaint)

        // Classic horizontal Type II racing stripes clipped to label card
        val labelClip = Path().apply { addRoundRect(labelRect, labelRadius, labelRadius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(labelClip)

        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        stripePaint.color = OneUiCanvasPalette.CASSETTE_LABEL_STRIPE_ORANGE
        canvas.drawRect(labelRect.left, labelRect.top + h * 0.128f, labelRect.right, labelRect.top + h * 0.156f, stripePaint)

        stripePaint.color = OneUiCanvasPalette.CASSETTE_LABEL_STRIPE_RED
        canvas.drawRect(labelRect.left, labelRect.top + h * 0.164f, labelRect.right, labelRect.top + h * 0.184f, stripePaint)

        stripePaint.color = OneUiCanvasPalette.CASSETTE_LABEL_STRIPE_AMBER
        canvas.drawRect(labelRect.left, labelRect.top + h * 0.190f, labelRect.right, labelRect.top + h * 0.204f, stripePaint)
        canvas.restore()

        val inkColor = if (isDarkTheme) {
            OneUiCanvasPalette.CASSETTE_LABEL_INK_DARK
        } else {
            OneUiCanvasPalette.CASSETTE_LABEL_INK_LIGHT
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = h * 0.072f
            textAlign = Paint.Align.LEFT
            isFakeBoldText = true
        }
        val sidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_LABEL_STRIPE_ORANGE
            textSize = h * 0.074f
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = true
        }
        val headerY = labelRect.top + h * 0.095f
        canvas.drawText("TYPE II CrO2 • 90", labelRect.left + w * 0.032f, headerY, headerPaint)
        canvas.drawText("A", labelRect.right - w * 0.032f, headerY, sidePaint)

        // Center dark smoky window revealing dynamic tape spools & 6-tooth hubs
        val winRect = RectF(w * 0.205f, h * 0.325f, w * 0.795f, h * 0.665f)
        val winRadius = winRect.height() * 0.24f
        val winPath = Path().apply { addRoundRect(winRect, winRadius, winRadius, Path.Direction.CW) }
        val winBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_WINDOW_BG
            style = Paint.Style.FILL
        }
        canvas.drawPath(winPath, winBgPaint)

        val leftSpoolX = w * 0.315f
        val rightSpoolX = w * 0.685f
        val spoolY = winRect.centerY()
        val hubR = h * 0.088f
        val minTapeR = hubR * 1.18f
        val maxTapeR = hubR * 2.42f
        val leftTapeR = interpolateTapeRadius(minTapeR, maxTapeR, 1f - progress)
        val rightTapeR = interpolateTapeRadius(minTapeR, maxTapeR, progress)
        val hubRotationDeg = progress * 240f

        canvas.save()
        canvas.clipPath(winPath)
        drawTapeSpool(canvas, leftSpoolX, spoolY, leftTapeR, hubR, hubRotationDeg)
        drawTapeSpool(canvas, rightSpoolX, spoolY, rightTapeR, hubR, hubRotationDeg)

        val glintPath = Path().apply {
            moveTo(winRect.left + w * 0.04f, winRect.top)
            lineTo(winRect.left + w * 0.18f, winRect.top)
            lineTo(winRect.left + w * 0.08f, winRect.bottom)
            lineTo(winRect.left - w * 0.04f, winRect.bottom)
            close()
        }
        val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_WINDOW_GLINT
            style = Paint.Style.FILL
        }
        canvas.drawPath(glintPath, glintPaint)
        canvas.restore()

        val winBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) {
                OneUiCanvasPalette.CASSETTE_WINDOW_BORDER_DARK
            } else {
                OneUiCanvasPalette.CASSETTE_WINDOW_BORDER_LIGHT
            }
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.005f).coerceAtLeast(1.4f)
        }
        canvas.drawPath(winPath, winBorderPaint)

        drawCassetteCounter(canvas, w * 0.5f, spoolY, w, h, progress)
    }

    private fun interpolateTapeRadius(minR: Float, maxR: Float, fraction: Float): Float {
        val clamped = fraction.coerceIn(0f, 1f)
        val areaSq = minR * minR + (maxR * maxR - minR * minR) * clamped
        return sqrt(areaSq)
    }

    private fun drawTapeSpool(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        tapeR: Float,
        hubR: Float,
        rotationDeg: Float
    ) {
        val tapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_TAPE_SPOOL_BROWN
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, tapeR, tapePaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_TAPE_RING
            style = Paint.Style.STROKE
            strokeWidth = (hubR * 0.08f).coerceAtLeast(1f)
        }
        val midR = (hubR + tapeR) * 0.52f
        if (midR > hubR * 1.15f) {
            canvas.drawCircle(cx, cy, midR, ringPaint)
        }
        canvas.drawCircle(cx, cy, tapeR * 0.94f, ringPaint)

        val reelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_REEL_WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, hubR, reelPaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_HUB_CORE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, hubR * 0.60f, corePaint)

        // 6 drive teeth projecting inward from the reel hub
        val toothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_REEL_WHITE
            style = Paint.Style.STROKE
            strokeWidth = (hubR * 0.18f).coerceAtLeast(1.5f)
            strokeCap = Paint.Cap.BUTT
        }
        val rimOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_HUB_TEETH
            style = Paint.Style.STROKE
            strokeWidth = (hubR * 0.08f).coerceAtLeast(1f)
        }
        canvas.drawCircle(cx, cy, hubR * 0.98f, rimOutlinePaint)

        for (i in 0 until 6) {
            val angleRad = Math.toRadians((rotationDeg + i * 60f).toDouble())
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()
            canvas.drawLine(
                cx + hubR * 0.68f * cosA,
                cy + hubR * 0.68f * sinA,
                cx + hubR * 0.40f * cosA,
                cy + hubR * 0.40f * sinA,
                toothPaint
            )
        }
    }

    private fun drawCassetteCounter(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        progress: Float
    ) {
        val boxW = w * 0.145f
        val boxH = h * 0.135f
        val boxRect = RectF(cx - boxW / 2f, cy - boxH / 2f, cx + boxW / 2f, cy + boxH / 2f)
        val corner = boxH * 0.22f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_COUNTER_BG
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_COUNTER_BORDER
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.004f).coerceAtLeast(1f)
        }
        canvas.drawRoundRect(boxRect, corner, corner, bgPaint)
        canvas.drawRoundRect(boxRect, corner, corner, borderPaint)

        val counterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_COUNTER_TEXT
            textSize = boxH * 0.62f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val digits = String.format(Locale.US, "%03d", (progress * 360f).roundToInt())
        val textY = boxRect.centerY() - (counterPaint.descent() + counterPaint.ascent()) / 2f
        canvas.drawText(digits, boxRect.centerX(), textY, counterPaint)
    }

    private fun drawCassetteBottomBridge(
        canvas: Canvas,
        shellRect: RectF,
        w: Float,
        h: Float,
        isDarkTheme: Boolean,
        isPlaying: Boolean
    ) {
        val botY = shellRect.bottom
        val topY = h * 0.775f
        val botHalfW = w * 0.31f
        val topHalfW = w * 0.265f
        val cx = w * 0.5f

        val bridgePath = Path().apply {
            moveTo(cx - botHalfW, botY)
            lineTo(cx - topHalfW, topY)
            lineTo(cx + topHalfW, topY)
            lineTo(cx + botHalfW, botY)
            close()
        }
        val bridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.CASSETTE_BRIDGE_DARK else OneUiCanvasPalette.CASSETTE_BRIDGE_LIGHT
            style = Paint.Style.FILL
        }
        val bridgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.CASSETTE_BEVEL_DARK else OneUiCanvasPalette.CASSETTE_BEVEL_LIGHT
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.0045f).coerceAtLeast(1.2f)
        }
        canvas.drawPath(bridgePath, bridgePaint)
        canvas.drawPath(bridgePath, bridgeStroke)

        // Subtle magnetic tape ribbon exposed along bottom head opening
        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_TAPE_RIBBON
            style = Paint.Style.STROKE
            strokeWidth = (h * 0.016f).coerceAtLeast(1.8f)
        }
        val ribbonY = botY - h * 0.025f
        canvas.drawLine(cx - topHalfW * 0.72f, ribbonY, cx + topHalfW * 0.72f, ribbonY, ribbonPaint)

        // Capstan & alignment pin holes + active playhead LED indicator
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.CASSETTE_HOLE_DARK
            style = Paint.Style.FILL
        }
        val holeY = (topY + botY) * 0.52f
        val holeR = (h * 0.032f).coerceAtLeast(3f)
        canvas.drawCircle(cx - w * 0.165f, holeY, holeR, holePaint)
        canvas.drawCircle(cx + w * 0.165f, holeY, holeR, holePaint)
        canvas.drawCircle(cx - w * 0.088f, holeY + h * 0.014f, holeR * 0.72f, holePaint)
        canvas.drawCircle(cx + w * 0.088f, holeY + h * 0.014f, holeR * 0.72f, holePaint)

        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isPlaying) {
                OneUiCanvasPalette.CASSETTE_PLAYHEAD_ACTIVE
            } else {
                OneUiCanvasPalette.CASSETTE_PLAYHEAD_IDLE
            }
            style = Paint.Style.FILL
        }
        val headW = w * 0.045f
        val headH = h * 0.024f
        canvas.drawRoundRect(
            RectF(cx - headW / 2f, topY + h * 0.03f, cx + headW / 2f, topY + h * 0.03f + headH),
            headH / 2f,
            headH / 2f,
            headPaint
        )
    }
}

private object VinylTurntablePainter {

    fun render(
        albumArt: Bitmap?,
        progressFraction: Float,
        sizePx: Int,
        isDarkTheme: Boolean,
        isPlaying: Boolean
    ): Bitmap {
        val safeSize = sizePx.coerceIn(120, 640)
        val progress = if (progressFraction.isNaN()) 0f else progressFraction.coerceIn(0f, 1f)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val s = safeSize.toFloat()

        val cx = s * 0.455f
        val cy = s * 0.525f
        val platterR = s * 0.42f
        val vinylR = platterR * 0.895f
        val labelR = vinylR * 0.355f

        drawVinylPlatterAndStrobe(canvas, cx, cy, platterR, s, isDarkTheme, isPlaying)
        drawVinylDiscAndGrooves(canvas, cx, cy, vinylR, labelR, s)
        drawVinylCenterLabel(canvas, cx, cy, labelR, s, albumArt)
        drawVinylTonearm(canvas, s, progress, isDarkTheme, isPlaying)

        return bitmap
    }

    private fun drawVinylPlatterAndStrobe(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        platterR: Float,
        s: Float,
        isDarkTheme: Boolean,
        isPlaying: Boolean
    ) {
        val recessPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) {
                OneUiCanvasPalette.VINYL_PLINTH_RECESS_DARK
            } else {
                OneUiCanvasPalette.VINYL_PLINTH_RECESS_LIGHT
            }
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, platterR * 1.032f, recessPaint)

        val platterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, cy - platterR, cx, cy + platterR,
                if (isDarkTheme) OneUiCanvasPalette.VINYL_PLATTER_TOP_DARK else OneUiCanvasPalette.VINYL_PLATTER_TOP_LIGHT,
                if (isDarkTheme) {
                    OneUiCanvasPalette.VINYL_PLATTER_BOTTOM_DARK
                } else {
                    OneUiCanvasPalette.VINYL_PLATTER_BOTTOM_LIGHT
                },
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, platterR, platterPaint)

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.VINYL_PLATTER_RIM_DARK else OneUiCanvasPalette.VINYL_PLATTER_RIM_LIGHT
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.0045f).coerceAtLeast(1.2f)
        }
        canvas.drawCircle(cx, cy, platterR, rimPaint)

        // Stroboscopic speed dots around brushed platter rim
        val strobeR = platterR * 0.946f
        val dotR = (s * 0.0048f).coerceAtLeast(1f)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.VINYL_STROBE_DOT_DARK else OneUiCanvasPalette.VINYL_STROBE_DOT_LIGHT
            style = Paint.Style.FILL
        }
        val dotCount = 42
        for (i in 0 until dotCount) {
            val angleRad = Math.toRadians((i * (360.0 / dotCount)))
            val dx = cx + (strobeR * cos(angleRad)).toFloat()
            val dy = cy + (strobeR * sin(angleRad)).toFloat()
            canvas.drawCircle(dx, dy, dotR, dotPaint)
        }

        if (isPlaying) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = OneUiCanvasPalette.VINYL_STROBE_GLOW_ORANGE
                style = Paint.Style.FILL
            }
            val strobeAngle = Math.toRadians(140.0)
            canvas.drawCircle(
                cx + (platterR * 1.01f * cos(strobeAngle)).toFloat(),
                cy + (platterR * 1.01f * sin(strobeAngle)).toFloat(),
                dotR * 1.9f,
                glowPaint
            )
        }
    }

    private fun drawVinylDiscAndGrooves(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        vinylR: Float,
        labelR: Float,
        s: Float
    ) {
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_DISC_BASE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, vinylR, discPaint)

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_DISC_RIM
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.0045f).coerceAtLeast(1.2f)
        }
        canvas.drawCircle(cx, cy, vinylR, rimPaint)

        val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_GROOVE_STROKE
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.0028f).coerceAtLeast(0.8f)
        }
        val leadOutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_LEADOUT_STROKE
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.0036f).coerceAtLeast(1f)
        }
        canvas.drawCircle(cx, cy, labelR * 1.12f, leadOutPaint)

        val startR = labelR * 1.24f
        val endR = vinylR * 0.96f
        val ringCount = 14
        for (i in 0 until ringCount) {
            val t = i.toFloat() / (ringCount - 1)
            canvas.drawCircle(cx, cy, startR + (endR - startR) * t, groovePaint)
        }

        // Diagonal anisotropic highlight sheen clipped strictly to the grooved vinyl annulus
        val grooveRing = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addCircle(cx, cy, vinylR * 0.99f, Path.Direction.CW)
            addCircle(cx, cy, labelR * 1.02f, Path.Direction.CW)
        }
        val sweepMatrix = Matrix().apply { setRotate(-35f, cx, cy) }
        val sheenShader = SweepGradient(
            cx,
            cy,
            intArrayOf(
                OneUiCanvasPalette.VINYL_SPECULAR_CLEAR,
                OneUiCanvasPalette.VINYL_SPECULAR_SHEEN,
                OneUiCanvasPalette.VINYL_SPECULAR_CLEAR,
                OneUiCanvasPalette.VINYL_SPECULAR_SOFT,
                OneUiCanvasPalette.VINYL_SPECULAR_CLEAR,
                OneUiCanvasPalette.VINYL_SPECULAR_SHEEN,
                OneUiCanvasPalette.VINYL_SPECULAR_CLEAR,
                OneUiCanvasPalette.VINYL_SPECULAR_SOFT,
                OneUiCanvasPalette.VINYL_SPECULAR_CLEAR
            ),
            floatArrayOf(0f, 0.11f, 0.24f, 0.37f, 0.50f, 0.61f, 0.74f, 0.87f, 1f)
        ).apply { setLocalMatrix(sweepMatrix) }

        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = sheenShader
            style = Paint.Style.FILL
        }
        canvas.save()
        canvas.clipPath(grooveRing)
        canvas.drawCircle(cx, cy, vinylR, sheenPaint)
        canvas.restore()
    }

    private fun isRenderableAlbumArt(albumArt: Bitmap): Boolean {
        if (albumArt.isRecycled) return false
        return albumArt.width > 0 && albumArt.height > 0
    }

    private fun drawVinylCenterLabel(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        labelR: Float,
        s: Float,
        albumArt: Bitmap?
    ) {
        if (albumArt != null && isRenderableAlbumArt(albumArt)) {
            val cropSide = minOf(albumArt.width, albumArt.height)
            val srcLeft = (albumArt.width - cropSide) / 2
            val srcTop = (albumArt.height - cropSide) / 2
            val srcRect = Rect(srcLeft, srcTop, srcLeft + cropSide, srcTop + cropSide)
            val dstRect = RectF(cx - labelR, cy - labelR, cx + labelR, cy + labelR)
            val labelClip = Path().apply { addCircle(cx, cy, labelR, Path.Direction.CW) }
            val artPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            canvas.save()
            canvas.clipPath(labelClip)
            canvas.drawBitmap(albumArt, srcRect, dstRect, artPaint)
            canvas.restore()
        } else {
            val outerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = OneUiCanvasPalette.VINYL_LABEL_OUTER
                style = Paint.Style.FILL
            }
            val innerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = OneUiCanvasPalette.VINYL_LABEL_INNER
                style = Paint.Style.FILL
            }
            val accentStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = OneUiCanvasPalette.VINYL_LABEL_ACCENT
                style = Paint.Style.STROKE
                strokeWidth = (s * 0.004f).coerceAtLeast(1f)
            }
            val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = OneUiCanvasPalette.VINYL_LABEL_TEXT
                textSize = labelR * 0.24f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawCircle(cx, cy, labelR, outerLabelPaint)
            canvas.drawCircle(cx, cy, labelR * 0.72f, innerLabelPaint)
            canvas.drawCircle(cx, cy, labelR * 0.72f, accentStroke)
            canvas.drawText("33⅓", cx, cy - labelR * 0.30f, labelTextPaint)
            canvas.drawText("HI-FI", cx, cy + labelR * 0.48f, labelTextPaint)
        }

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_DISC_RIM
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.004f).coerceAtLeast(1f)
        }
        canvas.drawCircle(cx, cy, labelR, rimPaint)

        // Chrome center spindle dot
        val spindleR = (s * 0.020f).coerceAtLeast(3f)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_SPINDLE_SHADOW
            style = Paint.Style.FILL
        }
        val spindlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx - spindleR, cy - spindleR, cx + spindleR, cy + spindleR,
                OneUiCanvasPalette.VINYL_SPINDLE_CHROME_LIGHT,
                OneUiCanvasPalette.VINYL_SPINDLE_CHROME_DARK,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, spindleR * 1.42f, shadowPaint)
        canvas.drawCircle(cx, cy, spindleR, spindlePaint)
    }

    private fun drawVinylTonearm(
        canvas: Canvas,
        s: Float,
        progress: Float,
        isDarkTheme: Boolean,
        isPlaying: Boolean
    ) {
        val pivotX = s * 0.855f
        val pivotY = s * 0.165f
        val armLength = s * 0.515f
        val armAngleDeg = if (isPlaying) {
            99.5f + (122.5f - 99.5f) * progress
        } else {
            85f
        }

        // Tonearm rest post on right deck
        val restPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_ARM_REST_DARK
            style = Paint.Style.FILL
        }
        canvas.drawCircle(s * 0.888f, s * 0.495f, s * 0.020f, restPaint)

        // Gimbal base plinth
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.VINYL_TONEARM_BASE_DARK else OneUiCanvasPalette.VINYL_TONEARM_BASE_LIGHT
            style = Paint.Style.FILL
        }
        val gimbalRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_TONEARM_GIMBAL_RING
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.006f).coerceAtLeast(1.5f)
        }
        val gimbalR = s * 0.082f
        canvas.drawCircle(pivotX, pivotY, gimbalR, basePaint)
        canvas.drawCircle(pivotX, pivotY, gimbalR * 0.72f, gimbalRingPaint)

        // Local S-shaped arm path from pivot (0, 0) to headshell mount (armLength * 0.84, 0)
        val tubeEnd = armLength * 0.84f
        val sCurvePath = Path().apply {
            moveTo(0f, 0f)
            cubicTo(
                armLength * 0.30f, -armLength * 0.088f,
                armLength * 0.58f, armLength * 0.088f,
                tubeEnd, 0f
            )
        }

        // Shadow pass shifted down-right in global coordinates
        val shadowOffset = s * 0.014f
        canvas.save()
        canvas.translate(pivotX + shadowOffset, pivotY + shadowOffset)
        canvas.rotate(armAngleDeg)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_TONEARM_SHADOW
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.022f).coerceAtLeast(2.5f)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(sCurvePath, shadowPaint)
        canvas.restore()

        // Metallic tonearm, rear counterweight, and cartridge headshell
        canvas.save()
        canvas.translate(pivotX, pivotY)
        canvas.rotate(armAngleDeg)

        val counterRect = RectF(-armLength * 0.21f, -s * 0.024f, -armLength * 0.08f, s * 0.024f)
        val armStubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_TONEARM_SILVER_DARK
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.018f).coerceAtLeast(2f)
        }
        canvas.drawLine(-armLength * 0.10f, 0f, 0f, 0f, armStubPaint)
        val counterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_COUNTERWEIGHT_DARK
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(counterRect, s * 0.008f, s * 0.008f, counterPaint)

        val armTubePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, -s * 0.02f, 0f, s * 0.02f,
                OneUiCanvasPalette.VINYL_TONEARM_SILVER_LIGHT,
                OneUiCanvasPalette.VINYL_TONEARM_SILVER_DARK,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.019f).coerceAtLeast(2.2f)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(sCurvePath, armTubePaint)

        // Angled cartridge headshell + Braun orange stylus stripe
        canvas.save()
        canvas.translate(tubeEnd, 0f)
        canvas.rotate(-14f)
        val shellW = armLength * 0.165f
        val shellH = s * 0.036f
        val shellRect = RectF(0f, -shellH / 2f, shellW, shellH / 2f)
        val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_HEADSHELL_BODY
            style = Paint.Style.FILL
        }
        val stylusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_HEADSHELL_ACCENT
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(shellRect, shellH * 0.25f, shellH * 0.25f, shellPaint)
        canvas.drawRect(shellW * 0.72f, -shellH * 0.34f, shellW * 0.90f, shellH * 0.34f, stylusPaint)
        canvas.restore()

        canvas.restore()

        // Gimbal pivot cap + orange center indicator
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_TONEARM_SILVER_LIGHT
            style = Paint.Style.FILL
        }
        val capAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.VINYL_HEADSHELL_ACCENT
            style = Paint.Style.FILL
        }
        canvas.drawCircle(pivotX, pivotY, s * 0.032f, capPaint)
        canvas.drawCircle(pivotX, pivotY, s * 0.013f, capAccent)
    }
}

private object MiniDiscBadgePainter {

    fun render(
        sizePx: Int,
        isDarkTheme: Boolean,
        isPlaying: Boolean
    ): Bitmap {
        val safeSize = sizePx.coerceIn(48, 320)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val s = safeSize.toFloat()
        val pad = s * 0.06f
        val shellRect = RectF(pad, pad, s - pad, s - pad)
        val corner = s * 0.085f

        val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, shellRect.top, 0f, shellRect.bottom,
                if (isDarkTheme) OneUiCanvasPalette.MINIDISC_SHELL_TOP_DARK else OneUiCanvasPalette.MINIDISC_SHELL_TOP_LIGHT,
                if (isDarkTheme) {
                    OneUiCanvasPalette.MINIDISC_SHELL_BOTTOM_DARK
                } else {
                    OneUiCanvasPalette.MINIDISC_SHELL_BOTTOM_LIGHT
                },
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(shellRect, corner, corner, shellPaint)

        val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_SHELL_BEVEL
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.018f).coerceAtLeast(1.4f)
        }
        canvas.drawRoundRect(shellRect, corner, corner, bevelPaint)

        // Insertion arrow at top-left edge
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_ARROW_ACCENT
            style = Paint.Style.FILL
        }
        val arrowPath = Path().apply {
            moveTo(shellRect.left + s * 0.14f, shellRect.top + s * 0.035f)
            lineTo(shellRect.left + s * 0.095f, shellRect.top + s * 0.095f)
            lineTo(shellRect.left + s * 0.185f, shellRect.top + s * 0.095f)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)

        // Smoky window on left/center showing iridescent magneto-optical disc
        val winRect = RectF(
            shellRect.left + shellRect.width() * 0.08f,
            shellRect.top + shellRect.height() * 0.14f,
            shellRect.left + shellRect.width() * 0.68f,
            shellRect.bottom - shellRect.height() * 0.14f
        )
        val winCorner = s * 0.045f
        val winClip = Path().apply { addRoundRect(winRect, winCorner, winCorner, Path.Direction.CW) }

        val discCx = shellRect.centerX() - s * 0.02f
        val discCy = shellRect.centerY()
        val discR = s * 0.34f
        val discMatrix = Matrix().apply {
            setRotate(if (isPlaying) 32f else -12f, discCx, discCy)
        }
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = SweepGradient(
                discCx,
                discCy,
                intArrayOf(
                    OneUiCanvasPalette.MINIDISC_DISC_TEAL,
                    OneUiCanvasPalette.MINIDISC_DISC_CYAN,
                    OneUiCanvasPalette.MINIDISC_DISC_VIOLET,
                    OneUiCanvasPalette.MINIDISC_DISC_MAGENTA,
                    OneUiCanvasPalette.MINIDISC_DISC_INDIGO,
                    OneUiCanvasPalette.MINIDISC_DISC_TEAL
                ),
                null
            ).apply { setLocalMatrix(discMatrix) }
            style = Paint.Style.FILL
        }

        canvas.save()
        canvas.clipPath(winClip)
        canvas.drawCircle(discCx, discCy, discR, discPaint)

        val discRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_DISC_RING
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.012f).coerceAtLeast(1f)
        }
        canvas.drawCircle(discCx, discCy, discR * 0.78f, discRingPaint)
        canvas.drawCircle(discCx, discCy, discR * 0.56f, discRingPaint)

        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_WINDOW_BG
            style = Paint.Style.FILL
        }
        canvas.drawRect(winRect, tintPaint)

        // Clamping hub in center of visible disc
        val hubOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_HUB_OUTER
            style = Paint.Style.FILL
        }
        val hubInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_HUB_INNER
            style = Paint.Style.FILL
        }
        val hubRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_HUB_RING
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.012f).coerceAtLeast(1f)
        }
        val hubR = s * 0.095f
        canvas.drawCircle(discCx, discCy, hubR, hubOuterPaint)
        canvas.drawCircle(discCx, discCy, hubR, hubRingPaint)
        canvas.drawCircle(discCx, discCy, hubR * 0.42f, hubInnerPaint)

        // Glass sheen across window
        val sheenPath = Path().apply {
            moveTo(winRect.left, winRect.top + winRect.height() * 0.30f)
            lineTo(winRect.left + winRect.width() * 0.42f, winRect.top)
            lineTo(winRect.left + winRect.width() * 0.65f, winRect.top)
            lineTo(winRect.left, winRect.top + winRect.height() * 0.54f)
            close()
        }
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_WINDOW_SHEEN
            style = Paint.Style.FILL
        }
        canvas.drawPath(sheenPath, sheenPaint)
        canvas.restore()

        val winBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_WINDOW_STROKE
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.014f).coerceAtLeast(1.2f)
        }
        canvas.drawPath(winClip, winBorderPaint)

        // Brushed silver sliding shutter plate on right edge
        val shutterRect = RectF(
            shellRect.left + shellRect.width() * 0.72f,
            shellRect.top + shellRect.height() * 0.04f,
            shellRect.right - shellRect.width() * 0.04f,
            shellRect.bottom - shellRect.height() * 0.04f
        )
        val shutterCorner = s * 0.032f
        val shutterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                shutterRect.left, shutterRect.top, shutterRect.left, shutterRect.bottom,
                intArrayOf(
                    OneUiCanvasPalette.MINIDISC_SHUTTER_TOP,
                    OneUiCanvasPalette.MINIDISC_SHUTTER_MID,
                    OneUiCanvasPalette.MINIDISC_SHUTTER_BOTTOM
                ),
                null,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        val shutterStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_SHUTTER_STROKE
            style = Paint.Style.STROKE
            strokeWidth = (s * 0.014f).coerceAtLeast(1f)
        }
        canvas.drawRoundRect(shutterRect, shutterCorner, shutterCorner, shutterPaint)
        canvas.drawRoundRect(shutterRect, shutterCorner, shutterCorner, shutterStroke)

        val shutterTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OneUiCanvasPalette.MINIDISC_SHUTTER_TEXT
            textSize = s * 0.088f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val mdY = shutterRect.centerY() - (shutterTextPaint.descent() + shutterTextPaint.ascent()) / 2f
        canvas.drawText("MD", shutterRect.centerX(), mdY, shutterTextPaint)

        return bitmap
    }
}
