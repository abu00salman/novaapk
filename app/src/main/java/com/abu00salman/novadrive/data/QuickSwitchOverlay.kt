package com.abu00salman.novadrive.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.MainActivity
import com.abu00salman.novadrive.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * NOVA QuickSwitch + Mini Player: a small handle at the screen edge while another app (CarPlay, maps,
 * music…) is in front. Tapping it opens a bar that jumps straight to NovaDrive, navigation, CarPlay or
 * music and controls the current track with the real media session; it folds back by itself.
 *
 * Optional (off until enabled in Settings) and needs "display over other apps". It is only shown while
 * NovaDrive is in the background, and is built with plain views so it costs almost nothing.
 */
class QuickSwitchOverlay(private val c: AppContainer) {
    private val ctx: Context = c.appContext
    private val prefs = ctx.getSharedPreferences("quickswitch", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val wm get() = ctx.getSystemService(WindowManager::class.java)
    private val _enabled = MutableStateFlow(prefs.getBoolean("enabled", false))
    val enabled: StateFlow<Boolean> = _enabled

    private var root: LinearLayout? = null
    private var bar: LinearLayout? = null
    private var title: TextView? = null
    private var playButton: ImageView? = null
    private var mediaJob: Job? = null
    private var inBackground = false
    private val collapse = Runnable { setExpanded(false) }

    val isShowing: Boolean get() = root != null
    val isExpanded: Boolean get() = bar?.visibility == View.VISIBLE

    fun canOverlay(): Boolean = Settings.canDrawOverlays(ctx)

    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean("enabled", on).apply()
        _enabled.value = on
        if (!on) main.post { hide() } else if (inBackground) main.post { show() }
    }

    /** NovaDrive came to the front: the overlay is never drawn over NovaDrive itself. */
    fun onForeground() { inBackground = false; main.post { hide() } }

    /** Another app is in front now. */
    fun onBackground() { inBackground = true; main.post { show() } }

