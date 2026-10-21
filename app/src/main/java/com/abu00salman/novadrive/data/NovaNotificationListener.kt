package com.abu00salman.novadrive.data

import android.app.Notification
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Live turn-by-turn guidance as published by the navigation app's ongoing notification. */
@Immutable
data class NavInfo(
    val packageName: String,
    /** Usually the distance to the next manoeuvre, e.g. "300 m". */
    val primary: String,
    /** Usually the road / instruction, e.g. "Toward King Abdullah Rd". */
    val secondary: String?,
    /** Usually remaining time · distance · arrival, when the app provides it. */
    val eta: String?,
    /** The app's own manoeuvre arrow, when it supplies one as the large icon. */
    val maneuver: ImageBitmap?,
    val open: PendingIntent?,
)

/**
 * Granting notification access lets NOVA read now-playing info (media sessions) and the
 * guidance card that Google Maps / Waze post while navigating. Nothing is stored or sent.
 */
class NovaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() = rescan()
    override fun onListenerDisconnected() { nav.value = null }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { if (sbn != null && isNav(sbn)) rescan() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { if (sbn != null && isNav(sbn)) rescan() }

    private fun rescan() {
        val list = runCatching { activeNotifications?.toList() }.getOrNull().orEmpty()
        nav.value = list.filter(::isNav).maxByOrNull { it.postTime }?.let(::parse)
    }

    private fun isNav(sbn: StatusBarNotification): Boolean =
        sbn.packageName in NAV_APPS && sbn.isOngoing ||
            sbn.notification.category == Notification.CATEGORY_NAVIGATION

    private fun parse(sbn: StatusBarNotification): NavInfo? {
        val n = sbn.notification
        val ex = n.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        if (title.isEmpty()) return null
        val icon: Bitmap? = runCatching {
            n.getLargeIcon()?.loadDrawable(this)?.let { d ->
                (d as? BitmapDrawable)?.bitmap ?: d.toBitmap(96, 96)
            }
        }.getOrNull()
        return NavInfo(
            packageName = sbn.packageName,
            primary = title,
            secondary = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()?.takeIf { it.isNotEmpty() },
            eta = ex.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()?.takeIf { it.isNotEmpty() },
            maneuver = icon?.asImageBitmap(),
            open = n.contentIntent,
        )
    }

    companion object {
        val NAV_APPS = setOf("com.google.android.apps.maps", "com.waze", "com.here.app.maps", "com.sygic.aura")
        private val nav = MutableStateFlow<NavInfo?>(null)
        val navigation: StateFlow<NavInfo?> = nav
    }
}
