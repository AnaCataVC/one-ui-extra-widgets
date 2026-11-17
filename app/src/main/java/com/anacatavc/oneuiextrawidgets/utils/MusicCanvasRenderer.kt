package com.anacatavc.oneuiextrawidgets.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiCanvasPalette
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import kotlin.math.cos
import kotlin.math.sin

/**
 * Deterministic Canvas bitmap renderers for the iPod Classic Click Wheel and Discman body.
 */
object MusicCanvasRenderer {

    /**
     * Renders the iPod Classic Click Wheel ring with MENU text, geometric media icons, and center button.
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

        return bitmap
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

        // Bottom: Play triangle + Pause double bar
        val bottomY = cy + outerR * 0.71f
        val playPath = Path().apply {
            moveTo(cx - iconR * 1.15f, bottomY - iconR * 0.72f)
            lineTo(cx - iconR * 0.1f, bottomY)
            lineTo(cx - iconR * 1.15f, bottomY + iconR * 0.72f)
            close()
        }
        canvas.drawPath(playPath, iconPaint)
        val barW = iconR * 0.3f
        val barH = iconR * 1.35f
        canvas.drawRect(cx + iconR * 0.18f, bottomY - barH / 2f, cx + iconR * 0.18f + barW, bottomY + barH / 2f, iconPaint)
        canvas.drawRect(cx + iconR * 0.72f, bottomY - barH / 2f, cx + iconR * 0.72f + barW, bottomY + barH / 2f, iconPaint)

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
                val dy = cy + (r * sin(angleRad)).toFloat()
                canvas.drawCircle(dx, dy, dotRadius, dotPaint)
            }
        }

        return if (alphaInt >= 255) {
            bitmap
        } else {
            val fadedBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val fadedCanvas = Canvas(fadedBitmap)
            val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = alphaInt }
            fadedCanvas.drawBitmap(bitmap, 0f, 0f, layerPaint)
            bitmap.recycle()
            fadedBitmap
        }
    }
}
