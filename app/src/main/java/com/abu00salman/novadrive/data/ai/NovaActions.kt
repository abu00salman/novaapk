package com.abu00salman.novadrive.data.ai

import android.Manifest
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.AppsRepository
import com.abu00salman.novadrive.data.nav.Geo
import com.abu00salman.novadrive.data.nav.LatLon
import com.abu00salman.novadrive.data.nav.Place
import com.abu00salman.novadrive.data.nav.RouteService
import com.abu00salman.novadrive.ui.nav.parseCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Outcome of a real action: what happened, in the user's language, and whether it worked. */
data class ActionResult(val ok: Boolean, val message: String)

/**
 * The real things Nova can do on this unit, shared by the local command parser and the AI tools.
 * Each one performs the action (or fails) and reports exactly what happened.
 */
object NovaActions {

    /** Arabic / casual names → words found in the installed app's label or package. */
    val appAliases = mapOf(
        "يوتيوب" to "youtube", "اليوتيوب" to "youtube", "الخرائط" to "maps", "خرائط" to "maps", "قوقل ماب" to "maps", "جوجل ماب" to "maps",
        "ويز" to "waze", "سبوتيفاي" to "spotify", "انغامي" to "anghami", "أنغامي" to "anghami", "واتساب" to "whatsapp", "الواتس" to "whatsapp",
        "نتفلكس" to "netflix", "نتفليكس" to "netflix", "كروم" to "chrome", "المتصفح" to "browser", "الكاميرا" to "camera", "المتجر" to "store",
        "سناب" to "snapchat", "تيك توك" to "tiktok", "شاهد" to "shahid", "الراديو" to "radio", "تلقرام" to "telegram", "تيليجرام" to "telegram",
        "انستقرام" to "instagram", "تويتر" to "twitter", "اكس" to "twitter", "ابل ميوزك" to "music", "الصور" to "photos", "الملفات" to "files",
        "الحاسبه" to "calculator", "التقويم" to "calendar", "الرسائل" to "messag", "شات جي بي تي" to "chatgpt", "تشات جي بي تي" to "chatgpt",
        "كلود" to "claude", "كاربلاي" to "zlink",
    )

    fun findApp(c: AppContainer, name: String): AppEntry? {
        val n = name.trim().lowercase(Locale.ROOT)
        if (n.isEmpty()) return null
        val key = appAliases.entries.firstOrNull { it.key == n }?.value ?: n.removePrefix("ال")
        if (key == "maps") return c.apps.byPackage(c.settings.current.navPackage) ?: c.apps.guess(AppsRepository.NAV_PKGS, AppsRepository.NAV_KEYS)
        return c.apps.search(key) ?: c.apps.search(n)
    }

    fun openApp(c: AppContainer, name: String, ar: Boolean, driving: Boolean): ActionResult {
        val app = findApp(c, name) ?: return ActionResult(false, if (ar) "لم أجد تطبيق \"$name\"" else "No app called \"$name\"")
        if (driving && com.abu00salman.novadrive.core.isVideoApp(app.packageName, app.label))
            return ActionResult(false, if (ar) "الفيديو متاح عند التوقف" else "Video is available when parked")
        return if (c.apps.launch(app)) ActionResult(true, if (ar) "فتحت ${app.label}" else "Opened ${app.label}")
        else ActionResult(false, if (ar) "تعذّر فتح ${app.label}" else "Couldn't open ${app.label}")
    }

    /**
     * "شغّل سورة البقرة": asks the playing (or preferred) music app to find and play it — through
     * the media session's playFromSearch, else Android's MEDIA_PLAY_FROM_SEARCH intent.
     */
    fun playFromSearch(c: AppContainer, query: String, ar: Boolean): ActionResult {
        if (c.media.playFromSearch(query)) return ActionResult(true, if (ar) "طلبت تشغيل \"$query\"" else "Asked the player for \"$query\"")
        val pkg = c.settings.current.musicPackage ?: c.apps.guess(AppsRepository.MUSIC_PKGS, emptyList())?.packageName
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            .putExtra(SearchManager.QUERY, query).putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .apply { if (pkg != null) setPackage(pkg) }
        return if (runCatching { c.appContext.startActivity(intent) }.isSuccess) ActionResult(true, if (ar) "أبحث عن \"$query\" وأشغّله" else "Searching and playing \"$query\"")
        else ActionResult(false, if (ar) "لا يوجد تطبيق يدعم التشغيل بالبحث" else "No player supports play-by-search")
    }

    suspend fun navigateTo(c: AppContainer, query: String, ar: Boolean): ActionResult {
        val s = c.settings.current
        val target = when (query.trim().lowercase(Locale.ROOT)) {
            "home", "البيت", "المنزل", "بيتي" -> s.homeAddress.ifBlank { query }
            "work", "العمل", "الدوام", "الشغل" -> s.workAddress.ifBlank { query }
            else -> query
        }
        val near = c.fix.value?.let { LatLon(it.lat, it.lon) }
        val place = parseCoordinates(target)?.let { Place(target, target, it) } ?: RouteService.search(target, near, ar).firstOrNull()
            ?: return ActionResult(false, if (ar) "لم أجد \"$target\" (تحقق من الإنترنت)" else "Couldn't find \"$target\" (check the connection)")
        c.navEngine.start(place)
        c.voiceActions.tryEmit("screen:NAV")
        return ActionResult(true, (if (ar) "بدأت الملاحة إلى " else "Navigating to ") + place.name +
            (near?.let { " (" + Geo.formatDistance(Geo.distance(it, place.point), ar) + ")" } ?: ""))
    }

