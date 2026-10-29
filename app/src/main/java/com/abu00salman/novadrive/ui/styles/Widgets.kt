package com.abu00salman.novadrive.ui.styles

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Adjust
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.core.SunCalc
import com.abu00salman.novadrive.core.WidgetGrid
import com.abu00salman.novadrive.core.WidgetItem
import com.abu00salman.novadrive.core.WidgetKind
import com.abu00salman.novadrive.core.WidgetSize
import com.abu00salman.novadrive.data.Link
import com.abu00salman.novadrive.data.VehicleData
import com.abu00salman.novadrive.ui.components.FixedLtr
import com.abu00salman.novadrive.ui.components.UiDirection
import com.abu00salman.novadrive.ui.home.HomeActions
import com.abu00salman.novadrive.ui.home.HomeUi
import com.abu00salman.novadrive.ui.home.compassLabel
import com.abu00salman.novadrive.ui.home.uiLocale
import com.abu00salman.novadrive.ui.shell.GpsState
import com.abu00salman.novadrive.ui.shell.rememberMinuteClock
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.tr
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================================ Widgets interface

/**
 * «واجهة الويدجت»: a board of live gauges and readings, arranged like phone widgets. Long-press any
 * widget (or tap Edit) to add, remove, resize or reorder; the board is saved on the unit.
 */
@Composable
internal fun WidgetsLayout(ui: HomeUi, act: HomeActions, startEditing: Boolean = false) {
    val k = LocalSkin.current
    var editing by rememberSaveable { mutableStateOf(startEditing) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var board by remember(ui.widgets) { mutableStateOf(ui.widgets) }
    fun change(n: List<WidgetItem>) { board = n; act.onWidgets(n) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TopRow(ui, act, Modifier.fillMaxWidth().height(56.dp), timeSize = 30f) {
                if (editing) {
                    EditPill(Icons.Rounded.Add, tr("Add widget", "إضافة ويدجت"), k.accent, filled = true) { adding = true }
                    Spacer(Modifier.width(8.dp))
                    EditPill(null, tr("Done", "تم"), k.accent, filled = false) { editing = false }
                } else EditPill(Icons.Rounded.Edit, tr("Edit", "تعديل"), k.accent, filled = false) { editing = true }
                Spacer(Modifier.width(16.dp))
            }
            WidgetBoard(board, editing, ui, act, Modifier.weight(1f).fillMaxWidth(),
                onEdit = { editing = true },
                onRemove = { change(WidgetGrid.remove(board, it)) },
                onResize = { change(WidgetGrid.resize(board, it)) },
                onMove = { id, by -> change(WidgetGrid.move(board, id, by)) },
            )
            AppStrip(ui, act, Modifier.fillMaxWidth().height(88.dp), icon = 50.dp, label = 13.sp, tile = true)
        }
        if (adding) AddWidgetSheet(
            onAdd = { change(WidgetGrid.add(board, it)); adding = false },
            onReset = { change(WidgetGrid.DEFAULT); adding = false },
            onClose = { adding = false },
        )
    }
}

@Composable
private fun EditPill(icon: ImageVector?, label: String, color: Color, filled: Boolean, onClick: () -> Unit) {
    val k = LocalSkin.current
    Row(
        Modifier.height(44.dp).clip(CircleShape).background(if (filled) color else k.surface).border(1.dp, if (filled) color else k.border, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (filled) Color.White else color, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)) }
        UiDirection { Text(label, style = st(15.sp, FontWeight.SemiBold, if (filled) Color.White else color), maxLines = 1) }
    }
}

