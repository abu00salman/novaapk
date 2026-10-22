package com.abu00salman.novadrive.data

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Spoken replies for «أمرني» through the device's own text-to-speech engine. Arabic is used when
 * the engine has it; otherwise the reply is still spoken with the engine's default voice.
 */
class Speaker(private val ctx: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, Boolean>? = null

    /** False until the engine has started; then whether it can speak Arabic. */
    var arabicAvailable: Boolean = false
        private set

    fun speak(text: String, arabic: Boolean) {
        val engine = tts
        when {
            engine == null -> {
                pending = text to arabic
                tts = TextToSpeech(ctx.applicationContext) { status ->
                    ready = status == TextToSpeech.SUCCESS
                    if (ready) {
                        tts?.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        pending?.let { (t, a) -> say(t, a) }
                    }
                    pending = null
                }
            }
            ready -> say(text, arabic)
            else -> pending = text to arabic
        }
    }

    private fun say(text: String, arabic: Boolean) {
        val engine = tts ?: return
        val ar = Locale.forLanguageTag("ar")
        arabicAvailable = engine.isLanguageAvailable(ar) >= TextToSpeech.LANG_AVAILABLE
        engine.language = if (arabic && arabicAvailable) ar else Locale.US
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nova-reply")
    }

    /** Opens the system screen where the user installs voice data (e.g. Arabic). */
    fun openSettings(): Boolean = runCatching {
        ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    }.getOrElse {
        runCatching {
            ctx.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        }.getOrDefault(false)
    }

    fun shutdown() { tts?.shutdown(); tts = null; ready = false }
}
