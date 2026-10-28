package com.abu00salman.novadrive.ui.styles

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.core.CarImageStore
import com.abu00salman.novadrive.core.StyleOptions
import com.abu00salman.novadrive.core.UiStyle
import com.abu00salman.novadrive.core.carDisplayName
import com.abu00salman.novadrive.data.Link
import com.abu00salman.novadrive.data.MapTiles
import com.abu00salman.novadrive.data.MediaInfo
import com.abu00salman.novadrive.data.WeatherRepository
import com.abu00salman.novadrive.ui.components.CarImage
import com.abu00salman.novadrive.ui.components.FixedLtr
import com.abu00salman.novadrive.ui.components.UiDirection
import com.abu00salman.novadrive.ui.home.Artwork
import com.abu00salman.novadrive.ui.home.DockIcon
import com.abu00salman.novadrive.ui.home.DockSlot
import com.abu00salman.novadrive.ui.home.HomeActions
import com.abu00salman.novadrive.ui.home.HomeUi
import com.abu00salman.novadrive.ui.home.MapMarker
import com.abu00salman.novadrive.ui.home.TileMap
import com.abu00salman.novadrive.ui.home.formatMs
import com.abu00salman.novadrive.ui.home.uiLocale
import com.abu00salman.novadrive.ui.home.weatherIcon
import com.abu00salman.novadrive.ui.home.weatherTint
import com.abu00salman.novadrive.ui.nav.NightMapTint
import com.abu00salman.novadrive.ui.shell.GpsState
import com.abu00salman.novadrive.ui.shell.rememberMinuteClock
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.NovaFonts
import com.abu00salman.novadrive.ui.theme.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

/** The resolved look of one of the new interfaces (colours, shapes, font, map treatment). */
@Immutable
data class Skin(
    val id: String,
    val dark: Boolean,
    val bg: List<Color>,
    val surface: Color,
    val surfaceHi: Color,
    val border: Color,
    val text: Color,
    val dim: Color,
    val accent: Color,
    val accent2: Color,
    val corner: Dp,
    val cut: Boolean,
    val font: FontFamily,
    val glow: Boolean,
    val mapTint: ColorFilter?,
    val mapBg: Color,
    val wallpaper: Int,
    val wallpaperVersion: Long,
) {
    val shape: Shape get() = if (cut) CutCornerShape(corner * 0.6f) else RoundedCornerShape(corner)
    fun shape(r: Dp): Shape = if (cut) CutCornerShape(r * 0.6f) else RoundedCornerShape(r)
}

val LocalSkin = staticCompositionLocalOf<Skin> { error("No interface skin") }

private fun c(v: Long) = Color(v)

/** Wallpaper colour presets offered for every customisable interface. */
val WallpaperPresets: List<List<Color>> = listOf(
    listOf(c(0xFF0F2027), c(0xFF203A43), c(0xFF2C5364)),
    listOf(c(0xFF2B1055), c(0xFF4B1D6B), c(0xFF0F172A)),
    listOf(c(0xFFE0F7FA), c(0xFFE3F2FD), c(0xFFF3E5F5)),
    listOf(c(0xFFFDF6E3), c(0xFFF1E4C8), c(0xFFE7D9BC)),
)

/** Olive/sepia recolouring of map tiles for the TANK off-road look. */
private val TopoTint = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    0.30f, 0.50f, 0.10f, 0f, -18f,
    0.28f, 0.52f, 0.10f, 0f, -14f,
    0.20f, 0.40f, 0.08f, 0f, -24f,
    0f, 0f, 0f, 1f, 0f,
)))

