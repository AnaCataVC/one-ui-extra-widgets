package com.anacatavc.oneuiextrawidgets.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
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
 * - One UI 7 Rain, AQI, UV, Sun Arc, Stargazing, and Battery Thermal radial gauges
 * - Retro music player hardware graphics (iPod, Discman, Cassette, Vinyl, MiniDisc)
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

        drawRadialGaugeTrack(canvas, arcRect, strokeW, isDarkTheme)

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

        drawRadialGaugeTrack(canvas, arcRect, strokeW, isDarkTheme)

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
        drawRadialTipIndicator(canvas, cx, cy, radius, strokeW, sweep, aqiColor, isDarkTheme)

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

        drawRadialGaugeTrack(canvas, arcRect, strokeW, isDarkTheme)

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
            color = OneUiCanvasPalette.UV_SUN_RAYS
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
     * Renders the iPod Classic Click Wheel ring with MENU text, geometric media icons, and center button.
     */
    fun renderIpodClickWheelBitmap(
        sizePx: Int = 220,
        isDarkTheme: Boolean = true,
        isPlaying: Boolean = false
    ): Bitmap =
        MusicCanvasRenderer.renderIpodClickWheelBitmap(
            sizePx = sizePx,
            isDarkTheme = isDarkTheme,
            isPlaying = isPlaying
        )

    /**
     * Renders a circular portable CD player (Discman) shell with metallic bevel rim,
     * COMPACT DISC / ESP ANTI-SHOCK branding, central pill module recess, and bottom speaker grille.
     */
    fun renderDiscmanBodyBitmap(
        sizePx: Int = 420,
        isDarkTheme: Boolean = true,
        opacityPct: Int = 100
    ): Bitmap = MusicCanvasRenderer.renderDiscmanBodyBitmap(
        sizePx = sizePx,
        isDarkTheme = isDarkTheme,
        opacityPct = opacityPct
    )

    /**
     * Renders a retro Walkman compact cassette deck with dynamic tape spools, 6-tooth hubs,
     * 3-digit counter, and bottom head bridge.
     */
    fun renderCassetteDeckBitmap(
        progressFraction: Float,
        sizePx: Int = 360,
        isDarkTheme: Boolean = true,
        isPlaying: Boolean = false,
        opacityPct: Int = 100
    ): Bitmap = MusicCanvasRenderer.renderCassetteDeckBitmap(
        progressFraction = progressFraction,
        sizePx = sizePx,
        isDarkTheme = isDarkTheme,
        isPlaying = isPlaying,
        opacityPct = opacityPct
    )

    /**
     * Renders a Braun / Dieter Rams vinyl turntable platter, stroboscopic speed rim,
     * micro-grooved LP disc with circular-clipped album art, and articulated S-shaped tonearm.
     */
    fun renderVinylTurntableBitmap(
        albumArt: Bitmap?,
        progressFraction: Float,
        sizePx: Int = 320,
        isDarkTheme: Boolean = true,
        isPlaying: Boolean = false
    ): Bitmap = MusicCanvasRenderer.renderVinylTurntableBitmap(
        albumArt = albumArt,
        progressFraction = progressFraction,
        sizePx = sizePx,
        isDarkTheme = isDarkTheme,
        isPlaying = isPlaying
    )

    /**
     * Renders a square Sony-style MiniDisc cartridge badge with smoky window,
     * iridescent magneto-optical disc, clamping hub, and brushed silver shutter.
     */
    fun renderMiniDiscBadgeBitmap(
        sizePx: Int = 128,
        isDarkTheme: Boolean = true,
        isPlaying: Boolean = false
    ): Bitmap = MusicCanvasRenderer.renderMiniDiscBadgeBitmap(
        sizePx = sizePx,
        isDarkTheme = isDarkTheme,
        isPlaying = isPlaying
    )

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

    /**
     * Renders a One UI 270° Sun Arc & Golden/Blue Hour radial gauge with an interior horizon line,
     * golden daytime parabola, dashed twilight curve, and sun orb tracking [sunAltitudeDeg] and
     * [dayProgressFraction].
     */
    fun renderSunArcGaugeBitmap(
        sunAltitudeDeg: Double,
        dayProgressFraction: Float,
        sizePx: Int = 160,
        isDarkTheme: Boolean = true
    ): Bitmap = EnvironmentalGaugePainter.renderSunArcGauge(
        sunAltitudeDeg = sunAltitudeDeg,
        dayProgressFraction = dayProgressFraction,
        sizePx = sizePx,
        isDarkTheme = isDarkTheme
    )

    /**
     * Renders a One UI 270° Stargazing Clarity radial gauge with an indigo-to-cyan arc,
     * indicator dot, central night-sky constellation emblem with 4-point sparkles, and percentage.
     */
    fun renderStargazingGaugeBitmap(
        stargazingIndexPct: Int,
        sizePx: Int = 160,
        isDarkTheme: Boolean = true
    ): Bitmap = EnvironmentalGaugePainter.renderStargazingGauge(
        stargazingIndexPct = stargazingIndexPct,
        sizePx = sizePx,
        isDarkTheme = isDarkTheme
    )

    /**
     * Renders a One UI 270° Battery Thermal radial gauge mapped across 15°C..50°C with
     * tier-colored arc (< 35°C green, 35..40°C amber, > 40°C red), indicator dot, and °C readout.
     */
    fun renderBatteryThermalGaugeBitmap(
        temperatureC: Double,
        sizePx: Int = 160,
        isDarkTheme: Boolean = true
    ): Bitmap = EnvironmentalGaugePainter.renderBatteryThermalGauge(
        temperatureC = temperatureC,
        sizePx = sizePx,
        isDarkTheme = isDarkTheme
    )
}

