package com.abu00salman.novadrive.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Screen brightness that works on every head unit. Many units drive the backlight from their own
 * MCU and ignore Android's brightness setting, so NovaDrive dims with a black, touch-through
 * overlay instead: over every app when "display over other apps" is allowed, otherwise inside
 * NovaDrive only (drawn by the UI from [level]). The level is remembered across restarts.
 */
class ScreenDimmer(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("dimmer", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val _level = MutableStateFlow(prefs.getFloat("level", 1f).coerceIn(MIN, 1f))
    /** 1 = full brightness, [MIN] = darkest. */
    val level: StateFlow<Float> = _level
    private var view: View? = null

    fun canOverlay(): Boolean = Settings.canDrawOverlays(ctx)

    fun set(fraction: Float) {
        val v = fraction.coerceIn(MIN, 1f)
        _level.value = v
        prefs.edit().putFloat("level", v).apply()
        apply()
    }

    /** Shows, updates or removes the system-wide overlay to match [level]. */
    fun apply() = main.post {
        val alpha = overlayAlpha(_level.value)
        val wm = ctx.getSystemService(WindowManager::class.java) ?: return@post
        if (!canOverlay() || alpha <= 0.01f) {
            view?.let { v -> runCatching { wm.removeView(v) } }
            view = null
            return@post
        }
        val lp = (view?.layoutParams as? WindowManager.LayoutParams) ?: WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        // Window alpha (not view alpha) so Android keeps passing touches through (limit 0.8).
        lp.alpha = alpha
        val v = view
        if (v == null) {
            val nv = View(ctx).apply { setBackgroundColor(Color.BLACK) }
            runCatching { wm.addView(nv, lp); view = nv }
        } else runCatching { wm.updateViewLayout(v, lp) }
    }

    fun requestOverlayPermission(): Boolean = runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ); true
    }.getOrDefault(false)

    companion object {
        const val MIN = 0.1f
        /** Darkness for a brightness level; capped below Android's 0.8 touch-through limit. */
        fun overlayAlpha(level: Float): Float = ((1f - level) * 0.83f).coerceIn(0f, 0.75f)
    }
}
