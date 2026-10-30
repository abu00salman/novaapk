package com.abu00salman.novadrive.data

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.Immutable
import com.abu00salman.novadrive.data.voice.SpeakOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** Which language Nova speaks in: detected from the reply text, or always Arabic/English. */
enum class VoiceLang { AUTO, ARABIC, ENGLISH }

/** Where spoken replies are played. Media is what car units route most reliably to the speakers. */
enum class VoiceChannel { MEDIA, NAVIGATION }

@Immutable
data class VoicePrefs(
    val lang: VoiceLang = VoiceLang.AUTO,
    /** Name of the chosen engine voice, or empty for the best installed one. */
    val voiceName: String = "",
    val rate: Float = 1f,
    val volume: Float = 1f,
    val channel: VoiceChannel = VoiceChannel.MEDIA,
)

@Immutable
data class TtsStatus(
    val ready: Boolean = false,
    val failed: Boolean = false,
    val engine: String? = null,
    val arabic: Boolean = false,
    val english: Boolean = false,
    /** Installed voices: name to "language-country (quality, offline/online)". */
    val voices: List<Pair<String, String>> = emptyList(),
    val error: String? = null,
)

/**
 * Text-to-speech for Nova AI replies and navigation prompts, on the unit's own Android engine (no
 * paid cloud voice). Each utterance gets its own completion callback, called exactly once — when it
 * finishes, is stopped, fails, or a watchdog decides the engine went silent — so callers never wait
 * forever. Arabic uses the best installed Arabic voice (Saudi first); audio focus ducks the music.
 */
class Speaker(private val ctx: Context, private val prefs: () -> VoicePrefs = { VoicePrefs() }) {
    private val main = Handler(Looper.getMainLooper())
    private val audio = ctx.getSystemService(AudioManager::class.java)
    private var tts: TextToSpeech? = null
    private var ready = false
    private val queued = ArrayList<() -> Unit>()
    private val callbacks = HashMap<String, (SpeakOutcome, String?) -> Unit>()
    private val watchdogs = HashMap<String, Runnable>()
    private var focus: AudioFocusRequest? = null
    private var counter = 0

    private val _status = MutableStateFlow(TtsStatus())
    val status: StateFlow<TtsStatus> = _status

    /** True while something (a reply or a navigation prompt) is being spoken; main thread. */
    val isSpeaking: Boolean get() = callbacks.isNotEmpty()

    /** False until the engine has started; then whether it has an Arabic voice. */
    val arabicAvailable: Boolean get() = _status.value.arabic

