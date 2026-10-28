package com.abu00salman.novadrive.data.voice

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Nova AI's voice pipeline, free of Android types so it can be tested on the JVM:
 *
 * microphone → [SpeechPort] (speech-to-text) → [VoiceRouter] (local command or the selected AI)
 * → [SpeakerPort] (text-to-speech) → back to IDLE.
 *
 * Every step belongs to a numbered session. Starting anything new cancels the running session, and
 * callbacks from an older session are ignored, so a late recognizer result, a late API reply or a
 * late "speech finished" can never push the UI into a stale state. Every wait has a timeout, and every
 * path — success, failure, timeout, cancel — ends in a state where the microphone can be pressed again.
 */
enum class VoiceState { IDLE, LISTENING, PROCESSING, ACTING, SPEAKING, ERROR, CANCELLED, TIMEOUT }

/** States in which work is still running (the Stop button is shown). */
val VoiceState.busy: Boolean get() = this == VoiceState.LISTENING || this == VoiceState.PROCESSING || this == VoiceState.ACTING || this == VoiceState.SPEAKING

enum class SpeechError {
    /** Nothing understood ("Didn't catch that"). */ NO_MATCH,
    /** Silence. */ NO_SPEECH,
    /** The recognizer needs the internet and there is none. */ NETWORK,
    /** Microphone could not record. */ AUDIO,
    /** RECORD_AUDIO was not granted. */ PERMISSION,
    /** The recognizer is still busy with an older request. */ BUSY,
    /** The language isn't available (e.g. offline Arabic not downloaded). */ LANGUAGE,
    /** No speech recognizer on the unit. */ UNAVAILABLE,
    /** The system dialog was dismissed without a result. */ DISMISSED,
    OTHER,
}

interface SpeechCallback {
    fun onListening()
    fun onPartial(text: String)
    fun onLevel(level: Float)
    fun onResult(text: String)
    fun onError(error: SpeechError)
}

/** Speech-to-text (Android SpeechRecognizer in the app, or the system dialog as a fallback). */
interface SpeechPort {
    fun start(language: String, preferOffline: Boolean, callback: SpeechCallback)
    fun cancel()
}

enum class SpeakOutcome { DONE, STOPPED, FAILED }

/** Text-to-speech; [done] must be called exactly once per [speak], whatever happens. */
interface SpeakerPort {
    fun speak(text: String, done: (SpeakOutcome, String?) -> Unit)
    fun stop()
}

/** What answered: a local command (works offline, no API) or the selected AI provider. */
enum class Route { LOCAL, AI, NONE }

class RouteResult(val route: Route, val text: String)

/** Local command engine first, then the one AI provider the user selected (never several). */
interface VoiceRouter {
    /** "ChatGPT", "Claude" or null when no AI provider is selected and set up. */
    fun providerName(): String?
    suspend fun route(text: String, online: Boolean, history: List<Pair<String, String>>, onTool: (String) -> Unit): RouteResult
}

/** Thrown by the AI provider with a kind the session turns into a clear Arabic/English message. */
class ProviderException(val kind: Kind, detail: String? = null) : Exception(detail ?: kind.name) {
    enum class Kind { AUTH, QUOTA, RATE, MODEL, NETWORK, SERVER, TIMEOUT, OTHER }
}

data class SessionConfig(
    val arabic: Boolean = true,
    val speakReplies: Boolean = true,
    val keepContext: Boolean = true,
    /** Listen again automatically after a reply was spoken. */
    val followUp: Boolean = false,
    val preferOfflineSpeech: Boolean = false,
    val listenTimeoutMs: Long = 12_000,
    val replyTimeoutMs: Long = 30_000,
)

data class ChatLine(val user: String, val assistant: String, val route: Route)

