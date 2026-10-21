package com.abu00salman.novadrive.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.CarPaints
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.core.carModel
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.AppsRepository
import com.abu00salman.novadrive.data.MediaRepository
import com.abu00salman.novadrive.data.NovaNotificationListener
import com.abu00salman.novadrive.ui.theme.tr

/** Resolves user-chosen shortcut apps, falling back to smart detection. */
class Shortcuts(val carPlay: AppEntry?, val nav: AppEntry?, val music: AppEntry?, val radio: AppEntry?, val phone: AppEntry?)

@Composable
fun rememberShortcuts(c: AppContainer, s: NovaSettings, apps: List<AppEntry>): Shortcuts = remember(s, apps) {
    val r = c.apps
    Shortcuts(
        carPlay = r.byPackage(s.carPlayPackage) ?: r.guess(AppsRepository.CARPLAY_PKGS, AppsRepository.CARPLAY_KEYS),
        nav = r.byPackage(s.navPackage) ?: r.guess(AppsRepository.NAV_PKGS, AppsRepository.NAV_KEYS),
        music = r.byPackage(s.musicPackage) ?: r.guess(AppsRepository.MUSIC_PKGS, AppsRepository.MUSIC_KEYS),
        radio = r.byPackage(s.radioPackage) ?: r.guess(emptyList(), AppsRepository.RADIO_KEYS),
        phone = r.byPackage(s.phonePackage) ?: r.guess(emptyList(), AppsRepository.PHONE_KEYS),
    )
}

const val ANDROID_AUTO_PKG = "com.google.android.projection.gearhead"
const val YOUTUBE_PKG = "com.google.android.youtube"
const val NETFLIX_PKG = "com.netflix.mediaclient"

/** The eight dock positions, each bound to a real installed app when one exists. */
@Composable
fun dockSlots(c: AppContainer, sc: Shortcuts, apps: List<AppEntry>): List<DockSlot> {
    val maps = tr("Maps", "خرائط")
    val music = tr("Music", "الموسيقى")
    val phone = tr("Phone", "الهاتف")
    val all = tr("All apps", "جميع التطبيقات")
    return remember(sc, apps, maps) {
        val r = c.apps
        // ZLink/DiPlay also run Android Auto, so fall back to the CarPlay box app.
        val androidAuto = r.byPackage(ANDROID_AUTO_PKG) ?: r.guess(emptyList(), listOf("android auto", "androidauto"))
            ?: sc.carPlay?.takeIf { it.packageName.contains("zlink", true) || it.packageName.contains("diplay", true) }
        listOf(
            DockSlot("maps", maps, sc.nav, Icons.Rounded.Map, storePackage = "com.google.android.apps.maps"),
            DockSlot("music", music, sc.music, Icons.Rounded.MusicNote, storePackage = "com.spotify.music"),
            DockSlot("phone", phone, sc.phone, Icons.Rounded.Phone, builtIn = true),
            DockSlot("carplay", "Apple CarPlay", sc.carPlay, Icons.Rounded.PhoneIphone),
            DockSlot("auto", "Android Auto", androidAuto, Icons.Rounded.DirectionsCar, storePackage = ANDROID_AUTO_PKG),
            DockSlot("youtube", "YouTube", r.byPackage(YOUTUBE_PKG), Icons.Rounded.OndemandVideo, storePackage = YOUTUBE_PKG),
            DockSlot("netflix", "Netflix", r.byPackage(NETFLIX_PKG), Icons.Rounded.Movie, storePackage = NETFLIX_PKG),
            DockSlot("apps", all, null, Icons.Rounded.Apps, builtIn = true),
        )
    }
}

/** Collects live state for the dashboard. Each flow is only observed while home is visible. */
@Composable
fun HomeScreen(c: AppContainer, settings: NovaSettings, actions: (DashUi) -> DashActions) {
    val ctx = LocalContext.current
    val apps by c.apps.apps.collectAsStateWithLifecycle()
    val sc = rememberShortcuts(c, settings, apps)
    val fix by c.fix.collectAsStateWithLifecycle()
    val perms by c.permissions.collectAsStateWithLifecycle()
    val weather by c.weather.weather.collectAsStateWithLifecycle()
    val city by c.weather.city.collectAsStateWithLifecycle()
    val vehicle by c.obd.data.collectAsStateWithLifecycle()
    val media by c.mediaInfo.collectAsStateWithLifecycle()
    val nav by NovaNotificationListener.navigation.collectAsStateWithLifecycle()
    val hasLocation = remember(perms) { c.location.hasPermission() }
    val access = remember(perms) { MediaRepository.hasAccess(ctx) }
    val dock = dockSlots(c, sc, apps)

    val ui = DashUi(
        speedKmh = fix?.speedKmh,
        hasLocation = hasLocation,
        vehicle = vehicle,
        obdEnabled = settings.obdEnabled,
        car = carModel(settings.carBrand, settings.carModel),
        paint = CarPaints.getOrElse(settings.carPaint) { CarPaints[0] }.color,
        weather = weather,
        city = city,
        showWeather = settings.showWeather,
        media = media,
        notificationAccess = access,
        nav = nav,
        navAppLabel = sc.nav?.label,
        homeSet = settings.homeAddress.isNotBlank(),
        workSet = settings.workAddress.isNotBlank(),
        dock = dock,
        use24h = settings.use24h,
    )
    Dashboard(ui, remember(ui) { actions(ui) })
}
