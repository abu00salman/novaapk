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

/** A media notification: its session token when published, otherwise just the visible track. */
@Immutable
data class MediaNotice(
    val packageName: String,
    val token: android.media.session.MediaSession.Token?,
    val title: String?,
    val text: String?,
    val open: PendingIntent?,
    /** The notification's own buttons (usually previous · play/pause · next), in order. */
    val actions: List<NoticeAction> = emptyList(),
)

@Immutable
data class NoticeAction(val title: String, val intent: PendingIntent?)

/**
 * Granting notification access lets NOVA read now-playing info (media sessions) and the
 * guidance card that Google Maps / Waze post while navigating. Nothing is stored or sent.
 */
class NovaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() = rescan()
    override fun onListenerDisconnected() { nav.value = null; media.value = emptyList() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (isNav(sbn) || isMedia(sbn)) rescan() else if (isNewMessage(sbn)) {
            // Interface notification sound (only if the current interface has one; rate limited).
            runCatching { (applicationContext as com.abu00salman.novadrive.NovaApp).container.soundFx.play(com.abu00salman.novadrive.core.SoundEvent.NOTIFY) }
        }
    }

    /** A message/e-mail/social notification from another app — not ongoing, not a group summary. */
    private fun isNewMessage(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (sbn.isOngoing || sbn.packageName == packageName) return false
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        return n.category == Notification.CATEGORY_MESSAGE || n.category == Notification.CATEGORY_EMAIL || n.category == Notification.CATEGORY_SOCIAL
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { if (sbn != null && (isNav(sbn) || isMedia(sbn))) rescan() }

    private fun rescan() {
        val list = runCatching { activeNotifications?.toList() }.getOrNull().orEmpty()
        nav.value = list.filter(::isNav).maxByOrNull { it.postTime }?.let(::parse)
        media.value = list.filter(::isMedia).sortedByDescending { it.postTime }.mapNotNull(::parseMedia)
    }

    /**
     * Players whose session isn't listed as active (e.g. the head unit's own Bluetooth music app)
     * often still post a media notification; it carries the session token and/or the track.
     */
    private fun isMedia(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return true
        if (n.category == Notification.CATEGORY_TRANSPORT) return true
        val pkg = sbn.packageName.lowercase()
        return sbn.isOngoing && ("bluetooth" in pkg || "btmusic" in pkg || ".bt" in pkg || "a2dp" in pkg)
    }

    private fun parseMedia(sbn: StatusBarNotification): MediaNotice? {
        val ex = sbn.notification.extras
        @Suppress("DEPRECATION")
        val token = ex.getParcelable<android.media.session.MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        if (token == null && title == null) return null
        return MediaNotice(
            packageName = sbn.packageName,
            token = token,
            title = title,
            text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()?.takeIf { it.isNotEmpty() },
            open = sbn.notification.contentIntent,
            actions = sbn.notification.actions.orEmpty().map { NoticeAction(it.title?.toString().orEmpty(), it.actionIntent) },
        )
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
        private val media = MutableStateFlow<List<MediaNotice>>(emptyList())
        /** Current media notifications, newest first. */
        val mediaNotices: StateFlow<List<MediaNotice>> = media
    }
}
