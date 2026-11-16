package com.abu00salman.novadrive.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow

/**
 * Which apps the driver actually opens from NovaDrive, kept on the unit only. Ranking favours
 * recent use (a launch counts half as much after two weeks), which feeds the "Frequent" tab of the app
 * library and the smart dock slot.
 */
class AppUsage(ctx: Context, private val now: () -> Long = System::currentTimeMillis) {
    data class Use(val count: Float, val last: Long)

    private val prefs = ctx.getSharedPreferences("app_usage", Context.MODE_PRIVATE)
    private var uses: Map<String, Use> = prefs.all.mapNotNull { (k, v) -> parse(v as? String)?.let { k to it } }.toMap()
    private val _ranked = MutableStateFlow(rank(uses, now()))
    /** App keys (package/activity), most used first. */
    val ranked: StateFlow<List<String>> = _ranked

    fun record(key: String) {
        val t = now()
        val old = uses[key]
        // Older launches fade before the new one is added, so the score stays bounded.
        val n = Use((old?.let { decayed(it, t) } ?: 0f) + 1f, t)
        uses = (uses + (key to n)).let { m -> if (m.size > MAX) m.entries.sortedByDescending { decayed(it.value, t) }.take(MAX).associate { it.key to it.value } else m }
        prefs.edit().clear().apply { uses.forEach { (k, u) -> putString(k, "${u.count}|${u.last}") } }.apply()
        _ranked.value = rank(uses, t)
    }

    fun clear() { uses = emptyMap(); prefs.edit().clear().apply(); _ranked.value = emptyList() }

    companion object {
        const val MAX = 60
        private const val HALF_LIFE_MS = 14L * 24 * 3600_000

        fun decayed(u: Use, now: Long): Float = u.count * 0.5.pow((now - u.last).coerceAtLeast(0).toDouble() / HALF_LIFE_MS).toFloat()

        fun rank(uses: Map<String, Use>, now: Long): List<String> =
            uses.entries.sortedWith(compareByDescending<Map.Entry<String, Use>> { decayed(it.value, now) }.thenByDescending { it.value.last }).map { it.key }

        private fun parse(raw: String?): Use? {
            val p = raw?.split('|') ?: return null
            return Use(p.getOrNull(0)?.toFloatOrNull() ?: return null, p.getOrNull(1)?.toLongOrNull() ?: return null)
        }
    }
}
