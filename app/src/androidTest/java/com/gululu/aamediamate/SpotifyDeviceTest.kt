package com.gululu.aamediamate

import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in device integration check; requires Spotify and notification access on the test device. */
@RunWith(AndroidJUnit4::class)
class SpotifyDeviceTest {
    @Test
    fun spotifyCanResumePlayback() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(InstrumentationRegistry.getArguments().getString("spotifyIntegration") == "true")
        assertTrue("Grant notification access to the test build first", hasNotificationAccess(context))
        assertTrue("Spotify media receiver must be installed", SpotifyPlaybackStarter.isAvailable(context))
        val wasPlaying = MediaControllerManager.getAllControllers(context).any {
            it.packageName == SettingsManager.SPOTIFY_PACKAGE && it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        try {
            instrumentation.runOnMainSync {
                MediaBridgeSessionManager.init(context)
                SpotifyPlaybackStarter.play(context)
            }
            val deadline = SystemClock.elapsedRealtime() + 12_000L
            var playing = false
            while (SystemClock.elapsedRealtime() < deadline && !playing) {
                playing = MediaControllerManager.getAllControllers(context).any {
                    it.packageName == SettingsManager.SPOTIFY_PACKAGE && it.playbackState?.state == PlaybackState.STATE_PLAYING
                }
                if (!playing) SystemClock.sleep(250L)
            }
            assertTrue("Spotify must start playback requested by the bridge", playing)
        } finally {
            if (!wasPlaying) {
                MediaControllerManager.getAllControllers(context).firstOrNull {
                    it.packageName == SettingsManager.SPOTIFY_PACKAGE
                }?.transportControls?.pause()
            }
            instrumentation.runOnMainSync { MediaBridgeSessionManager.release() }
        }
    }
}
