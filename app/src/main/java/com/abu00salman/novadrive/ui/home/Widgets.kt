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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abu00salman.novadrive.data.MediaInfo
import com.abu00salman.novadrive.data.VehicleData
import com.abu00salman.novadrive.data.Weather
import com.abu00salman.novadrive.data.WeatherRepository
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Arabic text with Latin digits — the convention on Saudi car dashboards. */
fun uiLocale(arabic: Boolean): Locale =
    if (arabic) Locale.forLanguageTag("ar-SA-u-nu-latn") else Locale.US

@Composable
fun ClockCard(use24h: Boolean, modifier: Modifier = Modifier) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(1000 - value % 1000) }
    }
    val loc = uiLocale(ar)
    val timeFmt = remember(use24h, ar) { SimpleDateFormat(if (use24h) "HH:mm" else "h:mm", loc) }
    val dateFmt = remember(ar) { SimpleDateFormat("EEEE، d MMMM".let { if (ar) it else "EEEE, d MMMM" }, loc) }
    val cal = remember { Calendar.getInstance() }
    cal.timeInMillis = now
    val pm = cal.get(Calendar.AM_PM) == Calendar.PM

    Glass(modifier) {
        Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            AnalogClock(cal, Modifier.fillMaxHeight().aspectRatio(1f))
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(timeFmt.format(Date(now)), style = NovaType.display, color = p.text, maxLines = 1)
                    if (!use24h) {
                        Spacer(Modifier.width(6.dp))
                        Text(if (ar) (if (pm) "م" else "ص") else (if (pm) "PM" else "AM"),
                            style = NovaType.title, color = p.accent, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                Text(dateFmt.format(Date(now)), style = NovaType.body, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AnalogClock(cal: Calendar, modifier: Modifier) {
    val p = LocalNova.current
    val h = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f
    val m = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
    val s = cal.get(Calendar.SECOND).toFloat()
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.18f), p.accent.copy(alpha = 0f)), c, r), r, c)
        drawCircle(p.text.copy(alpha = 0.12f), r - 2f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        for (i in 0 until 12) rotate(i * 30f, c) {
            drawLine(p.text.copy(alpha = if (i % 3 == 0) 0.7f else 0.25f), Offset(c.x, c.y - r + 6f), Offset(c.x, c.y - r + if (i % 3 == 0) 16f else 11f), 3f, StrokeCap.Round)
        }
        rotate(h * 30f, c) { drawLine(p.text, c, Offset(c.x, c.y - r * 0.48f), 7f, StrokeCap.Round) }
        rotate(m * 6f, c) { drawLine(p.text.copy(alpha = 0.85f), c, Offset(c.x, c.y - r * 0.72f), 4.5f, StrokeCap.Round) }
        rotate(s * 6f, c) { drawLine(p.accent, Offset(c.x, c.y + r * 0.12f), Offset(c.x, c.y - r * 0.80f), 2.5f, StrokeCap.Round) }
        drawCircle(p.accent, 5f, c)
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
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Glass(Modifier.fillMaxHeight().aspectRatio(1f), corner = 18.dp, onClick = onOpenApp) {
                    val art = info?.art
                    if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Box(
                        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(p.accent.copy(alpha = 0.55f), p.accent2.copy(alpha = 0.45f))))
                    ) { Icon(Icons.Rounded.MusicNote, null, tint = p.text, modifier = Modifier.align(Alignment.Center).size(40.dp)) }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("NOW PLAYING", "يُشغَّل الآن"), style = NovaType.caps, color = p.accent)
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(info?.title ?: "", transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "title") { t ->
                        Text(t.ifEmpty { tr("Nothing playing", "لا يوجد تشغيل") }, style = NovaType.title, color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text(info?.artist ?: "", style = NovaType.body, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!hasAccess) Glass(Modifier.padding(top = 4.dp).height(30.dp), corner = 10.dp, onClick = onGrantAccess) {
                        Text(tr("Show track info", "إظهار معلومات المقطع"), style = NovaType.label, color = p.text, modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp))
                    }
                }
                if (info?.playing == true) EqBars(Modifier.size(28.dp, 24.dp))
            }
            ProgressLine(info)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                MediaButton(Icons.Rounded.SkipPrevious, 54, onPrev)
                MediaButton(if (info?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 68, onPlayPause, primary = true)
                MediaButton(Icons.Rounded.SkipNext, 54, onNext)
            }
        }
    }
}

@Composable
private fun ProgressLine(info: MediaInfo?) {
    val p = LocalNova.current
    val dur = info?.durationMs ?: 0L
    val pos by produceState(0L, info) {
        while (true) { value = info?.positionNow() ?: 0L; delay(1000) }
    }
    val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(p.text.copy(alpha = 0.10f))) {
        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Brush.horizontalGradient(listOf(p.accent2, p.accent))))
    }
}

@Composable
private fun MediaButton(icon: ImageVector, size: Int, onClick: () -> Unit, primary: Boolean = false) {
    val p = LocalNova.current
    Glass(Modifier.size(size.dp), corner = (size / 2).dp, tint = if (primary) p.accent else null, onClick = onClick) {
        Icon(icon, null, tint = if (primary) p.accent else p.text, modifier = Modifier.align(Alignment.Center).size((size * 0.5f).dp))
    }
}

@Composable
private fun EqBars(modifier: Modifier) {
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

fun weatherIcon(w: Weather): ImageVector = when (WeatherRepository.kind(w.code)) {
    WeatherRepository.Companion.Kind.CLEAR -> if (w.isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    WeatherRepository.Companion.Kind.PARTLY -> Icons.Rounded.WbCloudy
    WeatherRepository.Companion.Kind.CLOUDY, WeatherRepository.Companion.Kind.FOG -> Icons.Rounded.Cloud
    WeatherRepository.Companion.Kind.RAIN -> Icons.Rounded.Grain
    WeatherRepository.Companion.Kind.SNOW -> Icons.Rounded.AcUnit
    WeatherRepository.Companion.Kind.STORM -> Icons.Rounded.Thunderstorm
}

@Composable
fun WeatherBadge(w: Weather, modifier: Modifier = Modifier) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(weatherIcon(w), null, tint = p.accent, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("${Math.round(w.tempC)}°", style = NovaType.display, color = p.text)
            Text(WeatherRepository.label(w.code, ar), style = NovaType.label, color = p.textDim)
        }
    }
}

@Composable
fun VehicleStrip(v: VehicleData, modifier: Modifier = Modifier) {
    val p = LocalNova.current
    Glass(modifier, corner = 18.dp) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            when (v.state) {
                VehicleData.State.LIVE -> {
                    Metric(v.rpm?.toString() ?: "–", "RPM")
                    Spacer(Modifier.width(18.dp))
                    Metric(v.coolantC?.let { "$it°" } ?: "–", tr("COOLANT", "الحرارة"))
                    Spacer(Modifier.width(18.dp))
                    Metric(v.voltage?.let { String.format(Locale.US, "%.1fV", it) } ?: "–", tr("BATTERY", "البطارية"))
                }
                VehicleData.State.CONNECTING -> Text(tr("OBD connecting…", "جارٍ الاتصال بـ OBD…"), style = NovaType.label, color = p.textDim)
                VehicleData.State.ERROR -> Text(tr("OBD: ", "OBD: ") + (v.error ?: ""), style = NovaType.label, color = p.danger, maxLines = 1)
                VehicleData.State.OFF -> {}
            }
        }
    }
}

@Composable
private fun Metric(value: String, label: String) {
    val p = LocalNova.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = NovaType.title, color = p.text)
        Text(label, style = NovaType.caps, color = p.textDim)
    }
}
