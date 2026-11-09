package com.gululu.aamediamate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import com.gululu.aamediamate.diagnostics.DiagnosticLogger
import com.gululu.aamediamate.diagnostics.DiagnosticModule

/** Resumes Spotify using its session or its public, explicit media-button receiver. */
internal object SpotifyPlaybackStarter {
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var pendingCheck: Runnable? = null

    fun isAvailable(context: Context): Boolean = findReceiver(context) != null

    fun play(context: Context) {
        val appContext = context.applicationContext
        if (!Global.packageAllowed(appContext, SettingsManager.SPOTIFY_PACKAGE) || pendingCheck != null) return
        var playRequested = false
        val current = spotifyController(appContext)
        val started = runCatching {
            if (current != null) {
                current.transportControls.play()
                playRequested = true
            } else {
                val receiver = findReceiver(appContext) ?: return@runCatching false
                val now = SystemClock.uptimeMillis()
                // PLAY is idempotent; PLAY_PAUSE could pause a just-started session.
                for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
                    appContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_BUTTON)
                        .setComponent(receiver)
                        .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES or Intent.FLAG_RECEIVER_FOREGROUND)
                        .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(now, now, action, KeyEvent.KEYCODE_MEDIA_PLAY, 0)))
                }
                DiagnosticLogger.info(appContext, DiagnosticModule.MEDIA, "Spotify startup requested through media button")
            }
            true
        }.getOrDefault(false)
        if (!started) {
            showFailure(appContext)
            return
        }

        val deadline = SystemClock.elapsedRealtime() + 8_000L
        pendingCheck = object : Runnable {
            override fun run() {
                if (!Global.packageAllowed(appContext, SettingsManager.SPOTIFY_PACKAGE)) {
                    release()
                    return
                }
                val source = spotifyController(appContext)
                if (source?.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    DiagnosticLogger.info(appContext, DiagnosticModule.MEDIA, "Spotify playback confirmed")
                    MediaBridgeSessionManager.requestRefresh("Spotify playback started", 0L)
                    release()
                } else if (SystemClock.elapsedRealtime() >= deadline) {
                    showFailure(appContext)
                    release()
                } else {
                    if (source != null && !playRequested) {
                        playRequested = true
                        runCatching { source.transportControls.play() }
                        MediaBridgeSessionManager.requestRefresh("Spotify session became available", 0L)
                    }
                    handler.postDelayed(this, 250L)
                }
            }
        }.also { handler.postDelayed(it, 250L) }
    }

    fun release() {
        pendingCheck?.let(handler::removeCallbacks)
        pendingCheck = null
    }

    private fun spotifyController(context: Context) = MediaControllerManager.selectController(
        MediaControllerManager.getAllControllers(context).filter { it.packageName == SettingsManager.SPOTIFY_PACKAGE }
    )

    private fun findReceiver(context: Context): ComponentName? = runCatching {
        context.packageManager.queryBroadcastReceivers(
            Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(SettingsManager.SPOTIFY_PACKAGE), 0
        ).firstOrNull { it.activityInfo?.exported == true && it.activityInfo.enabled }?.activityInfo?.let {
            ComponentName(it.packageName, it.name)
        }
    }.getOrNull()

    private fun showFailure(context: Context) {
        DiagnosticLogger.warn(context, DiagnosticModule.MEDIA, "Spotify did not start; open Spotify on phone")
        MediaBridgeSessionManager.showBrowserError(context.getString(R.string.spotify_start_failed))
    }
}
