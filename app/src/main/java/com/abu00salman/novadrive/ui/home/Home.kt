package com.abu00salman.novadrive.ui.home

import com.abu00salman.novadrive.core.carDisplayName
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.R
import com.abu00salman.novadrive.core.CarBrand
import com.abu00salman.novadrive.core.CarModel
import com.abu00salman.novadrive.core.CarPaint
import com.abu00salman.novadrive.core.HomeMode
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.Link
import com.abu00salman.novadrive.data.MediaInfo
import com.abu00salman.novadrive.data.NetState
import com.abu00salman.novadrive.ui.shell.GpsState
import com.abu00salman.novadrive.data.NavInfo
import com.abu00salman.novadrive.data.VehicleData
import com.abu00salman.novadrive.data.Weather
import com.abu00salman.novadrive.data.WeatherRepository
import com.abu00salman.novadrive.ui.components.CarImage
import com.abu00salman.novadrive.ui.components.FixedLtr
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.LocalIconScale
import com.abu00salman.novadrive.ui.components.LocalIconTint
import com.abu00salman.novadrive.ui.components.iconFilter
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.components.RoundButton
import com.abu00salman.novadrive.ui.components.UiDirection
import com.abu00salman.novadrive.ui.shell.rememberMinuteClock
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One dock position. [app] is null when nothing suitable is installed. */
@Immutable
data class DockSlot(
    val id: String,
    val title: String,
    val app: AppEntry?,
    val fallback: ImageVector,
    /** Package to offer from the store when [app] is missing, if any. */
    val storePackage: String? = null,
    /** Always usable even without an app (e.g. the system dialer, the app library). */
    val builtIn: Boolean = false,
)

/** Everything the home screen shows. All values are live, or null when genuinely unavailable. */
@Immutable
data class HomeUi(
    /** Null until the first location fix. */
    val speedKmh: Float?,
    val hasLocation: Boolean,
    val vehicle: VehicleData,
    val obdEnabled: Boolean,
    val brand: CarBrand,
    val car: CarModel,
    val paint: CarPaint,
    val weather: Weather?,
    val city: String?,
    val showWeather: Boolean,
    val media: MediaInfo?,
    val notificationAccess: Boolean,
    val nav: NavInfo?,
    val navApp: AppEntry?,
    val homeSet: Boolean,
    val workSet: Boolean,
    val dock: List<DockSlot>,
    val use24h: Boolean,
    /** Settings › Car › custom image version; 0 shows the placeholder. */
    val carImageVersion: Long = 0,
    /** Installed YouTube app, offered beside NovaDrive in the music view when parked. */
    val video: AppEntry? = null,
    /** Settings › Appearance › layout style: the fixed "simple" layout instead of modes. */
    val simple: Boolean = false,
    /** Settings › Appearance › interface: "smart", "simple", "tesla" or "bmw". */
    val style: String = "smart",
    /** Last GPS position and direction of travel, for the live map of the Tesla/BMW interfaces. */
    val lat: Double? = null,
    val lon: Double? = null,
    val bearing: Float? = null,
    val bt: Link = Link.OFF,
    val net: NetState = NetState(Link.OFF, false),
    val gps: GpsState = GpsState.OFF,
    /** An iPhone is attached over USB (CarPlay through ZLink/DiPlay). */
    val carPlayConnected: Boolean = false,
    /** Settings › Appearance › side bar on the right (the Tesla map mirrors its shortcut column). */
    val railRight: Boolean = true,
    /** Active in-app route (drawn on every map), progress index and destination. */
    val routePoints: List<com.abu00salman.novadrive.data.nav.LatLon>? = null,
    val routeProgress: Int = 0,
    val destination: com.abu00salman.novadrive.data.nav.LatLon? = null,
)