data class VoiceUi(
    val visible: Boolean = false,
    val state: VoiceState = VoiceState.IDLE,
    val partial: String? = null,
    val heard: String? = null,
    val reply: String? = null,
    /** Short status/error line under the reply (e.g. why speech failed). */
    val notice: String? = null,
    val acting: String? = null,
    val level: Float = 0f,
    val provider: String? = null,
    val online: Boolean = true,
    /** Offer typing and quick commands (e.g. speech needs the internet and there is none). */
    val showKeyboard: Boolean = false,
    val canRetry: Boolean = false,
    val history: List<ChatLine> = emptyList(),
    /** Increases with each new session (lets the UI/tests see that a new session started). */
    val session: Long = 0,
)

class VoiceSession(
    private val scope: CoroutineScope,
    private val speech: SpeechPort,
    private val speaker: SpeakerPort,
    private val router: VoiceRouter,
    private val online: () -> Boolean,
    private val config: () -> SessionConfig,
) {
    private val _ui = MutableStateFlow(VoiceUi())
    val ui: StateFlow<VoiceUi> = _ui

    private var session = 0L
    private var job: Job? = null
    private var listenTimeout: Job? = null
    private var lastQuestion: String? = null
    private val history = ArrayList<ChatLine>()

    /** Number of requests actually sent to the AI provider (tests check there are no duplicates). */
    var aiRequests = 0
        private set

    private fun msg(en: String, ar: String) = if (config().arabic) ar else en

    private fun set(block: (VoiceUi) -> VoiceUi) { _ui.value = block(_ui.value) }

    /** Cancels whatever runs now and opens a new session; returns its number. */
    private fun newSession(): Long {
        session++
        job?.cancel(); job = null
        listenTimeout?.cancel(); listenTimeout = null
        speech.cancel()
        speaker.stop()
        return session
    }

    private fun base(id: Long) = _ui.value.copy(
        visible = true, session = id, provider = router.providerName(), online = online(),
        partial = null, notice = null, acting = null, level = 0f, canRetry = false, showKeyboard = false,
    )

    /** Microphone pressed: stop speaking, drop the old session, listen for a new question. */
    fun listen() {
        val id = newSession()
        val cfg = config()
        val net = online()
        _ui.value = base(id).copy(state = VoiceState.LISTENING, heard = null)
        listenTimeout = scope.launch {
            delay(cfg.listenTimeoutMs)
            if (id != session) return@launch
            speech.cancel()
            set { it.copy(state = VoiceState.TIMEOUT, partial = null, level = 0f, canRetry = true,
                notice = msg("I didn't hear anything — tap the microphone and try again", "لم أسمع شيئًا — اضغط الميكروفون وحاول مرة أخرى")) }
        }
        speech.start(if (cfg.arabic) "ar-SA" else "en-US", cfg.preferOfflineSpeech || !net, object : SpeechCallback {
            override fun onListening() { if (id == session) set { it.copy(state = VoiceState.LISTENING) } }
            override fun onPartial(text: String) { if (id == session && _ui.value.state == VoiceState.LISTENING) set { it.copy(partial = text) } }
            override fun onLevel(level: Float) { if (id == session && _ui.value.state == VoiceState.LISTENING) set { it.copy(level = level) } }
            override fun onResult(text: String) {
                if (id != session) return
                listenTimeout?.cancel()
                if (text.isBlank()) onError(SpeechError.NO_MATCH) else process(id, text.trim())
            }
            override fun onError(error: SpeechError) {
                if (id != session) return
                listenTimeout?.cancel()
                val offline = !online()
                set {
                    it.copy(
                        state = if (error == SpeechError.DISMISSED) VoiceState.CANCELLED else VoiceState.ERROR,
                        partial = null, level = 0f, canRetry = true, online = !offline,
                        showKeyboard = error == SpeechError.NETWORK || error == SpeechError.LANGUAGE || error == SpeechError.UNAVAILABLE || offline,
                        notice = speechMessage(error, offline),
                    )
                }
            }
        })
    }

    /** Typed question or quick-command chip (also used when speech recognition isn't available). */
    fun ask(text: String) {
        if (text.isBlank()) return
        // The same question while it is still being answered: don't send (and pay for) it twice.
        val u = _ui.value
        if (u.heard == text.trim() && (u.state == VoiceState.PROCESSING || u.state == VoiceState.ACTING)) return
        val id = newSession()
        _ui.value = base(id)
        process(id, text.trim())
    }

    /** Ask the last question again after an error or timeout. */
    fun retry() { lastQuestion?.let { ask(it) } ?: listen() }

    /** Speak the last reply again. */
    fun replay() {
        val reply = _ui.value.reply ?: return
        val id = newSession()
        _ui.value = base(id).copy(reply = reply)
        speak(id, reply)
    }

    private fun process(id: Long, text: String) {
        lastQuestion = text
        val cfg = config()
        set { it.copy(state = VoiceState.PROCESSING, heard = text, partial = null, level = 0f, reply = null, notice = null) }
        job = scope.launch {
            val net = online()
            val result = try {
                withTimeout(cfg.replyTimeoutMs) {
                    router.route(text, net, if (cfg.keepContext) history.map { it.user to it.assistant } else emptyList()) { tool ->
                        if (id == session) set { it.copy(state = VoiceState.ACTING, acting = tool) }
                    }.also { if (it.route == Route.AI) aiRequests++ }
                }
            } catch (e: TimeoutCancellationException) {
                if (id == session) fail(VoiceState.TIMEOUT, msg("The answer took too long — try again", "تأخر الرد كثيرًا — حاول مرة أخرى"))
                return@launch
            } catch (e: CancellationException) {
                throw e
            } catch (e: ProviderException) {
                if (id == session) fail(VoiceState.ERROR, providerMessage(e.kind, router.providerName(), config().arabic))
                return@launch
            } catch (e: Exception) {
                if (id == session) fail(VoiceState.ERROR, msg("Couldn't answer: ", "تعذّر الرد: ") + (e.message ?: e.javaClass.simpleName))
                return@launch
            }
            if (id != session) return@launch
            if (result.route != Route.NONE && cfg.keepContext) {
                history.add(ChatLine(text, result.text, result.route))
                while (history.size > MAX_TURNS) history.removeAt(0)
            }
            set { it.copy(reply = result.text, acting = null, history = history.toList(), online = online()) }
            speak(id, result.text)
        }
    }

    private fun speak(id: Long, text: String) {
        val cfg = config()
        if (!cfg.speakReplies || text.isBlank()) { finish(id, spoken = false); return }
        set { it.copy(state = VoiceState.SPEAKING) }
        speaker.speak(text) { outcome, reason ->
            if (id != session) return@speak
            when (outcome) {
                SpeakOutcome.DONE -> finish(id, spoken = true)
                SpeakOutcome.STOPPED -> set { it.copy(state = VoiceState.IDLE) }
                SpeakOutcome.FAILED -> set { it.copy(state = VoiceState.IDLE, notice = msg("Couldn't speak the reply", "تعذّر نطق الرد") + (reason?.let { r -> ": $r" } ?: "")) }
            }
        }
    }

    private fun finish(id: Long, spoken: Boolean) {
        if (id != session) return
        set { it.copy(state = VoiceState.IDLE) }
        if (spoken && config().followUp) listen()
    }

    private fun fail(state: VoiceState, message: String) {
        set { it.copy(state = state, acting = null, canRetry = true, notice = message, online = online()) }
    }

    /** Stop button: stop listening, the request in flight and speech. */
    fun stop() {
        val wasBusy = _ui.value.state.busy
        newSession()
        set { it.copy(state = if (wasBusy) VoiceState.CANCELLED else it.state, partial = null, acting = null, level = 0f, canRetry = wasBusy) }
    }

    /** App went to the background: never keep the microphone open there. */
    fun onBackground() {
        if (_ui.value.state == VoiceState.LISTENING) stop()
    }

    fun close() {
        newSession()
        _ui.value = VoiceUi(history = history.toList(), session = session)
    }

    fun clearConversation() {
        history.clear(); lastQuestion = null
        set { it.copy(history = emptyList(), heard = null, reply = null, notice = null) }
    }

    fun showKeyboard(show: Boolean) = set { it.copy(visible = true, showKeyboard = show, provider = router.providerName(), online = online()) }

    fun refreshOnline() = set { it.copy(online = online(), provider = router.providerName()) }

    private fun speechMessage(e: SpeechError, offline: Boolean): String = when (e) {
        SpeechError.NO_MATCH -> msg("I didn't catch that — tap the microphone and try again", "لم أفهم الكلام — اضغط الميكروفون وحاول مرة أخرى")
        SpeechError.NO_SPEECH -> msg("I didn't hear anything — try again", "لم أسمع كلامًا — حاول مرة أخرى")
        SpeechError.NETWORK -> if (offline) msg("No internet: voice recognition isn't available offline on this unit. Type a command or use the buttons.", "لا يوجد إنترنت: التعرف الصوتي غير متاح دون اتصال على هذا الجهاز. اكتب الأمر أو استخدم الأزرار.")
            else msg("Voice recognition couldn't reach its server — try again", "تعذّر وصول التعرف الصوتي لخادمه — حاول مرة أخرى")
        SpeechError.AUDIO -> msg("The microphone couldn't record", "تعذّر التسجيل من الميكروفون")
        SpeechError.PERMISSION -> msg("Allow microphone access for NovaDrive", "اسمح لـ NovaDrive باستخدام الميكروفون")
        SpeechError.BUSY -> msg("The recognizer was busy — try again", "خدمة التعرف كانت مشغولة — حاول مرة أخرى")
        SpeechError.LANGUAGE -> msg("This language isn't available for offline recognition. Type the command instead.", "اللغة غير متاحة للتعرف دون إنترنت. اكتب الأمر بدلًا من ذلك.")
        SpeechError.UNAVAILABLE -> msg("No speech recognition service on this unit. Type the command instead.", "لا توجد خدمة تعرف صوتي على الجهاز. اكتب الأمر بدلًا من ذلك.")
        SpeechError.DISMISSED -> msg("Cancelled", "تم الإلغاء")
        SpeechError.OTHER -> msg("Voice recognition stopped — try again", "توقف التعرف الصوتي — حاول مرة أخرى")
    }

    companion object { const val MAX_TURNS = 6 }
}

