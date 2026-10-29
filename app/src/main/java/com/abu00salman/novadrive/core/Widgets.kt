package com.abu00salman.novadrive.core

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Widget sizes in grid cells (columns × rows), like the iOS/Android home-screen sizes. */
enum class WidgetSize(val w: Int, val h: Int) { SMALL(1, 1), WIDE(2, 1), TALL(1, 2), LARGE(2, 2) }

/** Every gauge/reading the Widgets interface can show. All values are live from the unit or the car. */
enum class WidgetKind(val en: String, val ar: String, val sizes: List<WidgetSize>) {
    SPEED("Speed", "السرعة", listOf(WidgetSize.LARGE, WidgetSize.SMALL, WidgetSize.WIDE, WidgetSize.TALL)),
    CLOCK("Clock", "الساعة", listOf(WidgetSize.SMALL, WidgetSize.WIDE, WidgetSize.LARGE)),
    CALENDAR("Calendar", "التقويم", listOf(WidgetSize.SMALL, WidgetSize.WIDE)),
    WEATHER("Weather", "الطقس", listOf(WidgetSize.SMALL, WidgetSize.WIDE)),
    COMPASS("Compass", "البوصلة", listOf(WidgetSize.SMALL, WidgetSize.TALL, WidgetSize.LARGE)),
    ALTITUDE("Altitude", "الارتفاع", listOf(WidgetSize.SMALL, WidgetSize.WIDE)),
    TRIP("Trip", "الرحلة", listOf(WidgetSize.WIDE, WidgetSize.SMALL, WidgetSize.LARGE)),
    GFORCE("G-force", "قوة التسارع", listOf(WidgetSize.SMALL, WidgetSize.TALL)),
    TILT("Inclinometer", "مقياس الميلان", listOf(WidgetSize.WIDE, WidgetSize.LARGE)),
    MAP("Map", "الخريطة", listOf(WidgetSize.LARGE, WidgetSize.WIDE, WidgetSize.TALL)),
    NAV("Next turn", "التوجيه", listOf(WidgetSize.WIDE, WidgetSize.SMALL)),
    MEDIA("Music", "الموسيقى", listOf(WidgetSize.WIDE, WidgetSize.LARGE)),
    OBD("Car (OBD-II)", "السيارة (OBD-II)", listOf(WidgetSize.WIDE, WidgetSize.LARGE)),
    COORDS("Coordinates", "الإحداثيات", listOf(WidgetSize.WIDE, WidgetSize.SMALL)),
    SUN("Sunrise & sunset", "الشروق والغروب", listOf(WidgetSize.SMALL, WidgetSize.WIDE)),
    STATUS("Connections", "الاتصالات", listOf(WidgetSize.WIDE, WidgetSize.SMALL)),
    VOLUME("Volume", "الصوت", listOf(WidgetSize.WIDE)),
    BRIGHTNESS("Brightness", "السطوع", listOf(WidgetSize.WIDE)),
}

@Immutable
data class WidgetItem(val id: Int, val kind: WidgetKind, val size: WidgetSize)

/** Where a widget sits on the grid (in cells). */
data class WidgetPlacement(val item: WidgetItem, val col: Int, val row: Int)

object WidgetGrid {
    const val COLUMNS = 4

    /** Default board: speed, time, weather, compass, altitude, music, trip, then more below. */
    val DEFAULT: List<WidgetItem> = listOf(
        WidgetKind.SPEED to WidgetSize.LARGE, WidgetKind.CLOCK to WidgetSize.SMALL, WidgetKind.WEATHER to WidgetSize.SMALL,
        WidgetKind.COMPASS to WidgetSize.SMALL, WidgetKind.ALTITUDE to WidgetSize.SMALL,
        WidgetKind.MEDIA to WidgetSize.WIDE, WidgetKind.TRIP to WidgetSize.WIDE,
        WidgetKind.MAP to WidgetSize.LARGE, WidgetKind.TILT to WidgetSize.WIDE, WidgetKind.CALENDAR to WidgetSize.SMALL, WidgetKind.SUN to WidgetSize.SMALL,
    ).mapIndexed { i, (k, s) -> WidgetItem(i + 1, k, s) }

    /**
     * Packs widgets in order, each at the first free spot reading left-to-right, top-to-bottom —
     * the way phone home screens flow. Returns placements and the number of rows used.
     */
    fun pack(items: List<WidgetItem>, columns: Int = COLUMNS): Pair<List<WidgetPlacement>, Int> {
        val used = HashSet<Pair<Int, Int>>()
        var rows = 0
        val out = items.map { item ->
            val w = item.size.w.coerceAtMost(columns)
            val h = item.size.h
            var row = 0
            var spot: Pair<Int, Int>? = null
            while (spot == null) {
                for (col in 0..columns - w) {
                    val free = (0 until w).all { dx -> (0 until h).all { dy -> (col + dx to row + dy) !in used } }
                    if (free) { spot = col to row; break }
                }
                if (spot == null) row++
            }
            val (c, r) = spot
            for (dx in 0 until w) for (dy in 0 until h) used += (c + dx to r + dy)
            rows = maxOf(rows, r + h)
            WidgetPlacement(item, c, r)
        }
        return out to rows
    }

    fun encode(items: List<WidgetItem>) = items.joinToString(",") { "${it.kind.name}:${it.size.name}" }

    fun decode(s: String?): List<WidgetItem> {
        if (s.isNullOrBlank()) return DEFAULT
        return s.split(',').mapNotNull { part ->
            val (k, z) = part.split(':').let { it.getOrNull(0) to it.getOrNull(1) }
            val kind = runCatching { WidgetKind.valueOf(k ?: "") }.getOrNull() ?: return@mapNotNull null
            val size = runCatching { WidgetSize.valueOf(z ?: "") }.getOrNull()?.takeIf { it in kind.sizes } ?: kind.sizes.first()
            kind to size
        }.mapIndexed { i, (k, z) -> WidgetItem(i + 1, k, z) }
    }

    fun add(items: List<WidgetItem>, kind: WidgetKind) = items + WidgetItem((items.maxOfOrNull { it.id } ?: 0) + 1, kind, kind.sizes.first())

    fun remove(items: List<WidgetItem>, id: Int) = items.filterNot { it.id == id }

    /** Next size this widget supports (tap the resize handle to cycle). */
    fun resize(items: List<WidgetItem>, id: Int) = items.map {
        if (it.id != id) it else it.copy(size = it.kind.sizes[(it.kind.sizes.indexOf(it.size) + 1) % it.kind.sizes.size])
    }

    /** Moves a widget earlier (-1) or later (+1) in the flow. */
    fun move(items: List<WidgetItem>, id: Int, by: Int): List<WidgetItem> {
        val i = items.indexOfFirst { it.id == id }
        val j = i + by
        if (i < 0 || j !in items.indices) return items
        return items.toMutableList().apply { add(j, removeAt(i)) }
    }
}

/** The Widgets interface's board, saved on the unit. */
class WidgetRepository(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("widgets", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(WidgetGrid.decode(prefs.getString("board", null)))
    val items: StateFlow<List<WidgetItem>> = _items

    fun set(items: List<WidgetItem>) {
        prefs.edit().putString("board", WidgetGrid.encode(items)).apply()
        _items.value = WidgetGrid.decode(WidgetGrid.encode(items)).let { decoded ->
            // Keep ids stable for smooth animations while editing.
            if (decoded.size == items.size) items else decoded
        }
    }

    fun reset() = set(WidgetGrid.DEFAULT)
}
