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
    TAP(4, "tap", "Button tap", "الضغط على الأزرار", SoundGroup.TOUCH),
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
    /** NovaDrive Luxury v4: all eight sounds are built into the app. */
    const val LUXURY = "luxury"
    /** Only the sounds the user imported for this interface; nothing built in. */
    const val CUSTOM = "custom"
    val all = listOf(LUXURY, CUSTOM, NONE)

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

/** What a file really is, judged by its first bytes (the extension is only a hint). */
object AudioSniff {
    /** "mp3", "wav", "ogg" or null when it isn't one of the supported formats. */
    fun detect(head: ByteArray): String? {
        fun at(i: Int, t: String) = head.size >= i + t.length && t.indices.all { head[i + it] == t[it].code.toByte() }
        return when {
            at(0, "RIFF") && at(8, "WAVE") -> "wav"
            at(0, "OggS") -> "ogg"
            at(0, "ID3") -> "mp3"
            head.size >= 2 && head[0] == 0xFF.toByte() && (head[1].toInt() and 0xE0) == 0xE0 -> "mp3"
            else -> null
        }
    }
}

/** Reads a file's real duration with Android's decoder; null when it can't be decoded. */
object AudioProbe {
    fun durationMs(f: File): Long? = runCatching {
        val r = android.media.MediaMetadataRetriever()
        try {
            r.setDataSource(f.absolutePath)
            r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } finally { r.release() }
    }.getOrNull()
}

