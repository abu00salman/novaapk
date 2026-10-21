package com.abu00salman.novadrive.data

import android.location.Geocoder
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
data class Weather(
    val tempC: Double, val code: Int, val isDay: Boolean, val fetchedAt: Long,
    val maxC: Double? = null, val minC: Double? = null,
)

/** Open-Meteo (no API key). Cached on disk, so it shows instantly and offline. */
class WeatherRepository(ctx: Context, private val scope: CoroutineScope) {
    private val prefs = ctx.getSharedPreferences("nova_weather", Context.MODE_PRIVATE)
    private val _weather = MutableStateFlow(loadCached())
    val weather: StateFlow<Weather?> = _weather
    @Volatile private var lastAttempt = 0L
    private val appCtx = ctx.applicationContext
    private val _city = MutableStateFlow(prefs.getString("city", null))
    /** Locality name from Android's Geocoder, when the device has a geocoding backend. */
    val city: StateFlow<String?> = _city
    private var cityFor: Pair<Double, Double>? = null

    fun maybeResolveCity(lat: Double, lon: Double, arabic: Boolean) {
        val key = Pair(Math.round(lat * 20) / 20.0, Math.round(lon * 20) / 20.0)
        if (key == cityFor || !Geocoder.isPresent()) return
        cityFor = key
        scope.launch(Dispatchers.IO) {
            runCatching {
                val locale = if (arabic) Locale.forLanguageTag("ar") else Locale.US
                @Suppress("DEPRECATION")
                Geocoder(appCtx, locale).getFromLocation(lat, lon, 1)?.firstOrNull()
                    ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
            }.getOrNull()?.let { name ->
                _city.value = name
                prefs.edit().putString("city", name).apply()
            }
        }
    }

    private fun loadCached(): Weather? {
        if (!prefs.contains("t")) return null
        return Weather(
            prefs.getFloat("t", 0f).toDouble(), prefs.getInt("c", 0),
            prefs.getBoolean("d", true), prefs.getLong("at", 0L),
            prefs.getFloat("hi", Float.NaN).takeIf { !it.isNaN() }?.toDouble(),
            prefs.getFloat("lo", Float.NaN).takeIf { !it.isNaN() }?.toDouble(),
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
                        "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto",
                        lat, lon
                    )
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000; readTimeout = 8000
                }
                try {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val cur = json.getJSONObject("current")
                    val daily = json.optJSONObject("daily")
                    Weather(
                        cur.getDouble("temperature_2m"), cur.getInt("weather_code"),
                        cur.optInt("is_day", 1) == 1, System.currentTimeMillis(),
                        daily?.optJSONArray("temperature_2m_max")?.optDouble(0)?.takeIf { !it.isNaN() },
                        daily?.optJSONArray("temperature_2m_min")?.optDouble(0)?.takeIf { !it.isNaN() },
                    )
                } finally {
                    conn.disconnect()
                }
            }.onSuccess { w ->
                _weather.value = w
                prefs.edit().putFloat("t", w.tempC.toFloat()).putInt("c", w.code)
                    .putBoolean("d", w.isDay).putLong("at", w.fetchedAt)
                    .putFloat("hi", w.maxC?.toFloat() ?: Float.NaN).putFloat("lo", w.minC?.toFloat() ?: Float.NaN).apply()
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
