package com.abu00salman.novadrive.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.AppsRepository
import com.abu00salman.novadrive.data.MediaRepository
import com.abu00salman.novadrive.data.VehicleData
import com.abu00salman.novadrive.ui.components.AppIcon
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.IconTile
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import com.abu00salman.novadrive.ui.toast
import java.util.Calendar

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

@Composable
fun HomeScreen(
    c: AppContainer,
    settings: NovaSettings,
    driving: Boolean,
    onApps: () -> Unit,
    onSettings: () -> Unit,
    onQuick: () -> Unit,
    onSplit: () -> Unit,
    onVoice: () -> Unit,
    onRequestLocation: () -> Unit,
) {
    val ctx = LocalContext.current
    val apps by c.apps.apps.collectAsStateWithLifecycle()
    val iPhone by c.iPhoneConnected.collectAsStateWithLifecycle()
    val sc = rememberShortcuts(c, settings, apps)
    val ar = settings.arabic

    fun openApp(entry: AppEntry?, what: String) {
        if (entry == null || !c.apps.launch(entry)) {
            toast(ctx, if (ar) "اختر تطبيق $what من الإعدادات" else "Choose a $what app in Settings")
            onSettings()
        }
    }

    // Auto-launch CarPlay when an iPhone is plugged in (only while NOVA is on screen).
    val latest by rememberUpdatedState(Pair(settings, sc))
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var prev = c.iPhoneConnected.value
            c.iPhoneConnected.collect { now ->
                val (s, shortcuts) = latest
                if (now && !prev && s.autoLaunchCarPlay && !c.safeMode) shortcuts.carPlay?.let { c.apps.launch(it) }
                prev = now
            }
        }
    }

    Row(Modifier.fillMaxSize().padding(14.dp)) {
        // ---- Side rail ----
        Column(
            Modifier.width(84.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BrandMark()
            IconTile(Icons.Rounded.Apps, tr("Apps", "التطبيقات"), size = 58.dp, onClick = onApps)
            IconTile(
                Icons.Rounded.PhoneIphone, "CarPlay", size = 58.dp, active = iPhone,
                badge = if (iPhone) LocalNova.current.ok else null
            ) { openApp(sc.carPlay, "CarPlay") }
            IconTile(Icons.Rounded.Navigation, tr("Navi", "الملاحة"), size = 58.dp) { openApp(sc.nav, if (ar) "الملاحة" else "navigation") }
            IconTile(Icons.Rounded.Phone, tr("Phone", "الهاتف"), size = 58.dp) {
                if (sc.phone == null || !c.apps.launch(sc.phone)) c.controls.dial()
            }
            IconTile(Icons.Rounded.Mic, tr("Voice", "صوت"), size = 58.dp, onClick = onVoice)
            IconTile(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), size = 58.dp, onClick = onSettings)
        }
        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f).fillMaxHeight()) {
            Header(settings, iPhone, driving, onQuick)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.weight(0.36f).fillMaxHeight()) {
                    ClockCard(settings.use24h, Modifier.weight(0.40f).fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    val media by c.mediaInfo.collectAsStateWithLifecycle()
                    val perms by c.permissions.collectAsStateWithLifecycle()
                    val access = remember(perms) { MediaRepository.hasAccess(ctx) }
                    MediaCard(
                        info = media, hasAccess = access,
                        onOpenApp = {
                            val pkg = media?.packageName
                            if (pkg == null || !c.apps.launchPackage(pkg)) openApp(sc.music, if (ar) "الموسيقى" else "music")
                        },
                        onGrantAccess = { c.controls.openNotificationAccess() },
                        onPrev = c.media::previous, onPlayPause = c.media::playPause, onNext = c.media::next,
                        modifier = Modifier.weight(0.60f).fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(12.dp))
                HeroCard(c, settings, onRequestLocation, Modifier.weight(0.64f).fillMaxHeight())
            }
            Spacer(Modifier.height(12.dp))
            Dock(c, settings, sc, apps, driving, onSplit, ::openApp)
        }
    }
}

@Composable
private fun BrandMark() {
    val p = LocalNova.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("NOVA", style = NovaType.caps.copy(fontSize = 14.sp, letterSpacing = 4.sp), color = p.accent)
        Text("DRIVE", style = NovaType.caps.copy(fontSize = 8.sp, letterSpacing = 4.sp), color = p.textDim)
    }
}

@Composable
private fun Header(s: NovaSettings, iPhone: Boolean, driving: Boolean, onQuick: () -> Unit) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greet = when {
        hour < 12 -> if (ar) "صباح الخير" else "Good morning"
        hour < 17 -> if (ar) "مساء الخير" else "Good afternoon"
        else -> if (ar) "مساء الخير" else "Good evening"
    }
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$greet، ${s.profileName}".let { if (ar) it else it.replace("،", ",") },
            style = NovaType.title, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(14.dp))
        AnimatedVisibility(iPhone, enter = fadeIn(), exit = fadeOut()) {
            Pill(tr("iPhone connected", "الآيفون متصل"), p.ok, icon = Icons.Rounded.PhoneIphone, modifier = Modifier.padding(end = 8.dp))
        }
        AnimatedVisibility(driving, enter = fadeIn(), exit = fadeOut()) {
            Pill(tr("Driving mode", "وضع القيادة"), p.accent, icon = Icons.Rounded.DirectionsCar)
        }
        Spacer(Modifier.weight(1f))
        Glass(Modifier.size(40.dp), corner = 14.dp, onClick = onQuick) {
            androidx.compose.material3.Icon(Icons.Rounded.Tune, tr("Quick settings", "الإعدادات السريعة"), tint = p.text, modifier = Modifier.align(Alignment.Center).size(22.dp))
        }
    }
}

