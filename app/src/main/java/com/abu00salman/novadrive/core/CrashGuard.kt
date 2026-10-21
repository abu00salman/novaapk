package com.abu00salman.novadrive.core

import android.content.Context

/**
 * Records crashes. Two crashes within two minutes put the launcher in Safe Mode on the next
 * start: optional subsystems (weather, OBD, auto-launch) stay off and the user gets a one-tap
 * way back to the stock launcher.
 */
object CrashGuard {
    private const val PREFS = "nova_crash"
    private const val WINDOW_MS = 120_000L

    fun install(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val now = System.currentTimeMillis()
                val times = recent(prefs.getString("times", "") ?: "", now) + now
                prefs.edit()
                    .putString("times", times.joinToString(","))
                    .putString("last", error.stackTraceToString().take(6000))
                    .commit()
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun recent(raw: String, now: Long): List<Long> =
        raw.split(',').mapNotNull { it.toLongOrNull() }.filter { now - it < WINDOW_MS }

    fun isSafeMode(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("safe", false)) return true
        val crashes = recent(prefs.getString("times", "") ?: "", System.currentTimeMillis())
        if (crashes.size >= 2) {
            prefs.edit().putBoolean("safe", true).apply()
            return true
        }
        return false
    }

    fun exitSafeMode(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("safe", false).putString("times", "").apply()
    }

    fun lastCrash(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("last", null)
}
