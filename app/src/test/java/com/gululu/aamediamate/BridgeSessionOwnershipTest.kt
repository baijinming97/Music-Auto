package com.gululu.aamediamate

import android.content.Context
import com.gululu.aamediamate.models.MediaInfo
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BridgeSessionOwnershipTest {
    private lateinit var context: Context
    private val listener = Any()
    private val browser = Any()
    private val info = MediaInfo(SettingsManager.SPOTIFY_PACKAGE, "Spotify", "Song", "Artist", "Album",
        60_000, 0, false, null, null)

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("media_bridge_settings", Context.MODE_PRIVATE).edit().clear().commit()
        mockkObject(MediaControllerManager)
        every { MediaControllerManager.getActiveController(any()) } returns null
        mockkObject(MediaInformationRetriever)
        every { MediaInformationRetriever.refreshCurrentMediaInfo(any()) } returns info
        MediaBridgeSessionManager.release()
        MediaBridgeSessionManager.setCarConnected(false)
    }

    @After fun tearDown() {
        MediaBridgeSessionManager.release()
        MediaBridgeSessionManager.setCarConnected(false)
        unmockkObject(MediaControllerManager, MediaInformationRetriever)
    }

    @Test fun `browser disconnect keeps a resumable session while notification listener lives`() {
        MediaBridgeSessionManager.acquire(context, listener, false)
        MediaBridgeSessionManager.acquire(context, browser, true)
        MediaBridgeSessionManager.updateFromMediaInfo(info)
        val token = MediaBridgeSessionManager.getSessionToken()
        assertNotNull(token)
        MediaBridgeSessionManager.release(browser)
        assertSame(token, MediaBridgeSessionManager.getSessionToken())
        assertEquals(SettingsManager.SPOTIFY_PACKAGE, MediaBridgeSessionManager.getCurrentMediaPackage())
        MediaBridgeSessionManager.acquire(context, Any(), true)
        assertSame(token, MediaBridgeSessionManager.getSessionToken())
    }

    @Test fun `notification listener can publish the session before a browser binds`() {
        MediaBridgeSessionManager.acquire(context, listener, false)
        MediaBridgeSessionManager.updateFromMediaInfo(info)
        assertNotNull(MediaBridgeSessionManager.getSessionToken())
        assertEquals(SettingsManager.SPOTIFY_PACKAGE, MediaBridgeSessionManager.getCurrentMediaPackage())
    }

    @Test fun `last departing service releases resources and duplicate callbacks are harmless`() {
        MediaBridgeSessionManager.acquire(context, listener, false)
        MediaBridgeSessionManager.acquire(context, listener, false)
        MediaBridgeSessionManager.release(Any())
        assertNotNull(MediaBridgeSessionManager.getSessionToken())
        MediaBridgeSessionManager.release(listener)
        MediaBridgeSessionManager.release(listener)
        assertNull(MediaBridgeSessionManager.getSessionToken())
        assertNull(MediaBridgeSessionManager.getCurrentMediaPackage())
    }

    @Test fun `browser remains valid when notification listener disconnects first`() {
        MediaBridgeSessionManager.acquire(context, listener, false)
        MediaBridgeSessionManager.acquire(context, browser, true)
        val token = MediaBridgeSessionManager.getSessionToken()
        MediaBridgeSessionManager.updateFromMediaInfo(null)
        MediaBridgeSessionManager.release(listener)
        assertSame(token, MediaBridgeSessionManager.getSessionToken())
        assertNull(MediaBridgeSessionManager.getCurrentMediaPackage())
        MediaBridgeSessionManager.release(browser)
        assertNull(MediaBridgeSessionManager.getSessionToken())
    }

    @Test fun `listener-only session does not run lyric timers and browser rebind restores them`() {
        MediaBridgeSessionManager.acquire(context, listener, false)
        val lyricManager = mockk<LyricDisplayManager>(relaxed = true)
        val field = MediaBridgeSessionManager::class.java.getDeclaredField("lyricDisplayManager").apply { isAccessible = true }
        (field.get(MediaBridgeSessionManager) as LyricDisplayManager).close()
        field.set(MediaBridgeSessionManager, lyricManager)
        MediaBridgeSessionManager.updateFromMediaInfo(info.copy(isPlaying = true))
        verify(exactly = 0) { lyricManager.start(any(), any()) }
        MediaBridgeSessionManager.acquire(context, browser, true)
        MediaBridgeSessionManager.updateFromMediaInfo(info.copy(isPlaying = true))
        verify(exactly = 1) { lyricManager.start(any(), any()) }
        MediaBridgeSessionManager.release(browser)
        clearMocks(lyricManager, answers = false)
        MediaBridgeSessionManager.updateFromMediaInfo(info.copy(isPlaying = true))
        verify(exactly = 0) { lyricManager.start(any(), any()) }
        verify { lyricManager.stop() }
    }

    @Test fun `projection starts lyrics before browser bind and disconnect stops the work`() {
        MediaBridgeSessionManager.acquire(context, listener, false)
        val lyricManager = mockk<LyricDisplayManager>(relaxed = true)
        val field = MediaBridgeSessionManager::class.java.getDeclaredField("lyricDisplayManager").apply { isAccessible = true }
        (field.get(MediaBridgeSessionManager) as LyricDisplayManager).close()
        field.set(MediaBridgeSessionManager, lyricManager)
        MediaBridgeSessionManager.setCarConnected(true)
        MediaBridgeSessionManager.updateFromMediaInfo(info.copy(isPlaying = true))
        verify(exactly = 1) { lyricManager.start(any(), any()) }
        MediaBridgeSessionManager.setCarConnected(false)
        clearMocks(lyricManager, answers = false)
        MediaBridgeSessionManager.updateFromMediaInfo(info.copy(isPlaying = true))
        verify(exactly = 0) { lyricManager.start(any(), any()) }
        verify { lyricManager.stop() }
        assertNotNull(MediaBridgeSessionManager.getSessionToken())
    }
}
