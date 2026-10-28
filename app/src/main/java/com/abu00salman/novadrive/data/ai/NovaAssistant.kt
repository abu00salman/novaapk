package com.abu00salman.novadrive.data.ai

import android.view.Window
import androidx.compose.runtime.Immutable
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.data.VoiceCommands
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

enum class AiStatus { IDLE, LISTENING, THINKING, ACTING, SPEAKING, ERROR }

@Immutable
data class AiUi(
    val visible: Boolean = false,
    val status: AiStatus = AiStatus.IDLE,
    val heard: String? = null,
    val reply: String? = null,
    /** Tool being run right now, for "ينفّذ: …". */
    val acting: String? = null,
    /** "Claude", "ChatGPT" or null when only local commands are available. */
    val engine: String? = null,
)

/**
 * Nova AI: local commands first (instant, offline), then — when the user set up OpenAI or Claude —
 * the AI with real tools. Keeps a short conversation for follow-ups, speaks replies, and can be
 * interrupted at any time.
 */
class NovaAssistant(private val c: AppContainer) {
    private val _ui = MutableStateFlow(AiUi())
    val ui: StateFlow<AiUi> = _ui
    private val history = ArrayDeque<Turn>()
    private var job: Job? = null

    fun engineName(): String? {
        val cfg = c.ai.config.value
        return when (cfg.provider) {
            AiProvider.ANTHROPIC -> if (cfg.hasClaudeKey || cfg.proxyUrl.isNotBlank()) "Claude" else null
            AiProvider.OPENAI -> if (cfg.hasOpenAiKey || cfg.proxyUrl.isNotBlank()) "ChatGPT" else null
            AiProvider.LOCAL -> null
        }
    }

    /** The microphone is about to listen (the system recognizer shows its own UI). */
    fun listening() {
        c.speaker.stop()
        _ui.value = _ui.value.copy(visible = true, status = AiStatus.LISTENING, acting = null, engine = engineName())
    }

    /** Nothing was heard: go back to idle, hide if there's nothing to show. */
    fun heardNothing() {
        val u = _ui.value
        _ui.value = u.copy(status = AiStatus.IDLE, visible = u.reply != null)
    }

    fun ask(text: String, window: Window?, driving: Boolean) {
        job?.cancel()
        c.speaker.stop()
        val ar = c.settings.current.arabic
        _ui.value = AiUi(visible = true, status = AiStatus.THINKING, heard = text, engine = engineName())
        job = c.scope.launch {
            val reply = try {
                VoiceCommands.handle(c, text, window, driving) ?: aiReply(text, window, driving, ar)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: IOException) {
                fail(if (ar) "لا يوجد اتصال بالإنترنت — الأوامر المحلية ما زالت تعمل" else "No internet connection — local commands still work"); return@launch
            } catch (e: Exception) {
                fail((if (ar) "تعذّر الرد: " else "Couldn't answer: ") + (e.message ?: e.javaClass.simpleName)); return@launch
            }
            history.addLast(Turn(text, reply)); while (history.size > 6) history.removeFirst()
            _ui.value = _ui.value.copy(status = AiStatus.SPEAKING, reply = reply, acting = null)
            val s = c.settings.current
            if (s.voiceReply && reply.isNotBlank()) c.speaker.speak(reply, s.arabic) { done() } else done()
        }
    }

    private suspend fun aiReply(text: String, window: Window?, driving: Boolean, ar: Boolean): String {
        val cfg = c.ai.config.value
        val proxy = cfg.proxyUrl.ifBlank { null }
        val engine: AiEngine = when (cfg.provider) {
            AiProvider.ANTHROPIC -> {
                val key = c.ai.key(AiProvider.ANTHROPIC) ?: if (proxy != null) "proxy" else null
                key?.let { ClaudeEngine(it, cfg.claudeModel, proxy) }
            }
            AiProvider.OPENAI -> {
                val key = c.ai.key(AiProvider.OPENAI) ?: if (proxy != null) "" else null
                key?.let { OpenAiEngine(it, cfg.openAiModel, proxy) }
            }
            AiProvider.LOCAL -> null
        } ?: return if (ar) "لم أفهم: \"$text\". للأسئلة العامة فعّل Nova AI بـ ChatGPT أو Claude من الإعدادات."
            else "I didn't catch \"$text\". For open questions, set up ChatGPT or Claude in Settings › Nova AI."
        return engine.reply(
            NovaTools.systemPrompt(c), history.toList(), text, NovaTools.specs,
            run = { name, args -> NovaTools.execute(c, name, args, driving, window) },
            notice = { name -> _ui.value = _ui.value.copy(status = AiStatus.ACTING, acting = name) },
        ).ifBlank { if (ar) "تم." else "Done." }
    }

    private fun fail(message: String) {
        _ui.value = _ui.value.copy(status = AiStatus.ERROR, reply = message, acting = null)
        if (c.settings.current.voiceReply) c.speaker.speak(message, c.settings.current.arabic) { }
    }

    private fun done() {
        if (_ui.value.status == AiStatus.SPEAKING) _ui.value = _ui.value.copy(status = AiStatus.IDLE)
    }

    /** Stops the spoken reply / the request in flight. */
    fun interrupt() {
        job?.cancel()
        c.speaker.stop()
        _ui.value = _ui.value.copy(status = AiStatus.IDLE, acting = null)
    }

    fun close() {
        interrupt()
        _ui.value = AiUi()
    }

    /** Forget the conversation (a new topic). */
    fun reset() { history.clear() }
}
