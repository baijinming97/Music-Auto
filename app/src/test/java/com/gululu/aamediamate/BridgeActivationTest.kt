package com.gululu.aamediamate

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import com.gululu.aamediamate.models.MediaInfo
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BridgeActivationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("media_bridge_settings", Context.MODE_PRIVATE).edit().clear().commit()
        mockkObject(MediaControllerManager)
        every { MediaControllerManager.getActiveController(any()) } returns null
        MediaBridgeSessionManager.init(context)
    }

    @After
    fun tearDown() {
        MediaBridgeSessionManager.release()
        unmockkObject(MediaControllerManager)
    }

    @Test
    fun `missing or filtered sources clear metadata and deactivate the bridge`() {
        val sessionField = MediaBridgeSessionManager::class.java.getDeclaredField("mediaSession").apply { isAccessible = true }
        val session = spyk(sessionField.get(MediaBridgeSessionManager) as android.support.v4.media.session.MediaSessionCompat)
        sessionField.set(MediaBridgeSessionManager, session)
        // Robolectric does not forward metadata through the platform session binder.
        var metadata: MediaMetadataCompat? = null
        every { session.setMetadata(any()) } answers { metadata = firstArg() }
        val info = MediaInfo(SettingsManager.SPOTIFY_PACKAGE, "Spotify", "Song", "Artist", "Album",
            60_000, 0, true, null, null)
        assertNull(metadata)
        assertFalse(session.isActive)
        MediaBridgeSessionManager.updateFromMediaInfo(info)
        assertTrue(session.isActive)
        assertEquals("Song", metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
        assertEquals(SettingsManager.SPOTIFY_PACKAGE, MediaBridgeSessionManager.getCurrentMediaPackage())
        MediaBridgeSessionManager.updateFromMediaInfo(info.copy(appPackageName = "com.example.music"))
        assertNull(metadata)
        assertNull(MediaBridgeSessionManager.getCurrentMediaPackage())
        assertFalse(session.isActive)
        MediaBridgeSessionManager.updateFromMediaInfo(info)
        MediaBridgeSessionManager.updateFromMediaInfo(null)
        assertNull(metadata)
        assertNull(MediaBridgeSessionManager.getCurrentMediaPackage())
        assertFalse(session.isActive)
    }
}
