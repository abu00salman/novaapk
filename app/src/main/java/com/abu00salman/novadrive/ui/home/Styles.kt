package com.abu00salman.novadrive.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.R
import com.abu00salman.novadrive.core.carDisplayName
import com.abu00salman.novadrive.data.Link
import com.abu00salman.novadrive.data.MapTiles
import com.abu00salman.novadrive.data.MediaInfo
import com.abu00salman.novadrive.data.VehicleData
import com.abu00salman.novadrive.data.WeatherRepository
import com.abu00salman.novadrive.ui.components.CarImage
import com.abu00salman.novadrive.ui.components.FixedLtr
import com.abu00salman.novadrive.ui.components.UiDirection
import com.abu00salman.novadrive.ui.shell.GpsState
import com.abu00salman.novadrive.ui.shell.rememberMinuteClock
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

// ============================================================================ shared

/** Time, AM/PM suffix (empty in 24 h) and long date in the UI language, ticking each minute. */
@Composable
private fun clockStrings(use24h: Boolean): Triple<String, String, String> {
    val ar = LocalArabic.current
    val now = rememberMinuteClock()
    val loc = uiLocale(ar)
    return remember(now, use24h, ar) {
        val pm = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.AM_PM) == Calendar.PM
        Triple(
            SimpleDateFormat(if (use24h) "HH:mm" else "h:mm", loc).format(Date(now)),
            if (use24h) "" else if (ar) (if (pm) "م" else "ص") else (if (pm) "PM" else "AM"),
            SimpleDateFormat(if (ar) "EEEE d MMMM yyyy" else "EEEE, d MMMM yyyy", loc).format(Date(now)),
        )
    }
}

/** Recomposes every second while [active] so the media position moves. */
@Composable
private fun rememberSecondTick(active: Boolean): Long {
    val v by produceState(0L, active) { while (active) { delay(1000); value++ } }
    return v
}

/** Live media position as (fraction, elapsed, total), or null when the source doesn't report one. */
@Composable
private fun mediaPosition(m: MediaInfo?): Triple<Float, String, String>? {
    rememberSecondTick(m?.playing == true)
    if (m == null || m.positionMs < 0 || m.durationMs <= 0) return null
    val pos = m.positionNow().coerceIn(0, m.durationMs)
    return Triple(pos.toFloat() / m.durationMs, formatMs(pos), formatMs(m.durationMs))
}

private fun obdFuel(ui: HomeUi): Int? =
    if (ui.obdEnabled && ui.vehicle.state == VehicleData.State.LIVE) ui.vehicle.fuelPct else null

/** A 0..1 system level (volume/brightness) that reflects each change immediately. */
@Composable
private fun rememberLevel(read: () -> Float, write: (Float) -> Unit): Pair<Float, (Float) -> Unit> {
    var v by remember { mutableFloatStateOf(read().coerceIn(0f, 1f)) }
    return v to { delta: Float -> v = (v + delta).coerceIn(0f, 1f); write(v) }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.press(onClick: () -> Unit, onLong: (() -> Unit)? = null) =
    combinedClickable(onClick = onClick, onLongClick = onLong)

@Composable
private fun MapZoomState(): Pair<Float, (Float) -> Unit> {
    var zoom by rememberSaveable { mutableFloatStateOf(15f) }
    return zoom to { z: Float -> zoom = z }
}

@Composable
private fun ScaleBar(ui: HomeUi, zoom: Float, color: Color, modifier: Modifier) {
    val lat = ui.lat ?: return
    val ar = LocalArabic.current
    val d = LocalDensity.current
    val (metres, px) = mapScale(mapMetresPerPx(lat, zoom, d.density), with(d) { 96.dp.toPx() })
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(
            if (metres >= 1000) "${metres / 1000} " + tr("km", "كم") else "$metres " + tr("m", "م"),
            style = NovaType.small.copy(fontSize = 13.sp), color = color
        )
        Box(Modifier.width(with(d) { px.toDp() }).height(2.dp).background(color))
    }
}

// ============================================================================ Tesla

private val TBg = Color(0xFFE8EBEE)
private val TPanel = Color(0xFFF3F4F6)
private val TCard = Color(0xFFFBFBFC)
private val TText = Color(0xFF17191E)
private val TDim = Color(0xFF6A707C)
private val TLine = Color(0x1A000000)
private val TRed = Color(0xFFE82127)
private val TGreen = Color(0xFF22C55E)

/** OpenStreetMap tiles toned down to the soft grey look of the Tesla map (labels stay readable). */
private val TeslaMapTint = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.45f) })

/**
 * «واجهة Tesla»: light, full-screen. The car (the user's own photo) with live GPS speed on the left,
 * a large live street map with search and shortcuts on the right, media/weather/clock cards and a
 * bottom bar with brightness, the dock and volume. Every element is a working control.
 */
