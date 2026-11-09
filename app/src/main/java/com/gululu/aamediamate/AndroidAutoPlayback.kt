package com.gululu.aamediamate

import android.content.Context
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.gululu.aamediamate.diagnostics.DiagnosticLogger
import com.gululu.aamediamate.diagnostics.DiagnosticModule

/** Shares connection state across browser service recreation for the lifetime of the app process. */
internal object AndroidAutoPlayback {
    private var controller: CarAutoPlayController? = null

    fun start(context: Context) {
        if (controller != null) return
        val app = context.applicationContext
        controller = CarAutoPlayController(
            connectionType = CarConnection(app).type,
            canPlay = {
                SettingsManager.getCarAutoPlay(app) && SettingsManager.getSpotifySourceLock(app) &&
                    hasNotificationAccess(app) && Global.packageAllowed(app, SettingsManager.SPOTIFY_PACKAGE)
            },
            isPlaying = {
                MediaControllerManager.getAllControllers(app).any {
                    it.packageName == SettingsManager.SPOTIFY_PACKAGE &&
                        it.playbackState?.state in setOf(PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING)
                }
            },
            requestPlay = { SpotifyPlaybackStarter.play(app) },
            cancelStartup = { SpotifyPlaybackStarter.release() },
            log = { event -> DiagnosticLogger.info(app, DiagnosticModule.MEDIA, event) },
            onConnectionChanged = MediaBridgeSessionManager::setCarConnected
        ).also { it.start() }
    }

    fun setBridgeReady(ready: Boolean) { controller?.setBridgeReady(ready) }
    fun onTransportRequest() { controller?.onTransportRequest() }
}

/** Resumes once per projected connection, after the bridge is ready; never on discovery alone. */
internal class CarAutoPlayController(
    private val connectionType: LiveData<Int>,
    private val canPlay: () -> Boolean,
    private val isPlaying: () -> Boolean,
    private val requestPlay: () -> Unit,
    private val cancelStartup: () -> Unit,
    private val log: (String) -> Unit,
    private val onConnectionChanged: (Boolean) -> Unit = {},
    private val handler: Handler = Handler(Looper.getMainLooper())
) {
    private var connected = false
    private var bridgeReady = false
    private var handled = false
    private var pending: Runnable? = null
    private val observer = Observer<Int> { type ->
        val projected = type == CarConnection.CONNECTION_TYPE_PROJECTION
        onConnectionChanged(projected)
        if (projected != connected) {
            connected = projected
            log("Android Auto connection: ${if (connected) "connected" else "disconnected"}")
            if (!connected) {
                handled = false
                cancelPending()
                cancelStartup()
            }
        }
        scheduleIfReady()
    }

    fun start() { connectionType.observeForever(observer) }

    fun stop() {
        connectionType.removeObserver(observer)
        cancelPending()
    }

    fun setBridgeReady(ready: Boolean) {
        bridgeReady = ready
        if (!ready) cancelPending() else scheduleIfReady()
    }

    fun onTransportRequest() {
        // A pause or an AA/user play command takes precedence over the delayed fallback.
        if (connected) handled = true
        cancelPending()
    }

    private fun scheduleIfReady() {
        if (!connected || !bridgeReady || handled || pending != null) return
        log("Android Auto auto-play scheduled")
        pending = Runnable {
            pending = null
            if (!connected || !bridgeReady || handled) return@Runnable
            handled = true
            when {
                !canPlay() -> log("Android Auto auto-play skipped: disabled or permission missing")
                isPlaying() -> log("Android Auto auto-play skipped: Spotify already playing")
                else -> {
                    log("Android Auto auto-play requested")
                    requestPlay()
                }
            }
        }.also { handler.postDelayed(it, 2_000L) }
    }

    private fun cancelPending() {
        pending?.let(handler::removeCallbacks)
        pending = null
    }
}
