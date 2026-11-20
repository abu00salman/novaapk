package com.abu00salman.novadrive.data.ai

import android.view.Window
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.ThemeMode
import com.abu00salman.novadrive.core.UiStyles
import com.abu00salman.novadrive.data.WeatherRepository
import com.abu00salman.novadrive.ui.nav.toNavInfo
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A tool the AI may call: name, description and JSON-schema parameters. */
data class ToolSpec(val name: String, val description: String, val properties: Map<String, Map<String, Any>>, val required: List<String>)

/**
 * Nova AI's tools — each one maps to a real Android/NovaDrive action. Results tell the model
 * truthfully whether the action happened, so it never reports success it didn't achieve.
 */
object NovaTools {
    private fun str(desc: String) = mapOf("type" to "string", "description" to desc)
    private fun enumOf(desc: String, vararg v: String) = mapOf("type" to "string", "description" to desc, "enum" to v.toList())
    private fun int(desc: String) = mapOf("type" to "integer", "description" to desc)

    val specs: List<ToolSpec> = listOf(
        ToolSpec("open_app", "Open an installed app on the car unit by its name (e.g. YouTube, Spotify, Google Maps, ChatGPT, Claude, WhatsApp, CarPlay/ZLink).",
            mapOf("name" to str("App name as the user said it")), listOf("name")),
        ToolSpec("media_control", "Control the music/audio that is playing now.",
            mapOf("action" to enumOf("What to do", "play", "pause", "next", "previous")), listOf("action")),
        ToolSpec("play_media", "Ask the music player to search for and play something (a song, an artist, a Quran surah, a podcast).",
            mapOf("query" to str("What to play, e.g. \"سورة البقرة\"")), listOf("query")),
        ToolSpec("now_playing", "What is playing now (title and artist).", emptyMap(), emptyList()),
        ToolSpec("set_volume", "Set the media volume.", mapOf("percent" to int("0-100")), listOf("percent")),
        ToolSpec("set_brightness", "Set NovaDrive's screen brightness.", mapOf("percent" to int("10-100")), listOf("percent")),
        ToolSpec("navigate_to", "Start in-app turn-by-turn navigation to a place, an address, \"home\" or \"work\".",
            mapOf("destination" to str("Place name or address")), listOf("destination")),
        ToolSpec("navigate_nearby", "Navigate to the nearest place of a kind (searches OpenStreetMap around the car).",
            mapOf("kind" to enumOf("Kind of place", *NovaActions.nearbyKinds.keys.toTypedArray())), listOf("kind")),
        ToolSpec("navigation_status", "Remaining distance/time, arrival time and next instruction of the current trip.", emptyMap(), emptyList()),
        ToolSpec("stop_navigation", "End the current in-app navigation.", emptyMap(), emptyList()),
        ToolSpec("switch_interface", "Change NovaDrive's home interface.",
            mapOf("interface" to enumOf("Interface id", *UiStyles.all.map { it.id }.toTypedArray())), listOf("interface")),
        ToolSpec("set_theme", "Day/night look of NovaDrive.", mapOf("mode" to enumOf("Theme", "day", "night", "auto")), listOf("mode")),
        ToolSpec("open_screen", "Open a NovaDrive screen.",
            mapOf("screen" to enumOf("Screen", "home", "map", "music", "apps", "settings", "vehicle", "split")), listOf("screen")),
        ToolSpec("open_big_shortcut", "Open the app on the current interface's big shortcut (the phone unless the user changed it).", emptyMap(), emptyList()),
        ToolSpec("get_weather", "Current weather where the car is.", emptyMap(), emptyList()),
        ToolSpec("get_location", "Current city and coordinates (only if the user allowed sharing location with Nova AI).", emptyMap(), emptyList()),
        ToolSpec("get_news", "Latest news headlines, optionally about a topic.", mapOf("topic" to str("Optional topic")), emptyList()),
        ToolSpec("call_contact", "Find a contact by name and open the dialer with the number filled in. The user must press Call to confirm; never claim the call was placed.",
            mapOf("name" to str("Contact name")), listOf("name")),
    )

