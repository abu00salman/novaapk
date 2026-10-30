package com.abu00salman.novadrive.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.R
import com.abu00salman.novadrive.core.PlayContext
import com.abu00salman.novadrive.core.SoundEvent
import com.abu00salman.novadrive.core.SoundPacks
import com.abu00salman.novadrive.core.SoundPolicy
import com.abu00salman.novadrive.data.voice.VoiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface sound effects. Short cues are preloaded into a [SoundPool] (no delay, no audio focus, so
 * music keeps playing); cues longer than a couple of seconds (welcome, notification, chime) stream through
 * a [MediaPlayer]. Nothing plays during calls, spoken replies/guidance or while Nova listens, welcome
 * cues stay quiet while driving, and repeated events are rate limited. Only the sounds the user has
 * (the built-in pack or their own imports) are ever played — never Android's default sounds.
 */
class SoundEngine(private val c: AppContainer) {
    private val ctx: Context = c.appContext
    private val main = Handler(Looper.getMainLooper())
    private val audio = ctx.getSystemService(AudioManager::class.java)

    private data class Src(val key: String, val res: Int = 0, val file: File? = null)

    private val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private val pool: SoundPool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build()
    private val loaded = ConcurrentHashMap<String, Int>()      // key → soundId
    private val ready = ConcurrentHashMap.newKeySet<Int>()
    private val durations = ConcurrentHashMap<String, Long>()  // key → ms
    private var player: MediaPlayer? = null