fun skinFor(style: UiStyle, o: StyleOptions, appDark: Boolean): Skin {
    val dark = o.dark ?: style.defaultDark ?: appDark
    val accent = Color(style.accents.getOrElse(o.accent) { style.accents.firstOrNull() ?: 0xFF2F8BFF })
    val font = if (o.font == 1) FontFamily.Default else NovaFonts.family
    fun mk(bg: List<Long>, surface: Long, surfaceHi: Long, border: Long, text: Long, dim: Long, accent2: Long, corner: Dp, cut: Boolean = false, glow: Boolean = false) = Skin(
        style.id, dark, bg.map(::c), c(surface), c(surfaceHi), c(border), c(text), c(dim), accent, c(accent2), corner, cut, font, glow,
        if (style.id == "tank") TopoTint else if (dark) NightMapTint else null,
        if (dark) c(0xFF0D1528) else c(0xFFE6E8EB), o.wallpaper, o.wallpaperVersion,
    )
    return when (style.id) {
        "classic" -> if (dark) mk(listOf(0xFF0F141B, 0xFF0B0F14), 0xFF19212C, 0xFF222C39, 0xFF2C3746, 0xFFF8FAFC, 0xFFB6C2D1, 0xFF22C55E, 20.dp)
            else mk(listOf(0xFFF4F6F9, 0xFFE8ECF1), 0xFFFFFFFF, 0xFFF1F4F8, 0xFFD5DCE5, 0xFF0F172A, 0xFF475569, 0xFF16A34A, 20.dp)
        "mercedes" -> if (dark) mk(listOf(0xFF04050A, 0xFF0A0D1A, 0xFF120A1E), 0xD9101322, 0xE61A1E33, 0x5900C2FF, 0xFFF5F7FF, 0xFF9BA3C0, 0xFFE040FB, 28.dp, glow = true)
            else mk(listOf(0xFFEEF0F7, 0xFFDDE2F0), 0xE6FFFFFF, 0xFFF4F6FB, 0x407C4DFF, 0xFF0B0E1A, 0xFF4A5070, 0xFFE040FB, 28.dp, glow = true)
        "ford" -> if (dark) mk(listOf(0xFF0E1A26, 0xFF08111A), 0xFF14263A, 0xFF1B3149, 0xFF27466A, 0xFFF1F6FB, 0xFF9DB2C7, 0xFF64B5F6, 6.dp)
            else mk(listOf(0xFFE9F0F6, 0xFFD7E3EE), 0xFFFFFFFF, 0xFFF2F6FA, 0xFFB9CCDD, 0xFF0E1A26, 0xFF46607A, 0xFF1565C0, 6.dp)
        "apple" -> if (dark) mk(listOf(0xFF141428, 0xFF2A1640, 0xFF0B2645), 0x1FFFFFFF, 0x2EFFFFFF, 0x33FFFFFF, 0xFFFFFFFF, 0xA6FFFFFF, 0xFFFF375F, 34.dp)
            else mk(listOf(0xFFE7F0FF, 0xFFFBE7F3, 0xFFE0F2FE), 0x8CFFFFFF, 0xB3FFFFFF, 0xCCFFFFFF, 0xFF0A0A0F, 0xB33C3C43, 0xFFFF375F, 34.dp)
        "jetour" -> if (dark) mk(listOf(0xFF111316, 0xFF1C1F24), 0xFF1E2227, 0xFF272C33, 0xFF363C45, 0xFFF3F4F6, 0xFF9CA3AF, 0xFFFFC107, 18.dp, cut = true)
            else mk(listOf(0xFFF3F4F6, 0xFFE2E5EA), 0xFFFFFFFF, 0xFFF6F7F9, 0xFFD1D5DB, 0xFF111827, 0xFF4B5563, 0xFFF59E0B, 18.dp, cut = true)
        "byd" -> if (dark) mk(listOf(0xFF0A1414, 0xFF071010), 0xFF102020, 0xFF162A2A, 0xFF1D3A38, 0xFFE6FFFD, 0xFF8FB8B4, 0xFF38BDF8, 22.dp)
            else mk(listOf(0xFFF5FAFA, 0xFFE4F1EF), 0xFFFFFFFF, 0xFFF3F9F8, 0xFFD5E6E4, 0xFF0B1F1E, 0xFF4B6563, 0xFF0EA5E9, 22.dp)
        "haval" -> if (dark) mk(listOf(0xFF15171A, 0xFF0D0F11), 0xFF1C1F23, 0xFF24282D, 0xFF30353C, 0xFFF5F5F5, 0xFFA1A1AA, 0xFF9CA3AF, 12.dp)
            else mk(listOf(0xFFF4F4F5, 0xFFE4E4E7), 0xFFFFFFFF, 0xFFF8F8F9, 0xFFD4D4D8, 0xFF18181B, 0xFF52525B, 0xFF71717A, 12.dp)
        "tank" -> if (dark) mk(listOf(0xFF14150F, 0xFF1D2016), 0xFF22251A, 0xFF2B2F21, 0xFF4A4F33, 0xFFEDE8D5, 0xFFB3AD92, 0xFF9CB65A, 10.dp, cut = true)
            else mk(listOf(0xFFE9E4D2, 0xFFD8D1B8), 0xFFF3EFE2, 0xFFEAE4D0, 0xFFB5AC88, 0xFF23261B, 0xFF5F5A45, 0xFF6B7D3C, 10.dp, cut = true)
        else -> mk(listOf(0xFF0B1226, 0xFF060B18), 0xFF111A33, 0xFF18233F, 0xFF27365C, 0xFFF2F6FF, 0xFF9AA8C3, 0xFF8B5CF6, 20.dp)
    }
}

