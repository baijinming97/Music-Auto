package com.gululu.aamediamate

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.provider.Settings
import io.mockk.*
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SpotifySourceLockTest {
    private lateinit var context: Context
    private val spotify = SettingsManager.SPOTIFY_PACKAGE
    private val otherPlayer = "com.example.music"

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("media_bridge_settings", Context.MODE_PRIVATE).edit().clear().commit()
        MediaControllerManager.clearSelection()
        shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            packageName = spotify
            applicationInfo = ApplicationInfo().apply {
                packageName = spotify
                metaData = Bundle().apply { putInt("com.google.android.gms.car.application", 1) }
            }
        })
    }

    @After
    fun tearDown() {
        MediaControllerManager.clearSelection()
        unmockkAll()
    }

    @Test
    fun `Spotify lock survives the native-player filter and rejects other players`() {
        SettingsManager.setIgnoreNativeAutoApps(context, true)
        assertTrue(Global.isAndroidAutoApp(context, spotify))
        assertTrue(SettingsManager.getSpotifySourceLock(context))
        assertTrue(Global.packageAllowed(context, spotify))
        assertFalse(Global.packageAllowed(context, otherPlayer))
    }

    @Test
    fun `an explicit per-app opt-out remains effective with Spotify lock`() {
        SettingsManager.addOrUpdateBridgedApp(context, spotify, "Spotify")
        SettingsManager.setAppHeadUnitControlEnabled(context, spotify, false)
        assertFalse(Global.packageAllowed(context, spotify))
    }

    @Test
    fun `turning the lock off restores the native-player filter and automatic sources`() {
        SettingsManager.setSpotifySourceLock(context, false)
        SettingsManager.setIgnoreNativeAutoApps(context, true)
        assertFalse(Global.packageAllowed(context, spotify))
        assertTrue(Global.packageAllowed(context, otherPlayer))
        SettingsManager.setIgnoreNativeAutoApps(context, false)
        assertTrue(Global.packageAllowed(context, spotify))
    }

    @Test
    fun `parallel bridge installations cannot become a source even in automatic mode`() {
        SettingsManager.setSpotifySourceLock(context, false)
        assertFalse(Global.packageAllowed(context, "com.gululu.aamediamate"))
        assertFalse(Global.packageAllowed(context, "com.gululu.aamediamate.dev"))
    }

    @Test
    fun `source lock round-trips through backup and invalid values are rejected`() {
        SettingsManager.setSpotifySourceLock(context, false)
        val backup = SettingsManager.exportBackupSettings(context, false)
        SettingsManager.setSpotifySourceLock(context, true)
        SettingsManager.importBackupSettings(context, backup)
        assertFalse(SettingsManager.getSpotifySourceLock(context))
        assertThrows(IllegalArgumentException::class.java) {
            SettingsManager.importBackupSettings(context, JSONObject().put("spotify_source_lock", "true"))
        }
        assertFalse(SettingsManager.getSpotifySourceLock(context))
    }

    @Test
    fun `paused Spotify is retained and disappearing Spotify never falls back to another player`() {
        val manager = mockk<MediaSessionManager>()
        val testContext = controllerContext(manager)
        mockkObject(MediaBridgeSessionManager)
        every { MediaBridgeSessionManager.getCurrentMediaPackage() } returns spotify
        val paused = controller(spotify, PlaybackState.STATE_PAUSED)
        val other = controller(otherPlayer, PlaybackState.STATE_PLAYING)
        every { manager.getActiveSessions(any<ComponentName>()) } returns listOf(other, paused)
        assertSame(paused, MediaControllerManager.getFirstController(testContext))
        every { manager.getActiveSessions(any<ComponentName>()) } returns listOf(other)
        assertNull(MediaControllerManager.getFirstController(testContext))
        assertNull(MediaControllerManager.getActiveController(testContext))
    }

    @Test
    fun `a new playing Spotify session beats a stale paused session and receives controls`() {
        val manager = mockk<MediaSessionManager>()
        val testContext = controllerContext(manager)
        mockkObject(MediaBridgeSessionManager)
        every { MediaBridgeSessionManager.getCurrentMediaPackage() } returns spotify
        val old = controller(spotify, PlaybackState.STATE_PAUSED)
        val resumed = controller(spotify, PlaybackState.STATE_PLAYING)
        every { manager.getActiveSessions(any<ComponentName>()) } returns listOf(old)
        assertSame(old, MediaControllerManager.getFirstController(testContext))
        every { manager.getActiveSessions(any<ComponentName>()) } returns listOf(old, resumed)
        assertSame(resumed, MediaControllerManager.getFirstController(testContext))
        assertSame(resumed, MediaControllerManager.getActiveController(testContext))
    }

    @Test
    fun `session order changes do not move controls away from the displayed Spotify session`() {
        val manager = mockk<MediaSessionManager>()
        val testContext = controllerContext(manager)
        mockkObject(MediaBridgeSessionManager)
        every { MediaBridgeSessionManager.getCurrentMediaPackage() } returns spotify
        val first = controller(spotify, PlaybackState.STATE_PLAYING)
        val second = controller(spotify, PlaybackState.STATE_PLAYING)
        every { manager.getActiveSessions(any<ComponentName>()) } returns listOf(first, second)
        assertSame(first, MediaControllerManager.getFirstController(testContext))
        every { manager.getActiveSessions(any<ComponentName>()) } returns listOf(second, first)
        assertSame(first, MediaControllerManager.getActiveController(testContext))
        assertSame(first, MediaControllerManager.getFirstController(testContext))
    }

    @Test
    fun `a session with no metadata does not hide a usable Spotify session`() {
        val manager = mockk<MediaSessionManager>()
        val testContext = controllerContext(manager)
        mockkObject(MediaBridgeSessionManager)
        every { MediaBridgeSessionManager.getCurrentMediaPackage() } returns null
        val empty = controller(spotify, PlaybackState.STATE_PLAYING)
        shadowOf(empty).setMetadata(null)
        val usable = controller(spotify, PlaybackState.STATE_PAUSED)
        every { manager.getActiveSessions(any<ComponentName>()) } returns listOf(empty, usable)
        assertSame(usable, MediaControllerManager.getFirstController(testContext))
        assertSame(usable, MediaControllerManager.getActiveController(testContext))
    }

    @Test
    fun `automatic mode still follows a playing app over a paused app`() {
        val paused = controller(spotify, PlaybackState.STATE_PAUSED)
        val playing = controller(otherPlayer, PlaybackState.STATE_PLAYING)
        assertSame(playing, MediaControllerManager.selectController(listOf(paused, playing), spotify))
    }

    @Test
    fun `permission for the dev installation does not grant access to the official installation`() {
        val officialContext = mockk<Context>()
        every { officialContext.packageName } returns "com.gululu.aamediamate"
        every { officialContext.contentResolver } returns context.contentResolver
        Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners",
            "com.gululu.aamediamate.dev/com.gululu.aamediamate.MediaNotificationListener")
        assertFalse(hasNotificationAccess(officialContext))
        Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners",
            "com.gululu.aamediamate/.MediaNotificationListener")
        assertTrue(hasNotificationAccess(officialContext))
    }

    private fun controllerContext(manager: MediaSessionManager): Context = mockk {
        every { packageName } returns context.packageName
        every { applicationContext } returns context
        every { packageManager } returns context.packageManager
        every { getSharedPreferences(any(), any()) } answers { context.getSharedPreferences(firstArg(), secondArg()) }
        every { getSystemService(Context.MEDIA_SESSION_SERVICE) } returns manager
    }

    private fun controller(pkg: String, state: Int): MediaController {
        // Android session tokens are final platform objects; use a real token instead of redefining the class.
        val session = MediaSession(context, java.util.UUID.randomUUID().toString())
        val token = session.sessionToken
        session.release()
        return MediaController(context, token).also {
            shadowOf(it).setPackageName(pkg)
            shadowOf(it).setPlaybackState(PlaybackState.Builder().setState(state, 0L, 1f).build())
            shadowOf(it).setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "Song").build())
        }
    }
}