    private fun ensureEngine() {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext) { code -> main.post { onInit(code) } }
    }

    private fun onInit(code: Int) {
        val engine = tts ?: return
        if (code != TextToSpeech.SUCCESS) {
            _status.value = TtsStatus(failed = true, error = "init $code")
            runCatching { engine.shutdown() }; tts = null; ready = false
            val pending = queued.toList(); queued.clear(); pending.forEach { it() }
            return
        }
        ready = true
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { main.post { complete(id, SpeakOutcome.DONE, null) } }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { main.post { complete(id, SpeakOutcome.FAILED, null) } }
            override fun onError(id: String?, errorCode: Int) { main.post { complete(id, SpeakOutcome.FAILED, ttsError(errorCode)) } }
            override fun onStop(id: String?, interrupted: Boolean) { main.post { complete(id, SpeakOutcome.STOPPED, null) } }
        })
        refreshStatus()
        val pending = queued.toList(); queued.clear(); pending.forEach { it() }
    }

    private fun refreshStatus() {
        val engine = tts ?: return
        val voices = runCatching { engine.voices?.toList() }.getOrNull().orEmpty()
            .filter { it.locale.language == "ar" || it.locale.language == "en" }
            .sortedWith(compareBy<Voice>({ it.locale.language != "ar" }, { it.locale.country != "SA" }, { -it.quality }))
        fun available(tag: String) = runCatching { engine.isLanguageAvailable(Locale.forLanguageTag(tag)) >= TextToSpeech.LANG_AVAILABLE }.getOrDefault(false)
        _status.value = TtsStatus(
            ready = true,
            engine = runCatching { engine.defaultEngine }.getOrNull(),
            arabic = voices.any { it.locale.language == "ar" && !it.isNotInstalled() } || available("ar-SA") || available("ar"),
            english = available("en-US"),
            voices = voices.map { v ->
                v.name to "${v.locale.toLanguageTag()} · ${qualityLabel(v.quality)}${if (v.isNetworkConnectionRequired) " · online" else ""}"
            },
        )
    }

    private fun Voice.isNotInstalled() = features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true

    private fun qualityLabel(q: Int) = when {
        q >= Voice.QUALITY_VERY_HIGH -> "very high"
        q >= Voice.QUALITY_HIGH -> "high"
        q >= Voice.QUALITY_NORMAL -> "normal"
        else -> "low"
    }

    /** Starts the engine early so the first reply isn't delayed (and Settings can show the voices). */
    fun warmUp() = main.post { ensureEngine() }

    /** Speaks [text]; [done] runs exactly once with the outcome. */
    fun speak(text: String, arabic: Boolean, done: ((SpeakOutcome, String?) -> Unit)? = null) = speak(text, arabic, null, done)

    private fun speak(text: String, arabicUi: Boolean, channelOverride: VoiceChannel?, done: ((SpeakOutcome, String?) -> Unit)?) {
        main.post {
            ensureEngine()
            val run = { say(text, arabicUi, channelOverride, done ?: { _, _ -> }) }
            if (ready) run() else if (tts == null) done?.invoke(SpeakOutcome.FAILED, _status.value.error ?: "engine") else queued.add(run)
        }
    }

    private fun say(text: String, arabicUi: Boolean, channelOverride: VoiceChannel?, done: (SpeakOutcome, String?) -> Unit) {
        val engine = tts
        if (engine == null || !ready) { done(SpeakOutcome.FAILED, unavailableReason(arabicUi)); return }
        val p = prefs()
        val wantArabic = when (p.lang) {
            VoiceLang.ARABIC -> true
            VoiceLang.ENGLISH -> false
            VoiceLang.AUTO -> if (text.any { it in '؀'..'ۿ' }) true else if (text.any { it in 'A'..'z' }) false else arabicUi
        }
        if (!applyVoice(engine, wantArabic, p.voiceName)) {
            done(SpeakOutcome.FAILED, if (arabicUi) "لا يوجد صوت عربي مثبت في محرك النطق — ثبّته من إعدادات النطق" else "No ${if (wantArabic) "Arabic" else "English"} voice installed in the text-to-speech engine")
            return
        }
        engine.setSpeechRate(p.rate.coerceIn(0.5f, 2f))
        val channel = channelOverride ?: p.channel
        val attrs = AudioAttributes.Builder()
            .setUsage(if (channel == VoiceChannel.NAVIGATION) AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE else AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        engine.setAudioAttributes(attrs)
        val id = "nova-" + (++counter)
        callbacks[id] = done
        requestFocus(attrs)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, p.volume.coerceIn(0.1f, 1f)) }
        val res = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
        if (res != TextToSpeech.SUCCESS) { complete(id, SpeakOutcome.FAILED, "speak() $res"); return }
        // Engines occasionally never report back; never let a caller wait forever.
        val dog = Runnable { if (callbacks.containsKey(id)) { runCatching { engine.stop() }; complete(id, SpeakOutcome.FAILED, "timeout") } }
        watchdogs[id] = dog
        main.postDelayed(dog, (10_000 + text.length * 160 / p.rate.coerceIn(0.5f, 2f)).toLong())
    }

    /** Picks the chosen voice, else the best installed voice for the language. False if none fits. */
    private fun applyVoice(engine: TextToSpeech, arabic: Boolean, chosen: String): Boolean {
        val lang = if (arabic) "ar" else "en"
        val all = runCatching { engine.voices?.toList() }.getOrNull().orEmpty()
        all.firstOrNull { it.name == chosen && it.locale.language == lang && !it.isNotInstalled() }?.let { return engine.setVoice(it) == TextToSpeech.SUCCESS }
        val best = all.filter { it.locale.language == lang && !it.isNotInstalled() }
            .sortedWith(compareBy<Voice>(
                { if (arabic) it.locale.country != "SA" else it.locale.country != "US" },
                { it.isNetworkConnectionRequired },
                { -it.quality },
                { it.latency },
            )).firstOrNull()
        if (best != null && engine.setVoice(best) == TextToSpeech.SUCCESS) return true
        // Engines without a voice list: fall back to the locale.
        val tags = if (arabic) listOf("ar-SA", "ar") else listOf("en-US", "en")
        return tags.any { runCatching { engine.setLanguage(Locale.forLanguageTag(it)) >= TextToSpeech.LANG_AVAILABLE }.getOrDefault(false) }
    }

    private fun complete(id: String?, outcome: SpeakOutcome, reason: String?) {
        val cb = callbacks.remove(id ?: return) ?: return
        watchdogs.remove(id)?.let { main.removeCallbacks(it) }
        if (callbacks.isEmpty()) abandonFocus()
        cb(outcome, reason)
    }

    private fun requestFocus(attrs: AudioAttributes) {
        if (focus != null || audio == null) return
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attrs).build()
        if (runCatching { audio.requestAudioFocus(req) }.getOrNull() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) focus = req
    }

    private fun abandonFocus() {
        val f = focus ?: return
        focus = null
        runCatching { audio?.abandonAudioFocusRequest(f) }
    }

    private fun unavailableReason(arabic: Boolean) =
        if (arabic) "محرك النطق غير متاح على الجهاز — ثبّت Google Speech Services أو فعّل محركًا من إعدادات النطق"
        else "No text-to-speech engine available — install Google Speech Services or enable an engine in voice settings"

    private fun ttsError(code: Int): String = when (code) {
        TextToSpeech.ERROR_NETWORK, TextToSpeech.ERROR_NETWORK_TIMEOUT -> "network voice needs internet"
        TextToSpeech.ERROR_NOT_INSTALLED_YET -> "voice data not installed yet"
        TextToSpeech.ERROR_OUTPUT -> "audio output"
        TextToSpeech.ERROR_SYNTHESIS -> "synthesis"
        TextToSpeech.ERROR_SERVICE -> "engine service"
        else -> "error $code"
    }

    /** Interrupts whatever is being spoken; its callback reports STOPPED. */
    fun stop() = main.post {
        runCatching { tts?.stop() }
        callbacks.keys.toList().forEach { complete(it, SpeakOutcome.STOPPED, null) }
    }

    /** Settings › Voice test: speaks the greeting and reports a real result. */
    fun test(arabic: Boolean, done: (SpeakOutcome, String?) -> Unit) {
        val text = if (arabic) "مرحبًا، أنا نوفا، مساعدك الذكي. كيف أقدر أساعدك اليوم؟" else "Hello, I'm Nova, your smart assistant. How can I help you today?"
        speak(text, arabic, null) { o, r -> main.post { refreshStatus(); done(o, r) } }
    }

    /** Opens the system screen where the user installs voice data (e.g. Arabic). */
    fun openSettings(): Boolean = runCatching {
        ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    }.getOrElse {
        runCatching {
            ctx.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        }.getOrDefault(false)
    }

    fun shutdown() = main.post {
        callbacks.keys.toList().forEach { complete(it, SpeakOutcome.STOPPED, null) }
        runCatching { tts?.shutdown() }; tts = null; ready = false
        abandonFocus()
    }
}