/** A checked file waiting for the user's decision (preview, then use or cancel). */
data class StagedSound(val file: File, val ext: String, val durationMs: Long, val sourceName: String)

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
class SoundRepository(
    private val ctx: Context,
    /** Real duration of an audio file, or null when it can't be decoded (replaced in tests). */
    private val probe: (File) -> Long? = AudioProbe::durationMs,
) {
    private val prefs = ctx.getSharedPreferences("sounds", Context.MODE_PRIVATE)

    init {
        // Pack v4 replaces every earlier pack: imports from older betas (e.g. the first 07/08 files) are dropped once.
        if (prefs.getInt("pack_version", 0) < PACK_VERSION) {
            File(ctx.filesDir, "sounds").deleteRecursively()
            prefs.edit().putInt("pack_version", PACK_VERSION).apply()
        }
    }

    private val _global = MutableStateFlow(readGlobal())
    val global: StateFlow<GlobalSounds> = _global
    private val _styles = MutableStateFlow(UiStyles.all.associate { it.id to readStyle(it.id) })
    val styles: StateFlow<Map<String, StyleSounds>> = _styles
    /** Bumps whenever imported files change so the player reloads them right away. */
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

    // ---- imported sounds (mp3 / wav / ogg), kept in the app's private storage, per interface

    private fun dir(id: String) = File(ctx.filesDir, "sounds/$id")
    private fun stageDir() = File(ctx.cacheDir, "sound_stage").apply { mkdirs() }

    /** The imported file for [e] in [id], if any. */
    fun fileFor(id: String, e: SoundEvent): File? =
        dir(id).listFiles()?.firstOrNull { it.nameWithoutExtension == e.key && SoundPolicy.isAudioName(it.name) }

    /**
     * Copies [input] into a temporary file and checks it: size limit, real format by header (MP3, WAV or
     * OGG whatever the name says) and that Android can decode it with a sensible length. Null if not usable.
     */
    fun stage(sourceName: String, input: InputStream): StagedSound? {
        val tmp = File.createTempFile("snd", ".tmp", stageDir())
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
        if (!ok) { tmp.delete(); return null }
        val head = ByteArray(16).also { b -> tmp.inputStream().use { it.read(b) } }
        val ext = AudioSniff.detect(head)
        val ms = if (ext == null) null else probe(tmp)
        if (ext == null || ms == null || ms < MIN_MS || ms > MAX_MS) { tmp.delete(); return null }
        val named = File(tmp.parentFile, tmp.nameWithoutExtension + "." + ext)
        if (!tmp.renameTo(named)) { tmp.delete(); return null }
        return StagedSound(named, ext, ms, sourceName)
    }

    /** Makes a staged file the sound of [e] for [id]; it plays instead of the default straight away. */
    fun commit(id: String, e: SoundEvent, staged: StagedSound): Boolean {
        val d = dir(id).apply { mkdirs() }
        val target = File(d, "${e.key}.${staged.ext}")
        val ok = runCatching {
            d.listFiles()?.filter { it.nameWithoutExtension == e.key }?.forEach { it.delete() }
            staged.file.copyTo(target, overwrite = true)
        }.isSuccess
        staged.file.delete()
        if (ok) _files.value++
        return ok
    }

    fun discard(staged: StagedSound) { staged.file.delete() }

    /** Stage + commit in one step. */
    fun import(id: String, e: SoundEvent, name: String, input: InputStream): Boolean =
        stage(name, input)?.let { commit(id, e, it) } ?: false

    /**
     * Imports a whole pack: a ZIP whose audio files start with the event number (01_…, 07_…). The ZIP is
     * checked before anything is used: limited entry count and total size, names are never used as paths,
     * and every file goes through the same format/decoder checks. Returns the events imported.
     */
    fun importZip(id: String, input: InputStream): List<SoundEvent> {
        val done = LinkedHashSet<SoundEvent>()
        runCatching {
            ZipInputStream(input).use { zip ->
                var entries = 0
                var total = 0L
                var entry = zip.nextEntry
                while (entry != null && entries < MAX_ZIP_ENTRIES) {
                    entries++
                    val name = entry.name
                    val ev = if (!entry.isDirectory && SoundPolicy.isAudioName(name) && !name.contains("..")) SoundPolicy.eventForFileName(name) else null
                    if (ev != null) {
                        val counted = CountingLimit(zip, MAX_ZIP_TOTAL - total)
                        if (import(id, ev, name.substringAfterLast('/'), counted)) done += ev
                        total += counted.read
                        if (total >= MAX_ZIP_TOTAL) break
                    }
                    entry = zip.nextEntry
                }
            }
        }
        return done.toList()
    }

    /**
     * Writes the interface's effective sounds as a ZIP (01_welcome.mp3 … 08_chime.mp3 + pack.json), using the
     * user's imports where there are any and [builtIn] for the rest. Returns how many sounds were written.
     */
    fun exportZip(id: String, out: java.io.OutputStream, builtIn: (SoundEvent) -> InputStream?): Int {
        var n = 0
        java.util.zip.ZipOutputStream(out).use { zip ->
            val manifest = StringBuilder("{\"app\":\"NovaDrive\",\"interface\":\"$id\",\"pack\":\"${style(id).pack}\",\"sounds\":[")
            for (e in SoundEvent.entries) {
                val f = fileFor(id, e)
                val stream = f?.inputStream() ?: if (style(id).pack == SoundPacks.LUXURY) builtIn(e) else null
                val ext = f?.extension ?: "mp3"
                stream?.use { src ->
                    val name = "%02d_%s.%s".format(e.number, e.key, ext)
                    zip.putNextEntry(java.util.zip.ZipEntry(name)); src.copyTo(zip); zip.closeEntry()
                    if (n > 0) manifest.append(',')
                    manifest.append("{\"event\":\"${e.key}\",\"file\":\"$name\",\"custom\":${f != null}}")
                    n++
                }
            }
            manifest.append("]}")
            zip.putNextEntry(java.util.zip.ZipEntry("pack.json")); zip.write(manifest.toString().toByteArray()); zip.closeEntry()
        }
        return n
    }

    fun removeImported(id: String, e: SoundEvent) {
        dir(id).listFiles()?.filter { it.nameWithoutExtension == e.key }?.forEach { it.delete() }
        _files.value++
    }

    /** One interface back to its defaults: pack, switches, chime triggers and every imported sound. */
    fun resetStyle(id: String) {
        dir(id).deleteRecursively()
        prefs.edit().remove("$id.pack").remove("$id.off").remove("$id.chime").apply()
        _styles.value = _styles.value + (id to readStyle(id))
        _files.value++
    }

    /** Back to the defaults of every interface (imported files are removed too). */
    fun resetAll() {
        prefs.edit().clear().putInt("pack_version", PACK_VERSION).apply()
        File(ctx.filesDir, "sounds").deleteRecursively()
        _global.value = readGlobal()
        _styles.value = UiStyles.all.associate { it.id to readStyle(it.id) }
        _files.value++
    }

    /** Stops reading after [limit] bytes (a ZIP bomb can't fill the storage). */
    private class CountingLimit(private val s: InputStream, private val limit: Long) : InputStream() {
        var read = 0L
        override fun read(): Int { if (read >= limit) return -1; val b = s.read(); if (b >= 0) read++; return b }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (read >= limit) return -1
            val n = s.read(b, off, minOf(len.toLong(), limit - read).toInt())
            if (n > 0) read += n
            return n
        }
        override fun close() {}
    }

    companion object {
        const val MAX_BYTES = 3L * 1024 * 1024
        const val MAX_ZIP_ENTRIES = 64
        const val MAX_ZIP_TOTAL = 12L * 1024 * 1024
        const val MIN_MS = 30L
        const val MAX_MS = 30_000L
        const val PACK_VERSION = 4
    }
}