@Composable
internal fun TeslaLayout(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    Column(modifier.fillMaxSize().background(TBg)) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            TeslaCarPanel(ui, act, Modifier.width(430.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp, end = 10.dp)) {
                TeslaStatusBar(ui, act, Modifier.fillMaxWidth().height(62.dp))
                TeslaMap(ui, act, Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UiDirection { TeslaMedia(ui, act, Modifier.weight(1.2f).fillMaxHeight()) }
                    UiDirection { TeslaWeather(ui, act, Modifier.weight(0.62f).fillMaxHeight()) }
                    UiDirection { TeslaClock(ui, act, Modifier.weight(0.62f).fillMaxHeight()) }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        TeslaBottomBar(ui, act, Modifier.fillMaxWidth().height(108.dp))
    }
}

@Composable
private fun TCardBox(modifier: Modifier, corner: Dp = 18.dp, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier.clip(shape).background(TCard).border(1.dp, TLine, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

@Composable
private fun TeslaCarPanel(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val ar = LocalArabic.current
    Box(modifier.background(TPanel)) {
        Column(Modifier.fillMaxSize().padding(start = 28.dp, end = 24.dp, top = 20.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UiDirection {
                    Text(carDisplayName(ui.brand, ui.car, ar), style = NovaType.label.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        color = TText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                obdFuel(ui)?.let { f ->
                    Text("$f %", style = NovaType.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium), color = TText)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(54.dp, 20.dp).clip(RoundedCornerShape(5.dp)).background(TLine)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(f / 100f).background(if (f > 15) TGreen else TRed))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Column(Modifier.padding(start = 6.dp)) {
                if (ui.hasLocation) Text(
                    ui.speedKmh?.toInt()?.toString() ?: "0",
                    style = NovaType.hero.copy(fontSize = 100.sp, fontWeight = FontWeight.Light), color = TText, maxLines = 1
                ) else UiDirection {
                    Text(tr("Enable location ›", "فعّل الموقع ›"), style = NovaType.title, color = TRed,
                        modifier = Modifier.padding(vertical = 30.dp).clickable(onClick = act.onEnableLocation))
                }
                Text(tr("km/h", "كم/س"), style = NovaType.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal), color = TText,
                    modifier = Modifier.offset(y = (-6).dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth().clickable(onClick = act.onVehicle)) {
                // The lane the car sits in (scenery only).
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val lane = Path().apply { moveTo(w * 0.40f, 0f); lineTo(w * 0.60f, 0f); lineTo(w * 0.98f, h); lineTo(w * 0.02f, h); close() }
                    drawPath(lane, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.85f))))
                    for (s in listOf(-1f, 1f)) drawLine(
                        Brush.verticalGradient(listOf(Color(0x14000000), Color(0x40000000))),
                        Offset(w / 2 + s * w * 0.10f, 0f), Offset(w / 2 + s * w * 0.48f, h), strokeWidth = 3.dp.toPx()
                    )
                }
                CarImage(ui.carImageVersion, Modifier.align(Alignment.Center).fillMaxWidth(0.86f).fillMaxHeight(0.86f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(88.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TCardBox(Modifier.weight(1f).fillMaxHeight(), corner = 16.dp) {
                    Row(Modifier.fillMaxSize()) {
                        TeslaAction(Icons.Rounded.Home, tr("Home", "البيت"), Modifier.weight(1f), act.onHome)
                        Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 18.dp).background(TLine))
                        TeslaAction(Icons.Rounded.Work, tr("Work", "العمل"), Modifier.weight(1f), act.onWork)
                    }
                }
                TCardBox(Modifier.weight(1f).fillMaxHeight(), corner = 16.dp) {
                    Row(Modifier.fillMaxSize()) {
                        TeslaAction(Icons.Rounded.DirectionsCar, tr("Vehicle", "السيارة"), Modifier.weight(1f), act.onVehicle)
                        Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 18.dp).background(TLine))
                        TeslaAction(Icons.Rounded.VerticalSplit, tr("Split", "تقسيم"), Modifier.weight(1f), act.onNavBeside)
                    }
                }
            }
        }
    }
}

@Composable
private fun TeslaAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxHeight().clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = TText, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = NovaType.label.copy(fontSize = 14.sp), color = TText, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun TeslaStatusBar(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val (time, ampm, date) = clockStrings(ui.use24h)
    val w = ui.weather
    Row(modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$time $ampm".trim(), style = NovaType.title.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium), color = TText,
            modifier = Modifier.clickable(onClick = act.onQuick))
        if (ui.showWeather && w != null) {
            Spacer(Modifier.width(24.dp))
            Text("${Math.round(w.tempC)}°", style = NovaType.title.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium), color = TText)
            Spacer(Modifier.width(6.dp))
            Icon(weatherIcon(w), null, tint = weatherTint(w), modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.weight(1f))
        TStatus(if (ui.net.wifi == Link.OFF) Icons.Rounded.WifiOff else Icons.Rounded.Wifi, if (ui.net.wifi == Link.CONNECTED) TText else TDim, act.onWifi)
        TStatus(
            when (ui.bt) { Link.CONNECTED -> Icons.Rounded.BluetoothConnected; Link.ON -> Icons.Rounded.Bluetooth; Link.OFF -> Icons.Rounded.BluetoothDisabled },
            if (ui.bt == Link.OFF) TDim else TText, act.onBluetooth,
        )
        if (ui.net.cellular) TStatus(Icons.Rounded.SignalCellularAlt, TText, act.onWifi)
        TStatus(
            when (ui.gps) { GpsState.FIX -> Icons.Rounded.GpsFixed; GpsState.SEARCHING -> Icons.Rounded.GpsNotFixed; GpsState.OFF -> Icons.Rounded.LocationOff },
            when (ui.gps) { GpsState.FIX -> Color(0xFF2563EB); GpsState.SEARCHING -> TText; GpsState.OFF -> TRed }, act.onGps,
        )
        Spacer(Modifier.width(18.dp))
        Text(date, style = NovaType.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium), color = TText, maxLines = 1)
        Spacer(Modifier.width(14.dp))
        // «أمرني» lives in the profile spot of the original design.
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))))
                .clickable(onClick = act.onVoice),
            contentAlignment = Alignment.Center,
        ) { Icon(com.abu00salman.novadrive.ui.components.voiceIcon(), com.abu00salman.novadrive.ui.components.voiceName(), tint = Color.White, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun TStatus(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(25.dp))
    }
}