    /** Runs a tool call and returns a short factual result for the model. */
    suspend fun execute(c: AppContainer, name: String, argsJson: String, driving: Boolean, window: Window?): String {
        val ar = c.settings.current.arabic
        val a = runCatching { JSONObject(argsJson.ifBlank { "{}" }) }.getOrDefault(JSONObject())
        fun done(r: ActionResult) = (if (r.ok) "OK: " else "FAILED: ") + r.message
        fun ok(m: String) = "OK: $m"
        return when (name) {
            "open_app" -> done(NovaActions.openApp(c, a.optString("name"), ar, driving))
            "media_control" -> when (a.optString("action")) {
                "play" -> { c.media.play(); ok("play sent") }
                "pause" -> { c.media.pause(); ok("pause sent") }
                "next" -> { c.media.next(); ok("next sent") }
                "previous" -> { c.media.previous(); ok("previous sent") }
                else -> "FAILED: unknown action"
            }
            "play_media" -> done(NovaActions.playFromSearch(c, a.optString("query"), ar))
            "now_playing" -> c.media.nowPlaying()?.let { ok("${it.first} — ${it.second ?: ""}") } ?: "Nothing is playing"
            "set_volume" -> { c.controls.setVolume(a.optInt("percent", 50).coerceIn(0, 100) / 100f); ok("volume ${a.optInt("percent")}%") }
            "set_brightness" -> { c.controls.setBrightness(a.optInt("percent", 70).coerceIn(10, 100) / 100f, window); ok("brightness ${a.optInt("percent")}%") }
            "navigate_to" -> done(NovaActions.navigateTo(c, a.optString("destination"), ar))
            "navigate_nearby" -> done(NovaActions.navigateNearby(c, a.optString("kind"), ar))
            "navigation_status" -> c.navEngine.state.value?.let { st ->
                val n = st.toNavInfo(ar, c.settings.current.use24h)
                ok("to ${st.destination.name}: next ${n.primary} ${n.secondary ?: ""}; ${n.eta ?: ""}")
            } ?: com.abu00salman.novadrive.data.NovaNotificationListener.navigation.value?.let { ok("guidance from ${it.packageName}: ${it.primary} ${it.secondary ?: ""} ${it.eta ?: ""}") }
                ?: "No active route"
            "stop_navigation" -> if (c.navEngine.state.value != null) { c.navEngine.stop(); ok("navigation ended") } else "No active route"
            "switch_interface" -> {
                val id = a.optString("interface")
                if (UiStyles.all.none { it.id == id }) "FAILED: unknown interface"
                else { c.settings.update { it.copy(uiStyle = id) }; c.voiceActions.tryEmit("screen:HOME"); ok("interface is now ${UiStyles.get(id).en}") }
            }
            "set_theme" -> {
                val m = when (a.optString("mode")) { "day" -> ThemeMode.LIGHT; "night" -> ThemeMode.DARK; else -> ThemeMode.AUTO }
                c.settings.update { it.copy(themeMode = m) }; ok("theme ${m.name.lowercase()}")
            }
            "open_screen" -> {
                val act = when (a.optString("screen")) {
                    "map" -> "screen:NAV"; "music" -> "mode:MUSIC"; "apps" -> "screen:APPS"; "settings" -> "screen:SETTINGS"
                    "vehicle" -> "screen:VEHICLE"; "split" -> "mode:SPLIT"; else -> "screen:HOME"
                }
                c.voiceActions.tryEmit(act); ok("opened ${a.optString("screen")}")
            }
            "open_big_shortcut" -> { c.voiceActions.tryEmit("big"); ok("big shortcut opened") }
            "get_weather" -> c.weather.weather.value?.let { w ->
                ok("${Math.round(w.tempC)}°C, ${WeatherRepository.label(w.code, ar)}" + (w.maxC?.let { ", high ${Math.round(it)}°" } ?: "") + (w.minC?.let { ", low ${Math.round(it)}°" } ?: ""))
            } ?: "Weather unavailable"
            "get_location" -> if (!c.ai.config.value.shareLocation) "Not permitted: the user hasn't allowed sharing location with Nova AI (Settings › Nova AI)."
                else c.fix.value?.let { f -> ok(String.format(Locale.US, "%s (%.4f, %.4f)", c.weather.city.value ?: "", f.lat, f.lon)) } ?: "No GPS position yet"
            "get_news" -> NovaActions.headlines(ar, a.optString("topic").ifBlank { null }).let { h -> if (h.isEmpty()) "FAILED: couldn't load news" else ok(h.joinToString(" | ")) }
            "call_contact" -> done(NovaActions.dialContact(c, a.optString("name"), ar))
            else -> "FAILED: unknown tool $name"
        }
    }

    fun systemPrompt(c: AppContainer): String {
        val s = c.settings.current
        val now = SimpleDateFormat("EEEE yyyy-MM-dd HH:mm", Locale.US).format(Date())
        return """You are Nova AI, the voice assistant inside NovaDrive, an Android car launcher on a head unit.
The driver talks to you by voice; your reply is spoken aloud. Reply in the driver's language and dialect (Saudi Arabic is common; answer Arabic in Arabic), in one or two short sentences, without markdown or lists.
Use the tools to actually do things on the car unit. Only say an action happened if its tool result starts with "OK"; if it says FAILED or needs permission, say so plainly.
For calls, the dialer opens with the number and the driver presses Call — say that, never that you placed the call.
You can't read the car's own data (fuel, doors, gears, climate) or control the AC, doors or windows — the unit has no official interface for that; say so and don't invent any. Speed and location come only from GPS.
If a request is ambiguous or could be unsafe, ask one short question to confirm before acting.
Keep the driver's attention on the road: be brief, and don't ask more than one short question at a time.
Current local time: $now. Current interface: ${UiStyles.get(s.uiStyle).en}. Units: km, °C."""
    }
}