    private val lastPlay = HashMap<SoundEvent, Long>()
    private var pendingTap: Runnable? = null
    /** Startup cues already played this session, per pack. */
    private val sessionDone = HashSet<String>()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready += id }
        // A new drive (screen off → on) gets its welcome again.
        ContextCompat.registerReceiver(ctx, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { sessionDone.clear() }
        }, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_EXPORTED)
    }

    /** Preloads every sound off the main thread, and again whenever imports change. */
    fun warmUp() {
        c.scope.launch {
            delay(900) // never compete with the first frame
            withContext(Dispatchers.Default) { preload() }
            c.sounds.filesVersion.drop(1).collect { withContext(Dispatchers.Default) { preload() } }
        }
    }

    private fun preload() {
        for (id in com.abu00salman.novadrive.core.UiStyles.all.map { it.id }) for (e in SoundEvent.entries) sourceFor(id, e)?.let(::ensure)
    }

    private fun builtIn(e: SoundEvent): Int = when (e) {
        SoundEvent.WELCOME -> R.raw.nova_snd_welcome
        SoundEvent.READY -> R.raw.nova_snd_ready
        SoundEvent.NAV -> R.raw.nova_snd_nav
        SoundEvent.TAP -> R.raw.nova_snd_tap
        SoundEvent.CONFIRM -> R.raw.nova_snd_confirm
        SoundEvent.BACK -> R.raw.nova_snd_back
        // 07 / 08 are not bundled (redistribution rights not verified): the user imports them.
        SoundEvent.NOTIFY, SoundEvent.CHIME -> 0
    }

    /** The user's own file first, then the interface's built-in pack; null = silent. */
    private fun sourceFor(styleId: String, e: SoundEvent): Src? {
        c.sounds.fileFor(styleId, e)?.let { return Src("f:${it.absolutePath}:${it.lastModified()}", file = it) }
        if (c.sounds.style(styleId).pack == SoundPacks.LUXURY) builtIn(e).takeIf { it != 0 }?.let { return Src("r:$it", res = it) }
        return null
    }

    private fun ensure(src: Src) {
        if (loaded.containsKey(src.key) || durations.containsKey(src.key)) return
        val ms = runCatching {
            val r = MediaMetadataRetriever()
            try {
                if (src.file != null) r.setDataSource(src.file.absolutePath)
                else ctx.resources.openRawResourceFd(src.res).use { r.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally { r.release() }
        }.getOrDefault(0L)
        durations[src.key] = ms
        if (ms in 1..SHORT_MS) {
            val id = if (src.file != null) pool.load(src.file.absolutePath, 1) else pool.load(ctx, src.res, 1)
            loaded[src.key] = id
        }
    }

    // ---- when to play

    private fun context() = PlayContext(
        driving = c.settings.current.drivingMode && (c.fix.value?.speedKmh ?: 0f) > 8f,
        inCall = audio?.mode.let { it == AudioManager.MODE_IN_CALL || it == AudioManager.MODE_IN_COMMUNICATION || it == AudioManager.MODE_RINGTONE },
        speaking = c.speaker.isSpeaking,
        listening = c.assistant.ui.value.state == VoiceState.LISTENING,
    )

    /** An event of the current interface. */
    fun play(e: SoundEvent) = playFor(c.settings.current.uiStyle, e)

    fun playFor(styleId: String, e: SoundEvent) {
        main.post {
            val g = c.sounds.global.value
            val s = c.sounds.style(styleId)
            if (!SoundPolicy.allowed(e, g, s, context())) return@post
            val now = System.currentTimeMillis()
            if (now - (lastPlay[e] ?: 0L) < SoundPolicy.minGapMs(e)) return@post
            val src = sourceFor(styleId, e) ?: return@post
            if (e == SoundEvent.TAP) {
                // A tap that leads to a page change/back lets that cue speak alone.
                pendingTap?.let(main::removeCallbacks)
                pendingTap = Runnable { pendingTap = null; lastPlay[e] = System.currentTimeMillis(); start(src, g.volume) }
                main.postDelayed(pendingTap!!, 90)
            } else {
                if (e == SoundEvent.NAV || e == SoundEvent.BACK || e == SoundEvent.CONFIRM) pendingTap?.let { main.removeCallbacks(it); pendingTap = null }
                lastPlay[e] = now
                start(src, g.volume)
            }
        }
    }

    /** Opening an interface: its startup cue, once per session (Welcome, or Start/Ready if Welcome is off). */
    fun interfaceShown(styleId: String) {
        val s = c.sounds.style(styleId)
        if (s.pack == SoundPacks.NONE) return
        val cue = SoundPolicy.startupCue(s, styleId + s.pack in sessionDone) ?: return
        // Don't burn the session's cue if it is blocked right now (e.g. driving); it plays next time.
        if (!SoundPolicy.allowed(cue, c.sounds.global.value, s, context())) return
        sessionDone += styleId + s.pack
        playFor(styleId, cue)
    }

    /** The special chime, only for triggers the user switched on for the current interface. */
    fun chime(trigger: String) {
        val id = c.settings.current.uiStyle
        if (trigger in c.sounds.style(id).chime) playFor(id, SoundEvent.CHIME)
    }

    /** Settings preview: plays the sound even when effects are off, at the chosen volume (≥ 30 %). */
    fun preview(styleId: String, e: SoundEvent): Boolean {
        val src = sourceFor(styleId, e) ?: return false
        main.post { start(src, maxOf(c.sounds.global.value.volume, 0.3f)) }
        return true
    }

    fun hasSound(styleId: String, e: SoundEvent) = sourceFor(styleId, e) != null

    private fun start(src: Src, volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        val id = loaded[src.key]
        if (id != null) {
            if (id in ready) pool.play(id, v, v, 1, 0, 1f)
            return
        }
        if ((durations[src.key] ?: 0L) <= SHORT_MS) {
            // Not preloaded yet: load for next time instead of delaying the UI.
            c.scope.launch(Dispatchers.Default) { ensure(src) }
            return
        }
        runCatching {
            player?.release()
            val mp = MediaPlayer().apply {
                setAudioAttributes(attrs)
                if (src.file != null) setDataSource(src.file.absolutePath)
                else ctx.resources.openRawResourceFd(src.res).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                setVolume(v, v)
                setOnCompletionListener { it.release(); if (player === it) player = null }
                setOnErrorListener { mp, _, _ -> mp.release(); if (player === mp) player = null; true }
                prepare()
            }
            player = mp
            mp.start()
        }
    }

    /** Stops a long cue (e.g. when Nova starts listening). */
    fun stop() = main.post { runCatching { player?.stop() }; player?.release(); player = null }

    fun release() { pool.release(); player?.release() }

    companion object { private const val SHORT_MS = 2_500L }
}