/** The dock with YouTube right after Music (Tesla bar), unless the user already placed it. */
private fun teslaDock(ui: HomeUi): List<DockSlot> {
    val yt = ui.youtube ?: return ui.dock
    if (ui.driving || ui.dock.any { it.id == "youtube" || it.app?.packageName == yt.app?.packageName }) return ui.dock
    val at = ui.dock.indexOfFirst { it.id == "music" }
    return if (at < 0) ui.dock + yt else ui.dock.toMutableList().apply { add(at + 1, yt) }
}

@Composable
private fun TeslaMap(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    var video by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.clip(shape).border(1.dp, TLine, shape)) {
        if (video && ui.youtube != null) TeslaVideoPanel(ui, act, Modifier.fillMaxSize())
        else TeslaMapLayer(ui, act)
        // Maps | YouTube switch (the trip keeps running while YouTube is shown).
        if (ui.youtube != null) Row(
            Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 22.dp).clip(RoundedCornerShape(14.dp)).background(TCard)
                .border(1.dp, TLine, RoundedCornerShape(14.dp)),
        ) {
            TSegment(tr("Maps", "الخرائط"), !video) { video = false }
            TSegment("YouTube", video) { video = true }
        }
    }
}

@Composable
private fun TSegment(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(if (active) TText else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { UiDirection { Text(label, style = NovaType.label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = if (active) Color.White else TText, maxLines = 1) } }
}

/**
 * YouTube in the large card. Android offers no supported way to embed another app's screen inside
 * a launcher, so this uses the official routes: YouTube beside NovaDrive in split screen (Android
 * multi-window) or full screen. Video is unavailable while driving.
 */
@Composable
private fun TeslaVideoPanel(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val yt = ui.youtube ?: return
    Column(modifier.background(Color(0xFFF7F7F8)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        com.abu00salman.novadrive.ui.home.DockIcon(yt, 84.dp)
        Spacer(Modifier.height(14.dp))
        UiDirection {
            if (ui.driving) {
                Text(tr("Video is available when parked", "الفيديو متاح عند التوقف"), style = NovaType.title.copy(fontSize = 22.sp), color = TText)
                Text(tr("Navigation keeps running.", "الملاحة مستمرة."), style = NovaType.label, color = TDim)
            } else {
                Text("YouTube", style = NovaType.title.copy(fontSize = 22.sp), color = TText)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TCardBox(Modifier.height(58.dp), corner = 14.dp, onClick = act.onVideoBeside) {
                        Row(Modifier.align(Alignment.Center).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.VerticalSplit, null, tint = TText, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Beside NovaDrive", "بجانب NovaDrive"), style = NovaType.label.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = TText)
                        }
                    }
                    TCardBox(Modifier.height(58.dp), corner = 14.dp, onClick = { act.onDock(yt) }) {
                        Row(Modifier.align(Alignment.Center).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.OpenInNew, null, tint = TText, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Full screen", "ملء الشاشة"), style = NovaType.label.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = TText)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(tr("Navigation keeps running in the background.", "الملاحة تستمر في الخلفية."), style = NovaType.small, color = TDim)
            }
        }
    }
}

@Composable
private fun BoxScope.TeslaMapLayer(ui: HomeUi, act: HomeActions) {
    val (zoom, setZoom) = MapZoomState()
    var headingUp by rememberSaveable { mutableStateOf(true) }
    run {
        TileMap(
            ui.lat, ui.lon, ui.bearing, MapTiles.Style.LIGHT, MapMarker.TESLA, headingUp, zoom, setZoom,
            onTap = act.onNavDetails,
            modifier = Modifier.fillMaxSize(), tint = TeslaMapTint, background = Color(0xFFE6E8EB), focusY = 0.64f,
            route = ui.routePoints, routeProgress = ui.routeProgress, routeColor = Color(0xFF3E6AE1), destination = ui.destination,
        )
        if (ui.lat == null) UiDirection {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.GpsNotFixed, null, tint = TDim, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text(tr("Waiting for your location…", "بانتظار تحديد موقعك…"), style = NovaType.title.copy(fontSize = 18.sp), color = TText)
                if (!ui.hasLocation) Text(tr("Enable location ›", "فعّل الموقع ›"), style = NovaType.label, color = TRed,
                    modifier = Modifier.padding(top = 8.dp).clickable(onClick = act.onEnableLocation))
            }
        }
        // Search → NovaDrive destinations (search, home, work, recents).
        TCardBox(Modifier.align(Alignment.TopStart).padding(14.dp).width(420.dp).height(62.dp), corner = 14.dp, onClick = act.onNavDetails) {
            Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = TText, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                UiDirection {
                    Text(tr("Search for a destination", "البحث عن وجهة"), style = NovaType.title.copy(fontSize = 19.sp, fontWeight = FontWeight.Normal),
                        color = TDim, maxLines = 1, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        ui.nav?.let { nav ->
            TCardBox(Modifier.align(Alignment.TopStart).padding(start = 98.dp, top = 88.dp).width(336.dp), corner = 14.dp, onClick = act.onNavCard) {
                UiDirection {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        val icon = nav.maneuver
                        if (icon != null) Image(icon, null, Modifier.size(40.dp)) else Icon(Icons.Rounded.TurnRight, null, tint = Color(0xFF2563EB), modifier = Modifier.size(38.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(nav.primary, style = NovaType.title.copy(fontSize = 22.sp), color = TText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            nav.secondary?.let { Text(it, style = NovaType.small, color = TDim, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            nav.eta?.let { Text(it, style = NovaType.small, color = TDim, maxLines = 1) }
                        }
                    }
                }
            }
        }
        // Shortcut column (navigation · music · phone · apps · settings).
        TCardBox(
            Modifier.align(if (ui.railRight) Alignment.TopEnd else Alignment.TopStart)
                .padding(start = 14.dp, end = 14.dp, top = if (ui.railRight) 14.dp else 84.dp).width(60.dp),
            corner = 18.dp,
        ) {
            Column(Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TMapButton(Icons.Rounded.Navigation, tr("Navigation", "الملاحة"), act.onNavDetails)
                TMapButton(Icons.Rounded.MusicNote, tr("Music", "الموسيقى"), act.onMusicDetails)
                TMapButton(Icons.Rounded.Phone, tr("Phone", "الهاتف"), act.onPhone)
                TMapButton(Icons.Rounded.Apps, tr("Apps", "التطبيقات"), act.onApps)
                TMapButton(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), act.onSettings)
            }
        }
        // Compass (north-up / heading-up), volume, re-centre.
        TCardBox(
            Modifier.align(if (ui.railRight) Alignment.TopStart else Alignment.TopEnd)
                .padding(start = 14.dp, end = 14.dp, top = if (ui.railRight) 84.dp else 14.dp).width(64.dp),
            corner = 18.dp,
        ) {
            Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(52.dp).clip(CircleShape).clickable { headingUp = !headingUp }, contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.size(42.dp).clip(CircleShape).border(1.dp, TLine, CircleShape).rotate(if (headingUp) -(ui.bearing ?: 0f) else 0f),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.Navigation, null, tint = TRed, modifier = Modifier.size(16.dp))
                        Text("N", style = NovaType.small.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = TText)
                    }
                }
                TMapButton(Icons.AutoMirrored.Rounded.VolumeUp, tr("Volume", "الصوت"), act.onQuick)
                TMapButton(Icons.Rounded.MyLocation, tr("Centre", "موقعي")) { if (ui.lat == null) act.onEnableLocation() else setZoom(15f) }
            }
        }
        ScaleBar(ui, zoom, TText, Modifier.align(Alignment.BottomEnd).padding(14.dp))
        Text(MapTiles.ATTRIBUTION, style = NovaType.small.copy(fontSize = 10.sp), color = TDim, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
    }
}

@Composable
private fun TMapButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = TText, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun TeslaMedia(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val m = ui.media
    val pos = mediaPosition(m)
    TCardBox(modifier) {
        Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(m, 142.dp, 12.dp, act.onOpenMedia)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.Top) {
                    val needsAccess = m == null && !ui.notificationAccess
                    Column(Modifier.weight(1f).clickable(onClick = if (needsAccess) act.onGrantAccess else act.onOpenMedia)) {
                        Text(m?.title ?: tr("Nothing playing", "لا يوجد تشغيل"), style = NovaType.title.copy(fontSize = 20.sp), color = TText,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            m?.artist ?: if (needsAccess) tr("Allow notification access to show music ›", "اسمح بالوصول للإشعارات لعرض الموسيقى ›")
                            else tr("Tap to open music", "اضغط لفتح الموسيقى"),
                            style = NovaType.label, color = if (needsAccess) TRed else TDim,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.OpenInNew, tr("Queue & sources", "قائمة التشغيل"), tint = TText,
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = act.onMusicDetails).padding(8.dp))
                }
                Spacer(Modifier.weight(1f))
                FixedLtr {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        TIconButton(Icons.Rounded.SkipPrevious, tr("Previous", "السابق"), 40.dp, act.onPrev)
                        TIconButton(if (m?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, tr("Play / pause", "تشغيل / إيقاف"), 48.dp, act.onPlayPause)
                        TIconButton(Icons.Rounded.SkipNext, tr("Next", "التالي"), 40.dp, act.onNext)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (pos != null) FixedLtr {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pos.second, style = NovaType.small, color = TDim)
                        Box(Modifier.weight(1f).padding(horizontal = 10.dp).height(4.dp).clip(CircleShape).background(TLine)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(pos.first).background(TText))
                        }
                        Text(pos.third, style = NovaType.small, color = TDim)
                    }
                }
            }
        }
    }
}

@Composable
private fun TIconButton(icon: ImageVector, description: String, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size + 20.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = TText, modifier = Modifier.size(size))
    }
}

