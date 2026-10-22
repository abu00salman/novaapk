package com.abu00salman.novadrive.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.CarPaints
import com.abu00salman.novadrive.core.HomeMode
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.core.carBrand
import com.abu00salman.novadrive.core.carModel
import com.abu00salman.novadrive.core.resolveDock
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.AppsRepository
import com.abu00salman.novadrive.data.MediaRepository
import com.abu00salman.novadrive.data.NovaNotificationListener
import com.abu00salman.novadrive.ui.theme.LocalArabic

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

/** Localised dock title for an action id. */
fun dockTitle(id: String, ar: Boolean): String = when (id) {
    "maps" -> if (ar) "خرائط" else "Maps"
    "carplay" -> "Apple CarPlay"
    "auto" -> "Android Auto"
    "music" -> if (ar) "الموسيقى" else "Music"
    "phone" -> if (ar) "الهاتف" else "Phone"
    "radio" -> if (ar) "الراديو" else "Radio"
    "youtube" -> "YouTube"
    "netflix" -> "Netflix"
    "apps" -> if (ar) "التطبيقات" else "Apps"
    else -> id
}

/** The user's dock, bound to real installed apps; duplicates and (while driving) video are removed. */
@Composable
fun dockSlots(c: AppContainer, s: NovaSettings, sc: Shortcuts, apps: List<AppEntry>, driving: Boolean): List<DockSlot> {
    val ar = LocalArabic.current
    return remember(s.dock, sc, apps, driving, ar) {
        val r = c.apps
        val androidAuto = r.byPackage(ANDROID_AUTO_PKG) ?: r.guess(emptyList(), listOf("android auto", "androidauto"))
            ?: sc.carPlay?.takeIf { it.packageName.contains("zlink", true) || it.packageName.contains("diplay", true) }
        fun app(id: String): AppEntry? = when (id) {
            "maps" -> sc.nav
            "carplay" -> sc.carPlay
            "auto" -> androidAuto
            "music" -> sc.music
            "phone" -> sc.phone
            "radio" -> sc.radio
            "youtube" -> r.byPackage(YOUTUBE_PKG)
            "netflix" -> r.byPackage(NETFLIX_PKG)
            else -> null
        }
        resolveDock(s.dock, driving, packageOf = { app(it)?.packageName }) { id ->
            DockSlot(
                id, dockTitle(id, ar), app(id),
                when (id) {
                    "maps" -> Icons.Rounded.Map; "carplay" -> Icons.Rounded.PhoneIphone; "auto" -> Icons.Rounded.DirectionsCar
                    "music" -> Icons.Rounded.MusicNote; "phone" -> Icons.Rounded.Phone; "radio" -> Icons.Rounded.Radio
                    "youtube" -> Icons.Rounded.OndemandVideo; "netflix" -> Icons.Rounded.Movie; else -> Icons.Rounded.Apps
                },
                storePackage = when (id) {
                    "maps" -> "com.google.android.apps.maps"; "music" -> "com.spotify.music"; "auto" -> ANDROID_AUTO_PKG
                    "youtube" -> YOUTUBE_PKG; "netflix" -> NETFLIX_PKG; else -> null
                },
                builtIn = id == "phone" || id == "apps",
            )
        }
    }
}

/** Collects live state for the home screen. Flows are only observed while home is visible. */
@Composable
fun HomeScreen(
    c: AppContainer,
    settings: NovaSettings,
    driving: Boolean,
    mode: HomeMode,
    autoPromoted: Boolean,
    actions: (HomeUi) -> HomeActions,
) {
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
    val dock = dockSlots(c, settings, sc, apps, driving)

    val ui = HomeUi(
        speedKmh = fix?.speedKmh,
        hasLocation = hasLocation,
        vehicle = vehicle,
        obdEnabled = settings.obdEnabled,
        brand = carBrand(settings.carBrand),
        car = carModel(settings.carBrand, settings.carModel),
        paint = CarPaints.getOrElse(settings.carPaint) { CarPaints[0] },
        weather = weather,
        city = city,
        showWeather = settings.showWeather,
        media = media,
        notificationAccess = access,
        nav = nav,
        navApp = sc.nav,
        homeSet = settings.homeAddress.isNotBlank(),
        workSet = settings.workAddress.isNotBlank(),
        dock = dock,
        use24h = settings.use24h,
    )
    HomeLayout(ui, mode, autoPromoted, remember(ui) { actions(ui) })
}
