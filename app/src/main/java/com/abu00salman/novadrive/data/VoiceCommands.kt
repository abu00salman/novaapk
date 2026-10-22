package com.abu00salman.novadrive.data

import android.view.Window
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.ThemeMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A user-defined voice command: when the spoken text contains [phrase], open [packageName]. */
data class CustomCommand(val phrase: String, val packageName: String)

/**
 * Small, deterministic intent parser for Arabic and English car commands ("أمرني").
 * Speech-to-text is done by the device's installed recognizer (RecognizerIntent); the reply is
 * returned as text and spoken by the caller with Android's text-to-speech.
 */
object VoiceCommands {

    private val navPrefixes = listOf(
        "navigate to ", "take me to ", "directions to ", "drive to ", "go to ",
        "خذني إلى ", "خذني الى ", "خذني ل", "وديني ", "ودني ", "اذهب إلى ", "اذهب الى ", "روح ", "الطريق إلى ", "الطريق الى "
    )
    private val openPrefixes = listOf("open ", "launch ", "start ", "افتح ", "افتحي ", "شغل تطبيق ", "تشغيل تطبيق ", "شغّل ")
    private val homeWords = listOf("home", "البيت", "المنزل", "بيتي")
    private val workWords = listOf("work", "office", "الدوام", "العمل", "الشغل")

    /** Arabic (and casual) names → words that appear in the installed app's label or package. */
    private val appAliases = mapOf(
        "يوتيوب" to "youtube", "اليوتيوب" to "youtube",
        "الخرائط" to "maps", "خرائط" to "maps", "قوقل ماب" to "maps", "جوجل ماب" to "maps",
        "ويز" to "waze", "سبوتيفاي" to "spotify", "انغامي" to "anghami", "أنغامي" to "anghami",
        "واتساب" to "whatsapp", "الواتس" to "whatsapp", "نتفلكس" to "netflix", "نتفليكس" to "netflix",
        "كروم" to "chrome", "المتصفح" to "browser", "الكاميرا" to "camera", "المتجر" to "store",
        "سناب" to "snapchat", "تيك توك" to "tiktok", "شاهد" to "shahid", "الراديو" to "radio",
    )