@Composable
private fun TeslaWeather(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val ar = LocalArabic.current
    val w = ui.weather
    TCardBox(modifier, onClick = act.onQuick) {
        if (!ui.showWeather || w == null) {
            Text(if (!ui.showWeather) tr("Weather off", "الطقس متوقف") else tr("Weather unavailable", "الطقس غير متاح"),
                style = NovaType.label, color = TDim, modifier = Modifier.align(Alignment.Center))
            return@TCardBox
        }
        Column(Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(weatherIcon(w), null, tint = weatherTint(w), modifier = Modifier.size(54.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("${Math.round(w.tempC)}°", style = NovaType.display.copy(fontSize = 36.sp), color = TText)
                    Text(ui.city ?: WeatherRepository.label(w.code, ar), style = NovaType.label.copy(fontSize = 17.sp), color = TText, maxLines = 1)
                }
            }
            if (w.maxC != null && w.minC != null) {
                Spacer(Modifier.height(10.dp))
                Text(tr("H ", "العظمى ") + "${Math.round(w.maxC)}°  " + tr("L ", "الصغرى ") + "${Math.round(w.minC)}°", style = NovaType.label, color = TDim, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TeslaClock(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val (time, ampm, date) = clockStrings(ui.use24h)
    TCardBox(modifier, onClick = act.onQuick) {
        Column(Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(time, style = NovaType.display.copy(fontSize = 54.sp, fontWeight = FontWeight.Medium), color = TText, maxLines = 1)
                if (ampm.isNotEmpty()) Text(" $ampm", style = NovaType.title, color = TText, modifier = Modifier.padding(bottom = 10.dp))
            }
            Text(date, style = NovaType.label, color = TText, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TeslaBottomBar(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val (bright, stepBright) = rememberLevel(act.brightness, act.onBrightness)
    val (vol, stepVol) = rememberLevel(act.volume, act.onVolume)
    Box(modifier.background(TCard)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(TLine))
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape).clickable(onClick = act.onVehicle), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.DirectionsCar, tr("Vehicle", "السيارة"), tint = TText, modifier = Modifier.size(42.dp))
            }
            Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(48.dp).background(TLine))
            TStepper(Icons.Rounded.LightMode, "${(bright * 100).roundToInt()}%", { stepBright(-0.1f) }, { stepBright(0.1f) })
            Spacer(Modifier.width(10.dp))
            TStatus(Icons.Rounded.Tune, TText, act.onQuick)
            TStatus(Icons.Rounded.Mic, TText, act.onVoice)
            Spacer(Modifier.width(14.dp))
            Row(Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                teslaDock(ui).forEach { s ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight().press({ act.onDock(s) }, { act.onDockLong(s) }),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        DockIcon(s, 50.dp)
                        Spacer(Modifier.height(6.dp))
                        UiDirection {
                            Text(s.title, style = NovaType.label.copy(fontSize = 15.sp), color = if (s.app != null || s.builtIn) TText else TDim,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            TStepper(Icons.AutoMirrored.Rounded.VolumeUp, "${(vol * 100).roundToInt()}%", { stepVol(-0.1f) }, { stepVol(0.1f) })
        }
    }
}

@Composable
private fun TStepper(icon: ImageVector, value: String, onDown: () -> Unit, onUp: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TIconButton(Icons.Rounded.ChevronLeft, "−", 28.dp, onDown)
        Icon(icon, null, tint = TText, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(6.dp))
        Text(value, style = NovaType.title.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium), color = TText, maxLines = 1)
        TIconButton(Icons.Rounded.ChevronRight, "+", 28.dp, onUp)
    }
}

// ============================================================================ BMW

private val BAccent = Color(0xFF2F8BFF)

/** BMW cockpit colours: night (the original design, default) or day. */
@androidx.compose.runtime.Immutable
private class BmwPalette(
    val text: Color, val dim: Color, val panelTop: Color, val panelBottom: Color, val chip: Color,
    /** Frames, ticks, tracks and dividers. */
    val line: Color,
    /** Shade over the cluster photo. */
    val shade: Color,
    val mapBg: Color, val mapStyle: MapTiles.Style,
)

private val BmwNight = BmwPalette(
    Color(0xFFF2F6FF), Color(0xFF9AA8C3), Color(0xFF111C34), Color(0xFF080E1C), Color(0xE60A1222),
    Color.White, Color(0xFF070C18), Color(0xFF0D1528), MapTiles.Style.DARK,
)
private val BmwDay = BmwPalette(
    Color(0xFF0E1A2E), Color(0xFF5B6B85), Color(0xFFFFFFFF), Color(0xFFE6ECF5), Color(0xF2FFFFFF),
    Color(0xFF1B2A44), Color(0xFFF1F5FB), Color(0xFFE8ECF2), MapTiles.Style.LIGHT,
)
private val LocalBmw = androidx.compose.runtime.staticCompositionLocalOf { BmwNight }
private val BText: Color @Composable get() = LocalBmw.current.text
private val BDim: Color @Composable get() = LocalBmw.current.dim

/** OpenStreetMap tiles inverted to a dark navy map with light labels, matching the cockpit. */
private val BmwMapTint = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    // invert + 180° hue rotation (water stays blue, parks green) with a slight blue lift
    0.488f, -1.216f, -0.122f, 0f, 216.8f,
    -0.383f, -0.387f, -0.130f, 0f, 233.5f,
    -0.426f, -1.430f, 0.856f, 0f, 273.0f,
    0f, 0f, 0f, 1f, 0f,
)))

/**
 * «واجهة BMW»: dark sports cockpit. A speed cluster with the user's car photo, a live dark street
 * map with the real guidance card, time/weather, the player and CarPlay, over volume and
 * brightness controls either side of the dock.
 */
@Composable
internal fun BmwLayout(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    // Night by default (the original design); day when chosen in Settings › Appearance.
    val night = ui.styleOptions.dark ?: true
    androidx.compose.runtime.CompositionLocalProvider(LocalBmw provides if (night) BmwNight else BmwDay) { BmwContent(ui, act, modifier) }
}

@Composable
private fun BmwContent(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BmwCluster(ui, act, Modifier.weight(0.37f).fillMaxHeight())
            BmwMap(ui, act, Modifier.weight(0.37f).fillMaxHeight())
            Column(Modifier.weight(0.28f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                UiDirection { BmwClock(ui, act, Modifier.fillMaxWidth().weight(0.95f)) }
                UiDirection { BmwMedia(ui, act, Modifier.fillMaxWidth().weight(1.2f)) }
                UiDirection { BmwCarPlay(ui, act, Modifier.fillMaxWidth().weight(0.72f)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(116.dp), verticalAlignment = Alignment.CenterVertically) {
            BmwLevel(Icons.AutoMirrored.Rounded.VolumeUp, tr("Volume", "الصوت"), act.volume, act.onVolume, Modifier.width(196.dp))
            Spacer(Modifier.width(12.dp))
            BmwDock(ui, act, Modifier.weight(1f).fillMaxHeight())
            Spacer(Modifier.width(12.dp))
            BmwLevel(Icons.Rounded.LightMode, tr("Brightness", "السطوع"), act.brightness, act.onBrightness, Modifier.width(196.dp))
        }
    }
}

@Composable
private fun BmwPanel(modifier: Modifier, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(LocalBmw.current.panelTop, LocalBmw.current.panelBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(BAccent.copy(alpha = 0.55f), BAccent.copy(alpha = 0.10f))), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

@Composable
private fun BmwCluster(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val ar = LocalArabic.current
    val speed = if (ui.hasLocation) ui.speedKmh ?: 0f else 0f
    BmwPanel(modifier, onClick = act.onVehicle) {
        Image(painterResource(R.drawable.scenic_road), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, alpha = 0.6f)
        val k = LocalBmw.current
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to k.shade.copy(alpha = 0.2f), 0.45f to k.shade.copy(alpha = 0.8f), 1f to k.shade.copy(alpha = 0.95f))))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wDp = maxWidth; val hDp = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val frame = Brush.verticalGradient(listOf(k.line.copy(alpha = 0.85f), k.line.copy(alpha = 0.25f)))
                // Side frames of the cluster.
                for (s in listOf(-1f, 1f)) {
                    val x = { f: Float -> if (s < 0) w * f else w * (1 - f) }
                    val p = Path().apply { moveTo(x(0.30f), h * 0.07f); lineTo(x(0.05f), h * 0.50f); lineTo(x(0.17f), h * 0.88f) }
                    drawPath(p, frame, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }
                // Speed scale 0–260 km/h; lit up to the live GPS speed.
                for (i in 0..26) {
                    val t = i / 26f
                    val y = h * (0.84f - 0.70f * t)
                    val xr = w * (0.19f + 0.08f * t)
                    val len = w * if (i % 5 == 0) 0.07f else 0.04f
                    val lit = i * 10f <= speed && speed > 0f
                    drawLine(if (lit) BAccent else k.line.copy(alpha = 0.35f), Offset(xr - len, y), Offset(xr, y),
                        strokeWidth = (if (i % 5 == 0) 3.dp else 2.dp).toPx(), cap = StrokeCap.Round)
                }
                // Light trails under the car.
                for (s in listOf(-1f, 1f)) drawLine(
                    Brush.verticalGradient(listOf(BAccent.copy(alpha = 0f), BAccent), startY = h * 0.62f, endY = h * 0.92f),
                    Offset(w / 2 + s * w * 0.13f, h * 0.62f), Offset(w / 2 + s * w * 0.34f, h * 0.92f), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round
                )
            }
            for (v in listOf(0, 50, 100, 150, 200, 260)) {
                val t = v / 260f
                Text("$v", style = NovaType.small.copy(fontSize = 13.sp), color = BText,
                    modifier = Modifier.offset(x = wDp * (0.21f + 0.08f * t), y = hDp * (0.84f - 0.70f * t) - 9.dp))
            }
            Column(Modifier.align(Alignment.TopCenter).padding(top = hDp * 0.08f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (ui.hasLocation) Text(speed.toInt().toString(), style = NovaType.hero.copy(fontSize = 64.sp, fontWeight = FontWeight.Bold), color = BText, maxLines = 1)
                else UiDirection {
                    Text(tr("Enable location ›", "فعّل الموقع ›"), style = NovaType.title, color = Color(0xFFFF5470),
                        modifier = Modifier.padding(vertical = 14.dp).clickable(onClick = act.onEnableLocation))
                }
                Text(tr("km/h", "كم/س"), style = NovaType.label.copy(fontSize = 17.sp), color = BText)
            }
            // Direction of travel from GPS (no gear data is available on this unit).
            UiDirection {
                Column(Modifier.align(Alignment.CenterEnd).absolutePadding(right = wDp * 0.17f).offset(y = -hDp * 0.08f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(ui.bearing?.let { compassLabel(it, ar) } ?: "--", style = NovaType.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = BText, maxLines = 1)
                    Text(tr("HEADING", "الاتجاه"), style = NovaType.small.copy(fontSize = 12.sp), color = BDim)
                }
            }
            CarImage(ui.carImageVersion, Modifier.align(Alignment.Center).offset(y = hDp * 0.10f).fillMaxWidth(0.50f).fillMaxHeight(0.42f), hint = false)
            FixedLtr {
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    val fuel = obdFuel(ui)
                    if (ui.obdEnabled) {
                        Icon(Icons.Rounded.LocalGasStation, null, tint = BText, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(fuel?.let { "$it %" } ?: "--", style = NovaType.title.copy(fontSize = 19.sp), color = BText)
                    }
                    Spacer(Modifier.weight(1f))
                    UiDirection {
                        Text(carDisplayName(ui.brand, ui.car, ar), style = NovaType.small, color = BDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.weight(1f))
                    val w = ui.weather
                    if (ui.showWeather && w != null) {
                        Icon(Icons.Rounded.Thermostat, null, tint = BText, modifier = Modifier.size(22.dp))
                        Text(String.format(java.util.Locale.US, "%+.1f °C", w.tempC), style = NovaType.title.copy(fontSize = 19.sp), color = BText)
                    }
                }
            }
        }
    }
}

@Composable
private fun BmwMap(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val (zoom, setZoom) = MapZoomState()
    var headingUp by rememberSaveable { mutableStateOf(true) }
    BmwPanel(modifier) {
        TileMap(
            ui.lat, ui.lon, ui.bearing, LocalBmw.current.mapStyle, MapMarker.BMW, headingUp, zoom, setZoom,
            onTap = act.onNavDetails,
            modifier = Modifier.fillMaxSize(), tint = if (LocalBmw.current === BmwNight) BmwMapTint else null, background = LocalBmw.current.mapBg, focusY = 0.64f,
            route = ui.routePoints, routeProgress = ui.routeProgress, routeColor = BAccent, destination = ui.destination,
        )
        if (ui.lat == null) UiDirection {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.GpsNotFixed, null, tint = BDim, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text(tr("Waiting for your location…", "بانتظار تحديد موقعك…"), style = NovaType.title.copy(fontSize = 17.sp), color = BText)
                if (!ui.hasLocation) Text(tr("Enable location ›", "فعّل الموقع ›"), style = NovaType.label, color = BAccent,
                    modifier = Modifier.padding(top = 8.dp).clickable(onClick = act.onEnableLocation))
            }
        }
        // Guidance card (from the navigation app's own notification) or a shortcut to it.
        val nav = ui.nav
        Box(
            Modifier.align(Alignment.TopStart).padding(12.dp).width(280.dp).clip(RoundedCornerShape(16.dp))
                .background(LocalBmw.current.chip).border(1.dp, BAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .clickable(onClick = if (nav != null) act.onNavCard else act.onOpenNav)
        ) {
            UiDirection {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    val icon = nav?.maneuver
                    if (icon != null) Image(icon, null, Modifier.size(40.dp))
                    else Icon(if (nav != null) Icons.Rounded.TurnRight else Icons.Rounded.Map, null, tint = BText, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(nav?.primary ?: tr("No active route", "لا يوجد توجيه نشط"), style = NovaType.title.copy(fontSize = if (nav != null) 22.sp else 17.sp),
                            color = BText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(nav?.secondary ?: tr("Open " + (ui.navApp?.label ?: "maps"), "افتح الخرائط"), style = NovaType.small, color = BDim,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        nav?.eta?.let { Text(it, style = NovaType.small, color = BDim, maxLines = 1) }
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.TopEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BRound(Icons.Rounded.Navigation, tr("North up / heading up", "اتجاه الخريطة"), if (headingUp) -(ui.bearing ?: 0f) else 0f, Color(0xFFFF5470)) { headingUp = !headingUp }
            BRound(Icons.AutoMirrored.Rounded.VolumeUp, tr("Volume", "الصوت"), onClick = act.onQuick)
            BRound(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), onClick = act.onSettings)
            BRound(Icons.Rounded.MoreHoriz, tr("Destinations", "الوجهات"), onClick = act.onNavDetails)
        }
        Text(MapTiles.ATTRIBUTION, style = NovaType.small.copy(fontSize = 10.sp), color = BDim, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        ScaleBar(ui, zoom, BDim, Modifier.align(Alignment.BottomEnd).padding(12.dp))
    }
}

@Composable
private fun BRound(icon: ImageVector, description: String, rotation: Float = 0f, tint: Color = BText, onClick: () -> Unit) {
    Box(
        Modifier.size(54.dp).clip(CircleShape).background(LocalBmw.current.chip).border(1.dp, BAccent.copy(alpha = 0.3f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint, modifier = Modifier.size(26.dp).rotate(rotation)) }
}

@Composable
private fun BmwClock(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val ar = LocalArabic.current
    val (time, ampm, date) = clockStrings(ui.use24h)
    val w = ui.weather
    BmwPanel(modifier, onClick = act.onQuick) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.Center) {
            Text(date, style = NovaType.small.copy(fontSize = 14.sp), color = BText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(time, style = NovaType.display.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold), color = BText, maxLines = 1)
                    if (ampm.isNotEmpty()) Text(" $ampm", style = NovaType.title, color = BText, modifier = Modifier.padding(bottom = 8.dp))
                }
                Spacer(Modifier.weight(1f))
                if (ui.showWeather && w != null) Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(weatherIcon(w), null, tint = weatherTint(w), modifier = Modifier.size(38.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${Math.round(w.tempC)}°", style = NovaType.display.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold), color = BText)
                    }
                    Text(ui.city ?: WeatherRepository.label(w.code, ar), style = NovaType.small, color = BText, maxLines = 1)
                }
            }
            if (ui.showWeather && w != null && w.maxC != null && w.minC != null) Text(
                tr("H ", "العظمى ") + "${Math.round(w.maxC)}°  " + tr("L ", "الصغرى ") + "${Math.round(w.minC)}°",
                style = NovaType.small, color = BDim, maxLines = 1, modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun BmwMedia(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val m = ui.media
    val pos = mediaPosition(m)
    BmwPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(m, 84.dp, 12.dp, act.onOpenMedia)
                Spacer(Modifier.width(12.dp))
                val needsAccess = m == null && !ui.notificationAccess
                Column(Modifier.weight(1f).clickable(onClick = if (needsAccess) act.onGrantAccess else act.onOpenMedia)) {
                    Text(m?.title ?: tr("Nothing playing", "لا يوجد تشغيل"), style = NovaType.title.copy(fontSize = 18.sp), color = BText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        m?.artist ?: if (needsAccess) tr("Allow notification access to show music ›", "اسمح بالوصول للإشعارات لعرض الموسيقى ›")
                        else tr("Tap to open music", "اضغط لفتح الموسيقى"),
                        style = NovaType.small, color = if (needsAccess) BAccent else BDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.OpenInNew, tr("Queue & sources", "قائمة التشغيل"), tint = BText,
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = act.onMusicDetails).padding(8.dp))
            }
            if (pos != null) FixedLtr {
                Column {
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(LocalBmw.current.line.copy(alpha = 0.15f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(pos.first).background(Brush.horizontalGradient(listOf(BAccent, Color(0xFF8B7CFF)))))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(pos.second, style = NovaType.small, color = BDim); Spacer(Modifier.weight(1f)); Text(pos.third, style = NovaType.small, color = BDim)
                    }
                }
            }
            FixedLtr {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    BIcon(Icons.Rounded.SkipPrevious, tr("Previous", "السابق"), act.onPrev)
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).border(2.dp, BAccent, CircleShape).background(BAccent.copy(alpha = 0.15f)).clickable(onClick = act.onPlayPause),
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (m?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, tr("Play / pause", "تشغيل / إيقاف"), tint = BText, modifier = Modifier.size(32.dp)) }
                    BIcon(Icons.Rounded.SkipNext, tr("Next", "التالي"), act.onNext)
                }
            }
        }
    }
}

@Composable
private fun BIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(54.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = BText, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun BmwCarPlay(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    val ar = LocalArabic.current
    BmwPanel(modifier, onClick = act.onCarPlay) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PhoneIphone, null, tint = BText, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (ui.carPlayConnected) tr("iPhone connected", "iPhone متصل") else tr("Connect iPhone by USB", "وصّل iPhone بمنفذ USB"),
                    style = NovaType.small, color = BDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Apple CarPlay", style = NovaType.title.copy(fontSize = 19.sp), color = BText, maxLines = 1)
            }
            Image(painterResource(R.drawable.carplay_icon), null, Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)))
            Icon(if (ar) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight, null, tint = BText, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun BmwLevel(icon: ImageVector, label: String, read: () -> Float, write: (Float) -> Unit, modifier: Modifier) {
    val (v, step) = rememberLevel(read, write)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BIcon(Icons.Rounded.Remove, "−") { step(-0.1f) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
                Icon(icon, label, tint = BDim, modifier = Modifier.size(20.dp))
                Text("${(v * 100).roundToInt()}%", style = NovaType.title.copy(fontSize = 26.sp, fontWeight = FontWeight.Medium), color = BText, maxLines = 1)
            }
            BIcon(Icons.Rounded.Add, "+") { step(0.1f) }
        }
        Box(Modifier.padding(top = 6.dp).width(170.dp).height(4.dp).clip(CircleShape).background(LocalBmw.current.line.copy(alpha = 0.12f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(v).background(Brush.horizontalGradient(listOf(BAccent, Color(0xFFFF4D6D)))))
        }
    }
}

@Composable
private fun BmwDock(ui: HomeUi, act: HomeActions, modifier: Modifier) {
    // YouTube next to Music (Settings › Appearance › BMW), like the Tesla bar; hidden while driving.
    val dock = if (ui.styleOptions.showYoutube) teslaDock(ui) else ui.dock
    BmwPanel(modifier) {
        Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            dock.forEachIndexed { i, s ->
                if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight(0.6f).background(LocalBmw.current.line.copy(alpha = 0.10f)))
                Column(
                    Modifier.weight(1f).fillMaxHeight().press({ act.onDock(s) }, { act.onDockLong(s) }),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    DockIcon(s, 54.dp)
                    Spacer(Modifier.height(6.dp))
                    UiDirection {
                        Text(s.title, style = NovaType.label.copy(fontSize = 15.sp), color = if (s.app != null || s.builtIn) BText else BDim,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
