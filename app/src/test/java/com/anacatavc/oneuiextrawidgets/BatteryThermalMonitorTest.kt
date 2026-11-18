package com.anacatavc.oneuiextrawidgets

import android.os.BatteryManager
import com.anacatavc.oneuiextrawidgets.data.battery.BatteryThermalLevel
import com.anacatavc.oneuiextrawidgets.data.battery.BatteryThermalMonitor
import com.anacatavc.oneuiextrawidgets.data.battery.BatteryThermalSnapshot
import com.anacatavc.oneuiextrawidgets.data.battery.classifyBatteryThermalLevel
import com.anacatavc.oneuiextrawidgets.data.battery.computeBatteryPowerWatts
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryThermalMonitorTest {

    @Test
    fun `classifyBatteryThermalLevel maps temperature thresholds accurately`() {
        assertEquals(BatteryThermalLevel.COOL, classifyBatteryThermalLevel(Double.NaN))
        assertEquals(BatteryThermalLevel.COOL, classifyBatteryThermalLevel(24.0))
        assertEquals(BatteryThermalLevel.COOL, classifyBatteryThermalLevel(34.9))
        assertEquals(BatteryThermalLevel.WARM, classifyBatteryThermalLevel(35.0))
        assertEquals(BatteryThermalLevel.WARM, classifyBatteryThermalLevel(40.0))
        assertEquals(BatteryThermalLevel.HOT, classifyBatteryThermalLevel(40.1))
        assertEquals(BatteryThermalLevel.HOT, classifyBatteryThermalLevel(46.5))
    }

    @Test
    fun `computeBatteryPowerWatts computes non-negative watts from mV and mA`() {
        assertEquals(4.12, computeBatteryPowerWatts(voltageMv = 4120, currentMa = 1000), 1e-4)
        assertEquals(4.12, computeBatteryPowerWatts(voltageMv = 4120, currentMa = -1000), 1e-4)
        assertEquals(18.0, computeBatteryPowerWatts(voltageMv = 9000, currentMa = 2000), 1e-4)
    }

    @Test
    fun `normalizeCurrentMa and resolvePowerSource handle microamps, milliamps and charging sources`() {
        assertEquals(450, BatteryThermalMonitor.normalizeCurrentMa(0))
        assertEquals(450, BatteryThermalMonitor.normalizeCurrentMa(Int.MIN_VALUE))
        assertEquals(850, BatteryThermalMonitor.normalizeCurrentMa(-850_000))
        assertEquals(1500, BatteryThermalMonitor.normalizeCurrentMa(1_500_000))
        assertEquals(620, BatteryThermalMonitor.normalizeCurrentMa(620))

        assertEquals(
            "USB PD / Rápida" to "USB PD / Fast",
            BatteryThermalMonitor.resolvePowerSource(BatteryManager.BATTERY_PLUGGED_AC, isCharging = true, powerWatts = 22.5)
        )
        assertEquals(
            "Cargador AC" to "AC Charger",
            BatteryThermalMonitor.resolvePowerSource(BatteryManager.BATTERY_PLUGGED_AC, isCharging = true, powerWatts = 10.0)
        )
        assertEquals(
            "Puerto USB" to "USB Port",
            BatteryThermalMonitor.resolvePowerSource(BatteryManager.BATTERY_PLUGGED_USB, isCharging = true, powerWatts = 2.5)
        )
        assertEquals(
            "Inalámbrica" to "Wireless",
            BatteryThermalMonitor.resolvePowerSource(BatteryManager.BATTERY_PLUGGED_WIRELESS, isCharging = true, powerWatts = 12.0)
        )
        assertEquals(
            "En batería" to "On Battery",
            BatteryThermalMonitor.resolvePowerSource(0, isCharging = false, powerWatts = 2.0)
        )
    }

    @Test
    fun `BatteryThermalSnapshot provides localized thermal and power source labels`() {
        val cool = BatteryThermalSnapshot(
            temperatureC = 28.0,
            powerSourceEs = "Puerto USB",
            powerSourceEn = "USB Port",
            thermalLevel = BatteryThermalLevel.COOL
        )
        assertEquals("Óptima", cool.localizedThermalLabel(isSpanish = true))
        assertEquals("Optimal", cool.localizedThermalLabel(isSpanish = false))
        assertEquals("Puerto USB", cool.localizedPowerSource(isSpanish = true))
        assertEquals("USB Port", cool.localizedPowerSource(isSpanish = false))

        val warm = cool.copy(thermalLevel = BatteryThermalLevel.WARM)
        assertEquals("Templada", warm.localizedThermalLabel(isSpanish = true))
        assertEquals("Warm", warm.localizedThermalLabel(isSpanish = false))

        val hot = cool.copy(thermalLevel = BatteryThermalLevel.HOT)
        assertEquals("Caliente", hot.localizedThermalLabel(isSpanish = true))
        assertEquals("Hot", hot.localizedThermalLabel(isSpanish = false))
    }
}
