package com.abu00salman.novadrive.data

import android.view.Window
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.data.ai.NovaActions
import com.abu00salman.novadrive.core.ThemeMode
import com.abu00salman.novadrive.data.nav.LatLon
import com.abu00salman.novadrive.data.nav.Place
import com.abu00salman.novadrive.data.nav.RouteService
import com.abu00salman.novadrive.ui.nav.parseCoordinates
import com.abu00salman.novadrive.ui.nav.toNavInfo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A user-defined voice command: when the spoken text contains [phrase], open [packageName]. */
data class CustomCommand(val phrase: String, val packageName: String)

/**
 * Small, deterministic intent parser for Arabic and English car commands ("أمرني").
 * This is Nova AI's local command engine: it needs no API key or credit, and every command that
 * doesn't need data from the internet works offline. Speech-to-text and speaking the reply are done
 * by the voice session (data/voice).
 */
object VoiceCommands {

    private val navPrefixes = listOf(
        "navigate to ", "take me to ", "directions to ", "drive to ", "go to ",
        "خذني إلى ", "خذني الى ", "خذني ل", "وديني ", "ودني ", "اذهب إلى ", "اذهب الى ", "روح ", "الطريق إلى ", "الطريق الى ", "خذني ", "ودني على ", "وديني على "
    )
    private val openPrefixes = listOf("open ", "launch ", "start ", "افتح ", "افتحي ", "افتح لي ", "شغل تطبيق ", "تشغيل تطبيق ", "شغل ", "شغلي ")
    private val homeWords = listOf("home", "البيت", "المنزل", "بيتي")
    private val musicWords = listOf("الموسيقي", "موسيقي", "الاغاني", "اغنيه", "الاغنيه", "music", "song", "اغاني")
    private val workWords = listOf("work", "office", "الدوام", "العمل", "الشغل")

    /** Arabic (and casual) names → words that appear in the installed app's label or package. */
    private val appAliases = mapOf(
        "يوتيوب" to "youtube", "اليوتيوب" to "youtube",
        "الخرائط" to "maps", "خرائط" to "maps", "قوقل ماب" to "maps", "جوجل ماب" to "maps",
        "ويز" to "waze", "سبوتيفاي" to "spotify", "انغامي" to "anghami", "أنغامي" to "anghami",
        "واتساب" to "whatsapp", "الواتس" to "whatsapp", "نتفلكس" to "netflix", "نتفليكس" to "netflix",
        "كروم" to "chrome", "المتصفح" to "browser", "الكاميرا" to "camera", "المتجر" to "store",
        "سناب" to "snapchat", "تيك توك" to "tiktok", "شاهد" to "shahid", "الراديو" to "radio",
        "تلقرام" to "telegram", "تيليجرام" to "telegram", "انستقرام" to "instagram", "تويتر" to "twitter",
        "اكس" to "twitter", "ساوند كلاود" to "soundcloud", "ابل ميوزك" to "music", "الاستديو" to "gallery",
        "المعرض" to "gallery", "الصور" to "photos", "الملفات" to "files", "الحاسبه" to "calculator",
        "التقويم" to "calendar", "التسجيل" to "recorder", "الرسائل" to "messag", "البلوتوث" to "bluetooth",
    )

