package com.abu00salman.novadrive.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.R
import com.abu00salman.novadrive.data.MediaInfo
import com.abu00salman.novadrive.data.NavInfo
import com.abu00salman.novadrive.data.Weather
import com.abu00salman.novadrive.data.WeatherRepository
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.components.RoundButton
import com.abu00salman.novadrive.ui.shell.rememberMinuteClock
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** Arabic text with Latin digits — the convention on Saudi car dashboards. */
fun uiLocale(arabic: Boolean): Locale =
    if (arabic) Locale.forLanguageTag("ar-SA-u-nu-latn") else Locale.US

/** Dark glass used for cards that sit on top of the scenic artwork, in both themes. */
private val CardOnArt = Color(0xC70A1230)
private val OnArt = Color(0xFFF2F6FF)
private val OnArtDim = Color(0xFFB4C1DD)

// ------------------------------------------------------------------------ weather

fun weatherIcon(w: Weather): ImageVector = when (WeatherRepository.kind(w.code)) {
    WeatherRepository.Companion.Kind.CLEAR -> if (w.isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    WeatherRepository.Companion.Kind.PARTLY -> Icons.Rounded.WbCloudy
    WeatherRepository.Companion.Kind.CLOUDY, WeatherRepository.Companion.Kind.FOG -> Icons.Rounded.Cloud
    WeatherRepository.Companion.Kind.RAIN -> Icons.Rounded.Grain
    WeatherRepository.Companion.Kind.SNOW -> Icons.Rounded.AcUnit
    WeatherRepository.Companion.Kind.STORM -> Icons.Rounded.Thunderstorm
}

fun weatherTint(w: Weather): Color = when (WeatherRepository.kind(w.code)) {
    WeatherRepository.Companion.Kind.CLEAR -> if (w.isDay) Color(0xFFFFC53D) else Color(0xFFB9C6FF)
    WeatherRepository.Companion.Kind.PARTLY -> Color(0xFFFFD27A)
    WeatherRepository.Companion.Kind.RAIN, WeatherRepository.Companion.Kind.STORM -> Color(0xFF6FB6FF)
    else -> Color(0xFFC9D4EA)
}

@Composable
fun WeatherCard(weather: Weather?, city: String?, enabled: Boolean, modifier: Modifier = Modifier) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    Glass(modifier) {
        if (!enabled || weather == null) {
            Column(Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Cloud, null, tint = p.textDim, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    if (!enabled) tr("Weather is off", "الطقس متوقف") else tr("Waiting for location…", "بانتظار الموقع…"),
                    style = NovaType.label, color = p.textDim, maxLines = 2
                )
            }
            return@Glass
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(weatherIcon(weather), null, tint = weatherTint(weather), modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Text("${Math.round(weather.tempC)}°", style = NovaType.display.copy(fontSize = 40.sp), color = p.text, maxLines = 1)
            }
            Text(
                listOfNotNull(city, WeatherRepository.label(weather.code, ar)).joinToString(" · "),
                style = NovaType.label, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (weather.maxC != null && weather.minC != null) {
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(p.text.copy(alpha = 0.07f)).padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(tr("High", "العظمى") + " ${Math.round(weather.maxC)}°", style = NovaType.small, color = p.textDim, maxLines = 1)
                    Text(tr("Low", "الصغرى") + " ${Math.round(weather.minC)}°", style = NovaType.small, color = p.textDim, maxLines = 1)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------ clock

@Composable
fun ClockCard(use24h: Boolean, modifier: Modifier = Modifier) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val now = rememberMinuteClock()
    val loc = uiLocale(ar)
    val cal = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
    val time = remember(now, use24h) { SimpleDateFormat(if (use24h) "HH:mm" else "h:mm", loc).format(Date(now)) }
    val date = remember(now, ar) { SimpleDateFormat(if (ar) "EEEE d MMMM yyyy" else "EEE, d MMM yyyy", loc).format(Date(now)) }
    val pm = cal.get(Calendar.AM_PM) == Calendar.PM
    Glass(modifier) {
        Column(Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(time, style = NovaType.display.copy(fontSize = 50.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp), color = p.text, maxLines = 1)
                if (!use24h) {
                    Spacer(Modifier.width(6.dp))
                    Text(if (ar) (if (pm) "م" else "ص") else (if (pm) "PM" else "AM"),
                        style = NovaType.title, color = p.textDim, modifier = Modifier.padding(bottom = 10.dp))
                }
            }
            Text(date, style = NovaType.label, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ------------------------------------------------------------------------ scenic road

/** Image-space geometry of the road painted in scenic_road.webp (900×1000, see tools/). */
private fun roadAt(t: Float): Triple<Float, Float, Float> {
    val cx = 450f + 117f * sin(t * PI.toFloat() * 0.85f) * t.pow(0.9f)
    val y = 1000f - 494f * (1f - (1f - t).pow(2.3f))
    val half = 378f * (1f - t).pow(1.3f) + 1f
    return Triple(cx, y, half)
}

/**
 * Decorative scenery: original mountain-road artwork with a glowing route ribbon and a
 * position marker. It's static (no frame loop), so it costs nothing while driving.
 */
@Composable
fun ScenicBackdrop(modifier: Modifier = Modifier, route: Boolean = true) {
    val p = LocalNova.current
    Box(modifier) {
        Image(painterResource(R.drawable.scenic_road), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (!p.dark) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.06f)))
        Canvas(Modifier.fillMaxSize()) {
            val s = maxOf(size.width / 900f, size.height / 1000f)
            val ox = (size.width - 900f * s) / 2f
            val oy = (size.height - 1000f * s) / 2f
            fun at(t: Float, side: Float, k: Float): Offset {
                val (cx, y, half) = roadAt(t)
                return Offset(ox + (cx + side * half * k) * s, oy + y * s)
            }
            // legibility veils
            drawRect(Brush.verticalGradient(listOf(Color(0x99050A1C), Color.Transparent), endY = size.height * 0.45f))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x88050A1C)), startY = size.height * 0.7f))
            if (!route) return@Canvas
            val steps = 40
            val end = 0.82f
            for ((k, a) in listOf(0.42f to 0.16f, 0.22f to 0.85f)) {
                val path = Path()
                for (i in 0..steps) { val o = at(end * i / steps, -1f, k); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
                for (i in steps downTo 0) { val o = at(end * i / steps, 1f, k); path.lineTo(o.x, o.y) }
                path.close()
                drawPath(path, Brush.verticalGradient(
                    listOf(p.accent.copy(alpha = 0f), p.accent.copy(alpha = a * 0.6f), p.accent.copy(alpha = a)),
                    startY = at(end, 0f, 0f).y, endY = size.height
                ))
            }
            // position marker
            val m = at(0.09f, 0f, 0f)
            val r = size.width * 0.075f
            drawOval(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.55f), Color.Transparent), m, r * 1.8f),
                Offset(m.x - r * 1.8f, m.y - r * 0.7f), androidx.compose.ui.geometry.Size(r * 3.6f, r * 1.4f))
            val arrow = Path().apply {
                moveTo(m.x, m.y - r * 1.15f)
                lineTo(m.x + r * 0.85f, m.y + r * 0.45f)
                lineTo(m.x, m.y + r * 0.05f)
                lineTo(m.x - r * 0.85f, m.y + r * 0.45f)
                close()
            }
            drawPath(arrow, Brush.verticalGradient(listOf(Color(0xFF8FC3FF), p.accent), startY = m.y - r, endY = m.y + r * 0.5f))
            drawPath(arrow, Color.White, style = Stroke(r * 0.09f, join = StrokeJoin.Round))
        }
    }
}

