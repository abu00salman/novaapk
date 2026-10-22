package com.abu00salman.novadrive.core

import com.abu00salman.novadrive.data.AppCategory
import com.abu00salman.novadrive.data.AppEntry

/** Every action the dock can hold, in the order offered in Settings. */
val DOCK_CHOICES = listOf("maps", "carplay", "auto", "music", "phone", "radio", "youtube", "netflix", "apps")
const val DOCK_MAX = 6

/** Video apps are hidden from the dock and blocked from launching while driving. */
val VIDEO_SLOTS = setOf("youtube", "netflix")

private val VIDEO_KEYS = listOf("youtube", "netflix", "video", "iptv", "shahid", "vlc", "mxtech", "tv")

fun isVideoApp(packageName: String, label: String): Boolean {
    if (packageName == "com.google.android.apps.youtube.music") return false
    val hay = "$packageName $label".lowercase()
    return VIDEO_KEYS.any { hay.contains(it) }
}

/**
 * Resolves the user's dock order to concrete entries: unknown ids and duplicates are dropped,
 * Android Auto is skipped when it resolves to the same app as CarPlay (e.g. ZLink handles both),
 * and video apps disappear while driving. At most [DOCK_MAX] entries.
 */
fun <T> resolveDock(ids: List<String>, driving: Boolean, packageOf: (String) -> String?, build: (String) -> T): List<T> {
    val seen = mutableSetOf<String>()
    val out = mutableListOf<T>()
    for (id in ids.distinct()) {
        if (id !in DOCK_CHOICES) continue
        if (driving && id in VIDEO_SLOTS) continue
        val pkg = packageOf(id)
        if (pkg != null && !seen.add(pkg)) continue
        out += build(id)
        if (out.size == DOCK_MAX) break
    }
    return out
}

enum class AppTab { FAVORITES, NAVIGATION, MEDIA, CAR, TOOLS, ALL }

/** App-library filter: favorites keep the user's order, other tabs are alphabetical. */
fun filterApps(
    apps: List<AppEntry>,
    tab: AppTab,
    favorites: List<String>,
    query: String,
    showSystem: Boolean,
): List<AppEntry> {
    val q = query.trim()
    val base = when (tab) {
        AppTab.FAVORITES -> favorites.mapNotNull { k -> apps.firstOrNull { it.key == k } }
        AppTab.ALL -> apps.filter { showSystem || !it.system }
        AppTab.NAVIGATION -> apps.filter { it.category == AppCategory.NAVIGATION }
        AppTab.MEDIA -> apps.filter { it.category == AppCategory.MEDIA }
        AppTab.CAR -> apps.filter { it.category == AppCategory.CAR }
        AppTab.TOOLS -> apps.filter { it.category == AppCategory.TOOLS && (showSystem || !it.system) }
    }
    return if (q.isEmpty()) base else base.filter { it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true) }
}
