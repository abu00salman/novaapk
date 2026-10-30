package com.abu00salman.novadrive.core

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/** The moments an interface can make a sound. Numbers follow the sound pack (01…08). */
enum class SoundEvent(val number: Int, val key: String, val en: String, val ar: String, val group: SoundGroup) {
    WELCOME(1, "welcome", "Welcome", "الترحيب", SoundGroup.WELCOME),
    READY(2, "ready", "Start / Ready", "بدء التشغيل", SoundGroup.WELCOME),
    NAV(3, "nav", "Navigation switch", "التنقل بين الصفحات", SoundGroup.NAV),
    TAP(4, "tap", "Button tap", "لمس الأزرار", SoundGroup.TOUCH),
    CONFIRM(5, "confirm", "Confirm", "التأكيد", SoundGroup.OTHER),
    BACK(6, "back", "Back", "الرجوع", SoundGroup.NAV),
    NOTIFY(7, "notify", "Notification", "الإشعارات", SoundGroup.OTHER),
    CHIME(8, "chime", "Special chime", "النغمة المميزة", SoundGroup.OTHER);

    companion object {
        fun byKey(k: String) = entries.firstOrNull { it.key == k }
        fun byNumber(n: Int) = entries.firstOrNull { it.number == n }
    }
}

enum class SoundGroup { WELCOME, TOUCH, NAV, OTHER }

/** Special-chime triggers the user can switch on (the chime never plays on ordinary taps). */
object ChimeTriggers {
    const val ARRIVAL = "arrival"
    const val BLUETOOTH = "bluetooth"
    const val IPHONE = "iphone"
    val all = listOf(ARRIVAL, BLUETOOTH, IPHONE)
}

object SoundPacks {
    const val NONE = "none"
    /** NovaDrive Luxury: the six original sounds are built in; 07/08 only exist if the user imports them. */
    const val LUXURY = "luxury"
    val all = listOf(LUXURY, NONE)

    /** The pack an interface starts with: BMW gets the luxury pack, everything else stays silent. */
    fun defaultFor(styleId: String) = if (styleId == "bmw") LUXURY else NONE
}

@Immutable
data class GlobalSounds(val enabled: Boolean = true, /** Quiet by default. */ val volume: Float = 0.45f)

/** One interface's sound setup — independent of every other interface. */
@Immutable
data class StyleSounds(
    val pack: String,
    /** Events switched off for this interface. */
    val off: Set<SoundEvent> = emptySet(),
    /** Chime triggers switched on (empty = the chime never plays by itself). */
    val chime: Set<String> = emptySet(),
) {
    fun on(e: SoundEvent) = e !in off
}

/** What is going on around the sound; the effects stay out of the way of calls, voice and guidance. */
data class PlayContext(val driving: Boolean, val inCall: Boolean, val speaking: Boolean, val listening: Boolean)

object SoundPolicy {
    /** Cues that would startle or distract a moving driver. */
    private val QUIET_WHILE_DRIVING = setOf(SoundEvent.WELCOME, SoundEvent.READY)

    /** Whether [e] may play now, ignoring whether a sound is actually assigned to it. */
    fun allowed(e: SoundEvent, g: GlobalSounds, s: StyleSounds, ctx: PlayContext): Boolean = when {
        !g.enabled || g.volume <= 0.01f -> false
        s.pack == SoundPacks.NONE && e != SoundEvent.CHIME -> false
        !s.on(e) -> false
        ctx.inCall || ctx.speaking || ctx.listening -> false
        ctx.driving && e in QUIET_WHILE_DRIVING -> false
        else -> true
    }

    /** Minimum gap between two plays of the same event, so rapid taps don't machine-gun. */
    fun minGapMs(e: SoundEvent): Long = when (e) {
        SoundEvent.TAP -> 140
        SoundEvent.NAV, SoundEvent.BACK -> 260
        SoundEvent.CONFIRM -> 600
        SoundEvent.NOTIFY -> 4_000
        SoundEvent.CHIME -> 8_000
        SoundEvent.WELCOME, SoundEvent.READY -> 0
    }

    /**
     * Startup cue for the session: Welcome, or Start/Ready when Welcome is off — never both at once.
     * Returns null when the cue was already played this session.
     */
    fun startupCue(s: StyleSounds, alreadyPlayed: Boolean): SoundEvent? = when {
        alreadyPlayed -> null
        s.on(SoundEvent.WELCOME) -> SoundEvent.WELCOME
        s.on(SoundEvent.READY) -> SoundEvent.READY
        else -> null
    }

    /** Maps names like "07_BMW_Notification_User.mp3" or "7-notify.wav" to an event by their leading number. */
    fun eventForFileName(name: String): SoundEvent? {
        val digits = name.substringAfterLast('/').takeWhile { it.isDigit() }
        return digits.toIntOrNull()?.let(SoundEvent::byNumber)
    }