// ============================================================================ text

@Composable
fun st(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = LocalSkin.current.text): TextStyle =
    TextStyle(fontFamily = LocalSkin.current.font, fontSize = size, fontWeight = weight, color = color)

// ============================================================================ surfaces

/** The interface's background: its own gradient, a colour preset, or the user's photo. */
@Composable
fun SkinBackground(modifier: Modifier = Modifier) {
    val k = LocalSkin.current
    val ctx = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, k.id, k.wallpaper, k.wallpaperVersion) {
        value = if (k.wallpaper == StyleOptions.WALLPAPER_IMAGE)
            withContext(Dispatchers.IO) { CarImageStore.load(ctx, 1600, CarImageStore.wallpaperName(k.id)) } else null
    }
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(WallpaperPresets.getOrNull(k.wallpaper - 1) ?: k.bg))) {
        photo?.let {
            Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            // Keeps text readable over any photo.
            Box(Modifier.fillMaxSize().background(if (k.dark) Color(0x8C000000) else Color(0x66FFFFFF)))
        }
        if (k.glow) Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(k.accent.copy(alpha = 0.28f), Color.Transparent), Offset(0f, size.height), size.width * 0.5f),
                size.width * 0.5f, Offset(0f, size.height))
            drawCircle(Brush.radialGradient(listOf(k.accent2.copy(alpha = 0.22f), Color.Transparent), Offset(size.width, 0f), size.width * 0.45f),
                size.width * 0.45f, Offset(size.width, 0f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    fill: Color? = null,
    corner: Dp? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val k = LocalSkin.current
    val shape = corner?.let { k.shape(it) } ?: k.shape
    Box(
        modifier.clip(shape)
            .background(fill ?: k.surface)
            .border(1.dp, if (k.glow) Brush.linearGradient(listOf(k.accent.copy(alpha = 0.55f), k.accent2.copy(alpha = 0.35f))) else Brush.linearGradient(listOf(k.border, k.border)), shape)
            .then(if (onClick != null || onLongClick != null) Modifier.combinedClickable(onClick = { onClick?.invoke() }, onLongClick = onLongClick) else Modifier),
        content = content,
    )
}

@Composable
fun SRound(icon: ImageVector, description: String, size: Dp = 56.dp, filled: Boolean = false, tint: Color? = null, onClick: () -> Unit) {
    val k = LocalSkin.current
    Box(
        Modifier.size(size).clip(if (k.cut) CutCornerShape(size * 0.22f) else CircleShape)
            .background(if (filled) k.accent else k.surfaceHi)
            .border(1.dp, if (filled) k.accent else k.border, if (k.cut) CutCornerShape(size * 0.22f) else CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint ?: if (filled) Color.White else k.text, modifier = Modifier.size(size * 0.5f)) }
}

// ============================================================================ clock & status

@Immutable
data class ClockText(val time: String, val ampm: String, val date: String, val dateShort: String)

@Composable
fun rememberClock(use24h: Boolean): ClockText {
    val ar = LocalArabic.current
    val now = rememberMinuteClock()
    val loc = uiLocale(ar)
    return remember(now, use24h, ar) {
        val pm = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.AM_PM) == Calendar.PM
        ClockText(
            SimpleDateFormat(if (use24h) "HH:mm" else "h:mm", loc).format(Date(now)),
            if (use24h) "" else if (ar) (if (pm) "م" else "ص") else (if (pm) "PM" else "AM"),
            SimpleDateFormat(if (ar) "EEEE d MMMM yyyy" else "EEEE, d MMMM yyyy", loc).format(Date(now)),
            SimpleDateFormat(if (ar) "EEEE d MMMM" else "EEE d MMM", loc).format(Date(now)),
        )
    }
}