    /** Strips diacritics/tatweel and unifies alef/yaa/taa forms so "إفتح" and "افتح" match. */
    fun normalize(s: String): String = s.lowercase(Locale.ROOT).trim()
        .replace(Regex("[\\u064B-\\u0652\\u0640]"), "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ى', 'ي').replace('ة', 'ه')
        .replace(Regex("(من فضلك|لو سمحت|please)"), "").replace(Regex("\\s+"), " ").trim()

    /**
     * Runs a command that can be understood without AI and returns what happened, or null when
     * the sentence isn't a known command (Nova AI then takes over, when configured).
     */
    suspend fun handle(c: AppContainer, raw: String, window: Window? = null, driving: Boolean = false, aiReady: Boolean = false): String? {
        val s = c.settings.current
        val ar = s.arabic
        // "يا نوفا، افتح يوتيوب" → "افتح يوتيوب"
        val t = stripWake(normalize(raw))
        fun msg(en: String, a: String) = if (ar) a else en
        fun has(vararg words: String) = words.any { containsWord(t, normalize(it)) }

        // "افتح الخرائط وابحث عن محطة وقود": the selected AI does both (it has the same tools);
        // without AI each part runs as its own local command.
        val parts = splitCompound(t)
        if (parts.size > 1) {
            if (aiReady) return null
            val done = parts.mapNotNull { handle(c, it, window, driving, aiReady = false) }
            return if (done.isEmpty()) null else done.joinToString(if (ar) "، و" else ", ")
        }
        // A general question ("كم عاصمة اليابان؟", "اشرح لي…") is never mistaken for a car command.
        val question = isQuestion(t)
        if (question && aiReady && t.split(' ').size > 6) return null

        // User's own commands win.
        s.voiceCommands.firstOrNull { it.phrase.isNotBlank() && t.contains(normalize(it.phrase)) }?.let { cmd ->
            val app = c.apps.byPackage(cmd.packageName)
            return if (app != null && c.apps.launch(app)) msg("Opening ${app.label}", "تم، فتحت ${app.label}")
            else msg("That app isn't installed", "التطبيق غير مثبت")
        }

        fun emit(action: String) = c.voiceActions.tryEmit(action)

        // Small talk & help
        if (has("السلام عليكم", "مرحبا", "هلا", "اهلا", "hello", "hi nova", "hey nova")) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return if (ar) "${if (hour < 12) "صباح الخير" else "مساء الخير"}، أنا نوفا. كيف أقدر أساعدك؟"
            else "${if (hour < 12) "Good morning" else "Good evening"}, I'm Nova. How can I help?"
        }
        if (has("شكرا", "يعطيك العافيه", "thank")) return msg("You're welcome", "العفو، في الخدمة")
        if (has("من انت", "وش اسمك", "ما اسمك", "who are you", "your name")) return msg("I'm Nova, your car assistant", "أنا نوفا، مساعدك في السيارة")
        if (has("وش تقدر", "ماذا تستطيع", "ايش تسوي", "مساعده", "الاوامر", "what can you do", "help")) {
            return msg("Try: " + EXAMPLES_EN.take(6).joinToString(", "), "جرّب مثلاً: " + EXAMPLES_AR.take(6).joinToString("، "))
        }

        // Interface style & theme ("واجهة تسلا", "Mercedes interface", …)
        if (!question && has("واجهه", "interface", "theme", "شكل")) {
            val aliases = mapOf(
                "tesla" to listOf("تسلا", "tesla"), "bmw" to listOf("بي ام", "bmw", "بي ام دبليو"), "mercedes" to listOf("مرسيدس", "mercedes"),
                "ford" to listOf("فورد", "ford"), "apple" to listOf("ابل", "apple"), "jetour" to listOf("جيتور", "jetour"),
                "byd" to listOf("بي واي دي", "byd"), "haval" to listOf("هافال", "haval"), "tank" to listOf("تانك", "tank"),
                "classic" to listOf("كلاسيك", "الكلاسيكيه", "classic"),
            )
            aliases.entries.firstOrNull { (_, words) -> words.any { t.contains(normalize(it)) } }?.let { (id, _) ->
                c.settings.update { it.copy(uiStyle = id) }; emit("screen:HOME")
                val st = com.abu00salman.novadrive.core.UiStyles.get(id)
                return msg("${st.en} interface", "تم التبديل إلى ${st.ar}")
            }
        }
        if (!question && has("الواجهه البسيطه", "الشكل البسيط", "simple interface", "simple mode", "simple layout")) {
            c.settings.update { it.copy(uiStyle = "simple") }; emit("screen:HOME"); return msg("Simple interface", "تم التبديل إلى الواجهة البسيطة")
        }
        if (!question && has("الواجهه الذكيه", "الشكل الذكي", "smart interface", "smart mode", "smart layout")) {
            c.settings.update { it.copy(uiStyle = "smart") }; emit("screen:HOME"); return msg("Smart interface", "تم التبديل إلى الواجهة الذكية")
        }
        if (!question && has("الوضع التلقائي", "auto theme", "automatic theme")) {
            c.settings.update { it.copy(themeMode = ThemeMode.AUTO) }; return msg("Automatic theme", "الوضع التلقائي حسب الشروق والغروب")
        }

        // Home layouts and NovaDrive screens
        val show = !question && has("اعرض", "ورني", "وريني", "اظهر", "وضع", "شاشه", "show", "mode")
        if (!question && has("قسم الشاشه", "تقسيم الشاشه", "نصف الشاشه", "split")) { emit("mode:SPLIT"); return msg("Split view", "تم تقسيم الشاشة") }
        if (show && has("الملاحه", "الخريطه", "الخرائط", "map", "navigation")) { emit("mode:NAVIGATION"); return msg("Showing the map", "هذه الخريطة") }
        if (show && has("الموسيقي", "الاغاني", "music", "media")) { emit("mode:MUSIC"); return msg("Showing music", "هذه الموسيقى") }
        if (!question && has("الشاشه الرئيسيه", "الرئيسيه", "الصفحه الرئيسيه", "ارجع للرئيسيه", "home screen", "go home")) { emit("mode:CAR"); return msg("Home", "الشاشة الرئيسية") }
        if (!question && has("معلومات السياره", "حاله السياره", "vehicle", "car info")) { emit("screen:VEHICLE"); return msg("Vehicle", "معلومات السيارة") }
        if (!question && has("اعدادات نوفا", "الاعدادات", "settings")) { emit("screen:SETTINGS"); return msg("Settings", "الإعدادات") }
        if (!question && has("كل التطبيقات", "قائمه التطبيقات", "التطبيقات", "all apps", "app list")) { emit("screen:APPS"); return msg("All apps", "كل التطبيقات") }
        if (!question && has("الاختصارات", "التحكم السريع", "quick settings", "control center")) { emit("quick"); return msg("Quick controls", "التحكم السريع") }

        // Connectivity
        if (!question && has("البلوتوث", "bluetooth")) { c.controls.openBluetooth(); return msg("Bluetooth settings", "إعدادات البلوتوث") }
        if (!question && has("الواي فاي", "الوايفاي", "wifi", "wi-fi")) { c.controls.openWifi(); return msg("Wi-Fi", "الواي فاي") }

        // Ending an in-app trip
        if (c.navEngine.state.value != null && has("انه الملاحه", "انهاء الملاحه", "الغ الملاحه", "الغاء الملاحه", "اوقف الملاحه", "وقف الملاحه", "stop navigation", "end navigation", "cancel route")) {
            c.navEngine.stop(); return msg("Navigation ended", "تم إنهاء الملاحة")
        }

        // Route questions — answered from the in-app trip or the navigation app's own notification
        val nav = c.navEngine.state.value?.toNavInfo(ar, s.use24h) ?: NovaNotificationListener.navigation.value
        if (has("متي اوصل", "كم باقي", "الوقت المتبقي", "المسافه المتبقيه", "وقت الوصول", "eta", "how long", "when will i arrive", "how far")) {
            if (nav == null) return msg("No active route", "لا يوجد توجيه نشط الآن")
            return nav.eta?.let { msg("Remaining: $it", "المتبقي: $it") } ?: msg("Next: ${nav.primary}", "التالي: ${nav.primary}")
        }
        if (has("الالتفاف", "المنعطف", "الاتجاه التالي", "وين الف", "next turn")) {
            if (nav == null) return msg("No active route", "لا يوجد توجيه نشط الآن")
            return listOfNotNull(nav.primary, nav.secondary).joinToString(" — ")
        }
        if (has("وين انا", "اين انا", "موقعي", "where am i")) {
            val city = c.weather.city.value ?: return msg("Location isn't known yet", "الموقع غير معروف بعد")
            return msg("You're in $city", "أنت في $city")
        }

        // What's playing
        if (has("وش الاغنيه", "ايش الاغنيه", "اسم الاغنيه", "وش يشتغل", "ايش يشتغل", "what's playing", "what is playing", "what song")) {
            val np = c.media.nowPlaying() ?: return msg("Nothing is playing", "لا يوجد تشغيل حالياً")
            return np.second?.let { msg("${np.first} by $it", "${np.first} — $it") } ?: np.first
        }

        // Volume level / mute
        if (!question && has("اكتم", "كتم الصوت", "اسكت", "mute")) { c.controls.setVolume(0f); return msg("Muted", "تم كتم الصوت") }
        if (has("الصوت كم", "كم الصوت", "مستوي الصوت", "volume level")) return msg("Volume ${(c.controls.volume() * 100).toInt()}%", "مستوى الصوت ${(c.controls.volume() * 100).toInt()}٪")
        if (!question && has("الصوت علي الاخر", "اقصي صوت", "max volume")) { c.controls.setVolume(1f); return msg("Volume 100%", "الصوت ١٠٠٪") }

        // Brightness extremes
        if (!question && has("اقصي اضاءه", "اضاءه كامله", "اعلي اضاءه", "full brightness", "max brightness")) {
            c.controls.setBrightness(1f, window); return msg("Brightness 100%", "السطوع ١٠٠٪")
        }
        if (!question && has("اقل اضاءه", "اضاءه خفيفه", "minimum brightness")) {
            c.controls.setBrightness(0.05f, window); return msg("Brightness 5%", "السطوع ٥٪")
        }

        // Time, date, weather, speed
        if (has("كم الساعه", "الساعه كم", "وش الساعه", "what time") || t == "الوقت" || t == "الساعه" || t == "the time") {
            val fmt = SimpleDateFormat(if (s.use24h) "HH:mm" else "h:mm", if (ar) Locale.forLanguageTag("ar-SA-u-nu-latn") else Locale.US)
            val pm = Calendar.getInstance().get(Calendar.AM_PM) == Calendar.PM
            val now = fmt.format(Date())
            return if (ar) "الساعة الآن $now ${if (s.use24h) "" else if (pm) "مساءً" else "صباحاً"}".trim() else "It's $now${if (s.use24h) "" else if (pm) " PM" else " AM"}"
        }
        if (has("تاريخ اليوم", "اليوم كم", "وش اليوم", "اي يوم اليوم", "what day is it", "today's date") || t == "التاريخ" || t == "the date") {
            val d = SimpleDateFormat(if (ar) "EEEE d MMMM" else "EEEE, d MMMM", if (ar) Locale.forLanguageTag("ar-SA-u-nu-latn") else Locale.US).format(Date())
            return msg("Today is $d", "اليوم $d")
        }
        if (has("الطقس", "الحراره", "كيف الجو", "الجو اليوم", "weather", "temperature") && t.split(' ').size <= 5) {
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
        if (!question && (dimmer || brighter)) {
            val next = (c.controls.brightness() + if (brighter) 0.25f else -0.25f).coerceIn(0.05f, 1f)
            c.controls.setBrightness(next, window)
            return msg("Brightness ${(next * 100).toInt()}%", "السطوع ${(next * 100).toInt()}٪")
        }

        // Nearest fuel station / mosque / … ("ودّني لأقرب محطة بنزين")
        if (has("اقرب", "nearest", "closest", "ابحث عن", "دور علي", "دورلي", "find a", "search for")) {
            NovaActions.nearbyKinds.entries.firstOrNull { (_, words) -> words.any { t.contains(normalize(it)) } }?.let { (kind, _) ->
                return NovaActions.navigateNearby(c, kind, ar).message
            }
        }

        // Headlines
        if (has("الاخبار", "اخبار", "news", "headlines")) {
            val h = NovaActions.headlines(ar)
            return if (h.isEmpty()) msg("Couldn't load the news (check the connection)", "تعذّر جلب الأخبار (تحقق من الإنترنت)")
            else msg("Top headlines: ", "أبرز الأخبار: ") + h.take(3).joinToString(if (ar) "، ثم " else "; ")
        }

        // Everything below performs an action; a question never triggers one.
        if (question) return null

        // The interface's big shortcut
        if (has("الايقونه الكبيره", "الاختصار الكبير", "big shortcut")) { emit("big"); return msg("Opening the big shortcut", "فتحت الاختصار الكبير") }

        // Navigation
        navPrefixes.firstOrNull { t.startsWith(normalize(it) + " ") || t.startsWith(normalize(it)) }?.let { p ->
            val dest = t.removePrefix(normalize(p)).trim()
            if (dest.isEmpty()) return@let
            val target = when {
                homeWords.any { dest == normalize(it) } && s.homeAddress.isNotBlank() -> s.homeAddress
                workWords.any { dest == normalize(it) } && s.workAddress.isNotBlank() -> s.workAddress
                else -> dest
            }
            // Routed inside NovaDrive.
            return NovaActions.navigateTo(c, target, ar).message
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
            if (name.isEmpty() || musicWords.any { name == normalize(it) }) { c.media.play(); return msg("Playing", "تشغيل الموسيقى") }
            if (NovaActions.findApp(c, name) != null) return NovaActions.openApp(c, name, ar, driving).message
            // "شغّل سورة البقرة" — not an app: ask the music player to find and play it.
            if (p.trim() in listOf("شغل", "شغلي", "start")) return NovaActions.playFromSearch(c, name, ar).message
            return null
        }

        // "اتصل بأحمد" → contact's number in the dialer (the user confirms by pressing Call).
        Regex("^(?:اتصل ب|اتصل على |دق على |call )(.+)$").find(t)?.let { m ->
            val who = m.groupValues[1].trim()
            if (who.isNotEmpty() && who !in listOf("جهه اتصال", "a contact")) return NovaActions.dialContact(c, who, ar).message
        }
        // Calls (the unit's dialer)
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
        if (t.split(' ').size <= 3) (appAliases.entries.firstOrNull { normalize(it.key) == t }?.value ?: t).let { key ->
            c.apps.search(key)?.let { app -> if (c.apps.launch(app)) return msg("Opening ${app.label}", "تم، فتحت ${app.label}") }
        }
        return null
    }

    private val wake = Regex("^(يا نوفا|ي نوفا|نوفا|hey nova|ok nova|nova)[ ،,]*")
    fun stripWake(t: String) = t.replace(wake, "").trim()

    /**
     * Whole-word match, so "وقف" doesn't fire inside "موقف" or "الجو" inside "الجوال". Arabic
     * attached prefixes (و ب ل ف ك ال …) are allowed before the word.
     */
    fun containsWord(text: String, phrase: String): Boolean {
        if (phrase.isBlank()) return false
        val p = Regex.escape(phrase)
        return Regex("(^|[\\s،,.؟?!])(?:و|ف|ب|ل|ك|ال|وال|بال|فال|لل|كال)?$p($|[\\s،,.؟?!])").containsMatchIn(text)
    }

    private val questionStarts = listOf(
        "كم ", "ليش", "لماذا", "ليه ", "كيف", "وش ", "ايش", "شو ", "ماذا", "ما هو", "ما هي", "ماهو", "ماهي", "ما ", "متي", "وين", "اين", "هل ",
        "من هو", "من هي", "مين ", "اشرح", "احكي", "حدثني", "كلمني عن", "عرفني", "قل لي", "قولي", "علمني", "اعطني معلومات", "ابي اعرف", "ودي اعرف", "ابغي اعرف",
        "what", "why", "how", "who", "when", "where", "which", "explain", "tell me", "is ", "are ", "do ", "does ", "can you", "could you",
    )

    /** "كم عاصمة اليابان؟", "اشرح لي…", "what is…" — a question rather than a command. */
    fun isQuestion(t: String): Boolean = t.endsWith("؟") || t.endsWith("?") || questionStarts.any { t.startsWith(it) }

    private val compoundVerbs = "افتح|شغل|ابحث|دور|خذني|ودني|وديني|اتصل|ارفع|اخفض|قلل|زد|اعرض|اوقف|وقف|كمل"

    /** Splits "افتح الخرائط وابحث عن محطة" / "… ثم …" / "… and then …" into separate commands. */
    fun splitCompound(t: String): List<String> =
        t.split(Regex("\\s+(?:ثم|وبعدين|بعدها|and then|then)\\s+|\\s+و(?=(?:$compoundVerbs)(?:\\s|$))|\\s+and (?=(?:open|play|find|search|navigate|call)(?:\\s|$))"))
            .map { it.trim() }.filter { it.isNotEmpty() }

    /** Examples shown in Settings › Voice. */
    val EXAMPLES_AR = listOf(
        "يا نوفا، افتح يوتيوب", "ودّني لأقرب محطة بنزين", "شغّل سورة البقرة", "وش آخر الأخبار", "اتصل بأحمد", "واجهة كلاسيك",
        "افتح اليوتيوب", "خذني إلى البيت", "كم الساعة الآن", "قلل الإضاءة", "متى أوصل", "وش الأغنية",
        "افتح الخرائط", "اعرض الخريطة", "اعرض الموسيقى", "قسم الشاشة", "الواجهة البسيطة", "الواجهة الذكية",
        "كم الحرارة", "وين أنا", "كم سرعتي", "التالي", "ارفع الصوت", "اكتم الصوت", "أقصى إضاءة",
        "افتح كاربلاي", "اتصل", "افتح البلوتوث", "الإعدادات", "معلومات السيارة", "الوضع الليلي", "الوضع التلقائي",
    )
    val EXAMPLES_EN = listOf(
        "Open YouTube", "Take me home", "What time is it", "Dim the screen", "How long", "What's playing",
        "Open maps", "Show the map", "Show music", "Split screen", "Simple interface", "Smart interface",
        "Weather", "Where am I", "My speed", "Next", "Volume up", "Mute", "Full brightness",
        "Open CarPlay", "Call", "Bluetooth", "Settings", "Vehicle info", "Night mode", "Auto theme",
    )
}
