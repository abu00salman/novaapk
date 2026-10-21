package com.abu00salman.novadrive.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.abu00salman.novadrive.core.CarPaints
import com.abu00salman.novadrive.core.carBrand
import com.abu00salman.novadrive.core.carModel
import com.abu00salman.novadrive.ui.components.GlyphTile
import java.util.Locale
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

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greet = when {
        hour < 12 -> if (ar) "صباح الخير" else "Good morning"
        hour < 17 -> if (ar) "مساء الخير" else "Good afternoon"
        else -> if (ar) "مساء الخير" else "Good evening"
    }.let { "$it${if (ar) "، " else ", "}${settings.profileName}" }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Stage(c, settings, iPhone, driving, onRequestLocation, onQuick, Modifier.weight(0.60f).fillMaxHeight())
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(0.40f).fillMaxHeight()) {
                ClockCard(settings.use24h, greet, Modifier.weight(0.38f).fillMaxWidth())
                Spacer(Modifier.height(14.dp))
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
                    modifier = Modifier.weight(0.62f).fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Dock(c, settings, sc, apps, iPhone, driving, onApps, onSettings, onQuick, onSplit, onVoice, ::openApp)
    }
}

/** The driving scene: chosen car on a live road, real GPS speed, weather and quick destinations. */
@Composable
private fun Stage(
    c: AppContainer, s: NovaSettings, iPhone: Boolean, driving: Boolean,
    onRequestLocation: () -> Unit, onQuick: () -> Unit, modifier: Modifier,
) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val fix by c.fix.collectAsStateWithLifecycle()
    val weather by c.weather.weather.collectAsStateWithLifecycle()
    val vehicle by c.obd.data.collectAsStateWithLifecycle()
    val hasLocation = remember(c.permissions.collectAsStateWithLifecycle().value) { c.location.hasPermission() }
    val speed = fix?.speedKmh ?: 0f
    val brand = carBrand(s.carBrand)
    val model = carModel(s.carBrand, s.carModel)
    val paint = CarPaints.getOrElse(s.carPaint) { CarPaints[0] }
    val shape = RoundedCornerShape(36.dp)

    Box(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(p.glassTop.copy(alpha = p.glassTop.alpha * 0.55f), p.glassBottom.copy(alpha = p.glassBottom.alpha * 0.4f))))
            .border(1.dp, Brush.verticalGradient(listOf(p.strokeHi, p.stroke)), shape)
    ) {
        HorizonCanvas(speed, model, paint.color, Modifier.fillMaxSize())

        // Speed — the one big number
        Column(Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 18.dp)) {
            Text(if (fix?.fromGps == true || speed > 0f) "${speed.toInt()}" else "0", style = NovaType.hero, color = p.text)
            Text(tr("km/h", "كم/س"), style = NovaType.label, color = p.textDim, modifier = Modifier.padding(start = 4.dp))
        }
        // Car identity + status
        Column(Modifier.align(Alignment.TopCenter).padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${if (ar) brand.ar else brand.en}  ${model.name}".uppercase(Locale.ROOT).let { if (ar) "${brand.ar}  ${model.name}" else it },
                style = NovaType.caps, color = p.textDim, maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !hasLocation -> Pill(tr("Enable GPS", "فعّل الموقع"), p.danger, icon = Icons.Rounded.GpsNotFixed, onClick = onRequestLocation)
                    fix?.fromGps != true -> Pill(tr("Searching GPS…", "جارٍ البحث عن GPS…"), p.textDim, icon = Icons.Rounded.GpsNotFixed)
                }
                AnimatedVisibility(iPhone, enter = fadeIn(), exit = fadeOut()) {
                    Pill(tr("iPhone connected", "الآيفون متصل"), p.ok, icon = Icons.Rounded.PhoneIphone)
                }
                AnimatedVisibility(driving, enter = fadeIn(), exit = fadeOut()) {
                    Pill(tr("Driving", "قيادة"), p.accent, icon = Icons.Rounded.DirectionsCar)
                }
            }
        }
        // Weather
        val w = weather
        if (s.showWeather && w != null) WeatherBadge(w, Modifier.align(Alignment.TopEnd).padding(end = 26.dp, top = 22.dp))
        // Quick destinations
        Row(Modifier.align(Alignment.BottomStart).padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val navPkg = s.navPackage ?: c.apps.guess(AppsRepository.NAV_PKGS, AppsRepository.NAV_KEYS)?.packageName
            if (s.homeAddress.isNotBlank()) Pill(tr("Home", "البيت"), p.accent, icon = Icons.Rounded.Home) { c.controls.navigateTo(s.homeAddress, navPkg) }
            if (s.workAddress.isNotBlank()) Pill(tr("Work", "العمل"), p.accent2, icon = Icons.Rounded.Work) { c.controls.navigateTo(s.workAddress, navPkg) }
        }
        if (s.obdEnabled && vehicle.state != VehicleData.State.OFF) {
            VehicleStrip(vehicle, Modifier.align(Alignment.BottomEnd).padding(18.dp))
        }
    }
}