/** Wi-Fi, Bluetooth and GPS state, each opening its settings. */
@Composable
fun StatusIcons(ui: HomeUi, act: HomeActions, size: Dp = 24.dp) {
    val k = LocalSkin.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusGlyph(if (ui.net.wifi == Link.OFF) Icons.Rounded.WifiOff else Icons.Rounded.Wifi, if (ui.net.wifi == Link.CONNECTED) k.text else k.dim, size, act.onWifi)
        StatusGlyph(
            when (ui.bt) { Link.CONNECTED -> Icons.Rounded.BluetoothConnected; Link.ON -> Icons.Rounded.Bluetooth; Link.OFF -> Icons.Rounded.BluetoothDisabled },
            if (ui.bt == Link.OFF) k.dim else k.text, size, act.onBluetooth,
        )
        StatusGlyph(
            when (ui.gps) { GpsState.FIX -> Icons.Rounded.GpsFixed; GpsState.SEARCHING -> Icons.Rounded.GpsNotFixed; GpsState.OFF -> Icons.Rounded.LocationOff },
            when (ui.gps) { GpsState.FIX -> k.accent; GpsState.SEARCHING -> k.text; GpsState.OFF -> Color(0xFFEF4444) }, size, act.onGps,
        )
    }
}

@Composable
private fun StatusGlyph(icon: ImageVector, tint: Color, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size + 20.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size))
    }
}

// ============================================================================ navigation menu

@Immutable
data class MenuItem(val icon: ImageVector, val label: String, val onClick: () -> Unit, val special: Boolean = false)

@Composable
fun menuItems(act: HomeActions, withVoice: Boolean = true): List<MenuItem> = buildList {
    add(MenuItem(Icons.Rounded.Navigation, tr("Navigation", "الملاحة"), act.onNavDetails))
    add(MenuItem(Icons.Rounded.MusicNote, tr("Music", "الموسيقى"), act.onMusicDetails))
    add(MenuItem(Icons.Rounded.Phone, tr("Phone", "الهاتف"), act.onPhone))
    if (withVoice) add(MenuItem(Icons.Rounded.AutoAwesome, "Nova AI", act.onVoice, special = true))
    add(MenuItem(Icons.Rounded.DirectionsCar, tr("Vehicle", "السيارة"), act.onVehicle))
    add(MenuItem(Icons.Rounded.Apps, tr("Apps", "التطبيقات"), act.onApps))
    add(MenuItem(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), act.onSettings))
}

// ============================================================================ map

