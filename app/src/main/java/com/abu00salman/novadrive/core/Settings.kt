package com.abu00salman.novadrive.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { AUTO, LIGHT, DARK, SYSTEM }
enum class Lang { AR, EN }

/** Everything below is stored per driver profile. */
data class NovaSettings(
    val profileId: Int = 0,
    val profileNames: List<String> = listOf("Driver"),
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val accent: Int = 0,
    val language: Lang = Lang.AR,
    val use24h: Boolean = false,
    val favorites: List<String> = emptyList(),
    val carPlayPackage: String? = null,
    val navPackage: String? = null,
    val musicPackage: String? = null,
    val radioPackage: String? = null,
    val phonePackage: String? = null,
    val autoLaunchCarPlay: Boolean = false,
    val drivingMode: Boolean = true,
    val showWeather: Boolean = true,
    val homeAddress: String = "",
    val workAddress: String = "",
    val obdEnabled: Boolean = false,
    val obdAddress: String? = null,
) {
    val profileName: String get() = profileNames.getOrElse(profileId) { "Driver" }
    val arabic: Boolean get() = language == Lang.AR
}

class SettingsRepository(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("nova_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<NovaSettings> = _state.asStateFlow()
    val current: NovaSettings get() = _state.value

    private fun k(p: Int, key: String) = "p$p.$key"

    private fun load(): NovaSettings {
        val names = (prefs.getString("profiles", null) ?: "Driver")
            .split('|').filter { it.isNotBlank() }.ifEmpty { listOf("Driver") }
        val p = prefs.getInt("profile", 0).coerceIn(0, names.lastIndex)
        fun s(key: String) = prefs.getString(k(p, key), null)?.takeIf { it.isNotBlank() }
        fun b(key: String, d: Boolean) = prefs.getBoolean(k(p, key), d)
        return NovaSettings(
            profileId = p,
            profileNames = names,
            themeMode = runCatching { ThemeMode.valueOf(s("theme") ?: "AUTO") }.getOrDefault(ThemeMode.AUTO),
            accent = prefs.getInt(k(p, "accent"), 0),
            language = runCatching { Lang.valueOf(s("lang") ?: "AR") }.getOrDefault(Lang.AR),
            use24h = b("24h", false),
            favorites = (s("favs") ?: "").split('|').filter { it.isNotBlank() },
            carPlayPackage = s("carplay"),
            navPackage = s("nav"),
            musicPackage = s("music"),
            radioPackage = s("radio"),
            phonePackage = s("phone"),
            autoLaunchCarPlay = b("autoCarPlay", false),
            drivingMode = b("driving", true),
            showWeather = b("weather", true),
            homeAddress = s("home") ?: "",
            workAddress = s("work") ?: "",
            obdEnabled = b("obd", false),
            obdAddress = s("obdAddr"),
        )
    }

    private fun save(n: NovaSettings) {
        val p = n.profileId
        prefs.edit()
            .putInt("profile", p)
            .putString("profiles", n.profileNames.joinToString("|"))
            .putString(k(p, "theme"), n.themeMode.name)
            .putInt(k(p, "accent"), n.accent)
            .putString(k(p, "lang"), n.language.name)
            .putBoolean(k(p, "24h"), n.use24h)
            .putString(k(p, "favs"), n.favorites.joinToString("|"))
            .putString(k(p, "carplay"), n.carPlayPackage)
            .putString(k(p, "nav"), n.navPackage)
            .putString(k(p, "music"), n.musicPackage)
            .putString(k(p, "radio"), n.radioPackage)
            .putString(k(p, "phone"), n.phonePackage)
            .putBoolean(k(p, "autoCarPlay"), n.autoLaunchCarPlay)
            .putBoolean(k(p, "driving"), n.drivingMode)
            .putBoolean(k(p, "weather"), n.showWeather)
            .putString(k(p, "home"), n.homeAddress)
            .putString(k(p, "work"), n.workAddress)
            .putBoolean(k(p, "obd"), n.obdEnabled)
            .putString(k(p, "obdAddr"), n.obdAddress)
            .apply()
    }

    fun update(block: (NovaSettings) -> NovaSettings) {
        val next = block(_state.value)
        save(next)
        _state.value = next
    }

    fun toggleFavorite(key: String) = update {
        it.copy(favorites = if (key in it.favorites) it.favorites - key else it.favorites + key)
    }

    fun switchProfile(id: Int) {
        prefs.edit().putInt("profile", id).apply()
        _state.value = load()
    }

    fun addProfile(name: String) {
        val clean = name.replace("|", " ").trim().ifEmpty { return }
        val names = _state.value.profileNames + clean
        prefs.edit().putString("profiles", names.joinToString("|")).apply()
        switchProfile(names.lastIndex)
    }

    fun renameProfile(id: Int, name: String) {
        val clean = name.replace("|", " ").trim().ifEmpty { return }
        val names = _state.value.profileNames.toMutableList()
        if (id !in names.indices) return
        names[id] = clean
        update { it.copy(profileNames = names) }
    }

    /** Last known coarse position, used offline for night mode and weather. */
    fun lastLocation(): Pair<Double, Double>? {
        val lat = prefs.getFloat("lastLat", Float.NaN)
        val lon = prefs.getFloat("lastLon", Float.NaN)
        return if (lat.isNaN() || lon.isNaN()) null else Pair(lat.toDouble(), lon.toDouble())
    }

    fun saveLastLocation(lat: Double, lon: Double) {
        prefs.edit().putFloat("lastLat", lat.toFloat()).putFloat("lastLon", lon.toFloat()).apply()
    }
}
