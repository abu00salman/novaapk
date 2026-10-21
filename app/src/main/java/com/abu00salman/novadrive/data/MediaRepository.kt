package com.abu00salman.novadrive.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Immutable
data class MediaInfo(
    val title: String?,
    val artist: String?,
    val art: ImageBitmap?,
    val playing: Boolean,
    val packageName: String?,
    val durationMs: Long,
    val positionMs: Long,
    val positionAt: Long,
    val speed: Float,
    /** The app's own play queue (e.g. album or playlist), when it publishes one. */
    val queue: List<QueueEntry> = emptyList(),
    val queueTitle: String? = null,
    val activeQueueId: Long = -1,
) {
    fun positionNow(): Long {
        if (!playing || positionMs < 0) return positionMs.coerceAtLeast(0)
        val p = positionMs + ((SystemClock.elapsedRealtime() - positionAt) * speed).toLong()
        return if (durationMs > 0) p.coerceAtMost(durationMs) else p
    }
}

@Immutable
data class QueueEntry(val id: Long, val title: String, val subtitle: String?)

class MediaRepository(private val ctx: Context) {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var active: MediaController? = null
    private val audio = ctx.getSystemService(AudioManager::class.java)

    /** Live now-playing info. Emits null when access isn't granted or nothing is active. */
    fun sessions(): Flow<MediaInfo?> = callbackFlow {
        if (!hasAccess(ctx)) {
            trySend(null); awaitClose { }; return@callbackFlow
        }
        val msm = ctx.getSystemService(MediaSessionManager::class.java)
        val component = ComponentName(ctx, NovaNotificationListener::class.java)
        var current: MediaController? = null
        var lastArtKey: Any? = null
        var lastArt: ImageBitmap? = null

        fun publish() {
            val c = current
            if (c == null) { trySend(null); return }
            val md = c.metadata
            val st = c.playbackState
            val bmp: Bitmap? = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            if (bmp !== lastArtKey) {
                lastArtKey = bmp
                lastArt = bmp?.let { scaled(it).asImageBitmap() }
            }
            trySend(
                MediaInfo(
                    title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
                        ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
                    artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                        ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM),
                    art = lastArt,
                    playing = st?.state == PlaybackState.STATE_PLAYING,
                    packageName = c.packageName,
                    durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                    positionMs = st?.position ?: -1L,
                    positionAt = st?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
                    speed = st?.playbackSpeed ?: 1f,
                    queue = c.queue.orEmpty().take(40).map { q ->
                        QueueEntry(q.queueId, q.description.title?.toString() ?: "—", q.description.subtitle?.toString())
                    },
                    queueTitle = c.queueTitle?.toString(),
                    activeQueueId = st?.activeQueueItemId ?: -1,
                )
            )
        }

        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
            override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
            override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) = publish()
            override fun onSessionDestroyed() {
                current?.unregisterCallback(this); current = null; active = null; publish()
            }
        }

        fun pick(list: List<MediaController>?) {
            val chosen = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: list?.firstOrNull()
            if (chosen?.sessionToken != current?.sessionToken) {
                current?.unregisterCallback(callback)
                current = chosen
                chosen?.registerCallback(callback, main)
            }
            active = current
            publish()
        }

        val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }
        try {
            msm?.addOnActiveSessionsChangedListener(sessionsListener, component, main)
            pick(msm?.getActiveSessions(component))
        } catch (e: SecurityException) {
            trySend(null)
        }
        awaitClose {
            runCatching { msm?.removeOnActiveSessionsChangedListener(sessionsListener) }
            current?.unregisterCallback(callback)
            active = null
        }
    }

    private fun scaled(b: Bitmap): Bitmap {
        val max = 320
        if (b.width <= max && b.height <= max) return b
        val s = max.toFloat() / maxOf(b.width, b.height)
        return Bitmap.createScaledBitmap(b, (b.width * s).toInt().coerceAtLeast(1), (b.height * s).toInt().coerceAtLeast(1), true)
    }

    // Transport: uses the live session when available, otherwise system media keys
    // (works for the head unit's Bluetooth music without any permission).
    fun playPause() {
        val c = active
        if (c != null) {
            if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
            else c.transportControls.play()
        } else key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun playQueueItem(id: Long) { active?.transportControls?.skipToQueueItem(id) }

    fun play() { active?.transportControls?.play() ?: key(KeyEvent.KEYCODE_MEDIA_PLAY) }
    fun pause() { active?.transportControls?.pause() ?: key(KeyEvent.KEYCODE_MEDIA_PAUSE) }
    fun next() { active?.transportControls?.skipToNext() ?: key(KeyEvent.KEYCODE_MEDIA_NEXT) }
    fun previous() { active?.transportControls?.skipToPrevious() ?: key(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }

    private fun key(code: Int) {
        val am = audio ?: return
        runCatching {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
    }

    companion object {
        fun hasAccess(ctx: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
}
