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
import com.abu00salman.novadrive.ui.toast

class MainActivity : ComponentActivity() {

    // Nova AI: speech → text through the device's recognizer, then local commands or the AI.
    private val voice = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (text.isNullOrBlank()) { nova.assistant.heardNothing(); return@registerForActivityResult }
        val s = nova.settings.current
        val driving = s.drivingMode && (nova.fix.value?.speedKmh ?: 0f) > 8f
        nova.assistant.ask(text, window, driving)
    }

    // Asked only when the driver says "call <name>".
    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onDestroy() {
        if (isFinishing) nova.speaker.shutdown()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    }

    private fun startVoice() {
        val arabic = nova.settings.current.arabic
        nova.assistant.listening()
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (arabic) "ar-SA" else "en-US")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, if (arabic) "Nova AI… مثل: افتح يوتيوب، ودّني لأقرب محطة بنزين، كيف الجو" else "Nova AI… e.g. \"open YouTube\", \"nearest fuel station\", \"weather\"")
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        try {
            voice.launch(i)
        } catch (e: ActivityNotFoundException) {
            nova.assistant.heardNothing()
            toast(this, if (arabic) "لا توجد خدمة تعرّف صوتي على الجهاز (ثبّت تطبيق Google)" else "No speech recognizer installed (install the Google app)")
        }
    }
}
