package com.abu00salman.novadrive.data.voice

import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.data.ai.Turn
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** One diagnostic line: passed or not, and what was measured / why it failed. */
data class DiagResult(val ok: Boolean, val detail: String)

/**
 * Settings › Nova AI › Diagnostics. Every check does the real thing (records from the microphone,
 * recognises speech, calls the selected API, speaks) and reports what actually happened — a saved key
 * is not reported as "working" until a request with it succeeded.
 */
class NovaDiagnostics(private val c: AppContainer) {
    private val ar get() = c.settings.current.arabic
    private fun msg(en: String, a: String) = if (ar) a else en

    /** Listens once; reports the loudest microphone level and the recognised words. */
    suspend fun speech(): Pair<DiagResult, DiagResult> {
        c.assistant.stop()
        var peak = 0f
        var started = false
        val outcome = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Pair<String?, SpeechError?>> { cont ->
                c.assistant.speech.start(if (ar) "ar-SA" else "en-US", !c.assistant.online(), object : SpeechCallback {
                    override fun onListening() { started = true }
                    override fun onPartial(text: String) {}
                    override fun onLevel(level: Float) { started = true; peak = maxOf(peak, level) }
                    override fun onResult(text: String) { if (cont.isActive) cont.resume(text to null) }
                    override fun onError(error: SpeechError) { if (cont.isActive) cont.resume(null to error) }
                })
                cont.invokeOnCancellation { c.assistant.speech.cancel() }
            }
        } ?: run { c.assistant.speech.cancel(); null to SpeechError.NO_SPEECH }
        val (text, error) = outcome
        val mic = when {
            error == SpeechError.PERMISSION -> DiagResult(false, msg("Microphone permission not granted", "لم يُسمح باستخدام الميكروفون"))
            error == SpeechError.AUDIO -> DiagResult(false, msg("The microphone couldn't record", "تعذّر التسجيل من الميكروفون"))
            error == SpeechError.UNAVAILABLE -> DiagResult(false, msg("No speech service to record with", "لا توجد خدمة تعرف صوتي للتسجيل"))
            peak > 0.15f -> DiagResult(true, msg("Microphone works (level ${(peak * 100).toInt()}%)", "الميكروفون يعمل (المستوى ${(peak * 100).toInt()}٪)"))
            started -> DiagResult(true, msg("Microphone opened, but the voice was quiet (${(peak * 100).toInt()}%)", "الميكروفون فُتح لكن الصوت كان منخفضًا (${(peak * 100).toInt()}٪)"))
            else -> DiagResult(error == null, msg("Recorded through the system window", "تم التسجيل عبر نافذة النظام"))
        }
        val rec = if (!text.isNullOrBlank()) DiagResult(true, msg("Heard: «$text»", "سمعت: «$text»"))
        else DiagResult(false, msg("Nothing recognised", "لم يُتعرّف على كلام") + (error?.let { " (${it.name})" } ?: ""))
        return mic to rec
    }

    /** Speaks the greeting and waits until the engine reports it finished (or failed). */
    suspend fun tts(): DiagResult {
        c.assistant.stop()
        val r = withTimeoutOrNull(30_000) {
            suspendCancellableCoroutine<Pair<SpeakOutcome, String?>> { cont ->
                c.speaker.test(ar) { o, reason -> if (cont.isActive) cont.resume(o to reason) }
                cont.invokeOnCancellation { c.speaker.stop() }
            }
        } ?: (SpeakOutcome.FAILED to "timeout")
        val st = c.speaker.status.value
        return if (r.first == SpeakOutcome.DONE) DiagResult(true, msg("Spoken by ", "نطق بواسطة ") + (st.engine ?: "TTS") + if (ar && !st.arabic) msg("", " — تنبيه: لا يوجد صوت عربي") else "")
        else DiagResult(false, msg("Speech failed: ", "فشل النطق: ") + (r.second ?: r.first.name) +
            msg(". Open voice settings, install Google Speech Services and the Arabic voice data.", ". افتح إعدادات النطق وثبّت Google Speech Services وبيانات الصوت العربي."))
    }

    /** One tiny real request to the selected provider. */
    suspend fun api(): DiagResult {
        val name = c.assistant.engineName() ?: return DiagResult(false, msg("No AI provider selected / no key saved", "لم يُختر وكيل ذكاء اصطناعي أو لم يُحفظ مفتاح"))
        if (!c.assistant.online()) return DiagResult(false, msg("No internet connection", "لا يوجد اتصال بالإنترنت"))
        val engine = c.assistant.engine() ?: return DiagResult(false, msg("Key can't be read — save it again", "تعذّرت قراءة المفتاح — احفظه مرة أخرى"))
        val t0 = System.currentTimeMillis()
        return try {
            val answer = engine.ping()
            DiagResult(true, "$name ✓ ${System.currentTimeMillis() - t0} ms" + if (answer.isNotBlank()) " — «${answer.take(30)}»" else "")
        } catch (e: ProviderException) {
            DiagResult(false, providerMessage(e.kind, name, ar))
        } catch (e: Exception) {
            DiagResult(false, "$name: " + (e.message ?: e.javaClass.simpleName))
        }
    }

    /**
     * Five questions in a row through the selected provider, keeping context, each reply spoken to the
     * end before the next one — the same chain as a voice conversation, minus the microphone.
     */
    suspend fun conversation(onStep: (Int, DiagResult) -> Unit): DiagResult {
        val name = c.assistant.engineName() ?: return DiagResult(false, msg("No AI provider selected / no key saved", "لم يُختر وكيل ذكاء اصطناعي أو لم يُحفظ مفتاح"))
        val engine = c.assistant.engine() ?: return DiagResult(false, msg("Key can't be read — save it again", "تعذّرت قراءة المفتاح — احفظه مرة أخرى"))
        c.assistant.stop()
        val questions = if (ar) listOf(
            "ما عاصمة اليابان؟ أجب بجملة قصيرة.", "وكم عدد سكانها تقريبًا؟", "اذكر معلمًا مشهورًا فيها.",
            "كم ساعة طيران تقريبًا من الرياض إليها؟", "لخّص محادثتنا في جملة واحدة.",
        ) else listOf(
            "What is the capital of Japan? One short sentence.", "And roughly how many people live there?", "Name one famous landmark there.",
            "About how many hours is the flight from Riyadh?", "Summarise our conversation in one sentence.",
        )
        val history = ArrayList<Turn>()
        var passed = 0
        for ((i, q) in questions.withIndex()) {
            val t0 = System.currentTimeMillis()
            val step = try {
                val reply = engine.reply(com.abu00salman.novadrive.data.ai.NovaTools.systemPrompt(c), history.toList(), q, emptyList(), { _, _ -> "" }, { })
                history.add(Turn(q, reply))
                val spoken = withTimeoutOrNull(60_000) {
                    suspendCancellableCoroutine<SpeakOutcome> { cont ->
                        c.speaker.speak(reply, ar) { o, _ -> if (cont.isActive) cont.resume(o) }
                        cont.invokeOnCancellation { c.speaker.stop() }
                    }
                }
                val ms = System.currentTimeMillis() - t0
                if (reply.isBlank()) DiagResult(false, msg("empty reply", "رد فارغ"))
                else DiagResult(spoken == SpeakOutcome.DONE, "${ms} ms · " + reply.take(60) + if (spoken != SpeakOutcome.DONE) msg(" (not spoken)", " (لم يُنطق)") else "")
            } catch (e: ProviderException) {
                DiagResult(false, providerMessage(e.kind, name, ar))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                DiagResult(false, e.message ?: e.javaClass.simpleName)
            }
            if (step.ok) passed++
            onStep(i + 1, step)
            if (!step.ok && !c.assistant.online()) break
            delay(300)
        }
        return DiagResult(passed == questions.size, msg("$passed of ${questions.size} conversations passed", "نجحت $passed من ${questions.size} محادثات"))
    }
}
