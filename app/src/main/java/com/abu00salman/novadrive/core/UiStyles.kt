package com.abu00salman.novadrive.core

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** One home interface ("الواجهة") that can be chosen in Settings › Appearance. */
@Immutable
data class UiStyle(
    val id: String,
    val en: String,
    val ar: String,
    /** null = follows the app's day/night theme; true/false = the style's own default. */
    val defaultDark: Boolean?,
    /** The style draws its own menus and status bar edge to edge (no NovaDrive side bar). */
    val fullScreen: Boolean,
    /** Styles whose look is fixed (Tesla/BMW) don't offer colour/wallpaper/font options. */
    val customizable: Boolean,
    /** Accent choices, first is the style's own. */
    val accents: List<Long> = emptyList(),
)

object UiStyles {
    val all = listOf(
        UiStyle("smart", "Smart", "الواجهة الذكية", null, false, false),
        UiStyle("simple", "Simple", "الواجهة البسيطة", null, false, false),
        UiStyle("tesla", "Tesla", "واجهة Tesla", false, true, false),
        UiStyle("bmw", "BMW", "واجهة BMW", true, false, false),
        UiStyle("classic", "Classic", "واجهة كلاسيك", false, true, true,
            listOf(0xFF1F5FD6, 0xFF0F766E, 0xFF7C3AED, 0xFFB45309, 0xFF111827)),
        UiStyle("mercedes", "Mercedes", "واجهة Mercedes", true, true, true,
            listOf(0xFF00C2FF, 0xFFE040FB, 0xFF7C4DFF, 0xFFFFB74D, 0xFF64FFDA)),
        UiStyle("ford", "Ford", "واجهة Ford", true, true, true,
            listOf(0xFF2196F3, 0xFF00BFA5, 0xFFFF6D00, 0xFFE53935, 0xFFB0BEC5)),
        UiStyle("apple", "Apple Future Car", "واجهة Apple المستقبلية", null, true, true,
            listOf(0xFF0A84FF, 0xFFFF375F, 0xFF30D158, 0xFFFF9F0A, 0xFFBF5AF2)),
        UiStyle("jetour", "Jetour", "واجهة Jetour", true, true, true,
            listOf(0xFFFF7A1A, 0xFF00B8D4, 0xFFFFC400, 0xFFE53935, 0xFF8BC34A)),
        UiStyle("byd", "BYD", "واجهة BYD", false, true, true,
            listOf(0xFF00A99D, 0xFF2563EB, 0xFF16A34A, 0xFFDB2777, 0xFF0F172A)),
        UiStyle("haval", "Haval", "واجهة Haval", true, true, true,
            listOf(0xFFE11D2E, 0xFF3B82F6, 0xFFF59E0B, 0xFF10B981, 0xFFE5E7EB)),
        UiStyle("tank", "TANK", "واجهة TANK", true, true, true,
            listOf(0xFFD9A441, 0xFF9CB65A, 0xFFE07B39, 0xFF7FB3D5, 0xFFE8E2D0)),
        UiStyle("widgets", "Widgets", "واجهة الويدجت", true, true, true,
            listOf(0xFF0A84FF, 0xFF30D158, 0xFFFF9F0A, 0xFFFF375F, 0xFFBF5AF2)),
    )

    fun get(id: String): UiStyle = all.firstOrNull { it.id == id } ?: all[0]
}

/** Per-interface customisation, saved separately for every interface. */
@Immutable
data class StyleOptions(
    /** null = the interface's default (or the app theme). */
    val dark: Boolean? = null,
    /** Index into [UiStyle.accents]. */
    val accent: Int = 0,
    /** 0 = interface default, 1..[WALLPAPER_PRESETS] = colour presets, [WALLPAPER_IMAGE] = own photo. */
    val wallpaper: Int = 0,
    /** 0 = IBM Plex (NovaDrive), 1 = the system font. */
    val font: Int = 0,
    /** Changes whenever a new wallpaper photo is chosen. */
    val wallpaperVersion: Long = 0,
    /** Package opened by the interface's big shortcut (Classic/Ford); null = the phone. */
    val bigApp: String? = null,
    /** 0 = normal, 1 = large icon on the big shortcut. */
    val bigSize: Int = 0,
    /** BMW: YouTube in the bottom apps, next to Music. */
    val showYoutube: Boolean = true,
) {
    companion object {
        const val WALLPAPER_PRESETS = 4
        const val WALLPAPER_IMAGE = 99
    }
}

class StyleOptionsRepository(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("style_options", Context.MODE_PRIVATE)
    private val _all = MutableStateFlow(UiStyles.all.associate { it.id to read(it.id) })
    val all: StateFlow<Map<String, StyleOptions>> = _all

    fun get(id: String): StyleOptions = _all.value[id] ?: StyleOptions()

    fun update(id: String, block: (StyleOptions) -> StyleOptions) {
        val n = block(get(id))
        prefs.edit().putString(id, encode(n)).apply()
        _all.value = _all.value + (id to n)
    }

    private fun read(id: String): StyleOptions = decode(prefs.getString(id, null))

    companion object {
        fun encode(o: StyleOptions) = listOf(
            when (o.dark) { true -> "1"; false -> "0"; null -> "" }, o.accent, o.wallpaper, o.font, o.wallpaperVersion,
            o.bigApp.orEmpty(), o.bigSize, if (o.showYoutube) "1" else "0",
        ).joinToString("|")

        fun decode(s: String?): StyleOptions {
            if (s.isNullOrBlank()) return StyleOptions()
            val p = s.split('|')
            return StyleOptions(
                dark = when (p.getOrNull(0)) { "1" -> true; "0" -> false; else -> null },
                accent = p.getOrNull(1)?.toIntOrNull() ?: 0,
                wallpaper = p.getOrNull(2)?.toIntOrNull() ?: 0,
                font = p.getOrNull(3)?.toIntOrNull() ?: 0,
                wallpaperVersion = p.getOrNull(4)?.toLongOrNull() ?: 0,
                bigApp = p.getOrNull(5)?.takeIf { it.isNotBlank() },
                bigSize = p.getOrNull(6)?.toIntOrNull() ?: 0,
                showYoutube = p.getOrNull(7) != "0",
            )
        }
    }
}
