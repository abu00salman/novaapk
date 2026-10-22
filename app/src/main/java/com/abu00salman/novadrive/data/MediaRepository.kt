package com.abu00salman.novadrive.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.util.concurrent.Executors
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
    /** The session advertises ACTION_SEEK_TO, so a seek bar may be offered. */
    val canSeek: Boolean = false,
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
        var all: List<MediaController> = emptyList()
        var current: MediaController? = null
        var lastArtKey: Any? = null
        var lastArt: ImageBitmap? = null
        val io = Executors.newSingleThreadExecutor()

        fun title(md: MediaMetadata?) = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: md?.description?.title?.toString()

        fun publish() {
            val c = current
            if (c == null) { trySend(null); return }
            val md = c.metadata
            val st = c.playbackState
            val bmp: Bitmap? = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                ?: md?.description?.iconBitmap
            // Many apps only publish a content:// URI for the artwork; load it off the main thread.
            val uri: String? = if (bmp != null) null else md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ART_URI)
                ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
                ?: md?.description?.iconUri?.toString()
            val key: Any? = bmp ?: uri
            if (key != lastArtKey) {
                lastArtKey = key
                lastArt = bmp?.let { scaled(it).asImageBitmap() }
                if (bmp == null && uri != null) io.execute {
                    val loaded = loadArt(uri)
                    main.post { if (lastArtKey == uri && loaded != null) { lastArt = loaded; publish() } }
                }
            }
            trySend(
                MediaInfo(
                    title = title(md),
                    artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                        ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                        ?: md?.description?.subtitle?.toString()
                        ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM),
                    art = lastArt,
                    playing = isPlaying(c),
                    packageName = c.packageName,
                    durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                    // Unknown position (common for CarPlay/Bluetooth bridges) is -1 so the UI hides the timeline.
                    positionMs = if (st == null || st.state == PlaybackState.STATE_NONE) -1L else st.position,
                    positionAt = st?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
                    speed = st?.playbackSpeed ?: 1f,
                    queue = c.queue.orEmpty().take(40).map { q ->
                        QueueEntry(q.queueId, q.description.title?.toString() ?: "—", q.description.subtitle?.toString())
                    },
                    queueTitle = c.queueTitle?.toString(),
                    activeQueueId = st?.activeQueueItemId ?: -1,
                    canSeek = (st?.actions ?: 0L) and PlaybackState.ACTION_SEEK_TO != 0L,
                )
            )
        }

        /** Playing beats paused; a session that shows a track beats an empty one (e.g. idle radio). */
        fun choose() {
            val chosen = best(all)
            if (chosen?.sessionToken != current?.sessionToken) current = chosen
            active = current
            publish()
        }

        // Every session is watched, so when another app starts playing it takes over immediately.
        val callbacks = mutableMapOf<MediaSession.Token, MediaController.Callback>()
        fun watch(list: List<MediaController>) {
            callbacks.forEach { (token, cb) -> all.firstOrNull { it.sessionToken == token }?.unregisterCallback(cb) }
            callbacks.clear()
            all = list
            list.forEach { c ->
                val cb = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = choose()
                    override fun onMetadataChanged(metadata: MediaMetadata?) = choose()
                    override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) = choose()
                    override fun onSessionDestroyed() { all = all.filter { it.sessionToken != c.sessionToken }; choose() }
                }
                c.registerCallback(cb, main)
                callbacks[c.sessionToken] = cb
            }
            choose()
        }

        val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { watch(it.orEmpty()) }
        try {
            msm?.addOnActiveSessionsChangedListener(sessionsListener, component, main)
            watch(msm?.getActiveSessions(component).orEmpty())
            listSessions = { runCatching { msm?.getActiveSessions(component) }.getOrNull().orEmpty() }
        } catch (e: SecurityException) {
            trySend(null)
        }
        awaitClose {
            runCatching { msm?.removeOnActiveSessionsChangedListener(sessionsListener) }
            callbacks.forEach { (token, cb) -> all.firstOrNull { it.sessionToken == token }?.unregisterCallback(cb) }
            io.shutdown()
            active = null
            listSessions = { emptyList() }
        }
    }

    @Volatile private var listSessions: () -> List<MediaController> = { emptyList() }

    private fun best(list: List<MediaController>): MediaController? =
        list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.firstOrNull { it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null || it.metadata?.description?.title != null }
            ?: list.firstOrNull()

    /** The session to control right now (re-evaluated at tap time, not cached from a stale pick). */
    private fun target(): MediaController? = best(listSessions()) ?: active

    private fun loadArt(uri: String): ImageBitmap? = runCatching {
        val u = Uri.parse(uri)
        if (u.scheme != "content" && u.scheme != "file" && u.scheme != "android.resource") return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (maxOf(opts.outWidth, opts.outHeight) / (sample * 2) >= 320) sample *= 2
        ctx.contentResolver.openInputStream(u)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }?.let { scaled(it).asImageBitmap() }
    }.getOrNull()

    private fun scaled(b: Bitmap): Bitmap {
        val max = 320
        if (b.width <= max && b.height <= max) return b
        val s = max.toFloat() / maxOf(b.width, b.height)
        return Bitmap.createScaledBitmap(b, (b.width * s).toInt().coerceAtLeast(1), (b.height * s).toInt().coerceAtLeast(1), true)
    }

    // Transport: sends to the session that is actually playing; if the app ignores the command
    // (state/track unchanged shortly after) the same action is retried as a system media key.
    // Media keys also cover the head unit's Bluetooth music without any permission.
    /**
     * Some bridges (ZLink/CarPlay, Bluetooth audio) never report PLAYING; if they don't claim to be
     * paused and audio is actually coming out, treat them as playing.
     */
    private fun isPlaying(c: MediaController): Boolean {
        val st = c.playbackState?.state
        if (st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING) return true
        if (st == PlaybackState.STATE_PAUSED) return false
        return audio?.isMusicActive == true
    }

    fun playPause() {
        val c = target()
        if (c == null) { key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE); return }
        val wasPlaying = isPlaying(c)
        if (wasPlaying) c.transportControls.pause() else c.transportControls.play()
        escalate(c, { isPlaying(it) != wasPlaying }, if (wasPlaying) KeyEvent.KEYCODE_MEDIA_PAUSE else KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    fun next() = skip(KeyEvent.KEYCODE_MEDIA_NEXT) { it.transportControls.skipToNext() }
    fun previous() = skip(KeyEvent.KEYCODE_MEDIA_PREVIOUS) { it.transportControls.skipToPrevious() }

    private fun skip(code: Int, action: (MediaController) -> Unit) {
        val c = target()
        if (c == null) { key(code); return }
        val before = trackKey(c)
        action(c)
        escalate(c, { trackKey(it) != before }, code)
    }

    private fun trackKey(c: MediaController): Any? = c.metadata?.let {
        (it.getString(MediaMetadata.METADATA_KEY_TITLE) ?: it.description?.title?.toString()) to it.getString(MediaMetadata.METADATA_KEY_ARTIST)
    }

    /**
     * Apps differ in what they obey. If the transport command had no visible effect, the same
     * action is sent as a media button to that session, then as a system-wide media key.
     * Play/pause use the explicit PLAY or PAUSE keys so a late response can't toggle back.
     */
    private fun escalate(c: MediaController, changed: (MediaController) -> Boolean, code: Int) {
        fun ok() = runCatching { changed(c) }.getOrDefault(false)
        main.postDelayed({
            if (ok()) return@postDelayed
            runCatching {
                c.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
                c.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, code))
            }
            main.postDelayed({ if (!ok()) key(code) }, 1200)
        }, 1200)
    }

    fun seekTo(ms: Long) { target()?.transportControls?.seekTo(ms.coerceAtLeast(0)) }

    fun playQueueItem(id: Long) { target()?.transportControls?.skipToQueueItem(id) }

    fun play() { target()?.transportControls?.play() ?: key(KeyEvent.KEYCODE_MEDIA_PLAY) }
    fun pause() { target()?.transportControls?.pause() ?: key(KeyEvent.KEYCODE_MEDIA_PAUSE) }

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