class HomeActions(
    val onMode: (HomeMode) -> Unit = {},
    val onEnableLocation: () -> Unit = {},
    val onVehicle: () -> Unit = {},
    val onOpenNav: () -> Unit = {},
    val onNavCard: () -> Unit = {},
    val onNavDetails: () -> Unit = {},
    /** Opens the navigation app in the split-screen pane beside NovaDrive (if the unit supports it). */
    val onNavBeside: () -> Unit = {},
    /** Opens YouTube beside NovaDrive (parked only; Android decides if split-screen is possible). */
    val onVideoBeside: () -> Unit = {},
    val onHome: () -> Unit = {},
    val onWork: () -> Unit = {},
    val onGrantAccess: () -> Unit = {},
    val onOpenMedia: () -> Unit = {},
    val onMusicDetails: () -> Unit = {},
    val onPrev: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onSeek: (Long) -> Unit = {},
    val onDock: (DockSlot) -> Unit = {},
    /** Long-press a dock tile to put a different installed app there. */
    val onDockLong: (DockSlot) -> Unit = {},
    /** Tapping the clock/weather card opens quick controls (volume, brightness, theme). */
    val onQuick: () -> Unit = {},
    // Used by the full-screen interfaces (Tesla/BMW) that carry their own menus and controls.
    val onApps: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onPhone: () -> Unit = {},
    val onVoice: () -> Unit = {},
    val onCarPlay: () -> Unit = {},
    val onBluetooth: () -> Unit = {},
    val onWifi: () -> Unit = {},
    val onGps: () -> Unit = {},
    val volume: () -> Float = { 0f },
    val onVolume: (Float) -> Unit = {},
    val brightness: () -> Float = { 0f },
    val onBrightness: (Float) -> Unit = {},
)

/**
 * Home: a hero area driven by [mode] (car · navigation · music · split), a side column with
 * time/weather, a contextual mini card and the mode switcher, over the dock. The physical
 * arrangement stays fixed (driver side); text inside each card follows the UI language.
 */
