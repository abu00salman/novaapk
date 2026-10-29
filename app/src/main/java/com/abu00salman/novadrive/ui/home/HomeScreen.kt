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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.abu00salman.novadrive.ui.nav.toNavInfo
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.CarPaints
import com.abu00salman.novadrive.core.HomeMode
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.core.carBrand
import com.abu00salman.novadrive.core.carModel
import com.abu00salman.novadrive.core.CUSTOM_PREFIX
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
        // Prefer the unit's own dialer (built-in BT phone) over any name-based guess.
        phone = r.byPackage(s.phonePackage) ?: r.dialer() ?: r.guess(emptyList(), AppsRepository.PHONE_KEYS)
            ?.takeUnless { it.label.contains("finder", true) || it.packageName.contains("finder", true) },
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
        fun app(id: String): AppEntry? = if (id.startsWith(CUSTOM_PREFIX)) r.byPackage(id.removePrefix(CUSTOM_PREFIX)) else when (id) {
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
            val custom = id.startsWith(CUSTOM_PREFIX)
            DockSlot(
                id, if (custom) app(id)?.label ?: id.removePrefix(CUSTOM_PREFIX) else dockTitle(id, ar), app(id),
                when (id) {
                    "maps" -> Icons.Rounded.Map; "carplay" -> Icons.Rounded.PhoneIphone; "auto" -> Icons.Rounded.DirectionsCar
                    "music" -> Icons.Rounded.MusicNote; "phone" -> Icons.Rounded.Phone; "radio" -> Icons.Rounded.Radio
                    "youtube" -> Icons.Rounded.OndemandVideo; "netflix" -> Icons.Rounded.Movie; else -> Icons.Rounded.Apps
                },
                storePackage = if (custom) id.removePrefix(CUSTOM_PREFIX) else when (id) {
                    "maps" -> "com.google.android.apps.maps"; "music" -> "com.spotify.music"; "auto" -> ANDROID_AUTO_PKG
                    "youtube" -> YOUTUBE_PKG; "netflix" -> NETFLIX_PKG; else -> null
                },
                builtIn = id == "phone" || id == "apps",
            )
        }
            // A shortcut whose app is missing only stays when it can actually be installed (Play Store present).
            .filter { it.app != null || it.builtIn || (it.storePackage != null && hasStore(c)) }
    }
}

private fun hasStore(c: AppContainer): Boolean =
    runCatching { c.appContext.packageManager.getPackageInfo("com.android.vending", 0); true }.getOrDefault(false)

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
    val externalNav by NovaNotificationListener.navigation.collectAsStateWithLifecycle()
    val trip by c.navEngine.state.collectAsStateWithLifecycle()
    val styleOpts by c.styleOptions.all.collectAsStateWithLifecycle()
    // In-app guidance wins over a navigation app's notification.
    val nav = trip?.toNavInfo(settings.arabic, settings.use24h) ?: externalNav
    val hasLocation = remember(perms) { c.location.hasPermission() }
    val access = remember(perms) { MediaRepository.hasAccess(ctx) }
    val dock = dockSlots(c, settings, sc, apps, driving)
    val youtubeApp = remember(apps) { c.apps.byPackage(YOUTUBE_PKG) }
    val youtube = youtubeApp?.let { DockSlot("youtube", "YouTube", it, Icons.Rounded.OndemandVideo, storePackage = YOUTUBE_PKG) }
    val bigPkg = (styleOptsFor(c, settings.uiStyle)).bigApp
    val bigApp = remember(apps, bigPkg) { bigPkg?.let { c.apps.byPackage(it) } }
    val fullScreenStyle = settings.uiStyle == "tesla" || settings.uiStyle == "bmw"
    val bt by c.bluetooth.collectAsStateWithLifecycle()
    val net by c.network.collectAsStateWithLifecycle()
    val iPhone by remember(fullScreenStyle) { if (fullScreenStyle) c.carPlay.connected() else kotlinx.coroutines.flow.flowOf(false) }
        .collectAsStateWithLifecycle(false)
    val gps = when {
        !hasLocation || !c.status.gpsEnabled() -> com.abu00salman.novadrive.ui.shell.GpsState.OFF
        fix?.fromGps == true -> com.abu00salman.novadrive.ui.shell.GpsState.FIX
        else -> com.abu00salman.novadrive.ui.shell.GpsState.SEARCHING
    }

    val widgetsStyle = settings.uiStyle == "widgets"
    val board by c.widgets.items.collectAsStateWithLifecycle()
    if (widgetsStyle) LaunchedEffect(Unit) { c.trip.start() }
    val tripStats by c.trip.stats.collectAsStateWithLifecycle()
    val ui = HomeUi(
        widgets = board,
        trip = if (widgetsStyle) tripStats else null,
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
        carImageVersion = settings.carImageVersion,
        simple = settings.uiStyle == "simple",
        style = settings.uiStyle,
        lat = fix?.lat,
        lon = fix?.lon,
        bearing = fix?.bearing,
        bt = bt,
        net = net,
        gps = gps,
        carPlayConnected = iPhone,
        railRight = settings.railRight,
        youtube = youtube,
        driving = driving,
        bigApp = bigApp,
        bigAppMissing = bigPkg != null && bigApp == null,
        routePoints = trip?.route?.points,
        routeProgress = trip?.progressIndex ?: 0,
        destination = trip?.destination?.point,
        altitudeM = fix?.altitudeM,
        styleOptions = styleOpts[settings.uiStyle] ?: com.abu00salman.novadrive.core.StyleOptions(),
        video = if (driving) null else c.apps.byPackage(YOUTUBE_PKG),
    )
    HomeLayout(ui, mode, autoPromoted, remember(ui) { actions(ui) })
}

@Composable
private fun styleOptsFor(c: AppContainer, style: String): com.abu00salman.novadrive.core.StyleOptions {
    val all by c.styleOptions.all.collectAsStateWithLifecycle()
    return all[style] ?: com.abu00salman.novadrive.core.StyleOptions()
}
