package com.anacatavc.oneuiextrawidgets

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.anacatavc.oneuiextrawidgets.widgets.weather.WeatherRefreshWorker
import java.util.concurrent.TimeUnit

class OneUiWidgetsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicWeatherSync()
    }

    private fun schedulePeriodicWeatherSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "oneui_weather_periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
