package com.gululu.aamediamate

import android.content.Context
import android.os.Looper
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.MutableLiveData
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class CarAutoPlayControllerTest {
    private lateinit var type: MutableLiveData<Int>
    private lateinit var controller: CarAutoPlayController
    private var allowed = true
    private var playing = false
    private var playRequests = 0
    private var startupCancellations = 0

    @Before
    fun setUp() {
        type = MutableLiveData()
        controller = CarAutoPlayController(type, { allowed }, { playing },
            { playRequests++ }, { startupCancellations++ }, {})
        controller.start()
    }

    @After
    fun tearDown() { controller.stop() }

    private fun waitForAutoPlay() { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3)) }
    private fun connect() { type.value = CarConnection.CONNECTION_TYPE_PROJECTION }
    private fun disconnect() { type.value = CarConnection.CONNECTION_TYPE_NOT_CONNECTED }

    @Test
    fun `browser discovery without a projected car does not play`() {
        disconnect()
        controller.setBridgeReady(true)
        waitForAutoPlay()
        assertEquals(0, playRequests)
    }

    @Test
    fun `connection waits for bridge then resumes without a host play command`() {
        connect()
        waitForAutoPlay()
        assertEquals(0, playRequests)
        controller.setBridgeReady(true)
        waitForAutoPlay()
        assertEquals(1, playRequests)
    }

    @Test
    fun `already connected initial query resumes when bridge is ready`() {
        controller.setBridgeReady(true)
        connect()
        waitForAutoPlay()
        assertEquals(1, playRequests)
    }

    @Test
    fun `repeated broadcasts and service recreation do not override a pause`() {
        controller.setBridgeReady(true)
        connect()
        waitForAutoPlay()
        controller.onTransportRequest()
        controller.setBridgeReady(false)
        connect()
        controller.setBridgeReady(true)
        waitForAutoPlay()
        assertEquals(1, playRequests)
    }

    @Test
    fun `pause or host play before the delay cancels the fallback`() {
        controller.setBridgeReady(true)
        connect()
        controller.onTransportRequest()
        waitForAutoPlay()
        assertEquals(0, playRequests)
    }

    @Test
    fun `disconnect cancels a pending resume and next connection can play`() {
        controller.setBridgeReady(true)
        connect()
        disconnect()
        waitForAutoPlay()
        assertEquals(0, playRequests)
        assertEquals(1, startupCancellations)
        connect()
        waitForAutoPlay()
        assertEquals(1, playRequests)
    }

    @Test
    fun `new connection resumes again after a completed attempt`() {
        controller.setBridgeReady(true)
        connect()
        waitForAutoPlay()
        disconnect()
        connect()
        waitForAutoPlay()
        assertEquals(2, playRequests)
    }

    @Test
    fun `disabled setting or lost permission at execution prevents playback`() {
        controller.setBridgeReady(true)
        connect()
        allowed = false
        waitForAutoPlay()
        assertEquals(0, playRequests)
    }

    @Test
    fun `Spotify already playing consumes connection without another play`() {
        controller.setBridgeReady(true)
        connect()
        playing = true
        waitForAutoPlay()
        playing = false
        connect()
        waitForAutoPlay()
        assertEquals(0, playRequests)
    }

    @Test
    fun `bridge loss postpones playback until it is ready again`() {
        controller.setBridgeReady(true)
        connect()
        controller.setBridgeReady(false)
        waitForAutoPlay()
        assertEquals(0, playRequests)
        controller.setBridgeReady(true)
        waitForAutoPlay()
        assertEquals(1, playRequests)
    }

    @Test
    fun `native automotive connection does not trigger phone projection resume`() {
        controller.setBridgeReady(true)
        type.value = CarConnection.CONNECTION_TYPE_NATIVE
        waitForAutoPlay()
        assertEquals(0, playRequests)
    }

    @Test
    fun `auto-play preference survives backup and older settings import`() {
        val context = RuntimeEnvironment.getApplication() as Context
        context.getSharedPreferences("media_bridge_settings", Context.MODE_PRIVATE).edit().clear().commit()
        assertTrue(SettingsManager.getCarAutoPlay(context))
        SettingsManager.setCarAutoPlay(context, false)
        val backup = SettingsManager.exportBackupSettings(context, false)
        SettingsManager.setCarAutoPlay(context, true)
        SettingsManager.importBackupSettings(context, backup)
        assertFalse(SettingsManager.getCarAutoPlay(context))
        SettingsManager.importBackupSettings(context, org.json.JSONObject().put("lyrics_enabled", true))
        assertFalse(SettingsManager.getCarAutoPlay(context))
    }
}
