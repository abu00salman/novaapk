package com.abu00salman.novadrive.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RoundaboutLeft
import androidx.compose.material.icons.rounded.RoundaboutRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Straight
import androidx.compose.material.icons.rounded.TurnLeft
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.TurnSharpLeft
import androidx.compose.material.icons.rounded.TurnSharpRight
import androidx.compose.material.icons.rounded.TurnSlightLeft
import androidx.compose.material.icons.rounded.TurnSlightRight
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.data.NavInfo
import com.abu00salman.novadrive.data.MapTiles
import com.abu00salman.novadrive.data.nav.Geo
import com.abu00salman.novadrive.data.nav.Instructions
import com.abu00salman.novadrive.data.nav.LatLon
import com.abu00salman.novadrive.data.nav.NavState
import com.abu00salman.novadrive.data.nav.Place
import com.abu00salman.novadrive.data.nav.RouteService
import com.abu00salman.novadrive.data.nav.RouteStep
import com.abu00salman.novadrive.ui.home.MapMarker
import com.abu00salman.novadrive.ui.home.TileMap
import com.abu00salman.novadrive.ui.home.uiLocale
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

/** Same dark recolouring of OpenStreetMap tiles as the BMW interface, for night mode. */
val NightMapTint = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    0.488f, -1.216f, -0.122f, 0f, 216.8f,
    -0.383f, -0.387f, -0.130f, 0f, 233.5f,
    -0.426f, -1.430f, 0.856f, 0f, 273.0f,
    0f, 0f, 0f, 1f, 0f,
)))

/**
 * Full-screen navigation inside NovaDrive: a live street map, destination search, the route and
 * turn-by-turn guidance with voice. An external navigation app stays one tap away.
 */