/** Live guidance from the navigation app, or a clear call to action when there is none. */
@Composable
fun NavCard(
    nav: NavInfo?,
    hasAccess: Boolean,
    navAppLabel: String?,
    homeSet: Boolean,
    workSet: Boolean,
    onCard: () -> Unit,
    onOpenNav: () -> Unit,
    onGrantAccess: () -> Unit,
    onHome: () -> Unit,
    onWork: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val p = LocalNova.current
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.clip(shape).background(CardOnArt)
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))), shape)
            .clickable(onClick = if (nav != null) onCard else onOpenNav)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        if (nav != null) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = nav.maneuver
                    if (icon != null) Image(icon, null, Modifier.size(if (large) 72.dp else 52.dp))
                    else Icon(Icons.Rounded.TurnRight, null, tint = p.accent, modifier = Modifier.size(if (large) 72.dp else 52.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(nav.primary, style = NovaType.display.copy(fontSize = if (large) 38.sp else 30.sp, fontWeight = FontWeight.SemiBold), color = OnArt, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        nav.secondary?.let { Text(it, style = NovaType.title.copy(fontSize = if (large) 20.sp else 17.sp, fontWeight = FontWeight.Medium), color = OnArt, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    }
                }
                nav.eta?.let {
                    Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Place, null, tint = OnArtDim, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(it, style = NovaType.label, color = OnArtDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        } else {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Map, null, tint = p.accent, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(tr("No active route", "لا يوجد توجيه نشط"), style = NovaType.title, color = OnArt, maxLines = 1)
                        Text(
                            tr("Start a trip in ", "ابدأ رحلة في ") + (navAppLabel ?: tr("your maps app", "تطبيق الخرائط")),
                            style = NovaType.small.copy(fontSize = 13.sp), color = OnArtDim, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(tr("Open maps", "افتح الخرائط"), p.accent, icon = Icons.Rounded.Map, onClick = onOpenNav)
                    if (homeSet) Pill(tr("Home", "البيت"), p.accent, icon = Icons.Rounded.Home, onClick = onHome)
                    if (workSet) Pill(tr("Work", "العمل"), p.accent2, icon = Icons.Rounded.Work, onClick = onWork)
                }
                if (!hasAccess) Text(
                    tr("Allow notification access to show turn-by-turn here ›", "اسمح بالوصول للإشعارات لعرض الاتجاهات هنا ›"),
                    style = NovaType.small, color = Color(0xFF8FC3FF),
                    modifier = Modifier.padding(top = 8.dp).clickable(onClick = onGrantAccess)
                )
            }
        }
    }
}

// ------------------------------------------------------------------------ media

fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return if (s >= 3600) String.format(Locale.US, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
    else String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

@Composable
fun Artwork(info: MediaInfo?, size: Dp, corner: Dp, onClick: () -> Unit) {
    val p = LocalNova.current
    val shape = RoundedCornerShape(corner)
    Box(
        Modifier.size(size).clip(shape)
            .background(Brush.linearGradient(listOf(p.accent.copy(alpha = 0.75f), p.accent2.copy(alpha = 0.75f))))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(onClick = onClick)
    ) {
        val art = info?.art
        if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(size * 0.4f))
    }
}

@Composable
fun MediaProgress(info: MediaInfo?, modifier: Modifier = Modifier) {
    val p = LocalNova.current
    val dur = info?.durationMs ?: 0L
    val pos by produceState(info?.positionNow() ?: 0L, info) {
        while (true) { value = info?.positionNow() ?: 0L; if (info?.playing != true) break; delay(1000) }
    }
    val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(p.text.copy(alpha = 0.12f))) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(p.accent, Color(0xFF8FC3FF)))))
        }
        if (dur > 0) Row(Modifier.fillMaxWidth().padding(top = 5.dp)) {
            Text(formatMs(pos), style = NovaType.small, color = p.textDim)
            Spacer(Modifier.weight(1f))
            Text(formatMs(dur), style = NovaType.small, color = p.textDim)
        }
    }
}

