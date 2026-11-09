// Modified for Music Auto (2026-11-10); see NOTICE and CHANGELOG.md.
package com.gululu.aamediamate

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import com.gululu.aamediamate.diagnostics.DiagnosticLogger
import com.gululu.aamediamate.diagnostics.DiagnosticModule

object MediaControllerManager {
    private var selectedToken: MediaSession.Token? = null

    fun getAllControllers(context: Context): List<MediaController> {
        return try {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
            val component = ComponentName(context, MediaNotificationListener::class.java)
            val controllers = sessionManager.getActiveSessions(component)
                .filter { Global.packageAllowed(context, it.packageName) }
            DiagnosticLogger.debug(
                context,
                DiagnosticModule.MEDIA,
                "Active media controllers queried",
                mapOf("count" to controllers.size, "packages" to controllers.joinToString(",") { it.packageName })
            )
            controllers
        } catch (e: Exception) {
            // Likely SecurityException due to missing notification access
            DiagnosticLogger.error(
                context,
                DiagnosticModule.MEDIA,
                "Failed to query active media controllers",
                throwable = e
            )
            emptyList()
        }
    }

    fun getFirstController(context: Context): MediaController? {
        // A session without metadata must not hide a usable session further down the list.
        val controllers = getAllControllers(context).filter { it.metadata != null && it.playbackState != null }
        val selected = selectController(controllers, MediaBridgeSessionManager.getCurrentMediaPackage(), selectedToken)
        selectedToken = selected?.sessionToken
        return selected
    }

    fun getActiveController(context: Context): MediaController? {
        val target = MediaBridgeSessionManager.getCurrentMediaPackage()
            ?: SettingsManager.SPOTIFY_PACKAGE.takeIf { SettingsManager.getSpotifySourceLock(context) }
            ?: return null
        val controllers = getAllControllers(context).filter { it.packageName == target }
        // Keep controls on the same session that supplied the displayed metadata.
        return controllers.firstOrNull { it.sessionToken == selectedToken }
            ?: selectController(controllers, target)
    }

    internal fun selectController(
        controllers: List<MediaController>,
        currentPackage: String? = null,
        currentToken: MediaSession.Token? = null
    ): MediaController? {
        val playing = controllers.filter {
            it.playbackState?.state == PlaybackState.STATE_PLAYING ||
                it.playbackState?.state == PlaybackState.STATE_BUFFERING
        }
        val candidates = playing.ifEmpty { controllers }
        return candidates.firstOrNull { currentToken != null && it.sessionToken == currentToken }
            ?: candidates.firstOrNull { it.packageName == currentPackage }
            ?: candidates.firstOrNull()
    }

    internal fun clearSelection() {
        selectedToken = null
    }
}
