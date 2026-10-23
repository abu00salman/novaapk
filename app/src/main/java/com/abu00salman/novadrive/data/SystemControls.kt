package com.abu00salman.novadrive.data

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Window
import kotlin.math.roundToInt

/** Thin, honest wrappers over real Android APIs. Every call degrades gracefully. */
class SystemControls(private val ctx: Context) {
    private val audio = ctx.getSystemService(AudioManager::class.java)

    // ---- Volume -------------------------------------------------------------------------
    fun volume(): Float {
        val am = audio ?: return 0f
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return am.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    }

    fun setVolume(fraction: Float) {
        val am = audio ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, (fraction.coerceIn(0f, 1f) * max).roundToInt(), 0) }
    }

    fun volumeStep(up: Boolean) {
        runCatching {
            audio?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
        }
    }

    // ---- Brightness ---------------------------------------------------------------------
    fun canWriteSettings(): Boolean = Settings.System.canWrite(ctx)

    fun brightness(): Float = windowBrightness ?: runCatching {
        (Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f).coerceIn(0f, 1f)
    }.getOrDefault(0.5f)

    /**
     * System-wide when allowed. The launcher window is always set too: some head units ignore the
     * system value (they drive the backlight themselves), and the window value always applies
     * while NovaDrive is on screen.
     */
    fun setBrightness(fraction: Float, window: Window?) {
        val f = fraction.coerceIn(0.02f, 1f)
        if (canWriteSettings()) {
            runCatching {
                Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (f * 255).roundToInt())
            }
        }
        if (window != null) runCatching {
            val lp = window.attributes
            lp.screenBrightness = f
            window.attributes = lp
        }
        windowBrightness = f
    }

    /** Last level NovaDrive applied, so the controls show what the screen actually uses. */
    var windowBrightness: Float? = null
        private set

    fun requestWriteSettings() = start(
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName))
    )

    // ---- System panels ------------------------------------------------------------------
    fun openWifi() = if (Build.VERSION.SDK_INT >= 29) start(Intent(Settings.Panel.ACTION_WIFI))
        .let { ok -> if (!ok) start(Intent(Settings.ACTION_WIFI_SETTINGS)) else true }
    else start(Intent(Settings.ACTION_WIFI_SETTINGS))

    fun openBluetooth() = start(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    fun openSystemSettings() = start(Intent(Settings.ACTION_SETTINGS))
    /** Opens NovaDrive's own notification-access switch when the OS supports it (Android 11+). */
    fun openNotificationAccess(): Boolean {
        if (Build.VERSION.SDK_INT >= 30) {
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(ctx, com.abu00salman.novadrive.data.NovaNotificationListener::class.java).flattenToString())
            if (start(detail)) return true
        }
        return start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) || openAppDetails()
    }

    /**
     * Opens [intent] in the adjacent multi-window pane next to NovaDrive. Android decides: on units
     * without split-screen support the app simply opens full screen.
     */
    fun launchAdjacent(intent: Intent): Boolean = start(
        Intent(intent).addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    )
    fun openHomeSettings() = start(Intent(Settings.ACTION_HOME_SETTINGS)) || openSystemSettings()
    fun openLocationSettings() = start(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    fun openAppDetails() = start(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
    )

    fun dial() = start(Intent(Intent.ACTION_DIAL))

    // ---- Navigation ---------------------------------------------------------------------
    fun navigateTo(query: String, navPackage: String?): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return false
        if (navPackage == "com.waze") {
            if (start(Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=" + Uri.encode(q) + "&navigate=yes")).setPackage("com.waze"))) return true
        }
        val google = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(q)))
        if (navPackage == null || navPackage == "com.google.android.apps.maps") {
            if (start(Intent(google).setPackage("com.google.android.apps.maps"))) return true
        }
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(q)))
        if (navPackage != null && start(Intent(geo).setPackage(navPackage))) return true
        return start(google) || start(geo)
    }

    // ---- Split screen (best effort; depends on the unit's multi-window support) ----------
    fun launchSplit(primary: Intent, secondary: Intent): Boolean {
        val ok = start(primary)
        Handler(Looper.getMainLooper()).postDelayed({
            start(
                Intent(secondary).addFlags(
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
            )
        }, 700)
        return ok
    }

    // ---- Launcher role ------------------------------------------------------------------
    fun isDefaultLauncher(): Boolean {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val ri = ctx.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
        return ri?.activityInfo?.packageName == ctx.packageName
    }

    fun otherLaunchers(): List<ResolveInfo> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        return ctx.packageManager.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != ctx.packageName && it.activityInfo.packageName != "com.android.settings" }
    }

    fun label(ri: ResolveInfo): String = ri.loadLabel(ctx.packageManager).toString()

    fun openLauncher(ri: ResolveInfo): Boolean = start(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .setComponent(ComponentName(ri.activityInfo.packageName, ri.activityInfo.name))
    )

    // ---- Device info --------------------------------------------------------------------
    fun totalRamGb(): Double {
        val am = ctx.getSystemService(ActivityManager::class.java) ?: return 0.0
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.totalMem / 1_073_741_824.0
    }

    fun start(intent: Intent): Boolean = try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (e: Exception) {
        false
    }
}
