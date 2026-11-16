package com.anacatavc.oneuiextrawidgets.utils

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos

/**
 * Renders custom Canvas Bitmaps for Jetpack Glance widgets:
 * - Accurate Lunar Phase sphere with waxing/waning terminator curve
 * - iPod Classic Click Wheel graphic
 * - Portable CD Discman disc with album art mask & iridescent inner ring
 * - Signal quality bar/arc gauges for 5G & Wi-Fi
 */
object WidgetCanvasRenderer {

    /**
     * Draws a realistic Moon Phase sphere given phase angle in degrees [0..360).
     * 0 = New Moon, 90 = First Quarter, 180 = Full Moon, 270 = Third Quarter.
     */
    fun renderMoonPhaseBitmap(phaseAngleDeg: Double, sizePx: Int = 160): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = sizePx * 0.44f
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A2D37")
            style = Paint.Style.FILL
        }
        val litPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5F3CE")
            style = Paint.Style.FILL
        }
        val craterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22000000")
            style = Paint.Style.FILL
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
            // Right limb is lit
            path.arcTo(outerOval, -90f, 180f)
            if (terminatorScale >= 0f) {
                // Crescent: subtract inner ellipse
                path.arcTo(innerOval, 90f, -180f)
            } else {
                // Gibbous: add inner ellipse
                path.arcTo(innerOval, 90f, 180f)
            }
        } else {
            // Left limb is lit
            path.arcTo(outerOval, 90f, 180f)
            if (terminatorScale >= 0f) {
                path.arcTo(innerOval, -90f, -180f)
            } else {
                path.arcTo(innerOval, -90f, 180f)
            }
        }
        path.close()
        canvas.drawPath(path, litPaint)

        // Subtle craters
        canvas.drawCircle(cx - radius * 0.28f, cy - radius * 0.2f, radius * 0.14f, craterPaint)
        canvas.drawCircle(cx + radius * 0.22f, cy + radius * 0.18f, radius * 0.18f, craterPaint)
        canvas.drawCircle(cx + radius * 0.1f, cy - radius * 0.35f, radius * 0.1f, craterPaint)

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
            color = if (isDarkTheme) Color.parseColor("#2B2C30") else Color.parseColor("#E5E6EB")
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) Color.parseColor("#44464E") else Color.parseColor("#C6C8D0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, cy - innerR, cx, cy + innerR,
                if (isDarkTheme) Color.parseColor("#3E4047") else Color.parseColor("#FFFFFF"),
                if (isDarkTheme) Color.parseColor("#1D1E22") else Color.parseColor("#D4D6DE"),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) Color.parseColor("#D8DAE2") else Color.parseColor("#5A5D66")
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
     * Renders a Compact Disc (Discman) bitmap, masking the provided albumArt (or iridescent metallic surface)
     * with a transparent/hub center hole.
     */
    fun renderDiscmanCdBitmap(albumArt: Bitmap?, sizePx: Int = 200): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val outerR = sizePx * 0.46f
        val hubR = sizePx * 0.15f
        val holeR = sizePx * 0.06f

        if (albumArt != null) {
            val scaled = Bitmap.createScaledBitmap(albumArt, sizePx, sizePx, true)
            val artPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(cx, cy, outerR, artPaint)
        } else {
            val cdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                    intArrayOf(
                        Color.parseColor("#B8C6DB"),
                        Color.parseColor("#F5F7FA"),
                        Color.parseColor("#8EC5FC"),
                        Color.parseColor("#E0C3FC"),
                        Color.parseColor("#B8C6DB")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, outerR, cdPaint)
        }

        // Outer metallic rim & inner polycarbonate hub
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AAFFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC22252A")
            style = Paint.Style.FILL
        }
        val spindlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#141518")
            style = Paint.Style.FILL
        }

        canvas.drawCircle(cx, cy, outerR, rimPaint)
        canvas.drawCircle(cx, cy, hubR, hubPaint)
        canvas.drawCircle(cx, cy, hubR, rimPaint)
        canvas.drawCircle(cx, cy, holeR, spindlePaint)

        return bitmap
    }
}