/** The grid: 4 columns, 3 rows on screen, scrolls for more. Moves and resizes animate. */
@Composable
private fun WidgetBoard(
    board: List<WidgetItem>, editing: Boolean, ui: HomeUi, act: HomeActions, modifier: Modifier,
    onEdit: () -> Unit, onRemove: (Int) -> Unit, onResize: (Int) -> Unit, onMove: (Int, Int) -> Unit,
) {
    val (placements, rows) = remember(board) { WidgetGrid.pack(board) }
    BoxWithConstraints(modifier) {
        val gap = 12.dp
        val cols = WidgetGrid.COLUMNS
        val cellW = (maxWidth - gap * (cols - 1)) / cols
        val cellH = (maxHeight - gap * 2) / 3
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(cellH * rows.coerceAtLeast(1) + gap * (rows - 1).coerceAtLeast(0))) {
                placements.forEachIndexed { index, pl ->
                    key(pl.item.id) {
                        val w = pl.item.size.w; val h = pl.item.size.h
                        val x by animateDpAsState((cellW + gap) * pl.col, spring(stiffness = 500f), label = "x")
                        val y by animateDpAsState((cellH + gap) * pl.row, spring(stiffness = 500f), label = "y")
                        val ww by animateDpAsState(cellW * w + gap * (w - 1), spring(stiffness = 500f), label = "w")
                        val hh by animateDpAsState(cellH * h + gap * (h - 1), spring(stiffness = 500f), label = "h")
                        WidgetFrame(
                            pl.item, editing, Modifier.offset(x, y).size(ww, hh),
                            first = index == 0, last = index == placements.lastIndex,
                            onEdit = onEdit, onRemove = { onRemove(pl.item.id) }, onResize = { onResize(pl.item.id) },
                            onMove = { onMove(pl.item.id, it) },
                        ) { WidgetContent(pl.item, ui, act) }
                    }
                }
            }
        }
    }
}