    /** Strips diacritics/tatweel and unifies alef/yaa/taa forms so "إفتح" and "افتح" match. */
    fun normalize(s: String): String = s.lowercase(Locale.ROOT).trim()
        .replace(Regex("[\\u064B-\\u0652\\u0640]"), "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ى', 'ي').replace('ة', 'ه')
        .replace(Regex("(من فضلك|لو سمحت|please)"), "").replace(Regex("\\s+"), " ").trim()

    fun handle(c: AppContainer, raw: String, window: Window? = null): String {
        val s = c.settings.current
        val ar = s.arabic
        val t = normalize(raw)
        fun msg(en: String, a: String) = if (ar) a else en
        fun has(vararg words: String) = words.any { t.contains(normalize(it)) }

        // User's own commands win.
        s.voiceCommands.firstOrNull { it.phrase.isNotBlank() && t.contains(normalize(it.phrase)) }?.let { cmd ->
            val app = c.apps.byPackage(cmd.packageName)
            return if (app != null && c.apps.launch(app)) msg("Opening ${app.label}", "تم، فتحت ${app.label}")
            else msg("That app isn't installed", "التطبيق غير مثبت")
        }

        // Time, date, weather, speed
        if (has("كم الساعه", "الساعه كم", "الوقت", "what time", "the time")) {
            val fmt = SimpleDateFormat(if (s.use24h) "HH:mm" else "h:mm", if (ar) Locale.forLanguageTag("ar-SA-u-nu-latn") else Locale.US)
            val pm = Calendar.getInstance().get(Calendar.AM_PM) == Calendar.PM
            val now = fmt.format(Date())
            return if (ar) "الساعة الآن $now ${if (s.use24h) "" else if (pm) "مساءً" else "صباحاً"}".trim() else "It's $now${if (s.use24h) "" else if (pm) " PM" else " AM"}"
        }
        if (has("التاريخ", "اليوم كم", "وش اليوم", "اي يوم", "what day", "the date")) {
            val d = SimpleDateFormat(if (ar) "EEEE d MMMM" else "EEEE, d MMMM", if (ar) Locale.forLanguageTag("ar-SA-u-nu-latn") else Locale.US).format(Date())
            return msg("Today is $d", "اليوم $d")
        }
        if (has("الطقس", "الحراره", "الجو", "weather", "temperature")) {
            val w = c.weather.weather.value ?: return msg("Weather isn't available yet", "الطقس غير متاح حالياً")
            val label = WeatherRepository.label(w.code, ar)
            return msg("It's ${Math.round(w.tempC)} degrees, $label", "الحرارة ${Math.round(w.tempC)} درجة، $label")
        }
        if (has("سرعتي", "كم السرعه", "my speed", "how fast")) {
            val v = c.fix.value?.speedKmh?.toInt() ?: return msg("No GPS speed yet", "لا توجد سرعة من GPS بعد")
            return msg("$v kilometres per hour", "$v كيلومتر في الساعة")
        }

        // Brightness
        val dimmer = has("قلل الاضاءه", "خفف الاضاءه", "اخفض الاضاءه", "قلل السطوع", "خفف السطوع", "dim", "darker", "brightness down")
        val brighter = has("زد الاضاءه", "ارفع الاضاءه", "زود الاضاءه", "ارفع السطوع", "زد السطوع", "brighter", "brightness up")
        if (dimmer || brighter) {
            val next = (c.controls.brightness() + if (brighter) 0.25f else -0.25f).coerceIn(0.05f, 1f)
            c.controls.setBrightness(next, window)
            return msg("Brightness ${(next * 100).toInt()}%", "السطوع ${(next * 100).toInt()}٪")
        }

        // Navigation
        navPrefixes.firstOrNull { t.startsWith(normalize(it) + " ") || t.startsWith(normalize(it)) }?.let { p ->
            val dest = t.removePrefix(normalize(p)).trim()
            if (dest.isEmpty()) return@let
            val target = when {
                homeWords.any { dest == normalize(it) } && s.homeAddress.isNotBlank() -> s.homeAddress
                workWords.any { dest == normalize(it) } && s.workAddress.isNotBlank() -> s.workAddress
                else -> dest
            }
            val navPkg = s.navPackage ?: c.apps.guess(AppsRepository.NAV_PKGS, AppsRepository.NAV_KEYS)?.packageName
            return if (c.controls.navigateTo(target, navPkg)) msg("Navigating to $target", "جارٍ التوجيه إلى $target")
            else msg("No navigation app found", "لا يوجد تطبيق ملاحة")
        }

        // CarPlay
        if (has("carplay", "car play", "كاربلاي", "كار بلاي", "zlink")) {
            val pkg = s.carPlayPackage ?: c.apps.guess(AppsRepository.CARPLAY_PKGS, AppsRepository.CARPLAY_KEYS)?.packageName
            return if (c.apps.launchPackage(pkg)) msg("Opening CarPlay", "تم، فتحت كاربلاي")
            else msg("CarPlay app not found", "تطبيق كاربلاي غير موجود")
        }

        // Open an app by name (Arabic names mapped to the app's own label)
        openPrefixes.firstOrNull { t.startsWith(normalize(it)) }?.let { p ->
            val name = t.removePrefix(normalize(p)).trim()
            if (name in listOf("الخرائط", "خرائط", "الملاحه", "maps")) {
                val nav = c.apps.byPackage(s.navPackage) ?: c.apps.guess(AppsRepository.NAV_PKGS, AppsRepository.NAV_KEYS)
                if (nav != null && c.apps.launch(nav)) return msg("Opening ${nav.label}", "تم، فتحت الخرائط")
            }
            val key = appAliases.entries.firstOrNull { normalize(it.key) == name }?.value ?: name.removePrefix("ال")
            val app = c.apps.search(key) ?: c.apps.search(name)
            return if (app != null && c.apps.launch(app)) msg("Opening ${app.label}", "تم، فتحت ${app.label}")
            else msg("Couldn't find \"$name\"", "لم أجد تطبيق \"$name\"")
        }

        // Calls (dialer only; contact lookup by name would need contacts permission)
        if (t.startsWith("call") || t.startsWith("اتصل") || has("الهاتف", "التلفون") || t == "phone") {
            val ok = c.apps.launchPackage(s.phonePackage) || c.controls.dial()
            return if (ok) msg("Opening phone", "تم، فتحت الهاتف") else msg("No phone app", "لا يوجد تطبيق هاتف")
        }

        // Media & volume
        when {
            has("next", "skip", "التالي", "التاليه", "اللي بعده") -> { c.media.next(); return msg("Next track", "المقطع التالي") }
            has("previous", "back track", "السابق", "السابقه", "اللي قبله") -> { c.media.previous(); return msg("Previous track", "المقطع السابق") }
            has("pause", "stop music", "اوقف", "وقف", "ايقاف") -> { c.media.pause(); return msg("Paused", "تم الإيقاف") }
            has("volume up", "louder", "ارفع الصوت", "علي الصوت", "زد الصوت") -> { repeat(2) { c.controls.volumeStep(true) }; return msg("Volume up", "رفعت الصوت") }
            has("volume down", "quieter", "اخفض الصوت", "وطي الصوت", "قلل الصوت") -> { repeat(2) { c.controls.volumeStep(false) }; return msg("Volume down", "خفضت الصوت") }
            has("play", "resume", "music", "شغل", "تشغيل", "كمل", "الموسيقي", "اغنيه") -> { c.media.play(); return msg("Playing", "تشغيل") }
        }

        // Theme
        if (has("night mode", "dark mode", "الوضع الليلي", "الليلي")) {
            c.settings.update { it.copy(themeMode = ThemeMode.DARK) }; return msg("Night mode", "الوضع الليلي")
        }
        if (has("day mode", "light mode", "الوضع النهاري", "النهاري")) {
            c.settings.update { it.copy(themeMode = ThemeMode.LIGHT) }; return msg("Day mode", "الوضع النهاري")
        }

        // Last resort: maybe they just said an app name
        (appAliases.entries.firstOrNull { normalize(it.key) == t }?.value ?: t).let { key ->
            c.apps.search(key)?.let { app -> if (c.apps.launch(app)) return msg("Opening ${app.label}", "تم، فتحت ${app.label}") }
        }
        return msg("Sorry, I didn't catch that: \"$raw\"", "عذراً، لم أفهم: \"$raw\"")
    }

    /** Examples shown in Settings › Voice. */
    val EXAMPLES_AR = listOf("افتح اليوتيوب", "افتح الخرائط", "خذني إلى البيت", "قلل الإضاءة", "زد الإضاءة", "كم الساعة الآن", "كم الحرارة", "التالي", "ارفع الصوت", "الوضع الليلي")
    val EXAMPLES_EN = listOf("Open YouTube", "Open maps", "Take me home", "Dim the screen", "Brighter", "What time is it", "Weather", "Next", "Volume up", "Night mode")
}
