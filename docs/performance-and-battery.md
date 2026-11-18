# Performance & Battery Efficiency Architecture

**One UI Extra Widgets** is engineered to run with minimal CPU, Binder IPC, network radio, and battery overhead across Samsung One UI home screens.

## 1. Screen-Aware & Widget-Aware Media Progress Ticker
- **`MediaNotificationListener`** (`data/media/MediaNotificationListener.kt`) tracks active `MediaController` sessions via system callbacks (`OnActiveSessionsChangedListener`, `MediaController.Callback`) and filters `NotificationListenerService` events strictly to media notifications.
- **Screen-off pause:** A lightweight `BroadcastReceiver` listens for `Intent.ACTION_SCREEN_OFF` and `Intent.ACTION_SCREEN_ON`, alongside `PowerManager.isInteractive` checks inside `syncProgressTicker`. When the display is off, the 3-second progress ticker is cancelled immediately, resulting in zero wakeups and zero Binder `RemoteViews` dispatches during background audio playback.
- **Active widget check:** Before starting the ticker, `RetroMusicWidgetUpdater.hasAnyMusicWidgets(context)` verifies that at least one retro music widget is placed on the home screen.
- **Quantized Canvas Bitmaps:** `CassetteWalkmanWidget` and `VinylTurntableWidget` quantize playback progress via `quantizeProgressForCanvas()` (20 discrete steps, 5% increments) inside `remember(...)` keys, preventing unnecessary `320px–360px` `ARGB_8888` `Bitmap` allocations and Binder transfers on every 3-second tick.
- **Album art caching:** `resolveSquareAlbumArt()` scales artwork once to `160x160` px, recycles intermediate bitmaps, and caches by `System.identityHashCode`.

## 2. Offline Ephemeris & Memoized Glance Composition
- **Direct lunar illumination lookup:** `AstrologyEngine.computeStargazingSnapshot()` queries `illumination(Body.Moon, nowTime).phaseFraction` directly instead of computing full planetary transit & shadow windows for all 5 planets.
- **Memoized calculations & bitmaps:** `OneUiSunArcWidget`, `OneUiStargazingWidget`, `OneUiRainWidget`, `OneUiAqiWidget`, `OneUiUvWidget`, `OneUiBatteryThermalWidget`, `OneUiNetworkWidget`, and `OneUiAstrologyWidget` wrap ephemeris snapshots and `WidgetCanvasRenderer` bitmap calls in `remember(...)` keyed on their exact domain inputs.

## 3. Parallel Network Sync & WorkManager Constraints
- **Concurrent API calls:** `OpenMeteoClient.fetchWeatherAndAirQuality()` fetches the Forecast and Air Quality endpoints concurrently on `Dispatchers.IO` via `coroutineScope { async { ... } }`, halving active cellular/Wi-Fi radio time.
- **Battery-friendly scheduling:** `OneUiWidgetsApp.schedulePeriodicWeatherSync()` enforces `NetworkType.CONNECTED` and `setRequiresBatteryNotLow(true)` on `WeatherRefreshWorker` (30-minute interval).
- **Zero wasted background syncs:** `WeatherRefreshWorker.doWork()` checks `hasAnyWeatherWidgets(appContext)` first and exits immediately if no weather widgets are installed.

## 4. Tuned `updatePeriodMillis` Provider Intervals
- **Weather widgets (`Rain`, `AQI`, `UV`, `Sun Arc`, `Stargazing`):** `android:updatePeriodMillis="0"`, as `WeatherRefreshWorker` and `DataStore` flows already push updates without duplicate system `APPWIDGET_UPDATE` alarms.
- **Slow-changing astronomical widgets (`Moon Phase`, `Astrology`):** `android:updatePeriodMillis="14400000"` (4 hours).
- **On-demand hardware monitors (`Battery Thermal`, `5G/Wi-Fi`):** `BatteryThermalMonitor` reads the sticky `ACTION_BATTERY_CHANGED` broadcast and `NetworkQualityMonitor` reads `ConnectivityManager`/`TelephonyManager` only on update, with zero background polling.
