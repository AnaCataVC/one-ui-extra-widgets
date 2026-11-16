package com.anacatavc.oneuiextrawidgets.data.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.anacatavc.oneuiextrawidgets.widgets.music.RetroMusicWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class ActiveMediaState(
    val title: String = "One UI Retro Player",
    val artist: String = "Samsung Galaxy A55",
    val album: String = "Extra Widgets",
    val isPlaying: Boolean = false,
    val packageName: String = "",
    val albumArt: Bitmap? = null
)

/**
 * Bridges active Android MediaSessions (Spotify, YouTube Music, Apple Music, Samsung Music, etc.)
 * with the iPod Classic, 2000s MP3 LCD, and Portable CD Discman Glance widgets.
 */
class MediaNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshActiveMediaSession(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshActiveMediaSession(applicationContext)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshActiveMediaSession(applicationContext)
    }

    companion object {
        @Volatile
        var latestState: ActiveMediaState = ActiveMediaState()
            private set

        fun refreshActiveMediaSession(context: Context): ActiveMediaState {
            val controller = getActiveController(context)
            if (controller != null) {
                val meta = controller.metadata
                val playback = controller.playbackState
                val isPlaying = playback?.state == PlaybackState.STATE_PLAYING ||
                    playback?.state == PlaybackState.STATE_BUFFERING

                val art = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)

                latestState = ActiveMediaState(
                    title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
                        ?.takeIf { it.isNotBlank() } ?: "Sin título",
                    artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() } ?: "Artista desconocido",
                    album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
                    isPlaying = isPlaying,
                    packageName = controller.packageName ?: "",
                    albumArt = art
                )
            }
            CoroutineScope(Dispatchers.Main).launch {
                RetroMusicWidgetUpdater.updateAllMusicWidgets(context)
            }
            return latestState
        }

        fun sendTransportCommand(context: Context, command: TransportCommand) {
            val controller = getActiveController(context) ?: return
            when (command) {
                TransportCommand.PLAY_PAUSE -> {
                    val state = controller.playbackState?.state
                    if (state == PlaybackState.STATE_PLAYING) {
                        controller.transportControls.pause()
                    } else {
                        controller.transportControls.play()
                    }
                }
                TransportCommand.NEXT -> controller.transportControls.skipToNext()
                TransportCommand.PREVIOUS -> controller.transportControls.skipToPrevious()
            }
            refreshActiveMediaSession(context)
        }

        private fun getActiveController(context: Context): MediaController? {
            return runCatching {
                val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val component = ComponentName(context, MediaNotificationListener::class.java)
                val controllers = manager.getActiveSessions(component)
                controllers.firstOrNull {
                    it.playbackState?.state == PlaybackState.STATE_PLAYING
                } ?: controllers.firstOrNull()
            }.getOrNull()
        }
    }
}

enum class TransportCommand {
    PLAY_PAUSE,
    NEXT,
    PREVIOUS
}