@Composable
fun HomeLayout(ui: HomeUi, mode: HomeMode, autoPromoted: Boolean, act: HomeActions, modifier: Modifier = Modifier) = FixedLtr {
    when (ui.style) {
        "tesla" -> { TeslaLayout(ui, act, modifier); return@FixedLtr }
        "bmw" -> { BmwLayout(ui, act, modifier); return@FixedLtr }
    }
    if (ui.simple) { SimpleLayout(ui, act, modifier); return@FixedLtr }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(0.69f).fillMaxHeight()) {
                AnimatedContent(mode, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "hero") { m ->
                    UiDirection {
                        when (m) {
                            HomeMode.CAR -> CarHero(ui, act, Modifier.fillMaxSize())
                            HomeMode.NAVIGATION -> NavHero(ui, act, Modifier.fillMaxSize())
                            HomeMode.MUSIC -> MusicHero(ui, act, Modifier.fillMaxSize())
                            HomeMode.SPLIT -> FixedLtr {
                                Row(Modifier.fillMaxSize()) {
                                    UiDirection { NavHero(ui, act, Modifier.weight(1f).fillMaxHeight(), compact = true) }
                                    Spacer(Modifier.width(12.dp))
                                    UiDirection { MusicHero(ui, act, Modifier.weight(1f).fillMaxHeight(), compact = true) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(0.31f).fillMaxHeight()) {
                UiDirection { TimeWeatherCard(ui, Modifier.fillMaxWidth().height(128.dp), act.onQuick) }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    UiDirection {
                        when (mode) {
                            HomeMode.CAR -> MediaMini(ui, act, Modifier.fillMaxSize())
                            HomeMode.NAVIGATION -> CarMini(ui, act, Modifier.fillMaxSize())
                            HomeMode.MUSIC -> NavMini(ui, act, Modifier.fillMaxSize())
                            HomeMode.SPLIT -> CarMini(ui, act, Modifier.fillMaxSize())
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                UiDirection { ModeSwitcher(mode, autoPromoted, act.onMode, Modifier.fillMaxWidth()) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Dock(ui.dock, act, Modifier.fillMaxWidth().height(112.dp))
    }
}

// ============================================================================ mode switcher

/** Four large targets (≥ 56 dp). Changing mode only changes layout — playback is untouched. */
@Composable
fun ModeSwitcher(mode: HomeMode, autoPromoted: Boolean, onMode: (HomeMode) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple(HomeMode.CAR, Icons.Rounded.DirectionsCar, tr("Car", "السيارة")),
        Triple(HomeMode.NAVIGATION, Icons.Rounded.Navigation, tr("Navigation", "الملاحة")),
        Triple(HomeMode.MUSIC, Icons.Rounded.MusicNote, tr("Music", "الموسيقى")),
        Triple(HomeMode.SPLIT, Icons.Rounded.VerticalSplit, tr("Split", "تقسيم")),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in items.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (m, icon, label) ->
                ModeButton(icon, label, m == mode, badge = m == HomeMode.NAVIGATION && mode == m && autoPromoted, Modifier.weight(1f)) { onMode(m) }
            }
        }
    }
}

@Composable
private fun ModeButton(icon: ImageVector, label: String, active: Boolean, badge: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalNova.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.height(58.dp).clip(shape)
            .background(
                if (active) Brush.verticalGradient(listOf(p.accent, p.accent.copy(alpha = 0.72f)))
                else Brush.verticalGradient(listOf(p.glassTop, p.glassBottom))
            )
            .border(1.dp, if (active) Color.White.copy(alpha = 0.35f) else p.stroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (active) Color.White else p.text, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = NovaType.label.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = if (active) Color.White else p.text, maxLines = 1)
        if (badge) Text(" · " + tr("auto", "تلقائي"), style = NovaType.small, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
    }
}

// ============================================================================ hero panels

/** Rounded panel for the hero area; [night]/[day] gradients keep contrast in both themes. */
@Composable
private fun HeroPanel(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val p = LocalNova.current
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.clip(shape)
            .background(
                if (p.dark) Brush.verticalGradient(listOf(Color(0xFF14244A), Color(0xFF0A1226), Color(0xFF060B18)))
                else Brush.verticalGradient(listOf(Color(0xFFF4F8FF), Color(0xFFDCE6F4), Color(0xFFC9D5E8)))
            )
            .border(1.dp, Brush.verticalGradient(listOf(p.strokeHi, p.stroke)), shape)
    ) { content() }
}

private fun obdValue(ui: HomeUi, pick: (VehicleData) -> Any?, fmt: (Any) -> String): String? {
    if (!ui.obdEnabled || ui.vehicle.state != VehicleData.State.LIVE) return null
    return pick(ui.vehicle)?.let(fmt)
}

@Composable
private fun CarHero(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    HeroPanel(modifier) {
        // horizon glow behind the car
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(
                listOf(p.accent.copy(alpha = if (p.dark) 0.30f else 0.18f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.62f), radius = size.width * 0.55f
            ))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, if (p.dark) Color(0x66000000) else Color(0x22324A70)), startY = size.height * 0.62f, endY = size.height * 0.80f))
        }
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            FixedLtr {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    BoxWithConstraints(Modifier.weight(0.40f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        val dial = minOf(maxWidth, maxHeight)
                        Box(Modifier.size(dial), contentAlignment = Alignment.Center) {
                            Speedometer(ui.speedKmh ?: 0f, ui.car, ui.paint.color, Modifier.fillMaxSize(), showCar = false)
                            Column(Modifier.offset(y = -dial * 0.04f), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (ui.hasLocation) Text(
                                    ui.speedKmh?.toInt()?.toString() ?: "--",
                                    style = NovaType.hero.copy(fontSize = (dial.value * 0.26f).sp), color = p.text, maxLines = 1
                                ) else UiDirection { Pill(tr("Enable GPS", "فعّل الموقع"), p.danger, icon = Icons.Rounded.GpsNotFixed, onClick = act.onEnableLocation) }
                                Text(tr("km/h", "كم/س"), style = NovaType.title.copy(fontWeight = FontWeight.Medium), color = p.textDim)
                            }
                        }
                    }
                    Box(Modifier.weight(0.60f).fillMaxHeight().clickable(onClick = act.onVehicle)) {
                        UiDirection {
                            Column(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp), horizontalAlignment = Alignment.End) {
                                Text(carDisplayName(ui.brand, ui.car, ar), style = NovaType.title, color = p.text, maxLines = 1)
                                Text(if (ar) ui.paint.ar else ui.paint.en, style = NovaType.small, color = p.textDim)
                            }
                        }
                        CarImage(ui.carImageVersion, Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.82f).padding(bottom = 4.dp))
                    }
                }
            }
            // Engine readings only when an OBD-II adapter is set up; otherwise the car gets the space.
            if (ui.obdEnabled) Spacer(Modifier.height(10.dp))
            if (ui.obdEnabled) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Reading(Icons.Rounded.LocalGasStation, obdValue(ui, { it.fuelPct }) { "$it%" }, tr("Fuel level", "مستوى الوقود"), Modifier.weight(1f), act.onVehicle)
                Reading(Icons.Rounded.Thermostat, obdValue(ui, { it.coolantC }) { "$it°C" }, tr("Coolant", "حرارة المحرك"), Modifier.weight(1f), act.onVehicle)
                Reading(Icons.Rounded.BatteryChargingFull, obdValue(ui, { it.voltage }) { String.format(Locale.US, "%.1f V", it as Float) }, tr("Battery", "البطارية"), Modifier.weight(1f), act.onVehicle)
                ObdStatus(ui, Modifier.weight(1f), act.onVehicle)
            }
        }
    }
}

