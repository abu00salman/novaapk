package com.abu00salman.novadrive.core

import com.abu00salman.novadrive.data.AppCategory
import com.abu00salman.novadrive.data.AppEntry

/** Every action the dock can hold, in the order offered in Settings. */
/** "smart" is the Smart Dock slot: the app the driver opens most that isn't already in the dock. */
const val SMART_SLOT = "smart"
val DOCK_CHOICES = listOf("maps", "carplay", "auto", "music", "phone", "radio", "youtube", "netflix", SMART_SLOT, "apps")
const val DOCK_MAX = 6

/** A dock entry the user pointed at a specific installed app: "app:<package>". */
const val CUSTOM_PREFIX = "app:"

/** Replaces the dock entry at [index] with [packageName] (or appends it), never duplicating it. */
fun replaceDockEntry(dock: List<String>, index: Int, packageName: String): List<String> {
    val entry = CUSTOM_PREFIX + packageName
    val list = dock.filter { it != entry }.toMutableList()
    val at = index.coerceAtMost(list.size)
    if (at in list.indices) list[at] = entry else if (list.size < DOCK_MAX) list.add(entry)
    return list
}

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
        if (id !in DOCK_CHOICES && !id.startsWith(CUSTOM_PREFIX)) continue
        if (driving && id in VIDEO_SLOTS) continue
        val pkg = packageOf(id)
        if (driving && id.startsWith(CUSTOM_PREFIX) && isVideoApp(id.removePrefix(CUSTOM_PREFIX), "")) continue
        if (pkg != null && !seen.add(pkg)) continue
        out += build(id)
        if (out.size == DOCK_MAX) break
    }
    return out
}

enum class AppTab { FAVORITES, FREQUENT, NAVIGATION, MEDIA, CAR, TOOLS, ALL }

/**
 * Smart Dock: the most-used app ([frequent] = app keys, most used first) that no other dock slot
 * already shows; video apps are skipped while driving. Null until NovaDrive has seen some use.
 */
fun smartDockApp(frequent: List<String>, apps: List<AppEntry>, taken: Set<String>, driving: Boolean): AppEntry? =
    frequent.asSequence().mapNotNull { k -> apps.firstOrNull { it.key == k } }
        .firstOrNull { it.packageName !in taken && !(driving && isVideoApp(it.packageName, it.label)) }

/** App-library filter: favorites keep the user's order, other tabs are alphabetical. */
fun filterApps(
    apps: List<AppEntry>,
    tab: AppTab,
    favorites: List<String>,
    query: String,
    showSystem: Boolean,
    /** App keys by use, most used first (the Frequent tab). */
    frequent: List<String> = emptyList(),
): List<AppEntry> {
    val q = query.trim()
    val base = when (tab) {
        AppTab.FAVORITES -> favorites.mapNotNull { k -> apps.firstOrNull { it.key == k } }
        AppTab.FREQUENT -> frequent.mapNotNull { k -> apps.firstOrNull { it.key == k } }.take(18)
        AppTab.ALL -> apps.filter { showSystem || !it.system }
        AppTab.NAVIGATION -> apps.filter { it.category == AppCategory.NAVIGATION }
        AppTab.MEDIA -> apps.filter { it.category == AppCategory.MEDIA }
        AppTab.CAR -> apps.filter { it.category == AppCategory.CAR }
        AppTab.TOOLS -> apps.filter { it.category == AppCategory.TOOLS && (showSystem || !it.system) }
    }
    return if (q.isEmpty()) base else base.filter { it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true) }
}
