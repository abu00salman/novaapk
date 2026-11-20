package com.abu00salman.novadrive.data

import android.content.Context
import com.abu00salman.novadrive.data.nav.Geo
import com.abu00salman.novadrive.data.nav.LatLon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.abs

/** A fixed speed camera (OpenStreetMap `highway=speed_camera`). */
data class SpeedCamera(val at: LatLon, val maxKmh: Int?, val direction: Float?)

/** The camera the car is approaching. */
data class CameraAlert(val camera: SpeedCamera, val distanceM: Double)

/**
 * Speed cameras (ساهر) from OpenStreetMap, downloaded around the car while online and kept on the unit,
 * so alerts keep working without internet. Coverage is whatever the OSM community has mapped — real
 * positions only, nothing is guessed.
 */
class SpeedCameraRepository(private val ctx: Context) {
    private val file get() = File(ctx.filesDir, "speed_cameras.json")
    @Volatile var cameras: List<SpeedCamera> = emptyList(); private set
    private var centre: LatLon? = null
    private var fetchedAt = 0L

    fun load() {
        runCatching {
            val o = JSONObject(file.readText())
            centre = LatLon(o.getDouble("lat"), o.getDouble("lon")); fetchedAt = o.getLong("t")
            cameras = parse(o.getJSONArray("cams"))
        }
    }

    /** Refreshes the cache when the car is far from the last download or it is a week old. */
    suspend fun refreshIfNeeded(here: LatLon, online: Boolean) {
        if (!online) return
        val c = centre
        val stale = System.currentTimeMillis() - fetchedAt > 7L * 24 * 3600_000
        if (c != null && Geo.distance(c, here) < 25_000 && !stale) return
        val got = withContext(Dispatchers.IO) { runCatching { download(here) }.getOrNull() } ?: return
        cameras = got; centre = here; fetchedAt = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            runCatching {
                val arr = JSONArray()
                got.forEach { arr.put(JSONObject().put("lat", it.at.lat).put("lon", it.at.lon).putOpt("max", it.maxKmh).putOpt("dir", it.direction)) }
                file.writeText(JSONObject().put("lat", here.lat).put("lon", here.lon).put("t", fetchedAt).put("cams", arr).toString())
            }
        }
    }

    private fun download(here: LatLon): List<SpeedCamera> {
        val q = String.format(Locale.US, "[out:json][timeout:25];node[highway=speed_camera](around:60000,%.5f,%.5f);out;", here.lat, here.lon)
        val conn = (URL("https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(q, "UTF-8")).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 30_000
            setRequestProperty("User-Agent", "NovaDrive (Android car launcher)")
        }
        val body = try { conn.inputStream.bufferedReader().use { it.readText() } } finally { conn.disconnect() }
        val els = JSONObject(body).getJSONArray("elements")
        return (0 until els.length()).map { i ->
            val e = els.getJSONObject(i); val t = e.optJSONObject("tags")
            SpeedCamera(
                LatLon(e.getDouble("lat"), e.getDouble("lon")),
                t?.optString("maxspeed")?.takeWhile { it.isDigit() }?.toIntOrNull(),
                t?.optString("direction")?.toFloatOrNull(),
            )
        }
    }

    private fun parse(a: JSONArray) = (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        SpeedCamera(LatLon(o.getDouble("lat"), o.getDouble("lon")), if (o.has("max")) o.getInt("max") else null,
            if (o.has("dir")) o.getDouble("dir").toFloat() else null)
    }

    companion object {
        /**
         * The nearest camera ahead within [rangeM]: in front of the car (within 35° of its heading) and,
         * when the camera's direction is mapped, facing the car's direction of travel.
         */
        fun ahead(here: LatLon, heading: Float?, cams: List<SpeedCamera>, rangeM: Double = 700.0): CameraAlert? {
            if (heading == null) return null
            return cams.asSequence().mapNotNull { cam ->
                val d = Geo.distance(here, cam.at)
                if (d > rangeM || d < 15) return@mapNotNull null
                if (angle(Geo.bearing(here, cam.at).toFloat(), heading) > 35f) return@mapNotNull null
                if (cam.direction != null && angle(cam.direction, heading) > 60f) return@mapNotNull null
                CameraAlert(cam, d)
            }.minByOrNull { it.distanceM }
        }

        fun angle(a: Float, b: Float): Float { val d = abs(((a - b) % 360f + 540f) % 360f - 180f); return d }
    }
}
