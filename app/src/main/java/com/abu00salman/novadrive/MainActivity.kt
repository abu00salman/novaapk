package com.abu00salman.novadrive

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.abu00salman.novadrive.data.VoiceCommands
import com.abu00salman.novadrive.ui.NovaRoot
import com.abu00salman.novadrive.ui.toast

class MainActivity : ComponentActivity() {

    private val voice = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            ?: return@registerForActivityResult
        val reply = VoiceCommands.handle(nova, text, window)
        toast(this, reply)
        speak(reply)
    }

    private fun speak(text: String) {
        val st = nova.settings.current
        if (st.voiceReply) nova.speaker.speak(text, st.arabic)
    }

    override fun onDestroy() {
        if (isFinishing) nova.speaker.shutdown()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NovaRoot(onVoice = ::startVoice) }
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
    }

    private fun startVoice() {
        val arabic = nova.settings.current.arabic
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (arabic) "ar-SA" else "en-US")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, if (arabic) "أمرني… مثل: افتح اليوتيوب، قلل الإضاءة، كم الساعة" else "Ask me… e.g. \"open YouTube\", \"dim the screen\", \"what time is it\"")
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        try {
            voice.launch(i)
        } catch (e: ActivityNotFoundException) {
            toast(this, if (arabic) "لا توجد خدمة تعرّف صوتي على الجهاز (ثبّت تطبيق Google)" else "No speech recognizer installed (install the Google app)")
        }
    }
}
