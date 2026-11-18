package com.anacatavc.oneuiextrawidgets.data.media

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.KeyEvent
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper
import com.anacatavc.oneuiextrawidgets.utils.PermissionHelper
import com.anacatavc.oneuiextrawidgets.widgets.music.RetroMusicWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val ALBUM_ART_SIZE_PX = 160

private fun deviceModelLabel(): String {
    val model = Build.MODEL?.trim().orEmpty()
    val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
    return when {
        model.isEmpty() -> "Retro Player"
        manufacturer.isNotEmpty() && !model.startsWith(manufacturer, ignoreCase = true) ->
            "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
        else -> model
    }
}

data class ActiveMediaState(
    val title: String = "Retro Player",
    val artist: String = deviceModelLabel(),
    val album: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val packageName: String = "",
    val albumArt: Bitmap? = null,
    val hasNotificationAccess: Boolean = true
)

fun formatMediaTime(ms: Long, padMinutes: Boolean = false): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (padMinutes) {
        "%02d:%02d".format(java.util.Locale.US, minutes, seconds)
    } else {
        "%d:%02d".format(java.util.Locale.US, minutes, seconds)
    }
}

/**
 * Bridges active Android MediaSessions (Spotify, YouTube Music, Apple Music, Samsung Music, etc.)
 * with the iPod Classic, 2000s MP3 LCD, and Portable CD Discman Glance widgets.
 */
class MediaNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sessionManager: MediaSessionManager? = null
    private var sessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        registerActiveSessionsListener()
        refreshActiveMediaSession(applicationContext, serviceScope)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || isMediaNotification(sbn)) {
            refreshActiveMediaSession(applicationContext, serviceScope)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null || isMediaNotification(sbn)) {
            refreshActiveMediaSession(applicationContext, serviceScope)
        }
    }

    override fun onDestroy() {
        unregisterActiveSessionsListener()
        clearControllerCallback()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerActiveSessionsListener() {
        val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
        val component = ComponentName(this, MediaNotificationListener::class.java)
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            val preferred = pickPreferredController(controllers.orEmpty())
            syncFromController(applicationContext, preferred, serviceScope)
        }
        runCatching {
            unregisterActiveSessionsListener()
            manager.addOnActiveSessionsChangedListener(listener, component)
            sessionManager = manager
            sessionsListener = listener
        }.onFailure { err ->
            Log.w("MediaNotification", "Failed registering active sessions listener", err)
        }
    }

    private fun unregisterActiveSessionsListener() {
        val listener = sessionsListener ?: return
        runCatching {
            sessionManager?.removeOnActiveSessionsChangedListener(listener)
        }
        sessionsListener = null
    }

    private fun isMediaNotification(sbn: StatusBarNotification): Boolean {
        val extras = sbn.notification?.extras ?: return false
        return extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
            sbn.packageName == latestState.packageName
    }

    companion object {
        private val widgetUpdateScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val _mediaState = MutableStateFlow(ActiveMediaState())
        val mediaState: StateFlow<ActiveMediaState> = _mediaState.asStateFlow()

        val latestState: ActiveMediaState
            get() = _mediaState.value

        private var observedController: MediaController? = null
        private var observedCallback: MediaController.Callback? = null
        private var lastArtSourceId: Int = 0
        private var cachedSquareArt: Bitmap? = null

        fun refreshActiveMediaSession(
            context: Context,
            scope: CoroutineScope = widgetUpdateScope
        ): ActiveMediaState {
            val hasAccess = PermissionHelper.hasNotificationListenerAccess(context)
            if (!hasAccess) {
                clearControllerCallback()
                val isSpanish = LocaleHelper.isSpanishActive()
                val missingState = ActiveMediaState(
                    title = if (isSpanish) "Activar acceso multimedia" else "Enable media access",
                    artist = if (isSpanish) "Toca para sincronizar" else "Tap to sync",
                    album = "",
                    isPlaying = false,
                    packageName = "",
                    albumArt = null,
                    hasNotificationAccess = false
                )
                updateStateIfChanged(context, missingState, scope)
                return latestState
            }

            val controller = getActiveController(context)
            syncFromController(context, controller, scope)
            return latestState
        }

        private fun syncFromController(
            context: Context,
            controller: MediaController?,
            scope: CoroutineScope
        ) {
            attachControllerCallback(context, controller, scope)

            val isSpanish = LocaleHelper.isSpanishActive()
            val newState = if (controller != null) {
                val meta = controller.metadata
                val playback = controller.playbackState
                val isPlaying = playback?.state == PlaybackState.STATE_PLAYING ||
                    playback?.state == PlaybackState.STATE_BUFFERING

                val fallbackTitle = if (isSpanish) "Sin título" else "Untitled"
                val fallbackArtist = if (isSpanish) "Artista desconocido" else "Unknown Artist"

                val rawArt = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                val squareArt = resolveSquareAlbumArt(rawArt)
                val durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION)
                    ?.coerceAtLeast(0L) ?: 0L
                val positionMs = (playback?.position ?: 0L).coerceAtLeast(0L)

                ActiveMediaState(
                    title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
                        ?.takeIf { it.isNotBlank() } ?: fallbackTitle,
                    artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() } ?: fallbackArtist,
                    album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    packageName = controller.packageName ?: "",
                    albumArt = squareArt,
                    hasNotificationAccess = true
                )
            } else {
                lastArtSourceId = 0
                cachedSquareArt = null
                ActiveMediaState(
                    title = if (isSpanish) "Sin música activa" else "No active track",
                    artist = deviceModelLabel(),
                    album = "",
                    isPlaying = false,
                    packageName = "",
                    albumArt = null,
                    hasNotificationAccess = true
                )
            }

            updateStateIfChanged(context, newState, scope)
        }

        private fun updateStateIfChanged(
            context: Context,
            newState: ActiveMediaState,
            scope: CoroutineScope
        ) {
            if (newState != _mediaState.value) {
                _mediaState.value = newState
                scope.launch {
                    RetroMusicWidgetUpdater.updateAllMusicWidgets(context.applicationContext)
                }
            }
        }

        private fun attachControllerCallback(
            context: Context,
            controller: MediaController?,
            scope: CoroutineScope
        ) {
            if (observedController?.sessionToken == controller?.sessionToken && controller != null) {
                return
            }
            clearControllerCallback()
            if (controller == null) return

            val appContext = context.applicationContext
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    syncFromController(appContext, controller, scope)
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    syncFromController(appContext, controller, scope)
                }

                override fun onSessionDestroyed() {
                    refreshActiveMediaSession(appContext, scope)
                }
            }
            runCatching {
                controller.registerCallback(callback)
                observedController = controller
                observedCallback = callback
            }.onFailure { err ->
                Log.w("MediaNotification", "Failed registering controller callback", err)
            }
        }

        private fun clearControllerCallback() {
            val prevController = observedController
            val prevCallback = observedCallback
            if (prevController != null && prevCallback != null) {
                runCatching { prevController.unregisterCallback(prevCallback) }
            }
            observedController = null
            observedCallback = null
        }

        private fun resolveSquareAlbumArt(raw: Bitmap?): Bitmap? {
            if (raw == null || raw.isRecycled) {
                lastArtSourceId = 0
                cachedSquareArt = null
                return null
            }
            val sourceId = System.identityHashCode(raw)
            val cached = cachedSquareArt
            return if (sourceId == lastArtSourceId && cached != null && !cached.isRecycled) {
                cached
            } else {
                cropAndScaleSquare(raw, ALBUM_ART_SIZE_PX).also { square ->
                    lastArtSourceId = sourceId
                    cachedSquareArt = square
                }
            }
        }

        private fun cropAndScaleSquare(source: Bitmap, targetPx: Int): Bitmap {
            val side = minOf(source.width, source.height).coerceAtLeast(1)
            val x = (source.width - side) / 2
            val y = (source.height - side) / 2
            val cropped = if (source.width != source.height) {
                Bitmap.createBitmap(source, x, y, side, side)
            } else {
                source
            }
            val scaled = if (cropped.width != targetPx || cropped.height != targetPx) {
                Bitmap.createScaledBitmap(cropped, targetPx, targetPx, true)
            } else {
                cropped
            }
            if (cropped !== source && cropped !== scaled) {
                cropped.recycle()
            }
            return scaled
        }

        suspend fun sendTransportCommand(context: Context, command: TransportCommand) {
            val controller = getActiveController(context)
            if (controller != null) {
                when (command) {
                    TransportCommand.PLAY_PAUSE -> {
                        val state = controller.playbackState?.state
                        if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                            controller.transportControls.pause()
                        } else {
                            controller.transportControls.play()
                        }
                    }
                    TransportCommand.NEXT -> controller.transportControls.skipToNext()
                    TransportCommand.PREVIOUS -> controller.transportControls.skipToPrevious()
                }
            } else {
                dispatchMediaKeyFallback(context, command)
            }
            refreshActiveMediaSession(context)
            RetroMusicWidgetUpdater.updateAllMusicWidgets(context)
            delay(300L)
            refreshActiveMediaSession(context)
            RetroMusicWidgetUpdater.updateAllMusicWidgets(context)
        }

        private fun dispatchMediaKeyFallback(context: Context, command: TransportCommand) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val keyCode = when (command) {
                TransportCommand.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                TransportCommand.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
                TransportCommand.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            }
            val eventTime = SystemClock.uptimeMillis()
            audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
            audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
        }

        private fun getActiveController(context: Context): MediaController? {
            return runCatching {
                val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val component = ComponentName(context, MediaNotificationListener::class.java)
                val controllers = manager.getActiveSessions(component)
                pickPreferredController(controllers)
            }.onFailure { err ->
                Log.w("MediaNotification", "Failed querying active media sessions", err)
            }.getOrNull()
        }

        private fun pickPreferredController(controllers: List<MediaController>): MediaController? {
            return controllers.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING ||
                    it.playbackState?.state == PlaybackState.STATE_BUFFERING
            } ?: controllers.firstOrNull()
        }
    }
}

enum class TransportCommand {
    PLAY_PAUSE,
    NEXT,
    PREVIOUS
}
