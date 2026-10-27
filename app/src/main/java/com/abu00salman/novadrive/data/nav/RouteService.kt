package com.abu00salman.novadrive.data.nav

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

@Immutable
data class Place(val name: String, val address: String, val point: LatLon)

/** One manoeuvre of a route, as returned by OSRM. */
@Immutable
data class RouteStep(
    val location: LatLon,
    /** OSRM maneuver type: depart, turn, new name, merge, fork, roundabout, arrive, … */
    val type: String,
    /** left, slight left, sharp left, right, …, straight, uturn (may be empty). */
    val modifier: String,
    val road: String,
    /** Length of the stretch that follows this manoeuvre. */
    val distanceM: Double,
    val durationS: Double,
    val exit: Int?,
)

@Immutable
data class Route(
    val points: List<LatLon>,
    val distanceM: Double,
    val durationS: Double,
    val steps: List<RouteStep>,
)

/**
 * Destination search and road routing from OpenStreetMap services that need no API key:
 * Nominatim for search and OSRM (FOSSGIS server, with the OSRM demo server as fallback) for
 * routes. Both are free public services with fair-use limits, so requests are only made on
 * demand (search on submit, route on start/reroute) and Nominatim is rate-limited to 1/s.
 */
object RouteService {
    private const val UA = "NovaDrive/2.1 (Android car launcher; +https://github.com/abu00salman/novaapk)"
    private val nominatimGate = Mutex()
    @Volatile private var lastSearch = 0L

    suspend fun search(query: String, near: LatLon?, arabic: Boolean): List<Place> = withContext(Dispatchers.IO) {
        nominatimGate.withLock {
            val wait = 1100 - (System.currentTimeMillis() - lastSearch)
            if (wait > 0) Thread.sleep(wait)
            lastSearch = System.currentTimeMillis()
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            val bias = near?.let {
                String.format(Locale.US, "&viewbox=%.4f,%.4f,%.4f,%.4f", it.lon - 1.5, it.lat + 1.5, it.lon + 1.5, it.lat - 1.5)
            }.orEmpty()
            val body = get("https://nominatim.openstreetmap.org/search?q=$q&format=jsonv2&limit=8&addressdetails=0" +
                "&accept-language=${if (arabic) "ar,en" else "en,ar"}$bias") ?: return@withLock emptyList()
            parsePlaces(body)
        }
    }

    suspend fun route(from: LatLon, to: LatLon): Route? = withContext(Dispatchers.IO) {
        val coords = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", from.lon, from.lat, to.lon, to.lat)
        val query = "?overview=full&geometries=geojson&steps=true&alternatives=false"
        listOf(
            "https://routing.openstreetmap.de/routed-car/route/v1/driving/$coords$query",
            "https://router.project-osrm.org/route/v1/driving/$coords$query",
        ).firstNotNullOfOrNull { url -> get(url)?.let(::parseRoute) }
    }