@Composable
private fun HeroCard(c: AppContainer, s: NovaSettings, onRequestLocation: () -> Unit, modifier: Modifier) {
    val p = LocalNova.current
    val fix by c.fix.collectAsStateWithLifecycle()
    val weather by c.weather.weather.collectAsStateWithLifecycle()
    val vehicle by c.obd.data.collectAsStateWithLifecycle()
    val hasLocation = remember(c.permissions.collectAsStateWithLifecycle().value) { c.location.hasPermission() }
    val speed = fix?.speedKmh ?: 0f

    Glass(modifier) {
        HorizonCanvas(speed, Modifier.fillMaxSize())
        // Speed
        Row(Modifier.align(Alignment.TopStart).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                SpeedArc(speed, Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (fix?.fromGps == true || speed > 0f) "${speed.toInt()}" else "--", style = NovaType.display.copy(fontSize = 52.sp), color = p.text)
                    Text(tr("km/h", "كم/س"), style = NovaType.label, color = p.textDim)
                }
            }
        }
        // GPS status
        Box(Modifier.align(Alignment.TopCenter).padding(top = 18.dp)) {
            when {
                !hasLocation -> Pill(tr("Enable GPS", "فعّل الموقع"), p.danger, icon = Icons.Rounded.GpsNotFixed, onClick = onRequestLocation)
                fix?.fromGps != true -> Pill(tr("Searching GPS…", "جارٍ البحث عن GPS…"), p.textDim, icon = Icons.Rounded.GpsNotFixed)
                else -> {}
            }
        }
        // Weather
        val w = weather
        if (s.showWeather && w != null) WeatherBadge(w, Modifier.align(Alignment.TopEnd).padding(22.dp))
        else if (fix?.fromGps == true) Box(Modifier.align(Alignment.TopEnd).padding(22.dp)) {
            androidx.compose.material3.Icon(Icons.Rounded.GpsFixed, null, tint = p.ok)
        }
        // Quick destinations
        Row(Modifier.align(Alignment.BottomStart).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val navPkg = s.navPackage ?: c.apps.guess(AppsRepository.NAV_PKGS, AppsRepository.NAV_KEYS)?.packageName
            if (s.homeAddress.isNotBlank()) Pill(tr("Home", "البيت"), p.accent, icon = Icons.Rounded.Home) { c.controls.navigateTo(s.homeAddress, navPkg) }
            if (s.workAddress.isNotBlank()) Pill(tr("Work", "العمل"), p.accent2, icon = Icons.Rounded.Work) { c.controls.navigateTo(s.workAddress, navPkg) }
        }
        // Vehicle (OBD) — only when enabled and active
        if (s.obdEnabled && vehicle.state != VehicleData.State.OFF) {
            VehicleStrip(vehicle, Modifier.align(Alignment.BottomEnd).padding(16.dp))
        }
    }
}

@Composable
private fun Dock(
    c: AppContainer,
    s: NovaSettings,
    sc: Shortcuts,
    apps: List<AppEntry>,
    driving: Boolean,
    onSplit: () -> Unit,
    openApp: (AppEntry?, String) -> Unit,
) {
    val p = LocalNova.current
    val ctx = LocalContext.current
    val ar = s.arabic
    val favorites = remember(s.favorites, apps) { s.favorites.mapNotNull { k -> apps.firstOrNull { it.key == k } } }
    Glass(Modifier.fillMaxWidth().height(92.dp), corner = 30.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            DockAction(Icons.Rounded.MusicNote, tr("Music", "موسيقى")) { openApp(sc.music, if (ar) "الموسيقى" else "music") }
            DockAction(Icons.Rounded.Radio, tr("Radio", "راديو")) { openApp(sc.radio, if (ar) "الراديو" else "radio") }
            DockAction(Icons.Rounded.VerticalSplit, tr("Split", "تقسيم"), onSplit)
            Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(48.dp).background(p.stroke))
            if (favorites.isEmpty()) {
                Text(
                    tr("Long-press any app in the library to pin it here", "اضغط مطولاً على أي تطبيق لتثبيته هنا"),
                    style = NovaType.label, color = p.textDim, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
            } else {
                LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    items(favorites, key = { it.key }) { app ->
                        AppIcon(
                            app, size = 58.dp, showLabel = false,
                            onLongClick = {
                                if (driving) toast(ctx, if (ar) "متاح عند التوقف" else "Available when parked")
                                else c.settings.toggleFavorite(app.key)
                            }
                        ) { c.apps.launch(app) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconTile(icon, label, size = 52.dp, modifier = Modifier.padding(horizontal = 5.dp), onClick = onClick)
}
