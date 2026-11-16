package com.anacatavc.oneuiextrawidgets.data.weather

import com.anacatavc.oneuiextrawidgets.data.preferences.CachedWeatherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
                    "&current=temperature_2m,apparent_temperature,weather_code" +
                    "&hourly=precipitation_probability,uv_index&forecast_days=1&timezone=auto"

            val aqiUrl =
                "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$latitude&longitude=$longitude" +
                    "&current=european_aqi,pm10,pm2_5&timezone=auto"

            val forecastJson = JSONObject(httpGet(forecastUrl))
            val aqiJson = JSONObject(httpGet(aqiUrl))

            val current = forecastJson.getJSONObject("current")
            val hourly = forecastJson.getJSONObject("hourly")
            val rainProbArray = hourly.getJSONArray("precipitation_probability")
            val uvArray = hourly.getJSONArray("uv_index")

            // Take current hour or max over the next 6 hours for actionable rain probability
            val currentHourIndex = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                .coerceIn(0, (rainProbArray.length() - 1).coerceAtLeast(0))
            val rainProb = rainProbArray.optInt(currentHourIndex, 0)
            val uvIndex = uvArray.optDouble(currentHourIndex, 0.0)

            val currentAqi = aqiJson.optJSONObject("current")
            val euAqi = currentAqi?.optInt("european_aqi", 25) ?: 25
            val pm25 = currentAqi?.optDouble("pm2_5", 8.0) ?: 8.0
            val pm10 = currentAqi?.optDouble("pm10", 15.0) ?: 15.0

            CachedWeatherState(
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
                lastUpdatedMillis = System.currentTimeMillis()
            )
        }
    }

    private fun httpGet(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