    fun parsePlaces(body: String): List<Place> = runCatching {
        val arr = JSONArray(body)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val display = o.optString("display_name")
            val name = o.optString("name").ifBlank { display.substringBefore(',') }
            val lat = o.optString("lat").toDoubleOrNull() ?: return@mapNotNull null
            val lon = o.optString("lon").toDoubleOrNull() ?: return@mapNotNull null
            Place(name.trim(), display.trim(), LatLon(lat, lon))
        }
    }.getOrDefault(emptyList())

    fun parseRoute(body: String): Route? = runCatching {
        val json = JSONObject(body)
        if (json.optString("code") != "Ok") return null
        val r = json.getJSONArray("routes").getJSONObject(0)
        val coords = r.getJSONObject("geometry").getJSONArray("coordinates")
        val points = (0 until coords.length()).map { i -> coords.getJSONArray(i).let { LatLon(it.getDouble(1), it.getDouble(0)) } }
        val steps = mutableListOf<RouteStep>()
        val legs = r.getJSONArray("legs")
        for (l in 0 until legs.length()) {
            val arr = legs.getJSONObject(l).getJSONArray("steps")
            for (s in 0 until arr.length()) {
                val st = arr.getJSONObject(s)
                val m = st.getJSONObject("maneuver")
                val loc = m.getJSONArray("location")
                steps += RouteStep(
                    location = LatLon(loc.getDouble(1), loc.getDouble(0)),
                    type = m.optString("type"),
                    modifier = m.optString("modifier"),
                    road = st.optString("name").ifBlank { st.optString("ref") },
                    distanceM = st.optDouble("distance", 0.0),
                    durationS = st.optDouble("duration", 0.0),
                    exit = if (m.has("exit")) m.optInt("exit") else null,
                )
            }
        }
        if (points.size < 2) null else Route(points, r.optDouble("distance"), r.optDouble("duration"), steps)
    }.getOrNull()

    private fun get(url: String): String? = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().use { it.readText() } else null
        } finally { conn.disconnect() }
    }.getOrNull()
}

/** Spoken/written instruction for a manoeuvre, in Arabic or English. */
object Instructions {
    fun text(step: RouteStep, arabic: Boolean): String {
        val road = step.road.takeIf { it.isNotBlank() }
        val onto = road?.let { if (arabic) " إلى $it" else " onto $it" }.orEmpty()
        val dir = direction(step.modifier, arabic)
        return when (step.type) {
            "depart" -> if (arabic) "انطلق" + (road?.let { " على $it" } ?: "") else "Head out" + (road?.let { " on $it" } ?: "")
            "arrive" -> if (arabic) "الوصول إلى الوجهة" else "Arrive at your destination"
            "roundabout", "rotary", "roundabout turn" -> {
                val n = step.exit
                if (arabic) "في الدوار خذ " + (n?.let { "المخرج ${ordinalAr(it)}" } ?: "المخرج") + onto
                else "At the roundabout take the " + (n?.let { "${ordinalEn(it)} exit" } ?: "exit") + onto
            }
            "merge" -> (if (arabic) "اندمج $dir" else "Merge $dir").trim() + onto
            "on ramp" -> (if (arabic) "خذ المنحدر $dir" else "Take the ramp $dir").trim() + onto
            "off ramp" -> (if (arabic) "اخرج $dir" else "Take the exit $dir").trim() + onto
            "fork" -> (if (arabic) "عند التفرع الزم $dir" else "Keep $dir at the fork").trim() + onto
            "end of road" -> (if (arabic) "في نهاية الطريق انعطف $dir" else "At the end of the road turn $dir").trim() + onto
            "continue", "new name" ->
                if (step.modifier == "straight" || step.modifier.isBlank()) (if (arabic) "استمر" else "Continue") + onto
                else (if (arabic) "الزم $dir" else "Keep $dir") + onto
            else -> when (step.modifier) {
                "uturn" -> (if (arabic) "قم بالدوران للخلف" else "Make a U-turn") + onto
                "straight" -> (if (arabic) "استمر للأمام" else "Go straight") + onto
                else -> (if (arabic) "انعطف $dir" else "Turn $dir") + onto
            }
        }.replace("  ", " ").trim()
    }

    fun direction(modifier: String, arabic: Boolean): String = if (arabic) when (modifier) {
        "left" -> "يسارًا"; "right" -> "يمينًا"
        "slight left" -> "قليلًا لليسار"; "slight right" -> "قليلًا لليمين"
        "sharp left" -> "بحدة لليسار"; "sharp right" -> "بحدة لليمين"
        "straight" -> "للأمام"; "uturn" -> "للخلف"
        else -> ""
    } else modifier

    private fun ordinalAr(n: Int) = listOf("الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس", "السابع", "الثامن").getOrElse(n - 1) { "رقم $n" }
    private fun ordinalEn(n: Int) = when (n) { 1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${n}th" }
}