    private fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    private fun pill(color: Int, radiusDp: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radiusDp).toFloat() }

    @SuppressLint("ClickableViewAccessibility")
    private fun show() {
        if (root != null || !_enabled.value || !canOverlay()) return
        val ar = c.settings.current.arabic
        val container = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_LTR }

        val strip = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = pill(0xF2101522.toInt(), 30)
            setPadding(dp(8), dp(6), dp(8), dp(6))
            visibility = View.GONE
        }
        fun button(icon: Int, label: String, tint: Boolean = true, onClick: () -> Unit): ImageView = ImageView(ctx).apply {
            setImageResource(icon)
            if (!tint) clipToOutline = true
            contentDescription = label
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = pill(0x22FFFFFF, 28)
            layoutParams = LinearLayout.LayoutParams(dp(BUTTON_DP), dp(BUTTON_DP)).apply { marginStart = dp(3); marginEnd = dp(3) }
            setOnClickListener { touch(); onClick() }
        }
        strip.addView(button(R.drawable.ic_qs_home, if (ar) "نوفا درايف" else "NovaDrive") { openNova() })
        strip.addView(button(R.drawable.ic_qs_nav, if (ar) "الملاحة" else "Navigation") { open(shortcuts().nav, if (ar) "تطبيق الملاحة" else "navigation app") })
        strip.addView(button(R.drawable.ic_qs_phone, "CarPlay") { open(shortcuts().carPlay, "CarPlay") })
        strip.addView(button(R.drawable.ic_qs_music, if (ar) "الموسيقى" else "Music") {
            val playing = c.mediaInfo.value?.packageName?.let { c.apps.byPackage(it) }
            open(playing ?: shortcuts().music, if (ar) "تطبيق الموسيقى" else "music app")
        })
        strip.addView(View(ctx).apply { setBackgroundColor(0x33FFFFFF); layoutParams = LinearLayout.LayoutParams(dp(1), dp(36)).apply { marginStart = dp(6); marginEnd = dp(6) } })
        strip.addView(button(R.drawable.ic_qs_prev, if (ar) "السابق" else "Previous") { c.media.previous() })
        val play = button(R.drawable.ic_qs_play, if (ar) "تشغيل / إيقاف" else "Play / pause") { c.media.playPause() }
        strip.addView(play)
        strip.addView(button(R.drawable.ic_qs_next, if (ar) "التالي" else "Next") { c.media.next() })
        val text = TextView(ctx).apply {
            setTextColor(0xFFFFFFFF.toInt()); textSize = 15f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(dp(170), LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8); marginEnd = dp(4) }
        }
        strip.addView(text)
        strip.addView(button(R.drawable.ic_qs_close, if (ar) "طي" else "Fold") { setExpanded(false) })

        val handle = ImageView(ctx).apply {
            setImageResource(R.drawable.nova_logo)
            contentDescription = "NOVA QuickSwitch"
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = pill(0xCC101522.toInt(), 30)
            layoutParams = LinearLayout.LayoutParams(dp(BUTTON_DP), dp(BUTTON_DP)).apply { marginStart = dp(6) }
        }
        container.addView(strip); container.addView(handle)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.END or Gravity.TOP; x = 0; y = prefs.getInt("y", dp(150)) }

        // Tap the handle to open/fold; drag it up or down to move it out of the way.
        val slop = ViewConfiguration.get(ctx).scaledTouchSlop
        var downY = 0f; var startY = 0; var dragging = false
        handle.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; startY = lp.y; dragging = false }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragging && abs(e.rawY - downY) > slop) dragging = true
                    if (dragging) {
                        lp.y = (startY + (e.rawY - downY).toInt()).coerceIn(0, (ctx.resources.displayMetrics.heightPixels - dp(BUTTON_DP + 20)).coerceAtLeast(0))
                        runCatching { wm?.updateViewLayout(container, lp) }
                    }
                }
                MotionEvent.ACTION_UP -> if (dragging) prefs.edit().putInt("y", lp.y).apply() else setExpanded(!isExpanded)
            }
            true
        }

        if (runCatching { wm?.addView(container, lp) }.isFailure) return
        root = container; bar = strip; title = text; playButton = play
        mediaJob = c.scope.launch { c.mediaInfo.collect { render(it) } }
    }

    private fun render(m: MediaInfo?) {
        val ar = c.settings.current.arabic
        title?.text = m?.let { listOfNotNull(it.title, it.artist).joinToString(" — ") } ?: if (ar) "لا يوجد تشغيل" else "Nothing playing"
        playButton?.setImageResource(if (m?.playing == true) R.drawable.ic_qs_pause else R.drawable.ic_qs_play)
    }

    private fun touch() { main.removeCallbacks(collapse); main.postDelayed(collapse, FOLD_MS) }

    fun setExpanded(open: Boolean) {
        bar?.visibility = if (open) View.VISIBLE else View.GONE
        main.removeCallbacks(collapse)
        if (open) main.postDelayed(collapse, FOLD_MS)
    }

    private fun hide() {
        main.removeCallbacks(collapse)
        mediaJob?.cancel(); mediaJob = null
        root?.let { v -> runCatching { wm?.removeView(v) } }
        root = null; bar = null; title = null; playButton = null
    }

    private fun shortcuts() = Shortcuts.resolve(c.apps, c.settings.current)

    private fun open(app: AppEntry?, what: String) {
        setExpanded(false)
        if (app == null || !c.apps.launch(app)) {
            val ar = c.settings.current.arabic
            Toast.makeText(ctx, if (ar) "اختر $what من إعدادات NovaDrive ← الاختصارات" else "Choose a $what in NovaDrive Settings › Shortcuts", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNova() {
        setExpanded(false)
        runCatching {
            ctx.startActivity(Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    companion object {
        /** Automotive-size touch targets. */
        const val BUTTON_DP = 60
        private const val FOLD_MS = 7_000L
    }
}