private fun drawRadialGaugeTrack(
    canvas: Canvas,
    arcRect: RectF,
    strokeW: Float,
    isDarkTheme: Boolean
) {
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkTheme) OneUiCanvasPalette.GAUGE_TRACK_DARK else OneUiCanvasPalette.GAUGE_TRACK_LIGHT
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)
}

private fun drawRadialTipIndicator(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    strokeW: Float,
    sweep: Float,
    dotColor: Int,
    isDarkTheme: Boolean
) {
    val endAngleRad = Math.toRadians((135f + sweep).toDouble())
    val dotX = cx + (radius * cos(endAngleRad)).toFloat()
    val dotY = cy + (radius * sin(endAngleRad)).toFloat()
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkTheme) {
            OneUiCanvasPalette.INDICATOR_RING_DARK
        } else {
            OneUiCanvasPalette.INDICATOR_RING_LIGHT
        }
        style = Paint.Style.FILL
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dotColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(dotX, dotY, strokeW * 0.65f, ringPaint)
    canvas.drawCircle(dotX, dotY, strokeW * 0.42f, dotPaint)
}

private object EnvironmentalGaugePainter {

    fun renderSunArcGauge(
        sunAltitudeDeg: Double,
        dayProgressFraction: Float,
        sizePx: Int,
        isDarkTheme: Boolean
    ): Bitmap {
        val safeSize = sizePx.coerceIn(64, 360)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = safeSize / 2f
        val cy = safeSize / 2f
        val strokeW = safeSize * 0.105f
        val radius = safeSize * 0.41f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        drawRadialGaugeTrack(canvas, arcRect, strokeW, isDarkTheme)

        val safeAlt = if (sunAltitudeDeg.isNaN()) 0.0 else sunAltitudeDeg.coerceIn(-90.0, 90.0)
        val safeProg = if (dayProgressFraction.isNaN()) 0.5f else dayProgressFraction.coerceIn(0f, 1f)
        val isAboveHorizon = safeAlt >= 0.0
        val twilightColor = if (isDarkTheme) {
            OneUiCanvasPalette.SUN_ARC_TWILIGHT_DARK
        } else {
            OneUiCanvasPalette.SUN_ARC_TWILIGHT_LIGHT
        }

        val sweep = (safeProg * 270f).coerceAtLeast(16f)
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = if (isAboveHorizon) {
                LinearGradient(
                    cx - radius, cy, cx + radius, cy,
                    OneUiCanvasPalette.SUN_ARC_GOLD_START,
                    OneUiCanvasPalette.SUN_ARC_GOLD_END,
                    Shader.TileMode.CLAMP
                )
            } else {
                LinearGradient(
                    cx - radius, cy, cx + radius, cy,
                    if (isDarkTheme) OneUiCanvasPalette.STAR_ARC_INDIGO_DARK else OneUiCanvasPalette.STAR_ARC_INDIGO_LIGHT,
                    twilightColor,
                    Shader.TileMode.CLAMP
                )
            }
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, sweep, false, activePaint)

        // Interior horizon line, dashed twilight dip, and golden daytime solar parabola
        val horizonY = cy + safeSize * 0.055f
        val pathHalfW = safeSize * 0.245f
        val peakH = safeSize * 0.195f
        val twilightDip = safeSize * 0.095f