@Composable
private fun Reading(icon: ImageVector, value: String?, label: String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalNova.current
    Row(
        modifier.height(64.dp).clip(RoundedCornerShape(16.dp)).background(p.text.copy(alpha = if (p.dark) 0.06f else 0.55f))
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = p.accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value ?: "--", style = NovaType.title.copy(fontSize = 18.sp), color = if (value != null) p.text else p.textDim, maxLines = 1)
            Text(label, style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ObdStatus(ui: HomeUi, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalNova.current
    val v = ui.vehicle
    val (color, text) = when {
        !ui.obdEnabled -> p.textDim to tr("Not connected", "غير متصل")
        v.state == VehicleData.State.LIVE && v.mil == true -> p.warn to tr("Check engine", "فحص المحرك")
        v.state == VehicleData.State.LIVE -> p.ok to tr("Connected", "متصل")
        v.state == VehicleData.State.CONNECTING -> p.warn to tr("Connecting…", "جارٍ الاتصال…")
        else -> p.danger to tr("Not connected", "غير متصل")
    }
    Row(
        modifier.height(64.dp).clip(RoundedCornerShape(16.dp)).background(p.text.copy(alpha = if (p.dark) 0.06f else 0.55f))
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Bluetooth, null, tint = color, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text, style = NovaType.title.copy(fontSize = 16.sp), color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("OBD-II", style = NovaType.small, color = p.textDim, maxLines = 1)
        }
    }
}

/**
 * Navigation hero. NOVA has no embedded map (no licensed map SDK on this unit), so it mirrors the
 * real guidance the navigation app publishes and hands off to that app with one tap.
 */
@Composable
private fun NavHero(ui: HomeUi, act: HomeActions, modifier: Modifier, compact: Boolean = false) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    HeroPanel(modifier) {
        // A live street map once there is a position (tap: full-screen navigation); the animated
        // road remains as the backdrop until then.
        if (ui.lat != null && ui.lon != null) {
            var zoom by androidx.compose.runtime.saveable.rememberSaveable { mutableFloatStateOf(15f) }
            TileMap(
                ui.lat, ui.lon, ui.bearing,
                if (p.dark) com.abu00salman.novadrive.data.MapTiles.Style.DARK else com.abu00salman.novadrive.data.MapTiles.Style.LIGHT,
                MapMarker.DOT, true, zoom, { zoom = it }, onTap = act.onNavDetails, modifier = Modifier.fillMaxSize(),
                tint = if (p.dark) com.abu00salman.novadrive.ui.nav.NightMapTint else null,
                background = if (p.dark) Color(0xFF0D1528) else Color(0xFFE6E8EB), focusY = 0.62f,
                route = ui.routePoints, routeProgress = ui.routeProgress, routeColor = p.accent, destination = ui.destination, markerColor = p.accent,
            )
        } else LiveRoad(if (ui.hasLocation) ui.speedKmh ?: 0f else 0f, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            NavCard(
                ui.nav, ui.notificationAccess, ui.navApp?.label, ui.homeSet, ui.workSet,
                onCard = act.onNavCard, onOpenNav = act.onOpenNav, onGrantAccess = act.onGrantAccess,
                onHome = act.onHome, onWork = act.onWork, modifier = Modifier.fillMaxWidth(), large = !compact,
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Glass(Modifier.weight(1f).height(64.dp), corner = 20.dp, tint = p.accent, onClick = if (ui.nav != null) act.onNavCard else act.onOpenNav) {
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        val app = ui.navApp
                        if (app != null) Image(app.icon, null, Modifier.size(32.dp)) else Icon(Icons.Rounded.Map, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (ar) (if (ui.nav != null) "العودة للخرائط" else "افتح الخرائط")
                            else (if (ui.nav != null) "Back to " else "Open ") + (app?.label ?: "navigation"),
                            style = NovaType.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (ui.navApp != null) Glass(Modifier.height(64.dp), corner = 20.dp, onClick = act.onNavBeside) {
                    Row(Modifier.align(Alignment.Center).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VerticalSplit, tr("Half screen", "نصف الشاشة"), tint = p.text, modifier = Modifier.size(24.dp))
                        if (!compact) { Spacer(Modifier.width(8.dp)); Text(tr("Half screen", "نصف الشاشة"), style = NovaType.label, color = p.text, maxLines = 1) }
                    }
                }
                if (!compact) Glass(Modifier.size(64.dp), corner = 20.dp, onClick = act.onNavDetails) {
                    Icon(Icons.Rounded.OpenInNew, tr("Destinations", "الوجهات"), tint = p.text, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun MusicHero(ui: HomeUi, act: HomeActions, modifier: Modifier, compact: Boolean = false) {
    val p = LocalNova.current
    val m = ui.media
    HeroPanel(modifier) {
        if (m == null) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(if (compact) 96.dp else 132.dp).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(p.accent, p.accent2))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(56.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(tr("Nothing is playing", "لا يوجد تشغيل حالياً"), style = NovaType.display.copy(fontSize = 26.sp), color = p.text, textAlign = TextAlign.Center)
                Text(
                    tr("Start music in any app — it will appear here with its controls.", "شغّل الموسيقى من أي تطبيق وستظهر هنا مع أزرار التحكم."),
                    style = NovaType.body, color = p.textDim, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill(tr("Open music app", "افتح تطبيق الموسيقى"), p.accent, icon = Icons.Rounded.MusicNote, onClick = act.onOpenMedia)
                    RoundButton(Icons.Rounded.PlayArrow, tr("Resume", "استئناف"), 48.dp, primary = true, onClick = act.onPlayPause)
                }
                if (!ui.notificationAccess) Text(
                    tr("Allow notification access to show track details ›", "اسمح بالوصول للإشعارات لعرض تفاصيل المقطع ›"),
                    style = NovaType.small, color = p.accent, modifier = Modifier.padding(top = 10.dp).clickable(onClick = act.onGrantAccess)
                )
            }
            return@HeroPanel
        }
        val content: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start) {
                Text(m.title ?: "—", style = NovaType.display.copy(fontSize = if (compact) 24.sp else 32.sp), color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textAlign = if (compact) TextAlign.Center else TextAlign.Start)
                Text(m.artist ?: "", style = NovaType.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium), color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(14.dp))
                SeekBar(m, act.onSeek)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    RoundButton(Icons.Rounded.SkipPrevious, tr("Previous", "السابق"), if (compact) 60.dp else 68.dp, onClick = act.onPrev)
                    RoundButton(if (m.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, tr("Play / pause", "تشغيل / إيقاف"), if (compact) 76.dp else 88.dp, primary = true, onClick = act.onPlayPause)
                    RoundButton(Icons.Rounded.SkipNext, tr("Next", "التالي"), if (compact) 60.dp else 68.dp, onClick = act.onNext)
                }
            }
        }
        if (compact) Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Artwork(m, 150.dp, 22.dp, act.onOpenMedia)
            Spacer(Modifier.height(14.dp))
            content()
        } else Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(m, 250.dp, 28.dp, act.onOpenMedia)
            Spacer(Modifier.width(26.dp))
            Column(Modifier.weight(1f)) {
                content()
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(tr("Queue & sources", "قائمة التشغيل والمصادر"), p.accent, icon = Icons.Rounded.MusicNote, onClick = act.onMusicDetails)
                    ui.video?.let { v -> Glass(Modifier.height(36.dp), corner = 18.dp, onClick = act.onVideoBeside) {
                        Row(Modifier.align(Alignment.Center).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(v.icon, null, Modifier.size(22.dp)); Spacer(Modifier.width(6.dp))
                            Text(tr("YouTube in half screen", "يوتيوب في نصف الشاشة"), style = NovaType.label, color = p.text, maxLines = 1)
                        }
                    } }
                }
            }
        }
    }
}

