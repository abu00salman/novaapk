package com.abu00salman.novadrive.data

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

@Immutable
data class Weather(val tempC: Double, val code: Int, val isDay: Boolean, val fetchedAt: Long)

/** Open-Meteo (no API key). Cached on disk, so it shows instantly and offline. */
class WeatherRepository(ctx: Context, private val scope: CoroutineScope) {
    private val prefs = ctx.getSharedPreferences("nova_weather", Context.MODE_PRIVATE)
    private val _weather = MutableStateFlow(loadCached())
    val weather: StateFlow<Weather?> = _weather
    @Volatile private var lastAttempt = 0L

    private fun loadCached(): Weather? {
        if (!prefs.contains("t")) return null
        return Weather(
            prefs.getFloat("t", 0f).toDouble(), prefs.getInt("c", 0),
            prefs.getBoolean("d", true), prefs.getLong("at", 0L)
        )
    }

    fun maybeRefresh(lat: Double, lon: Double, force: Boolean = false) {
        val now = System.currentTimeMillis()
        val fresh = _weather.value?.let { now - it.fetchedAt < 30 * 60_000L } ?: false
        if (!force && (fresh || now - lastAttempt < 5 * 60_000L)) return
        lastAttempt = now
        scope.launch(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    String.format(
                        Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code,is_day",
                        lat, lon
                    )
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000; readTimeout = 8000
                }
                try {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val cur = JSONObject(body).getJSONObject("current")
                    Weather(
                        cur.getDouble("temperature_2m"), cur.getInt("weather_code"),
                        cur.optInt("is_day", 1) == 1, System.currentTimeMillis()
                    )
                } finally {
                    conn.disconnect()
                }
            }.onSuccess { w ->
                _weather.value = w
                prefs.edit().putFloat("t", w.tempC.toFloat()).putInt("c", w.code)
                    .putBoolean("d", w.isDay).putLong("at", w.fetchedAt).apply()
            }
        }
    }

    companion object {
        enum class Kind { CLEAR, PARTLY, CLOUDY, FOG, RAIN, SNOW, STORM }

        fun kind(code: Int): Kind = when (code) {
            0 -> Kind.CLEAR
            1, 2 -> Kind.PARTLY
            3 -> Kind.CLOUDY
            45, 48 -> Kind.FOG
            in 51..67, in 80..82 -> Kind.RAIN
            in 71..77, 85, 86 -> Kind.SNOW
            in 95..99 -> Kind.STORM
            else -> Kind.CLOUDY
        }

        fun label(code: Int, arabic: Boolean): String = when (kind(code)) {
            Kind.CLEAR -> if (arabic) "صافٍ" else "Clear"
            Kind.PARTLY -> if (arabic) "غائم جزئياً" else "Partly cloudy"
            Kind.CLOUDY -> if (arabic) "غائم" else "Cloudy"
            Kind.FOG -> if (arabic) "ضباب" else "Fog"
            Kind.RAIN -> if (arabic) "أمطار" else "Rain"
            Kind.SNOW -> if (arabic) "ثلوج" else "Snow"
            Kind.STORM -> if (arabic) "عواصف رعدية" else "Thunderstorm"
        }
    }
}
