package com.gululu.aamediamate

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.session.PlaybackState
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in, parked-car experiment. No audio-focus request; only zero-valued PCM samples. */
@RunWith(AndroidJUnit4::class)
class CarAudioOwnershipProbeTest {
    @Test
    fun observeHostSelectionDuringSilentPlayback() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("audioOwnershipProbe") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue("Connect AA before this opt-in experiment", projected(context))
        assumeTrue("Spotify must already be playing", spotifyPlaying(context))
        val owner = Any()
        var audioTrack: AudioTrack? = null
        try {
            instrumentation.runOnMainSync {
                AndroidAutoPlayback.start(context)
                MediaBridgeSessionManager.acquire(context, owner, renderLyrics = true)
                MediaBridgeSessionManager.refreshCurrentSession()
            }
            Log.i(TAG, "BASELINE: bridge ready, no PCM output for 10 seconds")
            SystemClock.sleep(10_000)
            assumeTrue(projected(context) && spotifyPlaying(context))

            val frames = 48_000
            val silence = ByteArray(frames * 2) // signed PCM16 zero is digital silence
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(48_000)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(silence.size)
                .build()
            audioTrack = track
            assertEquals(silence.size, track.write(silence, 0, silence.size))
            assertEquals(AudioTrack.STATE_INITIALIZED, track.state)
            assertEquals(AudioTrack.SUCCESS, track.setLoopPoints(0, frames, -1))
            track.play()
            Log.i(TAG, "PCM_ZERO_STARTED: 60 second maximum; no focus requested")
            val deadline = SystemClock.elapsedRealtime() + 60_000
            while (SystemClock.elapsedRealtime() < deadline && projected(context) && spotifyPlaying(context)) {
                SystemClock.sleep(500)
            }
            track.stop()
            track.release()
            audioTrack = null
            Log.i(TAG, "PCM_ZERO_STOPPED: observing normal playback for 10 seconds")
            SystemClock.sleep(10_000)
        } finally {
            audioTrack?.let { runCatching { it.stop() }; it.release() }
            instrumentation.runOnMainSync { MediaBridgeSessionManager.release(owner) }
            Log.i(TAG, "PROBE_FINISHED: all experiment-owned resources released")
        }
    }

    private fun projected(context: Context): Boolean = runCatching {
        context.contentResolver.query(
            Uri.parse("content://androidx.car.app.connection"), arrayOf("CarConnectionState"), null, null, null
        )?.use { it.moveToFirst() && it.getInt(0) == 2 } == true
    }.getOrDefault(false)

    private fun spotifyPlaying(context: Context): Boolean = MediaControllerManager.getAllControllers(context).any {
        it.packageName == SettingsManager.SPOTIFY_PACKAGE && it.playbackState?.state == PlaybackState.STATE_PLAYING
    }

    private companion object { const val TAG = "MusicAutoOwnershipProbe" }
}
