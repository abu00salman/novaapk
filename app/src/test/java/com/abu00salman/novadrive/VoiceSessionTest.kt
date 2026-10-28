package com.abu00salman.novadrive

import com.abu00salman.novadrive.data.VoiceCommands
import com.abu00salman.novadrive.data.voice.ProviderException
import com.abu00salman.novadrive.data.voice.Route
import com.abu00salman.novadrive.data.voice.RouteResult
import com.abu00salman.novadrive.data.voice.SessionConfig
import com.abu00salman.novadrive.data.voice.SpeakOutcome
import com.abu00salman.novadrive.data.voice.SpeakerPort
import com.abu00salman.novadrive.data.voice.SpeechCallback
import com.abu00salman.novadrive.data.voice.SpeechError
import com.abu00salman.novadrive.data.voice.SpeechPort
import com.abu00salman.novadrive.data.voice.VoiceRouter
import com.abu00salman.novadrive.data.voice.VoiceSession
import com.abu00salman.novadrive.data.voice.VoiceState
import com.abu00salman.novadrive.data.voice.busy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Acceptance tests for Nova AI's voice pipeline, with fakes for microphone, AI provider and TTS. */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceSessionTest {

    private class FakeSpeech : SpeechPort {
        var callback: SpeechCallback? = null
        var starts = 0
        var cancels = 0
        var lastOffline = false
        override fun start(language: String, preferOffline: Boolean, callback: SpeechCallback) {
            starts++; lastOffline = preferOffline; this.callback = callback; callback.onListening()
        }
        override fun cancel() { cancels++ }
        fun say(text: String) = callback!!.onResult(text)
        fun fail(e: SpeechError) = callback!!.onError(e)
    }

    private class FakeSpeaker : SpeakerPort {
        var mode = SpeakOutcome.DONE
        var hold = false
        var reason: String? = null
        val spoken = ArrayList<String>()
        private var pending: ((SpeakOutcome, String?) -> Unit)? = null
        override fun speak(text: String, done: (SpeakOutcome, String?) -> Unit) {
            spoken += text
            if (hold) pending = done else done(mode, reason)
        }
        override fun stop() { pending?.invoke(SpeakOutcome.STOPPED, null); pending = null }
        fun finish() { pending?.invoke(SpeakOutcome.DONE, null); pending = null }
        val speaking get() = pending != null
    }

    private class FakeRouter(var provider: String? = "ChatGPT") : VoiceRouter {
        var aiCalls = 0
        var delayMs = 0L
        var error: ProviderException? = null
        val historySizes = ArrayList<Int>()
        override fun providerName() = provider
        override suspend fun route(text: String, online: Boolean, history: List<Pair<String, String>>, onTool: (String) -> Unit): RouteResult {
            if (text.startsWith("افتح")) return RouteResult(Route.LOCAL, "تم، $text")
            if (!online || provider == null) return RouteResult(Route.NONE, "الأسئلة العامة تحتاج إنترنت")
            aiCalls++
            historySizes += history.size
            if (delayMs > 0) delay(delayMs)
            error?.let { throw it }
            return RouteResult(Route.AI, "$provider: جواب «$text»")
        }
    }

    private class Rig(scope: CoroutineScope, provider: String? = "ChatGPT", followUp: Boolean = false) {
        val speech = FakeSpeech()
        val speaker = FakeSpeaker()
        val router = FakeRouter(provider)
        var online = true
        val session = VoiceSession(scope, speech, speaker, router, { online }) {
            SessionConfig(arabic = true, followUp = followUp, listenTimeoutMs = 10_000, replyTimeoutMs = 30_000)
        }
        val ui get() = session.ui.value
    }

    private fun TestScope.rig(provider: String? = "ChatGPT", followUp: Boolean = false) = Rig(backgroundScope, provider, followUp)

    private fun TestScope.fiveVoiceConversations(provider: String) {
        val r = rig(provider)
        for (i in 1..5) {
            r.session.listen()
            assertEquals(VoiceState.LISTENING, r.ui.state)
            r.speech.say("سؤال $i")
            runCurrent()
            assertEquals("turn $i ends ready for the next question", VoiceState.IDLE, r.ui.state)
            assertEquals("$provider: جواب «سؤال $i»", r.ui.reply)
            assertEquals("reply $i is spoken", "$provider: جواب «سؤال $i»", r.speaker.spoken.last())
        }
        assertEquals(5, r.router.aiCalls)
        assertEquals("context grows with every turn", listOf(0, 1, 2, 3, 4), r.router.historySizes)
        assertEquals(5, r.speech.starts)
    }

    @Test fun fiveConversationsWithChatGpt() = runTest { fiveVoiceConversations("ChatGPT") }

    @Test fun fiveConversationsWithClaude() = runTest { fiveVoiceConversations("Claude") }

    @Test fun recognitionFailureThenRetry() = runTest {
        val r = rig()
        r.session.listen()
        r.speech.fail(SpeechError.NO_MATCH)
        assertEquals(VoiceState.ERROR, r.ui.state)
        assertTrue(r.ui.canRetry)
        assertTrue(r.ui.notice!!.contains("لم أفهم"))
        assertEquals("the selected provider is kept", "ChatGPT", r.ui.provider)
        r.session.listen()
        assertEquals(VoiceState.LISTENING, r.ui.state)
        r.speech.say("كم عاصمة اليابان؟")
        runCurrent()
        assertEquals(VoiceState.IDLE, r.ui.state)
        assertEquals(1, r.router.aiCalls)
    }

    @Test fun dismissedGoogleWindowIsNotStuck() = runTest {
        val r = rig()
        r.session.listen()
        r.speech.fail(SpeechError.DISMISSED)
        assertEquals(VoiceState.CANCELLED, r.ui.state)
        assertFalse(r.ui.state.busy)
    }

    @Test fun internetLostDuringConversation() = runTest {
        val r = rig()
        r.session.ask("سؤال 1"); runCurrent()
        assertEquals(1, r.router.aiCalls)
        r.online = false
        r.session.listen()
        assertTrue("recognizer is asked to work offline", r.speech.lastOffline)
        r.speech.fail(SpeechError.NETWORK)
        assertEquals(VoiceState.ERROR, r.ui.state)
        assertTrue("typing and quick commands are offered", r.ui.showKeyboard)
        assertFalse(r.ui.online)
        r.session.ask("سؤال 2"); runCurrent()
        assertEquals("no API call without internet", 1, r.router.aiCalls)
        assertEquals("الأسئلة العامة تحتاج إنترنت", r.ui.reply)
        r.session.ask("افتح الخرائط"); runCurrent()
        assertEquals("local commands still work offline", "تم، افتح الخرائط", r.ui.reply)
        assertEquals(VoiceState.IDLE, r.ui.state)
        r.online = true
        r.session.ask("سؤال 3"); runCurrent()
        assertEquals("the selected provider is used again", 2, r.router.aiCalls)
    }

    @Test fun apiTimeout() = runTest {
        val r = rig()
        r.router.delayMs = 120_000
        r.session.ask("سؤال بطيء")
        advanceTimeBy(31_000); runCurrent()
        assertEquals(VoiceState.TIMEOUT, r.ui.state)
        assertTrue(r.ui.canRetry)
        r.router.delayMs = 0
        r.session.retry(); runCurrent()
        assertEquals(VoiceState.IDLE, r.ui.state)
        assertEquals("ChatGPT: جواب «سؤال بطيء»", r.ui.reply)
    }

    @Test fun invalidApiKey() = runTest {
        val r = rig("Claude")
        r.router.error = ProviderException(ProviderException.Kind.AUTH)
        r.session.ask("مرحبا كيف حالك اليوم يا نوفا؟"); runCurrent()
        assertEquals(VoiceState.ERROR, r.ui.state)
        assertTrue(r.ui.notice!!.contains("مفتاح API غير صالح"))
        assertFalse(r.ui.state.busy)
    }

    @Test fun noCreditLeft() = runTest {
        val r = rig()
        r.router.error = ProviderException(ProviderException.Kind.QUOTA)
        r.session.ask("سؤال"); runCurrent()
        assertTrue(r.ui.notice!!.contains("رصيد"))
        r.session.ask("افتح يوتيوب"); runCurrent()
        assertEquals("local commands work without API credit", "تم، افتح يوتيوب", r.ui.reply)
    }

    @Test fun speechEngineFailure() = runTest {
        val r = rig()
        r.speaker.mode = SpeakOutcome.FAILED
        r.speaker.reason = "لا يوجد صوت عربي مثبت"
        r.session.ask("سؤال"); runCurrent()
        assertEquals(VoiceState.IDLE, r.ui.state)
        assertNotNull("the reply is still shown", r.ui.reply)
        assertTrue(r.ui.notice!!.contains("لا يوجد صوت عربي مثبت"))
    }

    @Test fun interruptReplyWithNewQuestion() = runTest {
        val r = rig()
        r.speaker.hold = true
        r.session.ask("سؤال طويل"); runCurrent()
        assertEquals(VoiceState.SPEAKING, r.ui.state)
        r.session.listen()
        assertFalse("speech stopped", r.speaker.speaking)
        assertEquals(VoiceState.LISTENING, r.ui.state)
        r.speech.say("سؤال جديد"); runCurrent()
        assertEquals(VoiceState.SPEAKING, r.ui.state)
        r.speaker.finish()
        assertEquals(VoiceState.IDLE, r.ui.state)
        assertEquals("ChatGPT: جواب «سؤال جديد»", r.ui.reply)
    }

    @Test fun staleResultsAreIgnored() = runTest {
        val r = rig()
        r.session.listen()
        val old = r.speech.callback!!
        r.session.listen() // pressed again
        old.onResult("نتيجة قديمة")
        runCurrent()
        assertEquals("old recognizer result doesn't start a request", 0, r.router.aiCalls)
        assertEquals(VoiceState.LISTENING, r.ui.state)

        r.router.delayMs = 5_000
        r.session.ask("سؤال 1"); runCurrent()
        r.session.ask("سؤال 2"); advanceTimeBy(20_000); runCurrent()
        assertEquals("ChatGPT: جواب «سؤال 2»", r.ui.reply)
    }

    @Test fun conversationContinuesAcrossInterfacesAndPanelClose() = runTest {
        val r = rig()
        r.session.ask("سؤال 1"); runCurrent()
        r.session.close()
        assertFalse(r.ui.visible)
        r.session.ask("سؤال 2"); runCurrent()
        assertEquals(listOf(0, 1), r.router.historySizes)
        r.session.clearConversation()
        r.session.ask("سؤال 3"); runCurrent()
        assertEquals(0, r.router.historySizes.last())
    }

    @Test fun localCommandOfflineNeedsNoApi() = runTest {
        val r = rig()
        r.online = false
        r.session.ask("افتح الخرائط"); runCurrent()
        assertEquals("تم، افتح الخرائط", r.ui.reply)
        assertEquals(0, r.router.aiCalls)
        assertEquals(0, r.session.aiRequests)
        assertEquals("تم، افتح الخرائط", r.speaker.spoken.last())
    }

    @Test fun noDuplicateApiRequests() = runTest {
        val r = rig()
        r.router.delayMs = 3_000
        r.session.ask("كم عاصمة اليابان؟")
        runCurrent()
        r.session.ask("كم عاصمة اليابان؟") // double tap / repeated result
        r.session.ask("كم عاصمة اليابان؟")
        advanceTimeBy(20_000); runCurrent()
        assertEquals(1, r.router.aiCalls)
        assertEquals(1, r.session.aiRequests)
    }

    @Test fun microphoneNeverStuck() = runTest {
        val r = rig()
        fun pressAgainWorks() {
            assertFalse("state ${r.ui.state} must not be busy", r.ui.state.busy)
            r.session.listen()
            assertEquals(VoiceState.LISTENING, r.ui.state)
            r.session.stop()
        }
        r.session.ask("سؤال"); runCurrent(); pressAgainWorks()                       // success
        r.session.listen(); r.speech.fail(SpeechError.NO_MATCH); pressAgainWorks()    // "Didn't catch that"
        r.session.listen(); r.speech.fail(SpeechError.BUSY); pressAgainWorks()        // recognizer busy
        r.session.listen(); advanceTimeBy(11_000); runCurrent(); pressAgainWorks()    // silence → listen timeout
        r.router.error = ProviderException(ProviderException.Kind.NETWORK)
        r.session.ask("سؤال"); runCurrent(); pressAgainWorks()                       // API error
        r.router.error = null; r.router.delayMs = 99_000
        r.session.ask("سؤال"); advanceTimeBy(31_000); runCurrent(); pressAgainWorks() // API timeout
        r.router.delayMs = 0; r.speaker.mode = SpeakOutcome.FAILED
        r.session.ask("سؤال"); runCurrent(); pressAgainWorks()                       // TTS failure
        r.speaker.mode = SpeakOutcome.DONE; r.speaker.hold = true
        r.session.ask("سؤال"); runCurrent(); r.session.stop(); pressAgainWorks()     // Stop while speaking
        r.session.listen(); r.session.onBackground()
        assertEquals("microphone closed in the background", VoiceState.CANCELLED, r.ui.state)
    }

    @Test fun followUpListensAgainAfterReply() = runTest {
        val r = rig(followUp = true)
        r.session.ask("سؤال"); runCurrent()
        assertEquals(VoiceState.LISTENING, r.ui.state)
        assertEquals(1, r.speech.starts)
    }

    @Test fun localOnlyProviderExplainsGeneralQuestions() = runTest {
        val r = rig(provider = null)
        r.session.ask("كم عاصمة اليابان؟"); runCurrent()
        assertEquals(0, r.router.aiCalls)
        assertEquals(VoiceState.IDLE, r.ui.state)
    }

    // ── Intent routing helpers (the real local command engine)

    @Test fun questionsAreNotCommands() {
        val q = listOf("كم عاصمه اليابان؟", "اشرح لي هذا الموضوع", "وش افضل وقت للسفر", "what is the capital of japan", "ليش السماء زرقاء")
        q.forEach { assertTrue(it, VoiceCommands.isQuestion(VoiceCommands.normalize(it))) }
        val c = listOf("افتح يوتيوب", "شغل الموسيقي", "ارفع الصوت", "open youtube", "خذني الي البيت")
        c.forEach { assertFalse(it, VoiceCommands.isQuestion(VoiceCommands.normalize(it))) }
    }

    @Test fun wholeWordMatching() {
        assertTrue(VoiceCommands.containsWord("وقف الموسيقي", "وقف"))
        assertFalse("«موقف» is not «وقف»", VoiceCommands.containsWord("وين اقرب موقف", "وقف"))
        assertFalse("«الجوال» is not «الجو»", VoiceCommands.containsWord("شحن الجوال", "الجو"))
        assertTrue("attached prefix", VoiceCommands.containsWord("ودني لاقرب محطه", "اقرب"))
        assertFalse(VoiceCommands.containsWord("display settings", "play"))
    }

    @Test fun compoundCommandsAreSplit() {
        assertEquals(listOf("افتح الخرائط", "ابحث عن محطه وقود"), VoiceCommands.splitCompound("افتح الخرائط وابحث عن محطه وقود"))
        assertEquals(listOf("شغل الموسيقي", "ارفع الصوت"), VoiceCommands.splitCompound("شغل الموسيقي ثم ارفع الصوت"))
        assertEquals(listOf("وين الوزاره"), VoiceCommands.splitCompound("وين الوزاره"))
        assertEquals(listOf("open maps", "play music"), VoiceCommands.splitCompound("open maps and play music"))
    }

    @Test fun wakeWordIsStripped() {
        assertEquals("افتح يوتيوب", VoiceCommands.stripWake(VoiceCommands.normalize("يا نوفا، افتح يوتيوب")))
    }

    @Test fun openAiErrorsAreClassified() {
        assertEquals(ProviderException.Kind.AUTH, com.abu00salman.novadrive.data.ai.openAiKind(401, "invalid_api_key"))
        assertEquals(ProviderException.Kind.QUOTA, com.abu00salman.novadrive.data.ai.openAiKind(429, "insufficient_quota insufficient_quota"))
        assertEquals(ProviderException.Kind.RATE, com.abu00salman.novadrive.data.ai.openAiKind(429, "rate_limit_exceeded"))
        assertEquals(ProviderException.Kind.MODEL, com.abu00salman.novadrive.data.ai.openAiKind(404, "model_not_found"))
        assertEquals(ProviderException.Kind.SERVER, com.abu00salman.novadrive.data.ai.openAiKind(503, ""))
    }
}