@Composable
fun EqBars(modifier: Modifier) {
    val p = LocalNova.current
    val inf = rememberInfiniteTransition(label = "eq")
    val a by inf.animateFloat(0.25f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "a")
    val b by inf.animateFloat(1f, 0.3f, infiniteRepeatable(tween(530), RepeatMode.Reverse), label = "b")
    val c by inf.animateFloat(0.4f, 0.95f, infiniteRepeatable(tween(360), RepeatMode.Reverse), label = "c")
    Canvas(modifier) {
        val bw = size.width / 5
        listOf(a, b, c).forEachIndexed { i, v ->
            val x = bw * (i * 2) + bw / 2
            drawLine(p.accent, Offset(x, size.height), Offset(x, size.height * (1 - v)), bw, StrokeCap.Round)
        }
    }
}

@Composable
fun MediaCard(
    info: MediaInfo?,
    hasAccess: Boolean,
    onOpenApp: () -> Unit,
    onGrantAccess: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalNova.current
    Glass(modifier) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(info, 112.dp, 18.dp, onOpenApp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    AnimatedContent(info?.title ?: "", transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "title") { t ->
                        Text(t.ifEmpty { tr("Nothing playing", "لا يوجد تشغيل") }, style = NovaType.title.copy(fontSize = 21.sp), color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text(info?.artist ?: tr("Tap the artwork to open your music app", "اضغط على الصورة لفتح تطبيق الموسيقى"),
                        style = NovaType.label, color = p.textDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!hasAccess) Text(
                        tr("Show track info ›", "إظهار معلومات المقطع ›"), style = NovaType.small, color = p.accent,
                        modifier = Modifier.padding(top = 4.dp).clickable(onClick = onGrantAccess)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                RoundButton(Icons.Rounded.SkipPrevious, tr("Previous", "السابق"), 54.dp, onClick = onPrev)
                RoundButton(if (info?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, tr("Play / pause", "تشغيل / إيقاف"), 66.dp, primary = true, onClick = onPlayPause)
                RoundButton(Icons.Rounded.SkipNext, tr("Next", "التالي"), 54.dp, onClick = onNext)
                Box(Modifier.size(30.dp, 26.dp)) { if (info?.playing == true) EqBars(Modifier.fillMaxSize()) }
            }
            MediaProgress(info, Modifier.fillMaxWidth())
        }
    }
}
