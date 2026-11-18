package com.anacatavc.oneuiextrawidgets.data.weather

import com.anacatavc.oneuiextrawidgets.data.preferences.CachedWeatherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

/**
 * Fetches real-time weather, precipitation probability, UV index, and Air Quality (AQI, PM2.5, PM10)
 * from the free, open-source Open-Meteo APIs without requiring any API key.
 */
object OpenMeteoClient {

    suspend fun fetchWeatherAndAirQuality(
        latitude: Double,
        longitude: Double,
        cityName: String
    ): Result<CachedWeatherState> = withContext(Dispatchers.IO) {
        runCatching {
            val forecastUrl =
                "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,apparent_temperature,weather_code,cloud_cover" +
                    "&hourly=precipitation_probability,uv_index&forecast_days=1&timezone=auto"

            val aqiUrl =
                "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$latitude&longitude=$longitude" +
                    "&current=european_aqi,pm10,pm2_5&timezone=auto"

            parseForecastAndAqi(
                forecastRaw = httpGet(forecastUrl),
                aqiRaw = httpGet(aqiUrl),
                latitude = latitude,
                longitude = longitude,
                cityName = cityName
            )
        }
    }

    internal fun parseForecastAndAqi(
        forecastRaw: String,
        aqiRaw: String,
        latitude: Double,
        longitude: Double,
        cityName: String,
        nowMillis: Long = System.currentTimeMillis()
    ): CachedWeatherState {
        val forecastJson = JSONObject(forecastRaw)
        val aqiJson = JSONObject(aqiRaw)

        val current = forecastJson.getJSONObject("current")
        val hourly = forecastJson.getJSONObject("hourly")
        val rainProbArray = hourly.getJSONArray("precipitation_probability")
        val uvArray = hourly.getJSONArray("uv_index")
        val hourlyTimes = hourly.optJSONArray("time")

        val currentTimePrefix = current.optString("time").take(13) // e.g. "2026-11-16T14"
        var currentHourIndex = -1
        if (currentTimePrefix.length == 13 && hourlyTimes != null) {
            for (i in 0 until hourlyTimes.length()) {
                if (hourlyTimes.optString(i).startsWith(currentTimePrefix)) {
                    currentHourIndex = i
                    break
                }
            }
        }
        if (currentHourIndex < 0) {
            currentHourIndex = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        }
        val maxIndex = (rainProbArray.length() - 1).coerceAtLeast(0)
        val safeHourIndex = currentHourIndex.coerceIn(0, maxIndex)

        // Take max rain probability over the next 6 hours for actionable precipitation alert
        val windowEnd = min(rainProbArray.length(), safeHourIndex + 6)
        var rainProb = rainProbArray.optInt(safeHourIndex, 0)
        for (i in safeHourIndex until windowEnd) {
            rainProb = maxOf(rainProb, rainProbArray.optInt(i, 0))
        }
        val uvIndex = uvArray.optDouble(safeHourIndex, 0.0)

        val currentAqi = aqiJson.optJSONObject("current")
        val euAqi = currentAqi?.optInt("european_aqi", 25) ?: 25
        val pm25 = currentAqi?.optDouble("pm2_5", 8.0) ?: 8.0
        val pm10 = currentAqi?.optDouble("pm10", 15.0) ?: 15.0
        val cloudCover = current.optInt("cloud_cover", 15).coerceIn(0, 100)

        return CachedWeatherState(
            cityName = cityName,
            latitude = latitude,
            longitude = longitude,
            temperatureC = current.optDouble("temperature_2m", 20.0),
            feelsLikeC = current.optDouble("apparent_temperature", 20.0),
            rainProbabilityPct = rainProb,
            uvIndex = uvIndex,
            europeanAqi = euAqi,
            pm25 = pm25,
            pm10 = pm10,
            weatherCode = current.optInt("weather_code", 0),
            cloudCoverPct = cloudCover,
            lastUpdatedMillis = nowMillis
        )
    }

    private fun httpGet(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = connection.responseCode
            if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("HTTP $code from Open-Meteo: $errBody")
            }
        } finally {
            connection.disconnect()
        }
    }
}
