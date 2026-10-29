package com.abu00salman.novadrive

import com.abu00salman.novadrive.core.StyleOptions
import com.abu00salman.novadrive.core.StyleOptionsRepository
import com.abu00salman.novadrive.core.SunCalc
import com.abu00salman.novadrive.core.WidgetGrid
import com.abu00salman.novadrive.core.WidgetItem
import com.abu00salman.novadrive.core.WidgetKind
import com.abu00salman.novadrive.core.WidgetSize
import com.abu00salman.novadrive.data.TripMath
import com.abu00salman.novadrive.data.TripStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class WidgetsTest {
    @Test fun defaultBoardPacksWithoutOverlap() {
        val (placements, rows) = WidgetGrid.pack(WidgetGrid.DEFAULT)
        val cells = HashSet<Pair<Int, Int>>()
        placements.forEach { p ->
            for (dx in 0 until p.item.size.w) for (dy in 0 until p.item.size.h) {
                assertTrue("overlap at ${p.col + dx},${p.row + dy}", cells.add(p.col + dx to p.row + dy))
                assertTrue(p.col + dx < WidgetGrid.COLUMNS)
            }
        }
        // Speed 2×2 top-left, then clock/weather/compass/altitude fill the right 2×2.
        assertEquals(0 to 0, placements[0].col to placements[0].row)
        assertEquals(2 to 0, placements[1].col to placements[1].row)
        assertEquals(3 to 0, placements[2].col to placements[2].row)
        assertEquals(2 to 1, placements[3].col to placements[3].row)
        assertEquals(3 to 1, placements[4].col to placements[4].row)
        assertEquals("first screen is full (3 rows)", 2, placements[5].row)
        assertTrue(rows >= 3)
    }

    @Test fun smallWidgetsFillGapsLikePhones() {
        val items = listOf(WidgetItem(1, WidgetKind.MEDIA, WidgetSize.WIDE), WidgetItem(2, WidgetKind.MAP, WidgetSize.LARGE),
            WidgetItem(3, WidgetKind.CLOCK, WidgetSize.SMALL))
        val (p, rows) = WidgetGrid.pack(items)
        assertEquals(0 to 0, p[0].col to p[0].row)
        assertEquals(2 to 0, p[1].col to p[1].row)
        assertEquals("fills the gap under the wide widget", 0 to 1, p[2].col to p[2].row)
        assertEquals(2, rows)
    }

    @Test fun editOperations() {
        var b = WidgetGrid.DEFAULT
        val n = b.size
        b = WidgetGrid.add(b, WidgetKind.OBD)
        assertEquals(n + 1, b.size)
        assertEquals(WidgetKind.OBD, b.last().kind)
        assertEquals("ids stay unique", b.size, b.map { it.id }.toSet().size)
        val speed = b.first { it.kind == WidgetKind.SPEED }
        b = WidgetGrid.resize(b, speed.id)
        assertEquals(WidgetSize.SMALL, b.first { it.id == speed.id }.size)
        b = WidgetGrid.move(b, speed.id, 1)
        assertEquals(speed.id, b[1].id)
        assertEquals("can't move past the start", b, WidgetGrid.move(b, b[0].id, -1))
        b = WidgetGrid.remove(b, speed.id)
        assertFalse(b.any { it.id == speed.id })
    }

    @Test fun boardSurvivesSaveAndIgnoresUnknownOrInvalid() {
        val saved = WidgetGrid.encode(WidgetGrid.DEFAULT)
        assertEquals(WidgetGrid.DEFAULT.map { it.kind to it.size }, WidgetGrid.decode(saved).map { it.kind to it.size })
        val odd = WidgetGrid.decode("SPEED:LARGE,NOPE:SMALL,VOLUME:LARGE")
        assertEquals(listOf(WidgetKind.SPEED to WidgetSize.LARGE, WidgetKind.VOLUME to WidgetSize.WIDE), odd.map { it.kind to it.size })
        assertEquals(WidgetGrid.DEFAULT, WidgetGrid.decode(null))
    }

    @Test fun tripCountsDrivingOnly() {
        var s = TripStats()
        var last: TripMath.Point? = null
        // 60 s at ~72 km/h heading north: 20 m/s ≈ 0.00018° latitude per second.
        for (i in 0..60) {
            val p = TripMath.Point(16.0 + i * 0.00018, 42.0, 72f, 5f, i * 1000L)
            s = TripMath.step(s, last, p); last = p
        }
        assertEquals(1.2, s.distanceKm, 0.05)
        assertEquals(60, s.movingSeconds)
        assertEquals(72f, s.maxKmh, 0.01f)
        assertEquals(72f, s.avgKmh, 3f)
        // Standing still with GPS jitter adds nothing; a 5 km jump is rejected.
        val still = TripMath.step(s, last, TripMath.Point(last!!.lat + 0.000003, 42.0, 0f, 5f, 61_000L))
        assertEquals(s.distanceKm, still.distanceKm, 1e-9)
        val jump = TripMath.step(s, last, TripMath.Point(last.lat + 0.045, 42.0, 70f, 5f, 61_000L))
        assertEquals(s.distanceKm, jump.distanceKm, 1e-9)
        // Poor accuracy is ignored.
        assertEquals(s, TripMath.step(s, last, TripMath.Point(last.lat + 0.0002, 42.0, 72f, 120f, 62_000L)))
    }

    @Test fun sunriseSunsetJazan() {
        val zone = TimeZone.getTimeZone("Asia/Riyadh")
        // 29 Oct 2026, Jazan: sunrise ≈ 06:00, sunset ≈ 17:35 local.
        val noon = java.util.Calendar.getInstance(zone).apply { set(2026, 9, 29, 12, 0, 0) }.timeInMillis
        val (rise, set) = SunCalc.riseSet(16.89, 42.55, noon, zone)
        assertNotNull(rise); assertNotNull(set)
        fun hour(t: Long) = java.util.Calendar.getInstance(zone).apply { timeInMillis = t }.let { it.get(java.util.Calendar.HOUR_OF_DAY) + it.get(java.util.Calendar.MINUTE) / 60.0 }
        assertEquals(6.0, hour(rise!!), 0.5)
        assertEquals(17.6, hour(set!!), 0.5)
    }

    @Test fun bmwOptionsRoundTrip() {
        val o = StyleOptions(dark = false, showYoutube = false)
        assertEquals(o, StyleOptionsRepository.decode(StyleOptionsRepository.encode(o)))
        assertTrue("old saves keep YouTube on", StyleOptionsRepository.decode("1|0|0|0|0||0").showYoutube)
    }
}