/** Widgets that draw their own card. */
private val SELF_CARD = setOf(WidgetKind.TILT, WidgetKind.MEDIA, WidgetKind.MAP)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetFrame(
    item: WidgetItem, editing: Boolean, modifier: Modifier, first: Boolean, last: Boolean,
    onEdit: () -> Unit, onRemove: () -> Unit, onResize: () -> Unit, onMove: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    val k = LocalSkin.current
    val wobble = rememberInfiniteTransition(label = "wobble")
    val angle by wobble.animateFloat(-0.9f, 0.9f, infiniteRepeatable(tween(130 + item.id % 3 * 25), RepeatMode.Reverse), label = "angle")
    val scale by animateFloatAsState(if (editing) 0.96f else 1f, label = "scale")
    Box(modifier.graphicsLayer { rotationZ = if (editing) angle else 0f; scaleX = scale; scaleY = scale }) {
        if (item.kind in SELF_CARD) Box(Modifier.fillMaxSize()) { content() }
        else SCard(Modifier.fillMaxSize(), onLongClick = onEdit) { content() }
        if (editing) {
            // Edit layer: blocks the widget's own buttons while arranging.
            Box(Modifier.fillMaxSize().clip(k.shape).background(Color.Black.copy(alpha = 0.12f))
                .clickable(remember { MutableInteractionSource() }, null) { })
            Handle(Icons.Rounded.Close, tr("Remove", "إزالة"), Color(0xFFEF4444), Modifier.align(Alignment.TopStart), onRemove)
            if (item.kind.sizes.size > 1) Handle(Icons.Rounded.OpenInFull, tr("Size", "الحجم"), k.accent, Modifier.align(Alignment.TopEnd), onResize)
            FixedLtr {
                Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // ExpandLess turned sideways: a plain arrow that never mirrors (the board flows left to right).
                    if (!first) Handle(Icons.Rounded.ExpandLess, tr("Earlier", "للأمام"), k.surfaceHi, Modifier, { onMove(-1) }, iconTint = k.text, turn = -90f)
                    if (!last) Handle(Icons.Rounded.ExpandLess, tr("Later", "للخلف"), k.surfaceHi, Modifier, { onMove(1) }, iconTint = k.text, turn = 90f)
                }
            }
            UiDirection {
                Text(if (LocalArabic.current) item.kind.ar else item.kind.en, style = st(13.sp, FontWeight.SemiBold, k.text),
                    modifier = Modifier.align(Alignment.Center).clip(CircleShape).background(k.surface.copy(alpha = 0.9f)).padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun Handle(icon: ImageVector, description: String, bg: Color, modifier: Modifier, onClick: () -> Unit, iconTint: Color = Color.White, turn: Float = 0f) {
    Box(
        modifier.padding(6.dp).size(40.dp).clip(CircleShape).background(bg).border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = iconTint, modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = turn }) }
}

@Composable
private fun WidgetContent(item: WidgetItem, ui: HomeUi, act: HomeActions) {
    val s = item.size
    when (item.kind) {
        WidgetKind.SPEED -> SpeedWidget(ui, act, s)
        WidgetKind.CLOCK -> ClockWidget(ui, s)
        WidgetKind.CALENDAR -> CalendarWidget(s)
        WidgetKind.WEATHER -> WeatherWidget(ui, act)
        WidgetKind.COMPASS -> CompassWidget(ui)
        WidgetKind.ALTITUDE -> Reading(Icons.Rounded.Terrain, tr("Altitude", "الارتفاع"),
            ui.altitudeM?.let { "${it.roundToInt()}" }, tr("m · GPS", "م · GPS"))
        WidgetKind.TRIP -> TripWidget(ui, act, s)
        WidgetKind.GFORCE -> GForceWidget()
        WidgetKind.TILT -> Inclinometer(Modifier.fillMaxSize())
        WidgetKind.MAP -> SkinMap(ui, act, Modifier.fillMaxSize(), focusY = 0.6f, guidance = s != WidgetSize.WIDE)
        WidgetKind.NAV -> NavWidget(ui, act)
        WidgetKind.MEDIA -> SkinMedia(ui, act, Modifier.fillMaxSize(), art = if (s == WidgetSize.LARGE) 120.dp else 80.dp, vertical = s == WidgetSize.LARGE)
        WidgetKind.OBD -> ObdWidget(ui, act)
        WidgetKind.COORDS -> Reading(Icons.Rounded.MyLocation, tr("Coordinates", "الإحداثيات"),
            if (ui.lat != null && ui.lon != null) String.format(Locale.US, "%.5f, %.5f", ui.lat, ui.lon) else null, "GPS", valueSize = 22f)
        WidgetKind.SUN -> SunWidget(ui)
        WidgetKind.STATUS -> StatusWidget(ui, act)
        WidgetKind.VOLUME -> Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { VolumeControl(act, Modifier.fillMaxWidth()) }
        WidgetKind.BRIGHTNESS -> Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { BrightnessControl(act, Modifier.fillMaxWidth()) }
    }
}

// ============================================================================ widgets

/** Generic reading: icon + title, big value (or "--" when the source has no data), unit. */
@Composable
private fun Reading(icon: ImageVector, title: String, value: String?, unit: String, valueSize: Float = 40f) {
    val k = LocalSkin.current
    UiDirection {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = k.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = st(15.sp, FontWeight.Medium, k.dim), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(value ?: "--", style = st(valueSize.sp, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(unit, style = st(13.sp, color = k.dim), maxLines = 1)
        }
    }
}

/** GPS speed on a 0–220 km/h arc. */
@Composable
private fun SpeedWidget(ui: HomeUi, act: HomeActions, widget: WidgetSize) {
    val k = LocalSkin.current
    val speed = if (ui.hasLocation) ui.speedKmh ?: 0f else 0f
    val anim by animateFloatAsState(speed, tween(600), label = "speed")
    if (widget == WidgetSize.SMALL || widget == WidgetSize.WIDE) {
        Box(Modifier.fillMaxSize().clickable(onClick = act.onVehicle), contentAlignment = Alignment.Center) { SpeedText(ui, 52.sp, act) }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize().clickable(onClick = act.onVehicle).padding(14.dp), contentAlignment = Alignment.Center) {
        val d = minOf(maxWidth, maxHeight)
        Canvas(Modifier.size(d)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2 + 4.dp.toPx()
            val arc = Size(size.width - inset * 2, size.height - inset * 2)
            val tl = Offset(inset, inset)
            drawArc(k.border, 150f, 240f, false, tl, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            val sweep = 240f * (anim / 220f).coerceIn(0f, 1f)
            if (sweep > 0.5f) drawArc(Brush.sweepGradient(listOf(k.accent, k.accent2, k.accent)), 150f, sweep, false, tl, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            val c = Offset(size.width / 2, size.height / 2)
            val r = arc.width / 2 - stroke * 1.2f
            for (v in 0..220 step 20) {
                val a = Math.toRadians(150.0 + 240.0 * v / 220.0)
                val len = if (v % 40 == 0) 12.dp.toPx() else 6.dp.toPx()
                drawLine(k.dim, Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat()),
                    Offset(c.x + ((r - len) * cos(a)).toFloat(), c.y + ((r - len) * sin(a)).toFloat()), strokeWidth = 2.dp.toPx())
            }
        }
        SpeedText(ui, (d.value * 0.24f).sp, act)
        ui.trip?.let { t ->
            Text(tr("Top ", "الأعلى ") + "${t.maxKmh.roundToInt()}", style = st(13.sp, color = k.dim), modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun ClockWidget(ui: HomeUi, size: WidgetSize) {
    val k = LocalSkin.current
    val clock = rememberClock(ui.use24h)
    if (size == WidgetSize.LARGE) {
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000L - now % 1000L) } }
        Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnalogClock(now, Modifier.weight(1f).fillMaxWidth())
            UiDirection { Text(clock.dateShort, style = st(16.sp, color = k.dim), maxLines = 1) }
        }
        return
    }
    UiDirection {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(clock.time, style = st(if (size == WidgetSize.WIDE) 56.sp else 44.sp, FontWeight.Bold), maxLines = 1)
                if (clock.ampm.isNotEmpty()) Text(" " + clock.ampm, style = st(18.sp, color = k.dim), modifier = Modifier.padding(bottom = 8.dp))
            }
            Text(clock.dateShort, style = st(15.sp, color = k.dim), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AnalogClock(now: Long, modifier: Modifier) {
    val k = LocalSkin.current
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val sec = cal.get(Calendar.SECOND); val min = cal.get(Calendar.MINUTE) + sec / 60f; val hr = cal.get(Calendar.HOUR) + min / 60f
    Canvas(modifier) {
        val r = size.minDimension / 2 * 0.92f
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(k.surfaceHi, r, c)
        drawCircle(k.border, r, c, style = Stroke(2.dp.toPx()))
        for (i in 0 until 60) rotate(i * 6f, c) {
            val len = if (i % 5 == 0) r * 0.12f else r * 0.05f
            drawLine(if (i % 5 == 0) k.text else k.dim, Offset(c.x, c.y - r * 0.95f), Offset(c.x, c.y - r * 0.95f + len), strokeWidth = (if (i % 5 == 0) 3 else 1).dp.toPx())
        }
        rotate(hr * 30f, c) { drawLine(k.text, c, Offset(c.x, c.y - r * 0.5f), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round) }
        rotate(min * 6f, c) { drawLine(k.text, c, Offset(c.x, c.y - r * 0.75f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round) }
        rotate(sec * 6f, c) { drawLine(k.accent, Offset(c.x, c.y + r * 0.12f), Offset(c.x, c.y - r * 0.85f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round) }
        drawCircle(k.accent, 5.dp.toPx(), c)
    }
}

/** Gregorian date and the Hijri (Umm al-Qura) date. */
@Composable
private fun CalendarWidget(size: WidgetSize) {
    val k = LocalSkin.current
    val ar = LocalArabic.current
    val now = rememberMinuteClock()
    val loc = uiLocale(ar)
    val day = remember(now, ar) { SimpleDateFormat("d", loc).format(Date(now)) }
    val weekday = remember(now, ar) { SimpleDateFormat("EEEE", loc).format(Date(now)) }
    val month = remember(now, ar) { SimpleDateFormat("MMMM yyyy", loc).format(Date(now)) }
    val hijri = remember(now, ar) { hijriDate(now, ar) }
    UiDirection {
        Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(weekday, style = st(16.sp, FontWeight.SemiBold, k.accent), maxLines = 1)
                Text(day, style = st(46.sp, FontWeight.Bold), maxLines = 1)
                Text(month, style = st(14.sp, color = k.dim), maxLines = 1)
            }
            if (size == WidgetSize.WIDE && hijri != null) Column(Modifier.weight(1f)) {
                Text(tr("Hijri", "هجري"), style = st(14.sp, color = k.dim))
                Text(hijri, style = st(19.sp, FontWeight.SemiBold), maxLines = 2)
            }
        }
        if (size == WidgetSize.SMALL && hijri != null) Box(Modifier.fillMaxSize().padding(10.dp)) {
            Text(hijri, style = st(11.sp, color = k.dim), maxLines = 1, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

private fun hijriDate(now: Long, ar: Boolean): String? = runCatching {
    val cal = android.icu.util.IslamicCalendar()
    cal.calculationType = android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
    cal.timeInMillis = now
    android.icu.text.DateFormat.getDateInstance(cal, android.icu.text.DateFormat.LONG, android.icu.util.ULocale(if (ar) "ar" else "en"))
        .format(cal.time).replace(" هـ", "").trim()
}.getOrNull()

@Composable
private fun WeatherWidget(ui: HomeUi, act: HomeActions) {
    Column(Modifier.fillMaxSize().clickable(onClick = act.onQuick).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
        UiDirection { WeatherLine(ui, 36.sp, 40.dp) }
        UiDirection { HiLo(ui, 13.sp) }
    }
}

@Composable
private fun CompassWidget(ui: HomeUi) {
    val k = LocalSkin.current
    val ar = LocalArabic.current
    Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Compass(ui.bearing, Modifier.weight(1f).fillMaxWidth())
        UiDirection {
            Text(ui.bearing?.let { "${compassLabel(it, ar)} · ${it.roundToInt()}°" } ?: tr("Direction appears while moving", "يظهر الاتجاه أثناء الحركة"),
                style = st(14.sp, FontWeight.Medium, if (ui.bearing != null) k.text else k.dim), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Trip computer from GPS (reset with the button). */
@Composable
private fun TripWidget(ui: HomeUi, act: HomeActions, size: WidgetSize) {
    val k = LocalSkin.current
    val t = ui.trip
    val dist = t?.let { String.format(Locale.US, "%.1f", it.distanceKm) } ?: "--"
    val time = t?.let { String.format(Locale.US, "%d:%02d", it.movingSeconds / 3600, it.movingSeconds / 60 % 60) } ?: "--"
    val avg = t?.let { "${it.avgKmh.roundToInt()}" } ?: "--"
    val top = t?.let { "${it.maxKmh.roundToInt()}" } ?: "--"
    Box(Modifier.fillMaxSize()) {
        UiDirection {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Route, null, tint = k.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Trip · GPS", "الرحلة · GPS"), style = st(15.sp, FontWeight.Medium, k.dim))
                }
                if (size == WidgetSize.SMALL) {
                    Text("$dist " + tr("km", "كم"), style = st(34.sp, FontWeight.Bold), maxLines = 1)
                    Text("$time · $avg " + tr("km/h", "كم/س"), style = st(13.sp, color = k.dim), maxLines = 1)
                } else {
                    val cells = listOf(
                        tr("Distance", "المسافة") to "$dist " + tr("km", "كم"), tr("Moving", "زمن الحركة") to time,
                        tr("Average", "المتوسط") to "$avg " + tr("km/h", "كم/س"), tr("Top", "الأعلى") to "$top " + tr("km/h", "كم/س"),
                    )
                    (if (size == WidgetSize.LARGE) cells.chunked(2) else listOf(cells)).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { (label, value) ->
                                Column(Modifier.weight(1f)) {
                                    Text(value, style = st(if (size == WidgetSize.LARGE) 30.sp else 21.sp, FontWeight.Bold), maxLines = 1)
                                    Text(label, style = st(12.sp, color = k.dim), maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { SRound(Icons.Rounded.Refresh, tr("Reset trip", "تصفير الرحلة"), 36.dp, onClick = act.onTripReset) }
    }
}

/** Total acceleration from the unit's linear-acceleration sensor, with the peak since shown. */
@Composable
private fun GForceWidget() {
    val k = LocalSkin.current
    val ctx = LocalContext.current
    var g by remember { mutableStateOf<Float?>(null) }
    var peak by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        var f = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val a = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / SensorManager.GRAVITY_EARTH
                f += 0.2f * (a - f)
                g = f; if (f > peak) peak = f
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }
    Box(Modifier.fillMaxSize().clickable { peak = 0f }) {
        Reading(Icons.Rounded.Adjust, tr("G-force", "قوة التسارع"), g?.let { String.format(Locale.US, "%.2f g", it) },
            if (g == null) tr("no sensor on this unit", "لا يوجد حساس في الجهاز")
            else tr("peak ", "الأعلى ") + String.format(Locale.US, "%.2f g", peak) + tr(" · tap to reset", " · اضغط للتصفير"),
            valueSize = 34f)
    }
}

@Composable
private fun NavWidget(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    val nav = ui.nav
    UiDirection {
        Row(Modifier.fillMaxSize().clickable(onClick = if (nav != null) act.onNavCard else act.onNavDetails).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = nav?.maneuver
            if (icon != null) androidx.compose.foundation.Image(icon, null, Modifier.size(52.dp))
            else Icon(if (nav != null) Icons.Rounded.TurnRight else Icons.Rounded.Map, null, tint = k.accent, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(nav?.primary ?: tr("No active route", "لا يوجد توجيه نشط"), style = st(if (nav != null) 26.sp else 18.sp, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(nav?.secondary ?: tr("Tap to search a destination", "اضغط للبحث عن وجهة"), style = st(14.sp, color = k.dim), maxLines = 1, overflow = TextOverflow.Ellipsis)
                nav?.eta?.let { Text(it, style = st(13.sp, color = k.dim), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/** Live OBD-II values only when the adapter is connected; otherwise says so. */
@Composable
private fun ObdWidget(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    val v = ui.vehicle
    val live = ui.obdEnabled && v.state == VehicleData.State.LIVE
    UiDirection {
        Column(Modifier.fillMaxSize().clickable(onClick = act.onVehicle).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DirectionsCar, null, tint = k.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("OBD-II · " + when {
                    !ui.obdEnabled -> tr("off — enable in Settings › Vehicle", "مطفأ — فعّله من الإعدادات ← المركبة")
                    live -> tr("live", "مباشر")
                    v.state == VehicleData.State.CONNECTING -> tr("connecting…", "جارٍ الاتصال…")
                    else -> tr("not connected", "غير متصل")
                }, style = st(14.sp, FontWeight.Medium, k.dim), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth()) {
                listOf(
                    tr("RPM", "الدوران") to (if (live) v.rpm?.toString() else null),
                    tr("Fuel", "الوقود") to (if (live) v.fuelPct?.let { "$it%" } else null),
                    tr("Coolant", "حرارة المحرك") to (if (live) v.coolantC?.let { "$it°" } else null),
                    tr("Battery", "البطارية") to (if (live) v.voltage?.let { String.format(Locale.US, "%.1fV", it) } else null),
                ).forEach { (label, value) ->
                    Column(Modifier.weight(1f)) {
                        Text(value ?: "--", style = st(22.sp, FontWeight.Bold), maxLines = 1)
                        Text(label, style = st(12.sp, color = k.dim), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun SunWidget(ui: HomeUi) {
    val k = LocalSkin.current
    val ar = LocalArabic.current
    val day = rememberMinuteClock() / 3_600_000L
    val times = remember(ui.lat?.let { (it * 10).roundToInt() }, ui.lon?.let { (it * 10).roundToInt() }, day) {
        val lat = ui.lat; val lon = ui.lon
        if (lat == null || lon == null) null else SunCalc.riseSet(lat, lon, System.currentTimeMillis())
    }
    val fmt = remember(ar, ui.use24h) { SimpleDateFormat(if (ui.use24h) "HH:mm" else "h:mm a", uiLocale(ar)) }
    UiDirection {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WbTwilight, null, tint = Color(0xFFFFB547), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("Sun", "الشمس"), style = st(15.sp, FontWeight.Medium, k.dim))
            }
            if (times == null) Text(tr("Waiting for location", "بانتظار الموقع"), style = st(15.sp, color = k.dim))
            else {
                Text(tr("Sunrise ", "الشروق ") + (times.first?.let { fmt.format(Date(it)) } ?: "--"), style = st(19.sp, FontWeight.SemiBold), maxLines = 1)
                Text(tr("Sunset ", "الغروب ") + (times.second?.let { fmt.format(Date(it)) } ?: "--"), style = st(19.sp, FontWeight.SemiBold), maxLines = 1)
            }
        }
    }
}

@Composable
private fun StatusWidget(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    UiDirection {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Wifi, null, tint = k.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("Connections", "الاتصالات"), style = st(15.sp, FontWeight.Medium, k.dim))
            }
            StatusIcons(ui, act, 34.dp)
            Text(listOf(
                "Wi-Fi " + when (ui.net.wifi) { Link.CONNECTED -> "✓"; Link.ON -> "…"; Link.OFF -> "✗" },
                "BT " + when (ui.bt) { Link.CONNECTED -> "✓"; Link.ON -> "…"; Link.OFF -> "✗" },
                "GPS " + when (ui.gps) { GpsState.FIX -> "✓"; GpsState.SEARCHING -> "…"; GpsState.OFF -> "✗" },
            ).joinToString("   "), style = st(13.sp, color = k.dim), maxLines = 1)
        }
    }
}

// ============================================================================ add sheet

private fun kindIcon(kind: WidgetKind): ImageVector = when (kind) {
    WidgetKind.SPEED -> Icons.Rounded.Speed
    WidgetKind.CLOCK -> Icons.Rounded.Schedule
    WidgetKind.CALENDAR -> Icons.Rounded.CalendarMonth
    WidgetKind.WEATHER -> Icons.Rounded.WbSunny
    WidgetKind.COMPASS -> Icons.Rounded.Explore
    WidgetKind.ALTITUDE -> Icons.Rounded.Terrain
    WidgetKind.TRIP -> Icons.Rounded.Route
    WidgetKind.GFORCE -> Icons.Rounded.Adjust
    WidgetKind.TILT -> Icons.Rounded.Straighten
    WidgetKind.MAP -> Icons.Rounded.Map
    WidgetKind.NAV -> Icons.Rounded.TurnRight
    WidgetKind.MEDIA -> Icons.Rounded.MusicNote
    WidgetKind.OBD -> Icons.Rounded.DirectionsCar
    WidgetKind.COORDS -> Icons.Rounded.MyLocation
    WidgetKind.SUN -> Icons.Rounded.WbTwilight
    WidgetKind.STATUS -> Icons.Rounded.Wifi
    WidgetKind.VOLUME -> Icons.AutoMirrored.Rounded.VolumeUp
    WidgetKind.BRIGHTNESS -> Icons.Rounded.LightMode
}

@Composable
private fun AddWidgetSheet(onAdd: (WidgetKind) -> Unit, onReset: () -> Unit, onClose: () -> Unit) {
    val k = LocalSkin.current
    val ar = LocalArabic.current
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.86f).clip(k.shape(28.dp)).background(k.bg.last()).border(1.dp, k.border, k.shape(28.dp))
                .clickable(remember { MutableInteractionSource() }, null) { }.padding(20.dp),
        ) {
            UiDirection {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Add a widget", "إضافة ويدجت"), style = st(24.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                    EditPill(Icons.Rounded.Restore, tr("Default board", "الترتيب الافتراضي"), k.accent, filled = false, onClick = onReset)
                    Spacer(Modifier.width(10.dp))
                    SRound(Icons.Rounded.Close, tr("Close", "إغلاق"), 44.dp, onClick = onClose)
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(WidgetKind.entries.size) { i ->
                    val kind = WidgetKind.entries[i]
                    Column(
                        Modifier.height(116.dp).clip(k.shape(18.dp)).background(k.surface).border(1.dp, k.border, k.shape(18.dp))
                            .clickable { onAdd(kind) }.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(kindIcon(kind), null, tint = k.accent, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(if (ar) kind.ar else kind.en, style = st(15.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        Text(kind.sizes.joinToString(" · ") { "${it.w}×${it.h}" }, style = st(11.sp, color = k.dim), maxLines = 1)
                    }
                }
            }
        }
    }
}