    /** OpenStreetMap categories the assistant can find nearby. */
    val nearbyKinds = mapOf(
        "fuel" to listOf("بنزين", "محطه", "وقود", "fuel", "gas"), "mosque" to listOf("مسجد", "جامع", "mosque"),
        "restaurant" to listOf("مطعم", "اكل", "restaurant"), "cafe" to listOf("كافيه", "قهوه", "مقهى", "cafe", "coffee"),
        "hospital" to listOf("مستشفى", "مستشفي", "hospital"), "pharmacy" to listOf("صيدليه", "pharmacy"),
        "parking" to listOf("موقف", "مواقف", "parking"), "atm" to listOf("صراف", "atm"), "supermarket" to listOf("سوبرماركت", "بقاله", "supermarket"),
    )

    /** Nearest place of a kind (Overpass/OpenStreetMap within 10 km), then starts the route. */
    suspend fun navigateNearby(c: AppContainer, kind: String, ar: Boolean): ActionResult {
        val here = c.fix.value?.let { LatLon(it.lat, it.lon) } ?: return ActionResult(false, if (ar) "لا يوجد موقع GPS بعد" else "No GPS position yet")
        val tag = if (kind in setOf("supermarket")) "shop" else "amenity"
        val place = withContext(Dispatchers.IO) {
            runCatching {
                val q = String.format(Locale.US, "[out:json][timeout:15];nwr[%s=%s](around:10000,%.5f,%.5f);out center 20;", tag, kind, here.lat, here.lon)
                val conn = (URL("https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(q, "UTF-8")).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000; readTimeout = 20_000
                    setRequestProperty("User-Agent", "NovaDrive/2.2 (Android car launcher)")
                }
                val body = try { conn.inputStream.bufferedReader().use { it.readText() } } finally { conn.disconnect() }
                val els = JSONObject(body).getJSONArray("elements")
                (0 until els.length()).mapNotNull { i ->
                    val e = els.getJSONObject(i)
                    val lat = if (e.has("lat")) e.getDouble("lat") else e.optJSONObject("center")?.optDouble("lat") ?: return@mapNotNull null
                    val lon = if (e.has("lon")) e.getDouble("lon") else e.optJSONObject("center")?.optDouble("lon") ?: return@mapNotNull null
                    val tags = e.optJSONObject("tags")
                    val name = tags?.optString(if (ar) "name:ar" else "name:en")?.ifBlank { null } ?: tags?.optString("name")?.ifBlank { null } ?: kind
                    Place(name, name, LatLon(lat, lon))
                }.minByOrNull { Geo.distance(here, it.point) }
            }.getOrNull()
        } ?: return ActionResult(false, if (ar) "لم أجد مكانًا قريبًا (تحقق من الإنترنت)" else "Nothing found nearby (check the connection)")
        c.navEngine.start(place)
        c.voiceActions.tryEmit("screen:NAV")
        return ActionResult(true, (if (ar) "أقرب مكان: " else "Nearest: ") + place.name + " (" + Geo.formatDistance(Geo.distance(here, place.point), ar) + ")")
    }

    /** Latest headlines from Google News (Saudi Arabia edition in Arabic). */
    suspend fun headlines(ar: Boolean, topic: String? = null): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val base = if (topic.isNullOrBlank()) "https://news.google.com/rss" else "https://news.google.com/rss/search?q=" + URLEncoder.encode(topic, "UTF-8") + "&"
            val url = base + (if (topic.isNullOrBlank()) "?" else "") + if (ar) "hl=ar&gl=SA&ceid=SA:ar" else "hl=en&gl=SA&ceid=SA:en"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 15_000 }
            val xml = try { conn.inputStream.bufferedReader().use { it.readText() } } finally { conn.disconnect() }
            Regex("<item>.*?<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL).findAll(xml).map {
                it.groupValues[1].replace("<![CDATA[", "").replace("]]>", "").replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").trim()
            }.take(5).toList()
        }.getOrDefault(emptyList())
    }

    fun hasContactsPermission(c: AppContainer) =
        ContextCompat.checkSelfPermission(c.appContext, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /**
     * Opens the dialer with the contact's number filled in. The user still presses Call —
     * NovaDrive never places a call by itself.
     */
    fun dialContact(c: AppContainer, name: String, ar: Boolean): ActionResult {
        if (!hasContactsPermission(c)) {
            c.voiceActions.tryEmit("perm:contacts")
            return ActionResult(false, if (ar) "أحتاج إذن جهات الاتصال — وافق عليه ثم أعد الطلب" else "Contacts permission needed — allow it, then ask again")
        }
        val found = runCatching {
            c.appContext.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?", arrayOf("%${name.trim()}%"), null,
            )?.use { cur -> if (cur.moveToFirst()) cur.getString(0) to cur.getString(1) else null }
        }.getOrNull() ?: return ActionResult(false, if (ar) "لم أجد \"$name\" في جهات الاتصال" else "No contact named \"$name\"")
        val ok = runCatching {
            c.appContext.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(found.second))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        return if (ok) ActionResult(true, if (ar) "فتحت الاتصال بـ${found.first} — اضغط اتصال للتأكيد" else "Dialer ready for ${found.first} — press Call to confirm")
        else ActionResult(false, if (ar) "تعذّر فتح الهاتف" else "Couldn't open the phone app")
    }
}