/** Live map with the active route; tap or the expand button opens full-screen navigation. */
@Composable
fun SkinMap(ui: HomeUi, act: HomeActions, modifier: Modifier, focusY: Float = 0.62f, guidance: Boolean = true, corner: Dp? = null) {
    val k = LocalSkin.current
    var zoom by rememberSaveable { mutableFloatStateOf(15f) }
    SCard(modifier, corner = corner) {
        TileMap(
            ui.lat, ui.lon, ui.bearing, if (k.dark) MapTiles.Style.DARK else MapTiles.Style.LIGHT, MapMarker.DOT, true, zoom, { zoom = it },
            onTap = act.onNavDetails, modifier = Modifier.fillMaxSize(), tint = k.mapTint, background = k.mapBg, focusY = focusY,
            route = ui.routePoints, routeProgress = ui.routeProgress, routeColor = k.accent, destination = ui.destination, markerColor = k.accent,
        )
        if (ui.lat == null) UiDirection {
            Column(Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.GpsNotFixed, null, tint = k.dim, modifier = Modifier.size(36.dp))
                Text(tr("Waiting for your location…", "بانتظار تحديد موقعك…"), style = st(16.sp, FontWeight.Medium), textAlign = TextAlign.Center)
                if (!ui.hasLocation) Text(tr("Enable location ›", "فعّل الموقع ›"), style = st(15.sp, FontWeight.SemiBold, k.accent),
                    modifier = Modifier.padding(top = 6.dp).clickable(onClick = act.onEnableLocation))
            }
        }
        val nav = ui.nav
        if (guidance && nav != null) UiDirection {
            Row(
                Modifier.align(Alignment.TopStart).padding(10.dp).fillMaxWidth(0.82f).clip(k.shape(16.dp))
                    .background(if (k.dark) Color(0xE6101522) else Color(0xF2FFFFFF)).clickable(onClick = act.onNavCard).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = nav.maneuver
                if (icon != null) Image(icon, null, Modifier.size(36.dp)) else Icon(Icons.Rounded.TurnRight, null, tint = k.accent, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(nav.primary, style = st(20.sp, FontWeight.Bold, if (k.dark) Color.White else Color(0xFF0B0F19)), maxLines = 1)
                    nav.secondary?.let { Text(it, style = st(13.sp, color = if (k.dark) Color(0xFFC9D2E3) else Color(0xFF475569)), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(10.dp)) { SRound(Icons.Rounded.Fullscreen, tr("Full screen map", "الخريطة بملء الشاشة"), 46.dp, onClick = act.onNavDetails) }
        Text("© OpenStreetMap", style = st(9.sp, color = k.dim), modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
    }
}

// ============================================================================ media

@Composable
private fun rememberSecondTick(active: Boolean): Long {
    val v by produceState(0L, active) { while (active) { delay(1000); value++ } }
    return v
}

/** (fraction, elapsed, total) of the playing item, or null when the source reports no position. */
@Composable
fun mediaProgress(m: MediaInfo?): Triple<Float, String, String>? {
    rememberSecondTick(m?.playing == true)
    if (m == null || m.positionMs < 0 || m.durationMs <= 0) return null
    val pos = m.positionNow().coerceIn(0, m.durationMs)
    return Triple(pos.toFloat() / m.durationMs, formatMs(pos), formatMs(m.durationMs))
}

@Composable
fun MediaTitle(ui: HomeUi, act: HomeActions, titleSize: TextUnit = 19.sp, modifier: Modifier = Modifier, center: Boolean = false) {
    val k = LocalSkin.current
    val m = ui.media
    val needsAccess = m == null && !ui.notificationAccess
    Column(modifier.clickable(onClick = if (needsAccess) act.onGrantAccess else act.onOpenMedia),
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start) {
        Text(m?.title ?: tr("Nothing playing", "لا يوجد تشغيل"), style = st(titleSize, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            m?.artist ?: if (needsAccess) tr("Allow notification access to show music ›", "اسمح بالوصول للإشعارات لعرض الموسيقى ›")
            else tr("Tap to open music", "اضغط لفتح الموسيقى"),
            style = st(titleSize * 0.74f, color = if (needsAccess) k.accent else k.dim), maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MediaButtons(ui: HomeUi, act: HomeActions, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    val m = ui.media
    FixedLtr {
        Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            SRound(Icons.Rounded.SkipPrevious, tr("Previous", "السابق"), size * 0.82f, onClick = act.onPrev)
            SRound(if (m?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, tr("Play / pause", "تشغيل / إيقاف"), size, filled = true, onClick = act.onPlayPause)
            SRound(Icons.Rounded.SkipNext, tr("Next", "التالي"), size * 0.82f, onClick = act.onNext)
        }
    }
}

@Composable
fun MediaBar(ui: HomeUi, modifier: Modifier = Modifier, times: Boolean = true) {
    val k = LocalSkin.current
    val pos = mediaProgress(ui.media) ?: return
    FixedLtr {
        Column(modifier) {
            Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(k.text.copy(alpha = 0.14f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(pos.first).background(Brush.horizontalGradient(listOf(k.accent, k.accent2))))
            }
            if (times) Row(Modifier.fillMaxWidth().padding(top = 3.dp)) {
                Text(pos.second, style = st(12.sp, color = k.dim)); Spacer(Modifier.weight(1f)); Text(pos.third, style = st(12.sp, color = k.dim))
            }
        }
    }
}

@Composable
fun MediaArt(ui: HomeUi, act: HomeActions, size: Dp, corner: Dp = 14.dp) = Artwork(ui.media, size, corner, act.onOpenMedia)

/** Ready-made player card: art, title, progress, controls. */
@Composable
fun SkinMedia(ui: HomeUi, act: HomeActions, modifier: Modifier, art: Dp = 96.dp, vertical: Boolean = false) {
    SCard(modifier) {
        if (vertical) Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
            MediaArt(ui, act, art, 18.dp)
            UiDirection { MediaTitle(ui, act, 20.sp, Modifier.fillMaxWidth(), center = true) }
            MediaBar(ui, Modifier.fillMaxWidth())
            MediaButtons(ui, act, 60.dp, Modifier.fillMaxWidth())
        } else Row(Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaArt(ui, act, art)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UiDirection { MediaTitle(ui, act, 18.sp, Modifier.weight(1f)) }
                    Icon(Icons.Rounded.OpenInNew, tr("Queue & sources", "قائمة التشغيل"), tint = LocalSkin.current.text,
                        modifier = Modifier.size(38.dp).clip(CircleShape).clickable(onClick = act.onMusicDetails).padding(8.dp))
                }
                MediaBar(ui, Modifier.fillMaxWidth())
                MediaButtons(ui, act, 54.dp, Modifier.fillMaxWidth())
            }
        }
    }
}

// ============================================================================ weather, speed, car

@Composable
fun WeatherLine(ui: HomeUi, tempSize: TextUnit = 34.sp, iconSize: Dp = 40.dp) {
    val k = LocalSkin.current
    val ar = LocalArabic.current
    val w = ui.weather
    if (!ui.showWeather || w == null) {
        Text(if (!ui.showWeather) tr("Weather off", "الطقس متوقف") else tr("Weather unavailable", "الطقس غير متاح"), style = st(15.sp, color = k.dim))
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(weatherIcon(w), null, tint = weatherTint(w), modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("${Math.round(w.tempC)}°", style = st(tempSize, FontWeight.SemiBold))
            Text(listOfNotNull(ui.city, WeatherRepository.label(w.code, ar)).joinToString(" · "), style = st(tempSize * 0.42f, color = k.dim), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun HiLo(ui: HomeUi, size: TextUnit = 14.sp) {
    val w = ui.weather ?: return
    if (!ui.showWeather || w.maxC == null || w.minC == null) return
    Text(tr("H ", "العظمى ") + "${Math.round(w.maxC)}°  " + tr("L ", "الصغرى ") + "${Math.round(w.minC)}°", style = st(size, color = LocalSkin.current.dim), maxLines = 1)
}

/** GPS speed — the only speed source NovaDrive trusts (labelled as such). */
@Composable
fun SpeedText(ui: HomeUi, size: TextUnit, act: HomeActions, unit: Boolean = true, color: Color = LocalSkin.current.text) {
    val k = LocalSkin.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (ui.hasLocation) Text(ui.speedKmh?.toInt()?.toString() ?: "--", style = st(size, FontWeight.Bold, color), maxLines = 1)
        else Text(tr("Enable location ›", "فعّل الموقع ›"), style = st(size * 0.3f, FontWeight.SemiBold, k.accent), modifier = Modifier.clickable(onClick = act.onEnableLocation))
        if (unit) Text(tr("km/h · GPS", "كم/س · GPS"), style = st(size * 0.2f, color = k.dim))
    }
}

@Composable
fun SkinCar(ui: HomeUi, act: HomeActions, modifier: Modifier, hint: Boolean = true) {
    Box(modifier.clickable(onClick = act.onVehicle), contentAlignment = Alignment.Center) {
        CarImage(ui.carImageVersion, Modifier.fillMaxSize(), hint = hint)
    }
}

@Composable
fun carName(ui: HomeUi): String = carDisplayName(ui.brand, ui.car, LocalArabic.current)

// ============================================================================ apps

/** The user's shortcuts (same dock as every interface; long-press to change one). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppStrip(
    ui: HomeUi, act: HomeActions, modifier: Modifier, icon: Dp = 56.dp, label: TextUnit = 15.sp,
    labels: Boolean = true, tile: Boolean = false, spacing: Dp = 8.dp,
) {
    val k = LocalSkin.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
        ui.dock.forEach { s -> AppItem(s, act, Modifier.weight(1f).fillMaxHeight(), icon, label, labels, tile) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(s: DockSlot, act: HomeActions, modifier: Modifier, icon: Dp, label: TextUnit, labels: Boolean = true, tile: Boolean = false) {
    val k = LocalSkin.current
    val available = s.app != null || s.builtIn
    Column(
        modifier.clip(k.shape(16.dp)).then(if (tile) Modifier.background(k.surface).border(1.dp, k.border, k.shape(16.dp)) else Modifier)
            .combinedClickable(onClick = { act.onDock(s) }, onLongClick = { act.onDockLong(s) }),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        DockIcon(s, icon)
        if (labels) {
            Spacer(Modifier.height(6.dp))
            UiDirection {
                Text(if (available) s.title else tr("Install ", "تثبيت ") + s.title, style = st(label, FontWeight.Medium, if (available) k.text else k.dim),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}

// ============================================================================ levels

/** Volume or brightness with − / + (brightness uses NovaDrive's screen dimmer). */
@Composable
fun LevelControl(icon: ImageVector, label: String, read: () -> Float, write: (Float) -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    val k = LocalSkin.current
    var v by remember { mutableFloatStateOf(read().coerceIn(0f, 1f)) }
    fun step(d: Float) { v = (v + d).coerceIn(0f, 1f); write(v) }
    FixedLtr {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            SRound(Icons.Rounded.Remove, "−", if (compact) 44.dp else 52.dp) { step(-0.1f) }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, label, tint = k.dim, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${(v * 100).roundToInt()}%", style = st(if (compact) 18.sp else 22.sp, FontWeight.SemiBold), maxLines = 1)
                }
                Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(k.text.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(v).background(k.accent))
                }
            }
            SRound(Icons.Rounded.Add, "+", if (compact) 44.dp else 52.dp) { step(0.1f) }
        }
    }
}

@Composable
fun VolumeControl(act: HomeActions, modifier: Modifier = Modifier, compact: Boolean = false) =
    LevelControl(Icons.AutoMirrored.Rounded.VolumeUp, tr("Volume", "الصوت"), act.volume, act.onVolume, modifier, compact)

@Composable
fun BrightnessControl(act: HomeActions, modifier: Modifier = Modifier, compact: Boolean = false) =
    LevelControl(Icons.Rounded.LightMode, tr("Brightness", "السطوع"), act.brightness, act.onBrightness, modifier, compact)

/** Quick row used by several interfaces: voice + quick controls. */
@Composable
fun RowScope.QuickButtons(act: HomeActions, size: Dp = 48.dp) {
    SRound(Icons.Rounded.AutoAwesome, "Nova AI", size, filled = true, onClick = act.onVoice)
    Spacer(Modifier.width(8.dp))
    SRound(Icons.Rounded.Tune, tr("Quick controls", "التحكم السريع"), size, onClick = act.onQuick)
}

// ============================================================================ big shortcut

/**
 * The interface's big shortcut: the phone by default, or any installed app the user picked
 * (long-press or Settings). Independent of the dock's Phone button.
 */
@Composable
fun BigShortcut(ui: HomeUi, act: HomeActions, modifier: Modifier, icon: Dp = 54.dp, label: TextUnit = 30.sp, phoneFill: Color = Color(0xFF16A34A)) {
    val k = LocalSkin.current
    val app = ui.bigApp
    val size = if (ui.styleOptions.bigSize == 1) icon * 1.35f else icon
    when {
        app != null -> SCard(modifier, onClick = act.onBig, onLongClick = act.onBigLong) {
            Row(Modifier.align(Alignment.Center).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(app.icon, null, Modifier.size(size))
                Spacer(Modifier.width(14.dp))
                UiDirection { Text(app.label, style = st(label, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        ui.bigAppMissing -> SCard(modifier, onClick = act.onBigLong, onLongClick = act.onBigLong) {
            UiDirection {
                Text(tr("The chosen app isn't installed — tap to choose another", "التطبيق المختار غير مثبت — اضغط لاختيار آخر"),
                    style = st(label * 0.6f, FontWeight.SemiBold, k.dim), textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(12.dp))
            }
        }
        else -> SCard(modifier, onClick = act.onBig, onLongClick = act.onBigLong, fill = phoneFill) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Phone, null, tint = Color.White, modifier = Modifier.size(size))
                Spacer(Modifier.width(14.dp))
                UiDirection { Text(tr("Phone", "الهاتف"), style = st(label, FontWeight.Bold, Color.White)) }
            }
        }
    }
}