/** What to tell the driver when the AI provider fails. */
fun providerMessage(k: ProviderException.Kind, name: String?, arabic: Boolean): String {
    fun msg(en: String, ar: String) = if (arabic) ar else en
    val n = name ?: "AI"
    return when (k) {
        ProviderException.Kind.AUTH -> msg("$n: the API key is invalid or not allowed — check it in Settings › Nova AI", "$n: مفتاح API غير صالح أو غير مصرّح — راجعه من الإعدادات ← Nova AI")
        ProviderException.Kind.QUOTA -> msg("$n: no API credit left on the account", "$n: لا يوجد رصيد API كافٍ في الحساب")
        ProviderException.Kind.RATE -> msg("$n: too many requests right now — wait a moment", "$n: طلبات كثيرة الآن — انتظر قليلًا")
        ProviderException.Kind.MODEL -> msg("$n: the model name in Settings isn't available", "$n: اسم النموذج في الإعدادات غير متاح")
        ProviderException.Kind.NETWORK -> msg("$n: couldn't connect — local commands still work", "$n: تعذّر الاتصال — الأوامر المحلية ما زالت تعمل")
        ProviderException.Kind.SERVER -> msg("$n is having problems — try again shortly", "$n يواجه مشكلة الآن — حاول بعد قليل")
        ProviderException.Kind.TIMEOUT -> msg("$n took too long — try again", "$n تأخر في الرد — حاول مرة أخرى")
        ProviderException.Kind.OTHER -> msg("$n couldn't answer", "$n لم يستطع الرد")
    }
}