    val audioExtensions = setOf("mp3", "wav", "ogg")
    fun isAudioName(name: String) = name.substringAfterLast('.', "").lowercase() in audioExtensions
}

/** Sound settings saved on the unit: global switches plus a separate setup per interface. */
class SoundRepository(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("sounds", Context.MODE_PRIVATE)
    private val _global = MutableStateFlow(readGlobal())
    val global: StateFlow<GlobalSounds> = _global
    private val _styles = MutableStateFlow(UiStyles.all.associate { it.id to readStyle(it.id) })
    val styles: StateFlow<Map<String, StyleSounds>> = _styles
    /** Bumps whenever imported files change so the player reloads them. */
    private val _files = MutableStateFlow(0)
    val filesVersion: StateFlow<Int> = _files

    private fun readGlobal() = GlobalSounds(prefs.getBoolean("enabled", true), prefs.getFloat("volume", 0.45f).coerceIn(0f, 1f))

    private fun readStyle(id: String) = StyleSounds(
        pack = prefs.getString("$id.pack", null)?.takeIf { it in SoundPacks.all } ?: SoundPacks.defaultFor(id),
        off = (prefs.getString("$id.off", "") ?: "").split(',').mapNotNull(SoundEvent::byKey).toSet(),
        chime = (prefs.getString("$id.chime", "") ?: "").split(',').filter { it in ChimeTriggers.all }.toSet(),
    )

    fun style(id: String): StyleSounds = _styles.value[id] ?: StyleSounds(SoundPacks.defaultFor(id))

    fun setGlobal(block: (GlobalSounds) -> GlobalSounds) {
        val n = block(_global.value)
        prefs.edit().putBoolean("enabled", n.enabled).putFloat("volume", n.volume.coerceIn(0f, 1f)).apply()
        _global.value = n
    }

    fun update(id: String, block: (StyleSounds) -> StyleSounds) {
        val n = block(style(id))
        prefs.edit().putString("$id.pack", n.pack).putString("$id.off", n.off.joinToString(",") { it.key })
            .putString("$id.chime", n.chime.joinToString(",")).apply()
        _styles.value = _styles.value + (id to n)
    }

    // ---- imported sounds (mp3 / wav / ogg), kept per interface

    private fun dir(id: String) = File(ctx.filesDir, "sounds/$id")

    /** The imported file for [e] in [id], if any. */
    fun fileFor(id: String, e: SoundEvent): File? =
        dir(id).listFiles()?.firstOrNull { it.nameWithoutExtension == e.key && SoundPolicy.isAudioName(it.name) }

    /** Saves [input] as [e]'s sound for [id]; false when it isn't audio, is too big or can't be read. */
    fun import(id: String, e: SoundEvent, ext: String, input: InputStream): Boolean {
        val extension = ext.lowercase().takeIf { it in SoundPolicy.audioExtensions } ?: return false
        val d = dir(id).apply { mkdirs() }
        val tmp = File(d, "${e.key}.tmp")
        val ok = runCatching {
            tmp.outputStream().use { out ->
                var total = 0L
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BYTES) error("too big")
                    out.write(buf, 0, n)
                }
            }
            tmp.length() > 512
        }.getOrDefault(false)
        if (!ok) { tmp.delete(); return false }
        d.listFiles()?.filter { it.nameWithoutExtension == e.key && it != tmp }?.forEach { it.delete() }
        tmp.renameTo(File(d, "${e.key}.$extension"))
        _files.value++
        return true
    }

    /**
     * Imports a whole pack: a ZIP whose files start with the event number (01_…, 07_…). Returns the
     * events that were imported.
     */
    fun importZip(id: String, input: InputStream): List<SoundEvent> {
        val done = ArrayList<SoundEvent>()
        runCatching {
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val ev = if (!entry.isDirectory && SoundPolicy.isAudioName(name)) SoundPolicy.eventForFileName(name) else null
                    if (ev != null && import(id, ev, name.substringAfterLast('.'), NoClose(zip))) done += ev
                    entry = zip.nextEntry
                }
            }
        }
        return done
    }

    fun removeImported(id: String, e: SoundEvent) {
        dir(id).listFiles()?.filter { it.nameWithoutExtension == e.key }?.forEach { it.delete() }
        _files.value++
    }

    /** Back to the defaults of every interface (imported files are removed too). */
    fun resetAll() {
        prefs.edit().clear().apply()
        File(ctx.filesDir, "sounds").deleteRecursively()
        _global.value = readGlobal()
        _styles.value = UiStyles.all.associate { it.id to readStyle(it.id) }
        _files.value++
    }

    private class NoClose(private val s: InputStream) : InputStream() {
        override fun read() = s.read()
        override fun read(b: ByteArray, off: Int, len: Int) = s.read(b, off, len)
        override fun close() {}
    }

    companion object { const val MAX_BYTES = 3L * 1024 * 1024 }
}