        val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) {
                OneUiCanvasPalette.SUN_ARC_HORIZON_DARK
            } else {
                OneUiCanvasPalette.SUN_ARC_HORIZON_LIGHT
            }
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.016f).coerceAtLeast(1.4f)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx - pathHalfW * 1.06f, horizonY, cx + pathHalfW * 1.06f, horizonY, horizonPaint)

        val dashLen = (safeSize * 0.032f).coerceAtLeast(3f)
        val twilightPath = Path().apply {
            moveTo(cx - pathHalfW, horizonY)
            quadTo(cx, horizonY + twilightDip * 1.85f, cx + pathHalfW, horizonY)
        }
        val twilightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = twilightColor
            alpha = 190
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.020f).coerceAtLeast(1.6f)
            strokeCap = Paint.Cap.ROUND
            pathEffect = DashPathEffect(floatArrayOf(dashLen, dashLen * 0.85f), 0f)
        }
        canvas.drawPath(twilightPath, twilightPaint)

        val solarPath = Path().apply {
            moveTo(cx - pathHalfW, horizonY)
            quadTo(cx, horizonY - peakH * 2f, cx + pathHalfW, horizonY)
        }
        val solarArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx - pathHalfW, horizonY, cx + pathHalfW, horizonY - peakH,
                intArrayOf(
                    OneUiCanvasPalette.SUN_ARC_GOLD_START,
                    OneUiCanvasPalette.SUN_ARC_NOON_PEAK,
                    OneUiCanvasPalette.SUN_ARC_GOLD_END
                ),
                null,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.028f).coerceAtLeast(2.2f)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(solarPath, solarArcPaint)

        // Position sun orb along the solar/twilight path
        val orbX = cx + (safeProg * 2f - 1f) * pathHalfW
        val orbY = if (isAboveHorizon) {
            val curveFactor = 4f * safeProg * (1f - safeProg)
            val altNorm = if (safeAlt <= 0.0) 0f else (safeAlt / 60.0).toFloat().coerceIn(0.10f, 1f)
            val heightNorm = if (safeAlt <= 0.0) 0f else (0.5f * curveFactor + 0.5f * altNorm)
            horizonY - peakH * heightNorm
        } else {
            val depthNorm = (-safeAlt / 36.0).toFloat().coerceIn(0.18f, 1f)
            horizonY + twilightDip * depthNorm
        }

        val orbR = (safeSize * 0.056f).coerceAtLeast(4.5f)
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isAboveHorizon) OneUiCanvasPalette.SUN_ORB_HALO else OneUiCanvasPalette.SUN_TWILIGHT_HALO
            style = Paint.Style.FILL
        }
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isAboveHorizon) OneUiCanvasPalette.SUN_ORB_CORE else OneUiCanvasPalette.SUN_TWILIGHT_ORB
            style = Paint.Style.FILL
        }
        canvas.drawCircle(orbX, orbY, orbR * 1.85f, haloPaint)
        canvas.drawCircle(orbX, orbY, orbR, orbPaint)

        if (isAboveHorizon) {
            val orbRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = OneUiCanvasPalette.SUN_ORB_RIM
                style = Paint.Style.STROKE
                strokeWidth = (safeSize * 0.012f).coerceAtLeast(1.2f)
            }
            canvas.drawCircle(orbX, orbY, orbR, orbRimPaint)
        }

        return bitmap
    }

    fun renderStargazingGauge(
        stargazingIndexPct: Int,
        sizePx: Int,
        isDarkTheme: Boolean
    ): Bitmap {
        val safeSize = sizePx.coerceIn(64, 360)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = safeSize / 2f
        val cy = safeSize / 2f
        val strokeW = safeSize * 0.105f
        val radius = safeSize * 0.41f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        drawRadialGaugeTrack(canvas, arcRect, strokeW, isDarkTheme)

        val clampedPct = stargazingIndexPct.coerceIn(0, 100)
        val sweep = (clampedPct / 100f).coerceAtLeast(0.06f) * 270f
        val indigoColor = if (isDarkTheme) {
            OneUiCanvasPalette.STAR_ARC_INDIGO_DARK
        } else {
            OneUiCanvasPalette.STAR_ARC_INDIGO_LIGHT
        }
        val cyanColor = if (isDarkTheme) {
            OneUiCanvasPalette.STAR_ARC_CYAN_DARK
        } else {
            OneUiCanvasPalette.STAR_ARC_CYAN_LIGHT
        }

        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx - radius, cy + radius, cx + radius, cy - radius,
                indigoColor,
                cyanColor,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, sweep, false, activePaint)
        drawRadialTipIndicator(canvas, cx, cy, radius, strokeW, sweep, cyanColor, isDarkTheme)

        // Central night-sky dome & constellation emblem
        val skyCy = cy - safeSize * 0.025f
        val skyR = safeSize * 0.255f
        val skyDomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.STAR_SKY_DOME_DARK else OneUiCanvasPalette.STAR_SKY_DOME_LIGHT
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, skyCy, skyR, skyDomePaint)

        val nodes = arrayOf(
            cx - skyR * 0.56f to skyCy + skyR * 0.26f,
            cx - skyR * 0.20f to skyCy - skyR * 0.24f,
            cx + skyR * 0.16f to skyCy + skyR * 0.10f,
            cx + skyR * 0.54f to skyCy - skyR * 0.34f
        )
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) {
                OneUiCanvasPalette.STAR_CONSTELLATION_LINE_DARK
            } else {
                OneUiCanvasPalette.STAR_CONSTELLATION_LINE_LIGHT
            }
            style = Paint.Style.STROKE
            strokeWidth = (safeSize * 0.014f).coerceAtLeast(1.2f)
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until nodes.size - 1) {
            canvas.drawLine(nodes[i].first, nodes[i].second, nodes[i + 1].first, nodes[i + 1].second, linePaint)
        }

        val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) {
                OneUiCanvasPalette.STAR_CONSTELLATION_CYAN_DARK
            } else {
                OneUiCanvasPalette.STAR_CONSTELLATION_CYAN_LIGHT
            }
            style = Paint.Style.FILL
        }
        for ((nx, ny) in nodes) {
            canvas.drawCircle(nx, ny, (safeSize * 0.018f).coerceAtLeast(1.5f), nodePaint)
        }

        val primarySparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.STAR_SPARKLE_GOLD else indigoColor
            style = Paint.Style.FILL
        }
        val secondarySparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.STAR_SPARKLE_WHITE else cyanColor
            style = Paint.Style.FILL
        }
        drawFourPointSparkle(canvas, cx - skyR * 0.12f, skyCy - skyR * 0.08f, safeSize * 0.085f, primarySparklePaint)
        drawFourPointSparkle(canvas, cx + skyR * 0.44f, skyCy + skyR * 0.28f, safeSize * 0.048f, secondarySparklePaint)
        drawFourPointSparkle(canvas, cx - skyR * 0.46f, skyCy - skyR * 0.38f, safeSize * 0.038f, secondarySparklePaint)

        // Crisp percentage readout seated in the open lower mouth of the 270° gauge
        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.TEXT_PRIMARY_DARK else OneUiCanvasPalette.TEXT_PRIMARY_LIGHT
            textSize = safeSize * 0.145f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("$clampedPct%", cx, cy + safeSize * 0.39f, pctPaint)

        return bitmap
    }

    private fun drawFourPointSparkle(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        paint: Paint
    ) {
        val path = Path().apply {
            moveTo(cx, cy - radius)
            quadTo(cx, cy, cx + radius, cy)
            quadTo(cx, cy, cx, cy + radius)
            quadTo(cx, cy, cx - radius, cy)
            quadTo(cx, cy, cx, cy - radius)
            close()
        }
        canvas.drawPath(path, paint)
    }

    fun renderBatteryThermalGauge(
        temperatureC: Double,
        sizePx: Int,
        isDarkTheme: Boolean
    ): Bitmap {
        val safeSize = sizePx.coerceIn(64, 360)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = safeSize / 2f
        val cy = safeSize / 2f
        val strokeW = safeSize * 0.105f
        val radius = safeSize * 0.41f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        drawRadialGaugeTrack(canvas, arcRect, strokeW, isDarkTheme)

        val safeTemp = if (temperatureC.isNaN()) 25.0 else temperatureC
        val thermalColor = when {
            safeTemp < 35.0 -> if (isDarkTheme) OneUiCanvasPalette.THERMAL_COOL_DARK else OneUiCanvasPalette.THERMAL_COOL_LIGHT
            safeTemp <= 40.0 -> if (isDarkTheme) OneUiCanvasPalette.THERMAL_WARM_DARK else OneUiCanvasPalette.THERMAL_WARM_LIGHT
            else -> if (isDarkTheme) OneUiCanvasPalette.THERMAL_HOT_DARK else OneUiCanvasPalette.THERMAL_HOT_LIGHT
        }

        val normalized = ((safeTemp - 15.0) / 35.0).coerceIn(0.06, 1.0).toFloat()
        val sweep = normalized * 270f
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = thermalColor
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, sweep, false, activePaint)
        drawRadialTipIndicator(canvas, cx, cy, radius, strokeW, sweep, thermalColor, isDarkTheme)

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) OneUiCanvasPalette.TEXT_PRIMARY_DARK else OneUiCanvasPalette.TEXT_PRIMARY_LIGHT
            textSize = safeSize * 0.25f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = thermalColor
            textSize = safeSize * 0.125f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("${safeTemp.roundToInt()}°", cx, cy + safeSize * 0.05f, valuePaint)
        canvas.drawText("°C", cx, cy + safeSize * 0.24f, labelPaint)

        return bitmap
    }
}



