package com.anacatavc.oneuiextrawidgets.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs
import kotlin.math.roundToInt

enum class BatteryThermalLevel {
    COOL,
    WARM,
    HOT
}

fun classifyBatteryThermalLevel(temperatureC: Double): BatteryThermalLevel {
    if (temperatureC.isNaN()) return BatteryThermalLevel.COOL
    return when {
        temperatureC < 35.0 -> BatteryThermalLevel.COOL
        temperatureC <= 40.0 -> BatteryThermalLevel.WARM
        else -> BatteryThermalLevel.HOT
    }
}

fun computeBatteryPowerWatts(voltageMv: Int, currentMa: Int): Double =
    abs(voltageMv.toLong() * currentMa.toLong()) / 1_000_000.0

data class BatteryThermalSnapshot(
    val levelPct: Int = 85,
    val temperatureC: Double = 29.5,
    val voltageMv: Int = 4120,
    val currentMa: Int = 450,
    val powerWatts: Double = 1.85,
    val isCharging: Boolean = false,
    val powerSourceEs: String = "En batería",
    val powerSourceEn: String = "On Battery",
    val thermalLevel: BatteryThermalLevel = classifyBatteryThermalLevel(temperatureC)
) {
    fun localizedThermalLabel(isSpanish: Boolean): String = when (thermalLevel) {
        BatteryThermalLevel.COOL -> if (isSpanish) "Óptima" else "Optimal"
        BatteryThermalLevel.WARM -> if (isSpanish) "Templada" else "Warm"
        BatteryThermalLevel.HOT -> if (isSpanish) "Caliente" else "Hot"
    }

    fun localizedPowerSource(isSpanish: Boolean): String =
        if (isSpanish) powerSourceEs else powerSourceEn
}

/**
 * Reads live battery charge, thermal state, voltage, current, and estimated wattage
 * without background polling using the system ACTION_BATTERY_CHANGED sticky broadcast
 * and BatteryManager properties.
 */
object BatteryThermalMonitor {

    private const val DEFAULT_LEVEL_PCT = 85
    private const val DEFAULT_TEMP_C = 29.5
    private const val DEFAULT_VOLTAGE_MV = 4120
    private const val DEFAULT_CURRENT_MA = 450
    private const val MICROAMP_THRESHOLD = 10_000L
    private const val FAST_CHARGE_WATTS_THRESHOLD = 15.0

    fun readSnapshot(context: Context): BatteryThermalSnapshot {
        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val rawLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val levelPct = if (rawLevel >= 0 && scale > 0) {
            ((rawLevel * 100f) / scale.toFloat()).roundToInt().coerceIn(0, 100)
        } else {
            DEFAULT_LEVEL_PCT
        }

        val rawTempTenths = intent?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE,
            Int.MIN_VALUE
        ) ?: Int.MIN_VALUE
        val temperatureC = if (rawTempTenths != Int.MIN_VALUE && rawTempTenths in -500..1500) {
            rawTempTenths / 10.0
        } else {
            DEFAULT_TEMP_C
        }

        val rawVoltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltageMv = when (rawVoltage) {
            in 1..99 -> rawVoltage * 1000
            in 100..25_000 -> rawVoltage
            else -> DEFAULT_VOLTAGE_MV
        }

        val rawCurrent = runCatching {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: Int.MIN_VALUE
        }.getOrDefault(Int.MIN_VALUE)
        val currentMa = normalizeCurrentMa(rawCurrent)

        val powerWatts = computeBatteryPowerWatts(voltageMv, currentMa)

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL ||
            plugged > 0

        val (powerSourceEs, powerSourceEn) = resolvePowerSource(
            plugged = plugged,
            isCharging = isCharging,
            powerWatts = powerWatts
        )

        return BatteryThermalSnapshot(
            levelPct = levelPct,
            temperatureC = temperatureC,
            voltageMv = voltageMv,
            currentMa = currentMa,
            powerWatts = powerWatts,
            isCharging = isCharging,
            powerSourceEs = powerSourceEs,
            powerSourceEn = powerSourceEn,
            thermalLevel = classifyBatteryThermalLevel(temperatureC)
        )
    }

    internal fun normalizeCurrentMa(rawCurrent: Int): Int {
        if (rawCurrent == Int.MIN_VALUE || rawCurrent == 0) {
            return DEFAULT_CURRENT_MA
        }
        val magnitude = abs(rawCurrent.toLong())
        val milliamps = if (magnitude >= MICROAMP_THRESHOLD) {
            magnitude / 1000L
        } else {
            magnitude
        }
        return milliamps.toInt().coerceAtLeast(1)
    }

    internal fun resolvePowerSource(
        plugged: Int,
        isCharging: Boolean,
        powerWatts: Double
    ): Pair<String, String> = when {
        (plugged and BatteryManager.BATTERY_PLUGGED_AC) != 0 -> {
            if (isCharging && powerWatts >= FAST_CHARGE_WATTS_THRESHOLD) {
                "USB PD / Rápida" to "USB PD / Fast"
            } else {
                "Cargador AC" to "AC Charger"
            }
        }
        (plugged and BatteryManager.BATTERY_PLUGGED_USB) != 0 ->
            "Puerto USB" to "USB Port"
        (plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0 ->
            "Inalámbrica" to "Wireless"
        else -> "En batería" to "On Battery"
    }
}