private val Gray1 = Color(0xFF9AA5BD)
private val Gray2 = Color(0xFF5A6580)

@Composable
private fun Dock(
    c: AppContainer,
    s: NovaSettings,
    sc: Shortcuts,
    apps: List<AppEntry>,
    iPhone: Boolean,
    driving: Boolean,
    onApps: () -> Unit,
    onSettings: () -> Unit,
    onQuick: () -> Unit,
    onSplit: () -> Unit,
    onVoice: () -> Unit,
    openApp: (AppEntry?, String) -> Unit,
) {
    val p = LocalNova.current
    val ctx = LocalContext.current
    val ar = s.arabic
    val favorites = remember(s.favorites, apps) { s.favorites.mapNotNull { k -> apps.firstOrNull { it.key == k } } }
    val gap = Modifier.padding(horizontal = 7.dp)
    Glass(Modifier.fillMaxWidth().height(100.dp), corner = 34.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            GlyphTile(Icons.Rounded.Apps, tr("Apps", "التطبيقات"), Gray1, Gray2, gap, onClick = onApps)
            GlyphTile(Icons.Rounded.PhoneIphone, "CarPlay", Color(0xFF3EE08F), Color(0xFF089A5B), gap, badge = if (iPhone) p.ok else null) { openApp(sc.carPlay, "CarPlay") }
            GlyphTile(Icons.Rounded.Navigation, tr("Navigation", "الملاحة"), Color(0xFF5BA8FF), Color(0xFF1F55E0), gap) { openApp(sc.nav, if (ar) "الملاحة" else "navigation") }
            GlyphTile(Icons.Rounded.Phone, tr("Phone", "الهاتف"), Color(0xFF55E07A), Color(0xFF16A34A), gap) {
                if (sc.phone == null || !c.apps.launch(sc.phone)) c.controls.dial()
            }
            GlyphTile(Icons.Rounded.MusicNote, tr("Music", "موسيقى"), Color(0xFFFF6B88), Color(0xFFE11D48), gap) { openApp(sc.music, if (ar) "الموسيقى" else "music") }
            GlyphTile(Icons.Rounded.Radio, tr("Radio", "راديو"), Color(0xFFFFB84D), Color(0xFFF2680F), gap) { openApp(sc.radio, if (ar) "الراديو" else "radio") }
            GlyphTile(Icons.Rounded.VerticalSplit, tr("Split", "تقسيم"), Color(0xFF9A8BFF), Color(0xFF5B3DF5), gap, onClick = onSplit)
            GlyphTile(Icons.Rounded.Mic, tr("Voice", "صوت"), Color(0xFFC07BFF), Color(0xFF7C3AED), gap, onClick = onVoice)
            Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(52.dp).background(p.stroke))
            if (favorites.isEmpty()) {
                Text(
                    tr("Long-press any app in the library to pin it here", "اضغط مطولاً على أي تطبيق لتثبيته هنا"),
                    style = NovaType.label, color = p.textDim, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 2
                )
            } else {
                LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    items(favorites, key = { it.key }) { app ->
                        AppIcon(
                            app, size = 56.dp, showLabel = false,
                            onLongClick = {
                                if (driving) toast(ctx, if (ar) "متاح عند التوقف" else "Available when parked")
                                else c.settings.toggleFavorite(app.key)
                            }
                        ) { c.apps.launch(app) }
                    }
                }
            }
            Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(52.dp).background(p.stroke))
            GlyphTile(Icons.Rounded.Tune, tr("Quick", "سريع"), Gray1, Gray2, gap, onClick = onQuick)
            GlyphTile(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), Gray1, Gray2, gap, onClick = onSettings)
        }
    }
}
