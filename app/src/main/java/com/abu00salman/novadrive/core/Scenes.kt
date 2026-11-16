package com.abu00salman.novadrive.core

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * NOVA Scenes: a saved dashboard — interface, home mode, theme, dock and widget board — that comes
 * back with one tap ("Commute", "Road trip", "Parked"…). Scenes belong to the driver profile.
 */
@Immutable
data class Scene(
    val name: String,
    val uiStyle: String,
    val homeMode: String,
    val themeMode: ThemeMode,
    val dock: List<String>,
    /** [WidgetGrid.encode] of the board. */
    val widgets: String,
)

object Scenes {
    const val MAX = 8
    const val MAX_NAME = 24

    fun cleanName(raw: String): String = raw.replace(Regex("\\s+"), " ").trim().take(MAX_NAME)

    fun capture(name: String, s: NovaSettings, board: List<WidgetItem>) =
        Scene(cleanName(name), s.uiStyle, s.homeMode, s.themeMode, s.dock, WidgetGrid.encode(board))

    /** Adds or replaces (same name, case-insensitive); the oldest scene makes room past [MAX]. */
    fun put(list: List<Scene>, scene: Scene): List<Scene> {
        if (scene.name.isEmpty()) return list
        val rest = list.filterNot { it.name.equals(scene.name, ignoreCase = true) }
        return (rest + scene).takeLast(MAX)
    }

    fun remove(list: List<Scene>, name: String) = list.filterNot { it.name.equals(name, ignoreCase = true) }

    /** The scene's settings applied to [s]; unknown interfaces/modes in an old scene fall back safely. */
    fun applyTo(s: NovaSettings, scene: Scene): NovaSettings = s.copy(
        uiStyle = UiStyles.get(scene.uiStyle).id,
        homeMode = runCatching { HomeMode.valueOf(scene.homeMode) }.getOrDefault(HomeMode.CAR).name,
        themeMode = scene.themeMode,
        dock = scene.dock.ifEmpty { NovaSettings.DEFAULT_DOCK },
    )

    fun encode(list: List<Scene>): String = JSONArray().apply {
        list.forEach { sc ->
            put(JSONObject().put("name", sc.name).put("style", sc.uiStyle).put("mode", sc.homeMode).put("theme", sc.themeMode.name)
                .put("dock", JSONArray(sc.dock)).put("widgets", sc.widgets))
        }
    }.toString()

    fun decode(raw: String?): List<Scene> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = cleanName(o.optString("name"))
            if (name.isEmpty()) return@mapNotNull null
            val dock = o.optJSONArray("dock")?.let { d -> (0 until d.length()).map { d.optString(it) }.filter { it.isNotBlank() } }.orEmpty()
            Scene(name, o.optString("style", "smart"), o.optString("mode", "CAR"),
                runCatching { ThemeMode.valueOf(o.optString("theme")) }.getOrDefault(ThemeMode.AUTO), dock, o.optString("widgets"))
        }.takeLast(MAX)
    }
}

class SceneRepository(ctx: Context, private val settings: SettingsRepository, private val widgets: () -> WidgetRepository) {
    private val prefs = ctx.getSharedPreferences("scenes", Context.MODE_PRIVATE)
    private val _all = MutableStateFlow(
        prefs.all.entries.mapNotNull { (k, v) -> k.removePrefix("p").toIntOrNull()?.let { it to Scenes.decode(v as? String) } }.toMap()
    )
    /** Scenes of every profile, by profile id. */
    val all: StateFlow<Map<Int, List<Scene>>> = _all

    fun list(profile: Int = settings.current.profileId): List<Scene> = _all.value[profile].orEmpty()

    private fun store(profile: Int, list: List<Scene>) {
        prefs.edit().putString("p$profile", Scenes.encode(list)).apply()
        _all.value = _all.value + (profile to list)
    }

    /** Saves the current dashboard under [name]; false when the name is empty. */
    fun saveCurrent(name: String): Boolean {
        val scene = Scenes.capture(name, settings.current, widgets().items.value)
        if (scene.name.isEmpty()) return false
        store(settings.current.profileId, Scenes.put(list(), scene))
        return true
    }

    fun delete(name: String) = store(settings.current.profileId, Scenes.remove(list(), name))

    fun apply(scene: Scene) {
        settings.update { Scenes.applyTo(it, scene) }
        if (scene.widgets.isNotBlank()) widgets().set(WidgetGrid.decode(scene.widgets))
    }

    fun find(name: String): Scene? = list().firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
}
