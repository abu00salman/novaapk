package com.abu00salman.novadrive.core

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Offline solar position (accurate to well under a degree) for automatic day/night. */
object SunCalc {
    private fun norm(x: Double, m: Double) = ((x % m) + m) % m

    /** Sun altitude in degrees for the given position and instant. */
    fun altitude(lat: Double, lon: Double, timeMillis: Long): Double {
        val n = timeMillis / 86_400_000.0 + 2440587.5 - 2451545.0
        val l = norm(280.460 + 0.9856474 * n, 360.0)
        val g = Math.toRadians(norm(357.528 + 0.9856003 * n, 360.0))
        val lambda = Math.toRadians(l + 1.915 * sin(g) + 0.020 * sin(2 * g))
        val eps = Math.toRadians(23.439 - 0.0000004 * n)
        val ra = atan2(cos(eps) * sin(lambda), cos(lambda))
        val dec = asin(sin(eps) * sin(lambda))
        val gmstHours = norm(18.697374558 + 24.06570982441908 * n, 24.0)
        val ha = Math.toRadians(gmstHours * 15.0 + lon) - ra
        val latR = Math.toRadians(lat)
        val alt = asin(sin(latR) * sin(dec) + cos(latR) * cos(dec) * cos(ha))
        return Math.toDegrees(alt)
    }

    /** Night once the sun is below the horizon (incl. refraction). */
    fun isNight(lat: Double, lon: Double, timeMillis: Long): Boolean =
        altitude(lat, lon, timeMillis) < -0.833

    /**
     * Sunrise and sunset for the local day that contains [timeMillis] (epoch ms, ±1 min), or null
     * when the sun doesn't cross the horizon that day.
     */
    fun riseSet(lat: Double, lon: Double, timeMillis: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): Pair<Long?, Long?> {
        val cal = java.util.Calendar.getInstance(zone).apply {
            this.timeInMillis = timeMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val step = 10 * 60_000L
        fun up(t: Long) = altitude(lat, lon, t) > -0.833
        fun refine(a: Long, b: Long): Long {
            var lo = a; var hi = b
            val target = up(b)
            while (hi - lo > 60_000L) { val m = (lo + hi) / 2; if (up(m) == target) hi = m else lo = m }
            return hi
        }
        var rise: Long? = null; var set: Long? = null
        var t = start
        while (t < start + 86_400_000L) {
            val a = up(t); val b = up(t + step)
            if (!a && b && rise == null) rise = refine(t, t + step)
            if (a && !b && set == null) set = refine(t, t + step)
            t += step
        }
        return rise to set
    }
}