@Composable
fun NavigationScreen(
    c: AppContainer,
    settings: NovaSettings,
    /** Typing is blocked above walking speed (voice «خذني إلى …» still works). */
    lockTyping: Boolean,
    externalNav: NavInfo?,
    navAppLabel: String?,
    onExternal: (LatLon?) -> Unit,
    onOpenGuidance: () -> Unit,
    onSetPlaces: () -> Unit,
    onEnableLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val fix by c.fix.collectAsStateWithLifecycle()
    val nav by c.navEngine.state.collectAsStateWithLifecycle()
    val voice by c.navEngine.voice.collectAsStateWithLifecycle()
    var zoom by rememberSaveable { mutableFloatStateOf(16f) }
    var view by remember { mutableStateOf<LatLon?>(null) }
    var headingUp by rememberSaveable { mutableStateOf(true) }
    val shape = RoundedCornerShape(24.dp)

    Box(modifier.fillMaxSize().clip(shape).border(1.dp, p.stroke, shape)) {
        TileMap(
            fix?.lat, fix?.lon, fix?.bearing, if (p.dark) MapTiles.Style.DARK else MapTiles.Style.LIGHT, MapMarker.DOT,
            headingUp && nav != null, zoom, { zoom = it }, onTap = {},
            modifier = Modifier.fillMaxSize(), tint = if (p.dark) NightMapTint else null,
            background = if (p.dark) Color(0xFF0D1528) else Color(0xFFE6E8EB),
            focusY = if (nav != null) 0.68f else 0.5f,
            route = nav?.route?.points, routeProgress = nav?.progressIndex ?: 0, routeColor = p.accent,
            destination = nav?.destination?.point, markerColor = p.accent,
            view = view, onViewChange = { view = it },
        )
        if (fix == null) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.GpsNotFixed, null, tint = p.textDim, modifier = Modifier.size(44.dp))
            Text(tr("Waiting for GPS…", "بانتظار تحديد الموقع…"), style = NovaType.title, color = p.text)
            Text(tr("Enable location ›", "فعّل الموقع ›"), style = NovaType.label, color = p.accent,
                modifier = Modifier.padding(top = 8.dp).clickable(onClick = onEnableLocation))
        }

        // Left panel: guidance while navigating, otherwise search and saved places.
        Column(Modifier.align(Alignment.TopStart).padding(14.dp).width(440.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val n = nav
            if (n != null) GuidanceCard(n, ar)
            else {
                SearchPanel(c, settings, lockTyping, fix?.let { LatLon(it.lat, it.lon) }, ar, onSetPlaces)
                if (externalNav != null) Panel(onClick = onOpenGuidance) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TurnRight, null, tint = p.accent, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(externalNav.primary, style = NovaType.title.copy(fontSize = 19.sp), color = p.text, maxLines = 1)
                            Text(tr("Guidance from ", "التوجيه من ") + (navAppLabel ?: tr("navigation app", "تطبيق الملاحة")),
                                style = NovaType.small, color = p.textDim, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Map controls.
        Column(Modifier.align(Alignment.TopEnd).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MapButton(Icons.Rounded.Add, tr("Zoom in", "تكبير")) { zoom = (zoom + 1).coerceAtMost(18f) }
            MapButton(Icons.Rounded.Remove, tr("Zoom out", "تصغير")) { zoom = (zoom - 1).coerceAtLeast(4f) }
            MapButton(Icons.Rounded.MyLocation, tr("Follow car", "تتبع السيارة"), active = view == null) {
                if (fix == null) onEnableLocation() else { view = null; zoom = 16f }
            }
            MapButton(Icons.Rounded.Navigation, tr("Heading up", "اتجاه السير"), active = headingUp) { headingUp = !headingUp }
            MapButton(Icons.Rounded.OpenInNew, tr("Open in ", "افتح في ") + (navAppLabel ?: tr("maps app", "تطبيق الخرائط"))) {
                onExternal(nav?.destination?.point)
            }
        }

        // Trip bar.
        nav?.let { n -> TripBar(n, voice, settings.use24h, ar, Modifier.align(Alignment.BottomCenter).padding(14.dp).fillMaxWidth(),
            onVoice = { c.navEngine.setVoice(!voice) }, onStop = { c.navEngine.stop() }, onRetry = { c.navEngine.retry() },
            onOverview = { view = null; zoom = 12f }) }

        Text("© OpenStreetMap · OSRM", style = NovaType.small.copy(fontSize = 10.sp), color = p.textDim,
            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 18.dp, vertical = if (nav != null) 104.dp else 10.dp))
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val p = LocalNova.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(if (p.dark) Color(0xF0101A30) else Color(0xF5FBFCFD)).border(1.dp, p.stroke, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) { content() }
}

@Composable
private fun MapButton(icon: ImageVector, description: String, active: Boolean = false, onClick: () -> Unit) {
    val p = LocalNova.current
    Box(
        Modifier.size(58.dp).clip(CircleShape)
            .background(if (active) p.accent else if (p.dark) Color(0xF0101A30) else Color(0xF5FBFCFD))
            .border(1.dp, p.stroke, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = if (active) Color.White else p.text, modifier = Modifier.size(28.dp)) }
}

@Composable
private fun SearchPanel(c: AppContainer, s: NovaSettings, locked: Boolean, near: LatLon?, ar: Boolean, onSetPlaces: () -> Unit) {
    val p = LocalNova.current
    val scope = rememberCoroutineScope()
    var q by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun search(text: String, go: Boolean = false) {
        if (text.isBlank()) return
        busy = true
        scope.launch {
            val found = parseCoordinates(text)?.let { listOf(Place(text, text, it)) } ?: RouteService.search(text, near, ar)
            busy = false
            if (go && found.isNotEmpty()) c.navEngine.start(found.first()) else results = found
        }
    }
    Panel {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(p.text.copy(alpha = 0.06f)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = p.textDim)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (locked) Text(tr("Typing is available when parked — say «Take me to …»", "الكتابة متاحة عند التوقف — قل لـ«أمرني»: خذني إلى …"),
                        style = NovaType.label, color = p.textDim, maxLines = 2)
                    else {
                        if (q.isEmpty()) Text(tr("Search for a destination", "البحث عن وجهة"), style = NovaType.body, color = p.textDim)
                        BasicTextField(
                            q, { q = it }, singleLine = true, textStyle = NovaType.body.copy(color = p.text),
                            cursorBrush = SolidColor(p.accent), modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { search(q) }),
                        )
                    }
                }
                if (busy) CircularProgressIndicator(Modifier.size(24.dp), color = p.accent, strokeWidth = 3.dp)
                else if (q.isNotEmpty() && !locked) Icon(Icons.Rounded.Close, tr("Clear", "مسح"), tint = p.textDim,
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { q = ""; results = null }.padding(6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(Icons.Rounded.Home, tr("Home", "البيت"), Modifier.weight(1f)) { if (s.homeAddress.isBlank()) onSetPlaces() else search(s.homeAddress, go = true) }
                Chip(Icons.Rounded.Work, tr("Work", "العمل"), Modifier.weight(1f)) { if (s.workAddress.isBlank()) onSetPlaces() else search(s.workAddress, go = true) }
                if (!locked && q.isNotBlank()) Chip(Icons.Rounded.Search, tr("Search", "بحث"), Modifier.weight(1f)) { search(q) }
            }
            val r = results
            if (r != null) {
                Spacer(Modifier.height(8.dp))
                if (r.isEmpty()) Text(tr("No results (check the internet connection)", "لا توجد نتائج (تحقق من الاتصال بالإنترنت)"),
                    style = NovaType.label, color = p.textDim, modifier = Modifier.padding(6.dp))
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(r) { place ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { results = null; c.navEngine.start(place) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Place, null, tint = p.accent, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(place.name, style = NovaType.body, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(place.address, style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            near?.let { Text(Geo.formatDistance(Geo.distance(it, place.point), ar), style = NovaType.small, color = p.textDim) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalNova.current
    Row(
        modifier.height(50.dp).clip(RoundedCornerShape(14.dp)).background(p.accent.copy(alpha = 0.14f)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = p.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = NovaType.label.copy(fontWeight = FontWeight.SemiBold), color = p.text, maxLines = 1)
    }
}

@Composable
private fun GuidanceCard(n: NavState, ar: Boolean) {
    val p = LocalNova.current
    Panel {
        val step = n.nextStep
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(78.dp).clip(RoundedCornerShape(18.dp)).background(p.accent), contentAlignment = Alignment.Center) {
                Icon(if (n.status == NavState.Status.ARRIVED) Icons.Rounded.Flag else maneuverIcon(step), null, tint = Color.White, modifier = Modifier.size(54.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                when (n.status) {
                    NavState.Status.ROUTING -> Text(tr("Calculating route…", "جارٍ حساب المسار…"), style = NovaType.title, color = p.text)
                    NavState.Status.REROUTING -> Text(tr("Recalculating…", "إعادة حساب المسار…"), style = NovaType.title, color = p.text)
                    NavState.Status.NO_ROUTE -> Text(tr("No route found — check the connection", "تعذّر حساب المسار — تحقق من الإنترنت"), style = NovaType.title, color = p.danger)
                    NavState.Status.NO_GPS -> Text(tr("Waiting for GPS…", "بانتظار تحديد الموقع…"), style = NovaType.title, color = p.text)
                    NavState.Status.ARRIVED -> Text(tr("You have arrived", "وصلت إلى وجهتك"), style = NovaType.display.copy(fontSize = 28.sp), color = p.text)
                    NavState.Status.GUIDING -> {
                        Text(Geo.formatDistance(n.toNextM, ar), style = NovaType.display.copy(fontSize = 34.sp), color = p.text, maxLines = 1)
                        step?.let { Text(Instructions.text(it, ar), style = NovaType.title.copy(fontSize = 19.sp), color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    }
                }
                Text(n.destination.name, style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TripBar(
    n: NavState, voice: Boolean, use24h: Boolean, ar: Boolean, modifier: Modifier,
    onVoice: () -> Unit, onStop: () -> Unit, onRetry: () -> Unit, onOverview: () -> Unit,
) {
    val p = LocalNova.current
    val loc = uiLocale(ar)
    Panel(modifier) {
        Row(Modifier.height(84.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (n.route != null) {
                    Text(Geo.formatDuration(n.remainingS, ar) + "  ·  " + Geo.formatDistance(n.remainingM, ar),
                        style = NovaType.title.copy(fontSize = 22.sp), color = p.text, maxLines = 1)
                    Text(tr("Arrival ", "الوصول ") + SimpleDateFormat(if (use24h) "HH:mm" else "h:mm a", loc).format(Date(System.currentTimeMillis() + (n.remainingS * 1000).toLong())),
                        style = NovaType.label, color = p.textDim, maxLines = 1)
                } else Text(n.destination.name, style = NovaType.title, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (n.status == NavState.Status.NO_ROUTE || n.status == NavState.Status.NO_GPS) BarButton(Icons.Rounded.Refresh, tr("Retry", "إعادة"), p.accent, onRetry)
            BarButton(Icons.Rounded.Map, tr("Overview", "نظرة عامة"), p.text.copy(alpha = 0.10f), onOverview, p.text)
            BarButton(if (voice) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff, tr("Voice", "الصوت"), p.text.copy(alpha = 0.10f), onVoice, p.text)
            BarButton(Icons.Rounded.Close, tr("End", "إنهاء"), p.danger, onStop)
        }
    }
}

@Composable
private fun BarButton(icon: ImageVector, label: String, bg: Color, onClick: () -> Unit, fg: Color = Color.White) {
    Row(
        Modifier.padding(start = 8.dp).height(58.dp).clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = NovaType.label.copy(fontWeight = FontWeight.SemiBold), color = fg, maxLines = 1)
    }
}

/** Icon for an OSRM manoeuvre. */
fun maneuverIcon(step: RouteStep?): ImageVector {
    if (step == null) return Icons.Rounded.Navigation
    if (step.type == "arrive") return Icons.Rounded.Flag
    if (step.type == "roundabout" || step.type == "rotary" || step.type == "roundabout turn")
        return if (step.modifier.contains("left")) Icons.Rounded.RoundaboutLeft else Icons.Rounded.RoundaboutRight
    return when (step.modifier) {
        "left" -> Icons.Rounded.TurnLeft
        "right" -> Icons.Rounded.TurnRight
        "slight left" -> Icons.Rounded.TurnSlightLeft
        "slight right" -> Icons.Rounded.TurnSlightRight
        "sharp left", "uturn" -> Icons.Rounded.TurnSharpLeft
        "sharp right" -> Icons.Rounded.TurnSharpRight
        else -> Icons.Rounded.Straight
    }
}

/** "16.88, 42.55" typed or saved as an address goes straight to those coordinates. */
fun parseCoordinates(text: String): LatLon? {
    val m = Regex("""^\s*(-?\d{1,2}\.\d+)\s*[, ]\s*(-?\d{1,3}\.\d+)\s*$""").find(text) ?: return null
    val lat = m.groupValues[1].toDouble(); val lon = m.groupValues[2].toDouble()
    return if (lat in -90.0..90.0 && lon in -180.0..180.0) LatLon(lat, lon) else null
}

/** The in-app trip as the guidance card every interface already shows. */
fun NavState.toNavInfo(arabic: Boolean, use24h: Boolean): NavInfo {
    val loc = uiLocale(arabic)
    val primary = when (status) {
        NavState.Status.GUIDING -> Geo.formatDistance(toNextM, arabic)
        NavState.Status.ARRIVED -> if (arabic) "وصلت" else "Arrived"
        NavState.Status.NO_ROUTE -> if (arabic) "تعذّر حساب المسار" else "No route"
        NavState.Status.NO_GPS -> if (arabic) "بانتظار GPS" else "Waiting for GPS"
        else -> if (arabic) "جارٍ حساب المسار…" else "Calculating route…"
    }
    val secondary = if (status == NavState.Status.GUIDING) nextStep?.let { Instructions.text(it, arabic) } ?: destination.name else destination.name
    val eta = route?.let {
        Geo.formatDuration(remainingS, arabic) + " · " + Geo.formatDistance(remainingM, arabic) + " · " +
            (if (arabic) "الوصول " else "arrive ") +
            SimpleDateFormat(if (use24h) "HH:mm" else "h:mm a", loc).format(Date(System.currentTimeMillis() + (remainingS * 1000).toLong()))
    }
    return NavInfo(IN_APP_NAV, primary, secondary, eta, null, null)
}

/** Package marker for guidance produced by NovaDrive itself (vs. Maps/Waze notifications). */
const val IN_APP_NAV = "com.abu00salman.novadrive"
