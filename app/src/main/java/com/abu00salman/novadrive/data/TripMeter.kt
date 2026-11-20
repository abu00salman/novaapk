package com.abu00salman.novadrive.data

import androidx.compose.runtime.Immutable
import com.abu00salman.novadrive.data.nav.Geo
import com.abu00salman.novadrive.data.nav.LatLon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Trip computer from GPS only: distance, moving time, average and top speed since the last reset. */
@Immutable
data class TripStats(
    val distanceKm: Double = 0.0,
    val movingSeconds: Long = 0,
    val maxKmh: Float = 0f,
    val startedAt: Long = System.currentTimeMillis(),
) {
    val avgKmh: Float get() = if (movingSeconds < 10) 0f else (distanceKm / (movingSeconds / 3600.0)).toFloat()
}

/** Pure accumulation step (tested): ignores poor fixes, standing jitter and impossible jumps. */
object TripMath {
    data class Point(val lat: Double, val lon: Double, val speedKmh: Float, val accuracyM: Float, val timeMs: Long)

    fun step(s: TripStats, last: Point?, now: Point): TripStats {
        if (now.accuracyM > 50f) return s
        val max = if (now.speedKmh in 0f..300f) maxOf(s.maxKmh, now.speedKmh) else s.maxKmh
        if (last == null) return s.copy(maxKmh = max)
        val dt = (now.timeMs - last.timeMs) / 1000.0
        if (dt <= 0 || dt > 120) return s.copy(maxKmh = max)
        val d = Geo.distance(LatLon(last.lat, last.lon), LatLon(now.lat, now.lon))
        val moving = now.speedKmh >= 3f || d / dt > 1.0
        // Faster than 300 km/h between two fixes is a GPS jump, not driving.
        if (!moving || d / dt > 83.0) return s.copy(maxKmh = max)
        return s.copy(distanceKm = s.distanceKm + d / 1000.0, movingSeconds = s.movingSeconds + Math.round(dt), maxKmh = max)
    }
}

class TripMeter(private val scope: CoroutineScope, private val fixes: StateFlow<GeoFix?>) {
    private val _stats = MutableStateFlow(TripStats())
    val stats: StateFlow<TripStats> = _stats
    private var job: Job? = null
    private var last: TripMath.Point? = null

    /** Starts counting (idempotent); keeps running in the background while NovaDrive is alive. */
    fun start() {
        if (job != null) return
        job = scope.launch {
            fixes.filterNotNull().collect { f ->
                val p = TripMath.Point(f.lat, f.lon, f.speedKmh, f.accuracyM, System.currentTimeMillis())
                _stats.value = TripMath.step(_stats.value, last, p)
                if (p.accuracyM <= 50f) last = p
            }
        }
    }

    fun reset() { last = null; _stats.value = TripStats() }
}
