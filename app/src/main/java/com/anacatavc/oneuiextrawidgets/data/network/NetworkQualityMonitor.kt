package com.anacatavc.oneuiextrawidgets.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import com.anacatavc.oneuiextrawidgets.utils.PermissionHelper

data class NetworkQualitySnapshot(
    val mobileGeneration: String = "5G NR",
    val carrierName: String = "Samsung 5G",
    val mobileDbm: Int = -85,
    val mobileLevelBars: Int = 4, // 0..4
    val isMobileConnected: Boolean = true,
    val wifiSsid: String = "Wi-Fi 6",
    val wifiRssiDbm: Int = -54,
    val wifiLinkSpeedMbps: Int = 866,
    val wifiBandLabel: String = "5 GHz",
    val wifiLevelBars: Int = 4, // 0..4
    val isWifiConnected: Boolean = true
) {
    fun qualityLabel(bars: Int, isSpanish: Boolean): String = when (bars) {
        4 -> if (isSpanish) "Excelente" else "Excellent"
        3 -> if (isSpanish) "Buena" else "Good"
        2 -> if (isSpanish) "Regular" else "Fair"
        1 -> if (isSpanish) "Débil" else "Weak"
        else -> if (isSpanish) "Sin señal" else "No Signal"
    }
}

object NetworkQualityMonitor {

    @SuppressLint("MissingPermission")
    fun readSnapshot(context: Context): NetworkQualitySnapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val activeNetwork = cm.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }

        // 1. Wi-Fi Metrics
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            caps?.transportInfo as? WifiInfo ?: wm.connectionInfo
        } else {
            @Suppress("DEPRECATION")
            wm.connectionInfo
        }

        val rssi = wifiInfo?.rssi?.takeIf { it in -120..-1 } ?: -60
        val wifiBars = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.calculateSignalLevel(rssi).coerceIn(0, 4)
        } else {
            @Suppress("DEPRECATION")
            WifiManager.calculateSignalLevel(rssi, 5)
        }
        val linkSpeed = wifiInfo?.linkSpeed?.coerceAtLeast(0) ?: 0
        val freqMhz = wifiInfo?.frequency ?: 5180
        val bandLabel = when {
            freqMhz >= 5925 -> "6 GHz"
            freqMhz >= 4900 -> "5 GHz"
            freqMhz >= 2400 -> "2.4 GHz"
            else -> "Wi-Fi"
        }
        val rawSsid = wifiInfo?.ssid?.removeSurrounding("\"")
        val ssid = if (rawSsid.isNullOrBlank() || rawSsid == "<unknown ssid>") "Wi-Fi ($bandLabel)" else rawSsid

        // 2. 5G NR / LTE Cellular Metrics
        val defaultCarrier = if (com.anacatavc.oneuiextrawidgets.utils.LocaleHelper.isSpanishActive()) {
            "Red Móvil"
        } else {
            "Mobile Network"
        }
        val carrier = tm.networkOperatorName?.takeIf { it.isNotBlank() } ?: defaultCarrier
        val signalStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) tm.signalStrength else null
        val mobileBars = signalStrength?.level?.coerceIn(0, 4) ?: 3

        var mobileDbm = -88
        var generation = "4G / 5G"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && signalStrength != null) {
            val nrStrength = signalStrength.getCellSignalStrengths(CellSignalStrengthNr::class.java).firstOrNull()
            val lteStrength = signalStrength.getCellSignalStrengths(CellSignalStrengthLte::class.java).firstOrNull()
            when {
                nrStrength != null && nrStrength.dbm in -140..-40 -> {
                    mobileDbm = nrStrength.dbm
                    generation = "5G NR"
                }
                lteStrength != null && lteStrength.dbm in -140..-40 -> {
                    mobileDbm = lteStrength.dbm
                    generation = "4G LTE"
                }
            }
        }

        if (PermissionHelper.hasPhoneStatePermission(context)) {
            runCatching {
                when (tm.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_NR -> generation = "5G NR"
                    TelephonyManager.NETWORK_TYPE_LTE -> generation = "4G LTE"
                    TelephonyManager.NETWORK_TYPE_HSPAP,
                    TelephonyManager.NETWORK_TYPE_HSPA -> generation = "3G HSPA+"
                }
            }.onFailure { err ->
                android.util.Log.w("NetworkQualityMonitor", "Failed reading dataNetworkType", err)
            }
        }

        return NetworkQualitySnapshot(
            mobileGeneration = generation,
            carrierName = carrier,
            mobileDbm = mobileDbm,
            mobileLevelBars = mobileBars,
            isMobileConnected = tm.simState == TelephonyManager.SIM_STATE_READY,
            wifiSsid = ssid,
            wifiRssiDbm = rssi,
            wifiLinkSpeedMbps = linkSpeed,
            wifiBandLabel = bandLabel,
            wifiLevelBars = if (isWifi) wifiBars else 0,
            isWifiConnected = isWifi
        )
    }
}
