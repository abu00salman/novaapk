package com.abu00salman.novadrive.data.nav

import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
data class LatLon(val lat: Double, val lon: Double)

/** Small, dependency-free geodesy used by the navigation engine (accurate to well under 1 %). */
object Geo {
    private const val R = 6_371_000.0

    fun distance(a: LatLon, b: LatLon): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).let { it * it }
        return 2 * R * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Initial bearing a → b in degrees (0 = north, clockwise). */
    fun bearing(a: LatLon, b: LatLon): Double {
        val la = Math.toRadians(a.lat); val lb = Math.toRadians(b.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val y = sin(dLon) * cos(lb)
        val x = cos(la) * sin(lb) - sin(la) * cos(lb) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /**
     * Distance from [p] to segment [a]–[b] in metres and the fraction (0..1) along it of the
     * closest point, using a local flat projection (fine for segments of a few km).
     */
    fun toSegment(p: LatLon, a: LatLon, b: LatLon): Pair<Double, Double> {
        val k = cos(Math.toRadians(p.lat)) * PI / 180 * R
        val m = PI / 180 * R
        val ax = (a.lon - p.lon) * k; val ay = (a.lat - p.lat) * m
        val bx = (b.lon - p.lon) * k; val by = (b.lat - p.lat) * m
        val dx = bx - ax; val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0)
        val cx = ax + t * dx; val cy = ay + t * dy
        return sqrt(cx * cx + cy * cy) to t
    }

    /** "850 م" / "3.4 كم" (or m/km in English), rounded the way drivers read them. */
    fun formatDistance(m: Double, arabic: Boolean): String = when {
        m < 50 -> "${(m / 5).roundToInt() * 5} " + if (arabic) "م" else "m"
        m < 1000 -> "${(m / 50).roundToInt() * 50} " + if (arabic) "م" else "m"
        m < 10_000 -> String.format(java.util.Locale.US, "%.1f ", m / 1000) + if (arabic) "كم" else "km"
        else -> "${(m / 1000).roundToInt()} " + if (arabic) "كم" else "km"
    }

    fun formatDuration(s: Double, arabic: Boolean): String {
        val min = (s / 60).roundToInt().coerceAtLeast(1)
        return if (min < 60) "$min " + (if (arabic) "دقيقة" else "min")
        else "${min / 60} " + (if (arabic) "س" else "h") + " ${min % 60} " + (if (arabic) "د" else "min")
    }
}
