package com.abu00salman.novadrive.data.ai

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.Window
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.data.VoiceCommands
import com.abu00salman.novadrive.data.voice.Route
import com.abu00salman.novadrive.data.voice.RouteResult
import com.abu00salman.novadrive.data.voice.SessionConfig
import com.abu00salman.novadrive.data.voice.SpeakerPort
import com.abu00salman.novadrive.data.voice.SpeechInput
import com.abu00salman.novadrive.data.voice.VoiceRouter
import com.abu00salman.novadrive.data.voice.VoiceSession
import com.abu00salman.novadrive.data.voice.VoiceUi
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

/**
 * Nova AI on the unit: wires the voice session to Android — speech recognition in the app
 * ([SpeechInput]), the local command engine ([VoiceCommands], works offline and without any API
 * credit), the ONE AI provider selected in Settings (ChatGPT or Claude; never both, never switched
 * automatically) and the unit's text-to-speech.
 */
class NovaAssistant(private val c: AppContainer) {
    val speech = SpeechInput(c.appContext) { !c.ai.config.value.inAppSpeech }
    private var windowRef = WeakReference<Window>(null)

    private val router = object : VoiceRouter {
        override fun providerName(): String? = engineName()

        override suspend fun route(text: String, online: Boolean, history: List<Pair<String, String>>, onTool: (String) -> Unit): RouteResult {
            val ar = c.settings.current.arabic
            val window = windowRef.get()
            val driving = driving()
            val ai = if (online) engine() else null
            // Direct car/app commands run locally and instantly — no API call.
            VoiceCommands.handle(c, text, window, driving, aiReady = ai != null)?.let { return RouteResult(Route.LOCAL, it) }
            if (ai == null) return RouteResult(Route.NONE, noAiMessage(text, online, ar))
            val reply = ai.reply(
                NovaTools.systemPrompt(c), history.map { Turn(it.first, it.second) }, text, NovaTools.specs,
                run = { name, args -> NovaTools.execute(c, name, args, driving, window) },
                notice = onTool,
            )
            return RouteResult(Route.AI, reply.ifBlank { if (ar) "تم." else "Done." })
        }
    }

    private val speakerPort = object : SpeakerPort {
        override fun speak(text: String, done: (com.abu00salman.novadrive.data.voice.SpeakOutcome, String?) -> Unit) =
            c.speaker.speak(text, c.settings.current.arabic, done)
        override fun stop() { c.speaker.stop() }
    }

    private val session = VoiceSession(c.scope, speech, speakerPort, router, ::online) {
        val cfg = c.ai.config.value
        val s = c.settings.current
        SessionConfig(
            arabic = s.arabic, speakReplies = s.voiceReply, keepContext = cfg.keepContext, followUp = cfg.followUp,
            preferOfflineSpeech = cfg.preferOfflineSpeech,
            // The system window has its own timeout; ours is only a safety net there.
            listenTimeoutMs = if (cfg.inAppSpeech) cfg.listenTimeoutSec * 1000L + 4_000L else 60_000L, replyTimeoutMs = cfg.replyTimeoutSec * 1000L,
        )
    }

    val ui: StateFlow<VoiceUi> get() = session.ui

    fun attach(window: Window?) { windowRef = WeakReference(window) }

    fun listen() = session.listen()
    fun ask(text: String) = session.ask(text)
    fun retry() = session.retry()
    fun replay() = session.replay()
    fun stop() = session.stop()
    fun close() = session.close()
    fun clearConversation() = session.clearConversation()
    fun showKeyboard(show: Boolean) = session.showKeyboard(show)
    fun onBackground() = session.onBackground()
    fun refreshOnline() = session.refreshOnline()

    private fun driving(): Boolean = c.settings.current.drivingMode && (c.fix.value?.speedKmh ?: 0f) > 8f

    /** "Claude", "ChatGPT" or null when only local commands are available. */
    fun engineName(): String? {
        val cfg = c.ai.config.value
        return when (cfg.provider) {
            AiProvider.ANTHROPIC -> if (cfg.hasClaudeKey || cfg.proxyUrl.isNotBlank()) "Claude" else null
            AiProvider.OPENAI -> if (cfg.hasOpenAiKey || cfg.proxyUrl.isNotBlank()) "ChatGPT" else null
            AiProvider.LOCAL -> null
        }
    }

    /** The selected provider, ready to call, or null (local only / no key). */
    fun engine(): AiEngine? {
        val cfg = c.ai.config.value
        val proxy = cfg.proxyUrl.ifBlank { null }
        val timeout = cfg.replyTimeoutSec * 1000L
        return when (cfg.provider) {
            AiProvider.ANTHROPIC -> (c.ai.key(AiProvider.ANTHROPIC) ?: if (proxy != null) "proxy" else null)
                ?.let { ClaudeEngine(it, cfg.claudeModel, proxy, timeout) }
            AiProvider.OPENAI -> (c.ai.key(AiProvider.OPENAI) ?: if (proxy != null) "" else null)
                ?.let { OpenAiEngine(it, cfg.openAiModel, proxy, timeout) }
            AiProvider.LOCAL -> null
        }
    }

    private fun noAiMessage(text: String, online: Boolean, ar: Boolean): String {
        val provider = c.ai.config.value.provider
        return when {
            provider == AiProvider.LOCAL -> if (ar) "لم أفهم «$text» كأمر. للأسئلة العامة اختر ChatGPT أو Claude من الإعدادات ← Nova AI."
                else "I didn't understand \"$text\" as a command. For open questions choose ChatGPT or Claude in Settings › Nova AI."
            !online -> if (ar) "لا يوجد اتصال بالإنترنت. الأسئلة العامة تحتاج إنترنت، والأوامر المحلية مثل «افتح الخرائط» و«شغّل الموسيقى» تعمل الآن."
                else "No internet. Open questions need a connection; local commands like \"open maps\" or \"play music\" still work."
            else -> if (ar) "أضف مفتاح API للوكيل المختار من الإعدادات ← Nova AI." else "Add the API key for the selected assistant in Settings › Nova AI."
        }
    }

    /**
     * A network with internet is connected. (Not requiring Android's "validated" flag: phone hotspots
     * in cars are often not validated but work; a dead link is reported by the request itself.)
     */
    fun online(): Boolean = runCatching {
        val cm = c.appContext.getSystemService(ConnectivityManager::class.java) ?: return@runCatching false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return@runCatching false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)
}
