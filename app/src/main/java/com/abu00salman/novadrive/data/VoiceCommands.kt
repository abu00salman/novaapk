package com.abu00salman.novadrive.data

import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.ThemeMode

/**
 * Small, deterministic intent parser for Arabic and English car commands.
 * Speech-to-text itself is done by the device's installed recognizer (RecognizerIntent).
 */
object VoiceCommands {

    private val navPrefixes = listOf(
        "navigate to ", "take me to ", "directions to ", "drive to ", "go to ",
        "خذني إلى ", "خذني الى ", "خذني ل", "وديني ", "ودني ", "اذهب إلى ", "اذهب الى ", "روح ", "الطريق إلى ", "الطريق الى "
    )
    private val openPrefixes = listOf("open ", "launch ", "start ", "افتح ", "شغل تطبيق ", "تشغيل تطبيق ")
    private val homeWords = listOf("home", "البيت", "المنزل", "بيتي")
    private val workWords = listOf("work", "office", "الدوام", "العمل", "الشغل")

    fun handle(c: AppContainer, raw: String): String {
        val s = c.settings.current
        val ar = s.arabic
        val t = raw.trim().lowercase()
        fun msg(en: String, a: String) = if (ar) a else en

        // Navigation
        navPrefixes.firstOrNull { t.startsWith(it) }?.let { p ->
            val dest = t.removePrefix(p).trim()
            val target = when {
                homeWords.any { dest == it } && s.homeAddress.isNotBlank() -> s.homeAddress
                workWords.any { dest == it } && s.workAddress.isNotBlank() -> s.workAddress
                else -> dest
            }
            val navPkg = s.navPackage ?: c.apps.guess(AppsRepository.NAV_PKGS, AppsRepository.NAV_KEYS)?.packageName
            return if (c.controls.navigateTo(target, navPkg)) msg("Navigating to $target", "جارٍ التوجيه إلى $target")
            else msg("No navigation app found", "لا يوجد تطبيق ملاحة")
        }

        // CarPlay
        if (listOf("carplay", "car play", "كاربلاي", "كار بلاي", "zlink").any { t.contains(it) }) {
            val pkg = s.carPlayPackage
                ?: c.apps.guess(AppsRepository.CARPLAY_PKGS, AppsRepository.CARPLAY_KEYS)?.packageName
            return if (c.apps.launchPackage(pkg)) msg("Opening CarPlay", "فتح كاربلاي")
            else msg("CarPlay app not found", "تطبيق كاربلاي غير موجود")
        }

        // Open an app by name
        openPrefixes.firstOrNull { t.startsWith(it) }?.let { p ->
            val name = t.removePrefix(p).trim()
            val app = c.apps.search(name)
            return if (app != null && c.apps.launch(app)) msg("Opening ${app.label}", "فتح ${app.label}")
            else msg("Couldn't find \"$name\"", "لم أجد \"$name\"")
        }

        // Calls (dialer only; contact lookup by name would need contacts permission)
        if (t.startsWith("call") || t.startsWith("اتصل") || t.contains("الهاتف") || t == "phone") {
            val ok = c.apps.launchPackage(s.phonePackage) || c.controls.dial()
            return if (ok) msg("Opening phone", "فتح الهاتف") else msg("No phone app", "لا يوجد تطبيق هاتف")
        }

        // Media & volume
        when {
            listOf("next", "skip", "التالي", "التالية", "اللي بعده").any { t.contains(it) } -> { c.media.next(); return msg("Next track", "المقطع التالي") }
            listOf("previous", "back track", "السابق", "السابقة", "اللي قبله").any { t.contains(it) } -> { c.media.previous(); return msg("Previous track", "المقطع السابق") }
            listOf("pause", "stop music", "أوقف", "اوقف", "وقف", "إيقاف", "ايقاف").any { t.contains(it) } -> { c.media.pause(); return msg("Paused", "تم الإيقاف") }
            listOf("play", "resume", "music", "شغل", "تشغيل", "كمل").any { t.contains(it) } -> { c.media.play(); return msg("Playing", "تشغيل") }
            listOf("volume up", "louder", "ارفع الصوت", "علي الصوت", "ارفع").any { t.contains(it) } -> { repeat(2) { c.controls.volumeStep(true) }; return msg("Volume up", "رفع الصوت") }
            listOf("volume down", "quieter", "اخفض الصوت", "وطي الصوت", "اخفض", "وطي").any { t.contains(it) } -> { repeat(2) { c.controls.volumeStep(false) }; return msg("Volume down", "خفض الصوت") }
        }

        // Theme
        if (listOf("night mode", "dark mode", "الوضع الليلي", "الليلي").any { t.contains(it) }) {
            c.settings.update { it.copy(themeMode = ThemeMode.DARK) }; return msg("Night mode", "الوضع الليلي")
        }
        if (listOf("day mode", "light mode", "الوضع النهاري", "النهاري").any { t.contains(it) }) {
            c.settings.update { it.copy(themeMode = ThemeMode.LIGHT) }; return msg("Day mode", "الوضع النهاري")
        }

        // Last resort: maybe they just said an app name
        c.apps.search(t)?.let { app -> if (c.apps.launch(app)) return msg("Opening ${app.label}", "فتح ${app.label}") }
        return msg("Didn't catch that: \"$raw\"", "لم أفهم: \"$raw\"")
    }
}
