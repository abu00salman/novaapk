package com.abu00salman.novadrive.data.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

/** Opens the system's own recognition dialog (Google's window) — used only as a fallback. */
fun interface SystemDialogLauncher {
    /** False when no activity can show it. The result comes back through [SpeechCallback]. */
    fun launch(language: String, callback: SpeechCallback): Boolean
}

/**
 * Speech-to-text inside NovaDrive with Android's [SpeechRecognizer] (Google's recognition service on
 * most units) — no external Google window over the interface. A fresh recognizer is created for every
 * session and destroyed afterwards, which avoids the "recognizer busy" dead-ends of reusing one.
 * When the in-app recognizer can't be used (no service, no microphone permission, or the user prefers
 * the system window), the system dialog is used instead.
 */
class SpeechInput(private val ctx: Context, private val useSystemDialog: () -> Boolean) : SpeechPort {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var token = 0
    /** Set by the activity (it owns the permission prompt and the dialog launcher). */
    var dialog: SystemDialogLauncher? = null

    fun available(): Boolean = runCatching { SpeechRecognizer.isRecognitionAvailable(ctx) }.getOrDefault(false)

    /** Whether the unit can recognise speech without the internet (Android 13+ on-device recognizer). */
    fun onDeviceAvailable(): Boolean = Build.VERSION.SDK_INT >= 33 &&
        runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(ctx) }.getOrDefault(false)

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun start(language: String, preferOffline: Boolean, callback: SpeechCallback) {
        main.post {
            release()
            val my = ++token
            val inApp = !useSystemDialog() && available() && hasMicPermission()
            if (!inApp) {
                val d = dialog
                if (d == null || !d.launch(language, guarded(my, callback))) {
                    callback.onError(if (!hasMicPermission() && available()) SpeechError.PERMISSION else SpeechError.UNAVAILABLE)
                }
                return@post
            }
            val r = runCatching { SpeechRecognizer.createSpeechRecognizer(ctx) }.getOrNull()
            if (r == null) { callback.onError(SpeechError.UNAVAILABLE); return@post }
            recognizer = r
            val cb = guarded(my, callback)
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = cb.onListening()
                override fun onBeginningOfSpeech() {}
                // RMS dB is roughly -2..10; scaled to 0..1 for the level animation.
                override fun onRmsChanged(rmsdB: Float) = cb.onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { release(my); cb.onError(map(error)) }
                override fun onResults(results: Bundle?) {
                    release(my)
                    cb.onResult(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
                }
                override fun onPartialResults(partial: Bundle?) {
                    partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let(cb::onPartial)
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            if (preferOffline) intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            runCatching { r.startListening(intent) }.onFailure { release(my); cb.onError(SpeechError.OTHER) }
        }
    }

    /** Callbacks from an older session (or after cancel) are dropped. */
    private fun guarded(my: Int, cb: SpeechCallback) = object : SpeechCallback {
        override fun onListening() { if (my == token) cb.onListening() }
        override fun onPartial(text: String) { if (my == token) cb.onPartial(text) }
        override fun onLevel(level: Float) { if (my == token) cb.onLevel(level) }
        override fun onResult(text: String) { if (my == token) { token++; cb.onResult(text) } }
        override fun onError(error: SpeechError) { if (my == token) { token++; cb.onError(error) } }
    }

    override fun cancel() {
        main.post { token++; release() }
    }

    private fun release(my: Int) { if (my == token) release() }

    private fun release() {
        val r = recognizer ?: return
        recognizer = null
        runCatching { r.cancel() }
        runCatching { r.destroy() }
    }

    private fun map(code: Int): SpeechError = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> SpeechError.NO_MATCH
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NO_SPEECH
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER -> SpeechError.NETWORK
        SpeechRecognizer.ERROR_AUDIO -> SpeechError.AUDIO
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.PERMISSION
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechError.BUSY
        12, 13 -> SpeechError.LANGUAGE // ERROR_LANGUAGE_NOT_SUPPORTED / ERROR_LANGUAGE_UNAVAILABLE (API 31)
        11 -> SpeechError.NETWORK // ERROR_SERVER_DISCONNECTED
        else -> SpeechError.OTHER
    }
}
