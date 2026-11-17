package com.anacatavc.oneuiextrawidgets

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anacatavc.oneuiextrawidgets.data.astrology.AstrologyEngine
import com.anacatavc.oneuiextrawidgets.data.network.NetworkQualityMonitor
import com.anacatavc.oneuiextrawidgets.data.preferences.AppPreferences
import com.anacatavc.oneuiextrawidgets.data.preferences.CachedWeatherState
import com.anacatavc.oneuiextrawidgets.data.weather.OpenMeteoClient
import com.anacatavc.oneuiextrawidgets.ui.screens.AstrologyDetailScreen
import com.anacatavc.oneuiextrawidgets.ui.screens.HomeScreen
import com.anacatavc.oneuiextrawidgets.ui.screens.NetworkDetailScreen
import com.anacatavc.oneuiextrawidgets.ui.screens.WeatherDetailScreen
import com.anacatavc.oneuiextrawidgets.ui.theme.OneUiTheme
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.utils.PermissionHelper
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_INITIAL_TAB = "extra_initial_tab"
        const val TAB_HOME = 0
        const val TAB_WEATHER = 1
        const val TAB_ASTROLOGY = 2
        const val TAB_NETWORK = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialTab = intent?.getIntExtra(EXTRA_INITIAL_TAB, TAB_HOME) ?: TAB_HOME

        setContent {
            OneUiTheme {
                val scope = rememberCoroutineScope()
                var currentTab by remember { mutableIntStateOf(initialTab) }
                var selectedLanguage by remember { mutableStateOf(LocaleHelper.getCurrentLanguage()) }
                val isSpanish = LocaleHelper.isSpanishActive()

                val weatherState by AppPreferences.observeWeather(this)
                    .collectAsState(initial = CachedWeatherState())
                val astroSnapshot = remember { AstrologyEngine.computeSnapshot() }
                var networkSnapshot by remember {
                    mutableStateOf(NetworkQualityMonitor.readSnapshot(this))
                }
                var isRefreshingWeather by remember { mutableStateOf(false) }
                var weatherError by remember { mutableStateOf<String?>(null) }
                var isRefreshingNetwork by remember { mutableStateOf(false) }

                var hasLocationPerm by remember {
                    mutableStateOf(PermissionHelper.hasLocationPermission(this))
                }
                var hasPhonePerm by remember {
                    mutableStateOf(PermissionHelper.hasPhoneStatePermission(this))
                }
                var hasNotifAccess by remember {
                    mutableStateOf(PermissionHelper.hasNotificationListenerAccess(this))
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    hasLocationPerm = PermissionHelper.hasLocationPermission(this)
                    hasPhonePerm = PermissionHelper.hasPhoneStatePermission(this)
                    networkSnapshot = NetworkQualityMonitor.readSnapshot(this)
                    if (hasLocationPerm) {
                        scope.launch {
                            isRefreshingWeather = true
                            weatherError = null
                            val ok = refreshWeatherWithLocation(weatherState)
                            if (!ok) {
                                weatherError = if (isSpanish) {
                                    "No pudimos actualizar el clima. Verifica tu conexión e inténtalo de nuevo."
                                } else {
                                    "We couldn't refresh the weather. Check your connection and retry."
                                }
                            }
                            isRefreshingWeather = false
                        }
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasNotifAccess = PermissionHelper.hasNotificationListenerAccess(this@MainActivity)
                            networkSnapshot = NetworkQualityMonitor.readSnapshot(this@MainActivity)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == TAB_HOME,
                                onClick = { currentTab = TAB_HOME },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_nav_home),
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(R.string.tab_home)) }
                            )
                            NavigationBarItem(
                                selected = currentTab == TAB_WEATHER,
                                onClick = { currentTab = TAB_WEATHER },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_nav_weather),
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(R.string.tab_weather)) }
                            )
                            NavigationBarItem(
                                selected = currentTab == TAB_ASTROLOGY,
                                onClick = { currentTab = TAB_ASTROLOGY },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_nav_astrology),
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(R.string.tab_astrology)) }
                            )
                            NavigationBarItem(
                                selected = currentTab == TAB_NETWORK,
                                onClick = { currentTab = TAB_NETWORK },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_nav_network),
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(R.string.tab_network)) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            TAB_HOME -> HomeScreen(
                                isSpanish = isSpanish,
                                selectedLanguage = selectedLanguage,
                                hasLocationPermission = hasLocationPerm,
                                hasPhonePermission = hasPhonePerm,
                                hasNotificationAccess = hasNotifAccess,
                                onLanguageChange = { selectedLanguage = it },
                                onRequestRuntimePermissions = {
                                    permissionLauncher.launch(PermissionHelper.requiredRuntimePermissions())
                                },
                                onOpenNotificationAccess = {
                                    PermissionHelper.openNotificationListenerSettings(this@MainActivity)
                                }
                            )
                            TAB_WEATHER -> WeatherDetailScreen(
                                weather = weatherState,
                                lunarStatus = astroSnapshot.lunarStatus,
                                isSpanish = isSpanish,
                                isRefreshing = isRefreshingWeather,
                                errorMessage = weatherError,
                                onRefreshWeather = {
                                    scope.launch {
                                        isRefreshingWeather = true
                                        weatherError = null
                                        val ok = refreshWeatherWithLocation(weatherState)
                                        if (!ok) {
                                            weatherError = if (isSpanish) {
                                                "No pudimos actualizar el clima. Verifica tu conexión e inténtalo de nuevo."
                                            } else {
                                                "We couldn't refresh the weather. Check your connection and retry."
                                            }
                                        }
                                        isRefreshingWeather = false
                                    }
                                }
                            )
                            TAB_ASTROLOGY -> AstrologyDetailScreen(
                                snapshot = astroSnapshot,
                                isSpanish = isSpanish
                            )
                            TAB_NETWORK -> NetworkDetailScreen(
                                snapshot = networkSnapshot,
                                isSpanish = isSpanish,
                                isRefreshing = isRefreshingNetwork,
                                onRefresh = {
                                    isRefreshingNetwork = true
                                    networkSnapshot = NetworkQualityMonitor.readSnapshot(this@MainActivity)
                                    isRefreshingNetwork = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun refreshWeatherWithLocation(fallback: CachedWeatherState): Boolean {
        var lat = fallback.latitude
        var lon = fallback.longitude
        var city = fallback.cityName

        if (PermissionHelper.hasLocationPermission(this)) {
            runCatching {
                val client = LocationServices.getFusedLocationProviderClient(this)
                val loc = suspendCancellableCoroutine { cont ->
                    client.lastLocation
                        .addOnSuccessListener { result ->
                            if (cont.isActive) cont.resume(result)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                        .addOnCanceledListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
                if (loc != null) {
                    lat = loc.latitude
                    lon = loc.longitude
                    city = withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        val addresses = Geocoder(this@MainActivity, Locale.getDefault())
                            .getFromLocation(lat, lon, 1)
                        addresses?.firstOrNull()?.locality ?: city
                    }
                }
            }.onFailure { err ->
                android.util.Log.w("MainActivity", "Failed to resolve device location", err)
            }
        }

        return OpenMeteoClient.fetchWeatherAndAirQuality(
            latitude = lat,
            longitude = lon,
            cityName = city
        ).onSuccess { updated ->
            AppPreferences.saveWeather(this, updated)
            com.anacatavc.oneuiextrawidgets.widgets.weather.updateAllWeatherWidgets(this)
        }.onFailure { err ->
            android.util.Log.w("MainActivity", "Failed to fetch weather and air quality", err)
        }.isSuccess
    }
}
