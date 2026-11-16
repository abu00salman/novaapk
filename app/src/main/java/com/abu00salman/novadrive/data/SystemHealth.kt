package com.abu00salman.novadrive.data

import android.app.ActivityManager
import android.os.Build
import android.os.Debug
import android.os.StatFs
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.BuildConfig
import com.abu00salman.novadrive.core.CrashGuard
import java.io.File
import java.util.Locale

/** One line of NOVA Diagnostics. [ok] is null for plain information. */
data class HealthItem(val en: String, val ar: String, val value: String, val ok: Boolean? = null)

/**
 * NOVA Diagnostics: what NovaDrive can actually see about itself and the unit right now — measured
 * start-up time and memory, permissions, services and connections. Nothing here is estimated.
 */
class SystemHealth(private val c: AppContainer) {
    /** Process start → first frames of this run, measured on the unit; null until the first frame. */
    @Volatile var startupMs: Long? = null
        private set
    private var marked = false

    /** Called once after the first frames of the first activity of the process (a cold start). */
    fun markStarted() {
        if (marked) return
        marked = true
        if (Build.VERSION.SDK_INT >= 24) startupMs = android.os.SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime()
    }

    private fun dirSize(f: File): Long = runCatching { f.walkTopDown().filter { it.isFile }.sumOf { it.length() } }.getOrDefault(0L)
    private fun mb(bytes: Long) = String.format(Locale.US, "%.0f MB", bytes / 1_048_576.0)

    fun snapshot(): List<HealthItem> {
        val ctx = c.appContext
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mem = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        val pssKb = runCatching { Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }.totalPss }.getOrDefault(0)
        val free = runCatching { StatFs(ctx.filesDir.path).availableBytes }.getOrDefault(0L)
        val speech = c.assistant.speech
        val tts = c.speaker.status.value
        val obd = c.obd.data.value
        val s = c.settings.current
        return listOf(
            HealthItem("Version", "الإصدار", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.BUILD_TYPE}"),
            HealthItem("Start-up (process → first frame)", "زمن الإقلاع (بدء العملية ← أول إطار)", startupMs?.let { "$it ms" } ?: "--"),
            HealthItem("NovaDrive memory (PSS)", "ذاكرة NovaDrive (PSS)", if (pssKb > 0) mb(pssKb * 1024L) else "--"),
            HealthItem("Unit memory free", "الذاكرة المتاحة في الجهاز", "${mb(mem.availMem)} / ${mb(mem.totalMem)}", !mem.lowMemory),
            HealthItem("Storage free", "التخزين المتاح", mb(free), free > 200L * 1_048_576),
            HealthItem("Map tile cache", "ذاكرة الخرائط المؤقتة", mb(dirSize(File(ctx.cacheDir, "tiles")))),
            HealthItem("Installed apps found", "التطبيقات المكتشفة", "${c.apps.apps.value.size}", c.apps.apps.value.isNotEmpty()),
            HealthItem("Default launcher", "المشغّل الافتراضي", yes(c.controls.isDefaultLauncher())),
            HealthItem("Location permission", "إذن الموقع", yes(c.location.hasPermission()), c.location.hasPermission()),
            HealthItem("GPS", "GPS", when {
                !c.status.gpsEnabled() -> "off"
                c.fix.value?.fromGps == true -> "fix ±${c.fix.value?.accuracyM?.toInt()} m"
                else -> "searching"
            }, c.status.gpsEnabled()),
            HealthItem("Internet", "الإنترنت", yes(c.assistant.online()), c.assistant.online()),
            HealthItem("Bluetooth", "البلوتوث", c.bluetooth.value.name.lowercase()),
            HealthItem("Media / notification access", "الوصول للوسائط والإشعارات", yes(MediaRepository.hasAccess(ctx)), MediaRepository.hasAccess(ctx)),
            HealthItem("Now playing", "التشغيل الحالي", c.media.nowPlaying()?.first ?: "--"),
            HealthItem("Display over other apps", "الظهور فوق التطبيقات", yes(c.controls.dimmer.canOverlay()), c.controls.dimmer.canOverlay()),
            HealthItem("Speech recognizer", "خدمة التعرف الصوتي", yes(speech.available()), speech.available()),
            HealthItem("Microphone permission", "إذن الميكروفون", yes(speech.hasMicPermission())),
            HealthItem("Text-to-speech", "محرك النطق", when {
                tts.failed -> "failed"
                !tts.ready -> "not started"
                else -> (tts.engine ?: "ready") + if (tts.arabic) " · ar" else " · no Arabic voice"
            }, if (tts.ready) tts.arabic || !s.arabic else if (tts.failed) false else null),
            HealthItem("OBD-II", "OBD-II", if (!s.obdEnabled) "off" else obd.state.name.lowercase() + (obd.error?.let { " · $it" } ?: ""),
                if (!s.obdEnabled) null else obd.state == VehicleData.State.LIVE),
            HealthItem("Safe mode", "الوضع الآمن", yes(c.safeMode), !c.safeMode),
            HealthItem("Last crash report", "آخر تقرير عطل", if (CrashGuard.lastCrash(ctx) != null) "saved (Settings › Launcher)" else "none", CrashGuard.lastCrash(ctx) == null),
            HealthItem("Unit", "الجهاز", "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} · ${ctx.resources.displayMetrics.let { "${it.widthPixels}×${it.heightPixels} @${it.densityDpi}dpi" }}"),
        )
    }

    private fun yes(b: Boolean) = if (b) "yes" else "no"

    /** Plain-text report for sharing with support. No location, contacts or keys. */
    fun report(): String = snapshot().joinToString("\n") { "${it.en}: ${it.value}" }
}