/** Seek only when the session supports it; otherwise a read-only progress line. */
@Composable
private fun SeekBar(m: MediaInfo, onSeek: (Long) -> Unit) {
    val p = LocalNova.current
    if (!m.canSeek || m.durationMs <= 0) { MediaProgress(m, Modifier.fillMaxWidth()); return }
    var dragging by remember(m.title) { mutableFloatStateOf(-1f) }
    val pos = m.positionNow()
    val frac = if (dragging >= 0f) dragging else (pos.toFloat() / m.durationMs).coerceIn(0f, 1f)
    Column {
        Slider(
            value = frac, onValueChange = { dragging = it },
            onValueChangeFinished = { onSeek((dragging * m.durationMs).toLong()); dragging = -1f },
            colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.text.copy(alpha = 0.15f)),
            modifier = Modifier.heightIn(min = 48.dp),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatMs((frac * m.durationMs).toLong()), style = NovaType.small, color = p.textDim)
            Spacer(Modifier.weight(1f))
            Text(formatMs(m.durationMs), style = NovaType.small, color = p.textDim)
        }
    }
}

// ============================================================================ side column

@Composable
private fun TimeWeatherCard(ui: HomeUi, modifier: Modifier, onClick: () -> Unit = {}) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val now = rememberMinuteClock()
    val loc = uiLocale(ar)
    val time = remember(now, ui.use24h) { SimpleDateFormat(if (ui.use24h) "HH:mm" else "h:mm", loc).format(Date(now)) }
    val pm = remember(now) { Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.AM_PM) == Calendar.PM }
    val date = remember(now, ar) { SimpleDateFormat(if (ar) "EEEE d MMMM" else "EEE, d MMM", loc).format(Date(now)) }
    val w = ui.weather
    Glass(modifier, onClick = onClick) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (ui.showWeather && w != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${Math.round(w.tempC)}°", style = NovaType.display.copy(fontSize = 36.sp), color = p.text, maxLines = 1)
                        Spacer(Modifier.width(6.dp))
                        Icon(weatherIcon(w), null, tint = weatherTint(w), modifier = Modifier.size(32.dp))
                    }
                    Text(listOfNotNull(ui.city, WeatherRepository.label(w.code, ar)).joinToString(" · "), style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (w.maxC != null && w.minC != null) Text(
                        tr("H ", "العظمى ") + "${Math.round(w.maxC)}°  " + tr("L ", "الصغرى ") + "${Math.round(w.minC)}°",
                        style = NovaType.small, color = p.textDim, maxLines = 1
                    )
                } else Text(if (!ui.showWeather) tr("Weather off", "الطقس متوقف") else tr("Weather unavailable", "الطقس غير متاح"), style = NovaType.label, color = p.textDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(time, style = NovaType.display.copy(fontSize = 38.sp, fontWeight = FontWeight.Medium), color = p.text, maxLines = 1)
                    if (!ui.use24h) Text(if (ar) (if (pm) " م" else " ص") else (if (pm) " PM" else " AM"), style = NovaType.label, color = p.textDim, modifier = Modifier.padding(bottom = 6.dp))
                }
                Text(date, style = NovaType.small, color = p.textDim, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MediaMini(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val p = LocalNova.current
    val m = ui.media
    Glass(modifier) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(m, 64.dp, 14.dp, act.onOpenMedia)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).clickable(onClick = { act.onMode(HomeMode.MUSIC) })) {
                    Text(m?.title ?: tr("Nothing playing", "لا يوجد تشغيل"), style = NovaType.title.copy(fontSize = 17.sp), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(m?.artist ?: tr("Tap to open music", "اضغط لفتح الموسيقى"), style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                RoundButton(Icons.Rounded.SkipPrevious, tr("Previous", "السابق"), 50.dp, onClick = act.onPrev)
                RoundButton(if (m?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, tr("Play / pause", "تشغيل / إيقاف"), 58.dp, primary = true, onClick = act.onPlayPause)
                RoundButton(Icons.Rounded.SkipNext, tr("Next", "التالي"), 50.dp, onClick = act.onNext)
            }
            MediaProgress(m, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NavMini(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val p = LocalNova.current
    val nav = ui.nav
    Glass(modifier, onClick = if (nav != null) act.onNavCard else act.onOpenNav) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = nav?.maneuver
                if (icon != null) Image(icon, null, Modifier.size(44.dp))
                else Icon(if (nav != null) Icons.Rounded.TurnRight else Icons.Rounded.Map, null, tint = p.accent, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(nav?.primary ?: tr("No active route", "لا يوجد توجيه نشط"), style = NovaType.title.copy(fontSize = if (nav != null) 24.sp else 17.sp), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(nav?.secondary ?: tr("Open " + (ui.navApp?.label ?: "maps"), "افتح الخرائط"), style = NovaType.small, color = p.textDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            nav?.eta?.let { Text(it, style = NovaType.small, color = p.textDim, modifier = Modifier.padding(top = 8.dp), maxLines = 1) }
        }
    }
}

@Composable
private fun CarMini(ui: HomeUi, act: HomeActions, modifier: Modifier, toVehicle: Boolean = false) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    Glass(modifier, onClick = { if (toVehicle) act.onVehicle() else act.onMode(HomeMode.CAR) }) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(if (ui.hasLocation) ui.speedKmh?.toInt()?.toString() ?: "--" else "--", style = NovaType.display.copy(fontSize = 38.sp, fontWeight = FontWeight.SemiBold), color = p.text)
                Text(" " + tr("km/h", "كم/س"), style = NovaType.label, color = p.textDim, modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                Text(carDisplayName(ui.brand, ui.car, ar), style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 6.dp).weight(1f, fill = false))
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CarImage(ui.carImageVersion, Modifier.fillMaxSize(), hint = false)
            }
        }
    }
}

// ============================================================================ dock

@Composable
fun Dock(slots: List<DockSlot>, act: HomeActions, modifier: Modifier = Modifier, large: Boolean = false) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        slots.forEach { s -> Box(Modifier.weight(1f).fillMaxHeight()) { UiDirection { DockTile(s, { act.onDock(s) }, { act.onDockLong(s) }, Modifier.fillMaxSize(), large) } } }
    }
}

