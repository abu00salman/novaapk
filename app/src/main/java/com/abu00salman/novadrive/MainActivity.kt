package com.abu00salman.novadrive

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.abu00salman.novadrive.ui.NovaRoot
import com.abu00salman.novadrive.data.ai.GoogleVoice
import com.abu00salman.novadrive.data.ai.VoiceButton
import com.abu00salman.novadrive.data.voice.SpeechCallback
import com.abu00salman.novadrive.data.voice.SpeechError
import com.abu00salman.novadrive.data.voice.SystemDialogLauncher

class MainActivity : ComponentActivity() {

    // Fallback only: the system's recognition window (when the in-app recognizer can't be used).
    private var dialogCallback: SpeechCallback? = null
    private val voiceDialog = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val cb = dialogCallback ?: return@registerForActivityResult
        dialogCallback = null
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        when {
            !text.isNullOrBlank() -> cb.onResult(text)
            result.resultCode == RESULT_CANCELED -> cb.onError(SpeechError.DISMISSED)
            else -> cb.onError(SpeechError.NO_MATCH)
        }
    }

    // Asked the first time the microphone is used; if refused, the system window is used instead.
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        nova.assistant.listen()
    }

    // Asked only when the driver says "call <name>".
    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onDestroy() {
        nova.assistant.speech.dialog = null
        if (isFinishing) nova.speaker.shutdown()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nova.assistant.attach(window)
        nova.assistant.speech.dialog = SystemDialogLauncher { language, cb -> launchDialog(language, cb) }
        nova.speaker.warmUp()
        nova.soundFx.warmUp()
        setContent { NovaRoot(onVoice = ::startVoice) }
        lifecycleScope.launch {
            nova.voiceActions.collect { if (it == "perm:contacts") contactsPermission.launch(Manifest.permission.READ_CONTACTS) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            nova.homePressed.tryEmit(Unit)
        }
    }

    override fun onResume() {
        super.onResume()
        nova.refreshPermissions()
        nova.assistant.refreshOnline()
    }

    override fun onStop() {
        // Never leave the microphone open behind another app (the system voice window is ours, though).
        if (dialogCallback == null) nova.assistant.onBackground()
        super.onStop()
    }

    private fun startVoice() {
        val cfg = nova.ai.config.value
        if (!cfg.novaEnabled) {
            // «أمرني»: Google Assistant / Gemini open as they are; otherwise Google speech → local commands.
            val opened = when (cfg.voiceButton) {
                VoiceButton.ASSISTANT -> GoogleVoice.assistant(this)
                VoiceButton.GEMINI -> GoogleVoice.gemini(this) || GoogleVoice.assistant(this)
                VoiceButton.COMMANDS -> false
            }
            if (!opened) nova.assistant.listen()
            return
        }
        val speech = nova.assistant.speech
        val prefs = getSharedPreferences("nova_ai", MODE_PRIVATE)
        if (cfg.inAppSpeech && speech.available() && !speech.hasMicPermission() && !prefs.getBoolean("mic_asked", false)) {
            prefs.edit().putBoolean("mic_asked", true).apply()
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        nova.assistant.listen()
    }

    private fun launchDialog(language: String, cb: SpeechCallback): Boolean {
        val arabic = language.startsWith("ar")
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, if (arabic) "أمرني… مثل: افتح يوتيوب، ودّني لأقرب محطة بنزين، كيف الجو" else "Ask me… e.g. \"open YouTube\", \"nearest fuel station\", \"weather\"")
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        return try {
            dialogCallback = cb
            voiceDialog.launch(i)
            true
        } catch (e: ActivityNotFoundException) {
            dialogCallback = null
            false
        }
    }
}
