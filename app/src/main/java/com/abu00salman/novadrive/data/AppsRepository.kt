package com.abu00salman.novadrive.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Immutable
data class AppEntry(
    val key: String,
    val packageName: String,
    val className: String,
    val label: String,
    val icon: ImageBitmap,
    val category: AppCategory = AppCategory.OTHER,
    /** Pre-installed and never updated by the user: hidden from the library unless asked for. */
    val system: Boolean = false,
)

enum class AppCategory { NAVIGATION, MEDIA, CAR, TOOLS, OTHER }

/**
 * Categorises by Android's own app category first (API 26+), then by well-known package names
 * and keywords. Car-box apps (ZLink, DiPlay, radio, BT phone) are never treated as "system".
 */
object AppClassifier {
    private val NAV = listOf("maps", "waze", "navi", "here", "sygic", "osmand", "خرائط", "ملاحة")
    private val MEDIA = listOf("music", "spotify", "anghami", "youtube", "netflix", "radio", "fm", "podcast", "video", "player", "iptv", "موسيقى", "راديو", "فيديو")
    private val CAR = listOf("zlink", "carplay", "diplay", "autokit", "carlink", "carbit", "gearhead", "android auto", "obd", "torque", "btphone", "bluetooth", "dvr", "camera", "avm", "tpms", "canbus", "سيارة", "كاربلاي")
    private val TOOLS = listOf("settings", "file", "calculator", "clock", "browser", "chrome", "store", "vending", "installer", "manager", "weather", "calendar", "إعدادات", "ملفات")

    fun classify(pkg: String, label: String, androidCategory: Int): AppCategory {
        val hay = "$pkg $label".lowercase()
        if (CAR.any { hay.contains(it) }) return AppCategory.CAR
        when (androidCategory) {
            android.content.pm.ApplicationInfo.CATEGORY_MAPS -> return AppCategory.NAVIGATION
            android.content.pm.ApplicationInfo.CATEGORY_AUDIO, android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> return AppCategory.MEDIA
            android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.TOOLS
        }
        return when {
            NAV.any { hay.contains(it) } -> AppCategory.NAVIGATION
            MEDIA.any { hay.contains(it) } -> AppCategory.MEDIA
            TOOLS.any { hay.contains(it) } -> AppCategory.TOOLS
            else -> AppCategory.OTHER
        }
    }

    /** Only plain pre-installed extras count as system; anything car- or media-related stays visible. */
    fun isHiddenSystem(systemFlag: Boolean, category: AppCategory): Boolean =
        systemFlag && category != AppCategory.CAR && category != AppCategory.MEDIA && category != AppCategory.NAVIGATION
}

class AppsRepository(private val ctx: Context, private val scope: CoroutineScope) {
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps
    private var job: Job? = null
    private val iconPx = 144

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refresh(debounce = true)
    }

    init {
        refresh()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    fun refresh(debounce: Boolean = false) {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            if (debounce) delay(400)
            _apps.value = load()
        }
    }

    private fun load(): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0)
        }
        return list.asSequence()
            .filter { it.activityInfo.packageName != ctx.packageName }
            .mapNotNull { ri ->
                runCatching {
                    val ai = ri.activityInfo
                    val label = ri.loadLabel(pm).toString()
                    val appInfo = ai.applicationInfo
                    val cat = AppClassifier.classify(ai.packageName, label, if (Build.VERSION.SDK_INT >= 26) appInfo.category else -1)
                    val sysFlag = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 &&
                        appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
                    AppEntry(
                        key = "${ai.packageName}/${ai.name}",
                        packageName = ai.packageName,
                        className = ai.name,
                        label = label,
                        icon = ri.loadIcon(pm).toBitmap(iconPx, iconPx).asImageBitmap(),
                        category = cat,
                        system = AppClassifier.isHiddenSystem(sysFlag, cat),
                    )
                }.getOrNull()
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun byKey(key: String): AppEntry? = _apps.value.firstOrNull { it.key == key }
    fun byPackage(pkg: String?): AppEntry? =
        pkg?.let { p -> _apps.value.firstOrNull { it.packageName == p } }

    /** Best match by known package names, then by keywords in package or label. */
    /**
     * The head unit's own phone app: whatever handles ACTION_DIAL (on car units this is the
     * built-in Bluetooth phone). Ignores the system chooser when several apps qualify.
     */
    fun dialer(): AppEntry? = runCatching {
        val ri = ctx.packageManager.resolveActivity(Intent(Intent.ACTION_DIAL), PackageManager.MATCH_DEFAULT_ONLY)
        val pkg = ri?.activityInfo?.packageName
        if (pkg == null || pkg == "android" || pkg.contains("resolver")) null else byPackage(pkg)
    }.getOrNull()

    fun guess(packages: List<String>, keywords: List<String>): AppEntry? {
        val all = _apps.value
        packages.forEach { p -> all.firstOrNull { it.packageName == p }?.let { return it } }
        return all.firstOrNull { a ->
            val hay = (a.packageName + " " + a.label).lowercase()
            keywords.any { hay.contains(it) }
        }
    }

    fun search(query: String): AppEntry? {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null
        val all = _apps.value
        return all.firstOrNull { it.label.lowercase() == q }
            ?: all.firstOrNull { it.label.lowercase().startsWith(q) }
            ?: all.firstOrNull { it.label.lowercase().contains(q) }
            ?: all.firstOrNull { it.packageName.lowercase().contains(q.replace(" ", "")) }
    }

    fun intentFor(entry: AppEntry): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(entry.packageName, entry.className))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    fun launch(entry: AppEntry): Boolean = try {
        ctx.startActivity(intentFor(entry)); true
    } catch (e: Exception) {
        launchPackage(entry.packageName)
    }

    fun launchPackage(pkg: String?): Boolean {
        if (pkg == null) return false
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        val CARPLAY_PKGS = listOf("com.zjinnova.zlink", "com.zjinnova.zlinkcarplay", "com.suding.diplay")
        val CARPLAY_KEYS = listOf("zlink", "carplay", "car play", "diplay", "autokit", "carlink", "carbit", "كاربلاي")
        val NAV_PKGS = listOf("com.google.android.apps.maps", "com.waze", "com.here.app.maps")
        val NAV_KEYS = listOf("maps", "navi", "خرائط", "ملاحة")
        val MUSIC_PKGS = listOf("com.anghami", "com.spotify.music", "com.google.android.apps.youtube.music", "com.apple.android.music")
        val MUSIC_KEYS = listOf("music", "موسيقى", "player")
        val RADIO_KEYS = listOf("radio", "راديو", ".fm", "tuner")
        val PHONE_KEYS = listOf("btphone", "bt phone", "bluetooth phone", "bluetoothphone", "dialer", "هاتف", "اتصال")
    }
}