@Composable
private fun DockTile(s: DockSlot, onClick: () -> Unit, onLong: () -> Unit, modifier: Modifier, large: Boolean = false) {
    val p = LocalNova.current
    val available = s.app != null || s.builtIn
    val icon = (if (large) 70.dp else 56.dp) * LocalIconScale.current
    // Icon + one title in the UI language, centred. App labels from other languages aren't mixed in.
    Glass(modifier, corner = 22.dp, onClick = onClick, onLongClick = onLong) {
        Column(Modifier.align(Alignment.Center).padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DockIcon(s, icon)
            Spacer(Modifier.height(6.dp))
            Text(
                if (available) s.title else tr("Install ", "تثبيت ") + s.title,
                style = NovaType.label.copy(fontSize = if (large) 18.sp else 15.sp, fontWeight = FontWeight.SemiBold), color = if (available) p.text else p.textDim,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
            )
        }
    }
}


/** A dock entry's icon: the app's own icon, the CarPlay mark for CarPlay, or a glyph tile. */
@Composable
internal fun DockIcon(s: DockSlot, icon: Dp) {
    val p = LocalNova.current
    val available = s.app != null || s.builtIn
    val app = s.app
    val tint = LocalIconTint.current
    // The phone button always shows a phone, even when the unit's dialer uses a Bluetooth logo.
    // CarPlay shows the CarPlay mark (it still launches the unit's ZLink/DiPlay app).
    if (s.id == "carplay" && app != null) Image(painterResource(R.drawable.carplay_icon), null, Modifier.size(icon).clip(RoundedCornerShape(icon * 0.22f)), colorFilter = iconFilter())
    else if (app != null && s.id != "phone") Image(app.icon, null, Modifier.size(icon), colorFilter = iconFilter())
    else Box(
        Modifier.size(icon).clip(RoundedCornerShape(15.dp))
            .background(
                when {
                    !available -> Brush.linearGradient(listOf(p.text.copy(alpha = 0.10f), p.text.copy(alpha = 0.05f)))
                    tint != null -> Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.7f)))
                    s.id == "phone" -> Brush.linearGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A)))
                    else -> Brush.linearGradient(listOf(Color(0xFF5B7DB8), Color(0xFF2B3E68)))
                }
            )
            .alpha(if (available) 1f else 0.6f),
        contentAlignment = Alignment.Center
    ) { Icon(s.fallback, null, tint = if (available) Color.White else p.textDim, modifier = Modifier.size(icon * 0.55f)) }
}

/**
 * «الواجهة البسيطة»: one fixed screen — live guidance, the music player and time/weather with the
 * car — over a large dock. Every card is a working control (guidance → maps, artwork → music app,
 * car → vehicle info, clock → quick controls).
 */
@Composable
private fun SimpleLayout(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            UiDirection { NavHero(ui, act, Modifier.weight(0.34f).fillMaxHeight(), compact = true) }
            Spacer(Modifier.width(12.dp))
            UiDirection { MusicHero(ui, act, Modifier.weight(0.34f).fillMaxHeight(), compact = true) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(0.32f).fillMaxHeight()) {
                UiDirection { TimeWeatherCard(ui, Modifier.fillMaxWidth().height(140.dp), act.onQuick) }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) { UiDirection { CarMini(ui, act, Modifier.fillMaxSize(), toVehicle = true) } }
            }
        }
        Spacer(Modifier.height(12.dp))
        Dock(ui.dock, act, Modifier.fillMaxWidth().height(132.dp), large = true)
    }
}
