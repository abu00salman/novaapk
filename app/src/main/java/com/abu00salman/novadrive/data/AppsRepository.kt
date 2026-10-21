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
)

class AppsRepository(private val ctx: Context, private val scope: CoroutineScope) {
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps
    private var job: Job? = null
    private val iconPx = 112

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
                    AppEntry(
                        key = "${ai.packageName}/${ai.name}",
                        packageName = ai.packageName,
                        className = ai.name,
                        label = ri.loadLabel(pm).toString(),
                        icon = ri.loadIcon(pm).toBitmap(iconPx, iconPx).asImageBitmap(),
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
        val PHONE_KEYS = listOf("btphone", "bt phone", "bluetooth phone", "phone", "dial", "هاتف", "اتصال")
    }
}
