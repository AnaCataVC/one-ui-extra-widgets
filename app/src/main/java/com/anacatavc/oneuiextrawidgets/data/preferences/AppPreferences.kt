package com.anacatavc.oneuiextrawidgets.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appDataStore by preferencesDataStore(name = "oneui_extra_widgets_prefs")

data class CachedWeatherState(
    val cityName: String = "Santiago",
    val latitude: Double = -33.4489,
    val longitude: Double = -70.6693,
    val temperatureC: Double = 22.0,
    val feelsLikeC: Double = 21.5,
    val rainProbabilityPct: Int = 10,
    val uvIndex: Double = 5.0,
    val europeanAqi: Int = 28,
    val pm25: Double = 9.4,
    val pm10: Double = 18.2,
    val weatherCode: Int = 1,
    val lastUpdatedMillis: Long = 0L
)

object AppPreferences {
    private val KEY_CITY = stringPreferencesKey("weather_city")
    private val KEY_LAT = doublePreferencesKey("weather_lat")
    private val KEY_LON = doublePreferencesKey("weather_lon")
    private val KEY_TEMP = doublePreferencesKey("weather_temp_c")
    private val KEY_FEELS = doublePreferencesKey("weather_feels_c")
    private val KEY_RAIN_PROB = intPreferencesKey("weather_rain_prob")
    private val KEY_UV = doublePreferencesKey("weather_uv")
    private val KEY_AQI = intPreferencesKey("weather_aqi")
    private val KEY_PM25 = doublePreferencesKey("weather_pm25")
    private val KEY_PM10 = doublePreferencesKey("weather_pm10")
    private val KEY_WMO = intPreferencesKey("weather_wmo_code")
    private val KEY_UPDATED = longPreferencesKey("weather_updated_ms")

    fun observeWeather(context: Context): Flow<CachedWeatherState> {
        return context.appDataStore.data.map { prefs ->
            CachedWeatherState(
                cityName = prefs[KEY_CITY] ?: "Santiago",
                latitude = prefs[KEY_LAT] ?: -33.4489,
                longitude = prefs[KEY_LON] ?: -70.6693,
                temperatureC = prefs[KEY_TEMP] ?: 22.0,
                feelsLikeC = prefs[KEY_FEELS] ?: 21.5,
                rainProbabilityPct = prefs[KEY_RAIN_PROB] ?: 10,
                uvIndex = prefs[KEY_UV] ?: 5.0,
                europeanAqi = prefs[KEY_AQI] ?: 28,
                pm25 = prefs[KEY_PM25] ?: 9.4,
                pm10 = prefs[KEY_PM10] ?: 18.2,
                weatherCode = prefs[KEY_WMO] ?: 1,
                lastUpdatedMillis = prefs[KEY_UPDATED] ?: 0L
            )
        }
    }

    suspend fun saveWeather(context: Context, state: CachedWeatherState) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_CITY] = state.cityName
            prefs[KEY_LAT] = state.latitude
            prefs[KEY_LON] = state.longitude
            prefs[KEY_TEMP] = state.temperatureC
            prefs[KEY_FEELS] = state.feelsLikeC
            prefs[KEY_RAIN_PROB] = state.rainProbabilityPct
            prefs[KEY_UV] = state.uvIndex
            prefs[KEY_AQI] = state.europeanAqi
            prefs[KEY_PM25] = state.pm25
            prefs[KEY_PM10] = state.pm10
            prefs[KEY_WMO] = state.weatherCode
            prefs[KEY_UPDATED] = state.lastUpdatedMillis
        }
    }
}
