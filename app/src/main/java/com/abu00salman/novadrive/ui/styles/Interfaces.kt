package com.abu00salman.novadrive.ui.styles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.core.UiStyles
import com.abu00salman.novadrive.ui.components.FixedLtr
import com.abu00salman.novadrive.ui.components.UiDirection
import com.abu00salman.novadrive.ui.home.HomeActions
import com.abu00salman.novadrive.ui.home.HomeUi
import com.abu00salman.novadrive.ui.home.compassLabel
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.tr
import java.util.Locale
import kotlin.math.abs

/** Hosts one of the eight customisable interfaces with its resolved skin. */
@Composable
fun StyledHome(ui: HomeUi, act: HomeActions, modifier: Modifier = Modifier) {
    val style = UiStyles.get(ui.style)
    val skin = skinFor(style, ui.styleOptions, LocalNova.current.dark)
    CompositionLocalProvider(LocalSkin provides skin) {
        Box(modifier.fillMaxSize()) {
            SkinBackground()
            FixedLtr {
                when (style.id) {
                    "classic" -> ClassicLayout(ui, act)
                    "mercedes" -> MercedesLayout(ui, act)
                    "ford" -> FordLayout(ui, act)
                    "apple" -> AppleLayout(ui, act)
                    "jetour" -> JetourLayout(ui, act)
                    "byd" -> BydLayout(ui, act)
                    "haval" -> HavalLayout(ui, act)
                    "tank" -> TankLayout(ui, act)
                }
            }
        }
    }
}

/** Time · date on the start side, status icons, «أمرني» and settings on the end side. */
@Composable
private fun TopRow(
    ui: HomeUi, act: HomeActions, modifier: Modifier, timeSize: Float = 30f, dateSize: Float = 16f,
    voiceLabel: Boolean = false, lead: @Composable RowScope.() -> Unit = {},
) {
    val k = LocalSkin.current
    val clock = rememberClock(ui.use24h)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        lead()
        UiDirection {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.clickable(onClick = act.onQuick)) {
                Text(clock.time, style = st(timeSize.sp, FontWeight.SemiBold), maxLines = 1)
                if (clock.ampm.isNotEmpty()) Text(" " + clock.ampm, style = st((timeSize * 0.5f).sp, color = k.dim), modifier = Modifier.padding(bottom = (timeSize * 0.12f).dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        UiDirection { Text(clock.dateShort, style = st(dateSize.sp, color = k.dim), maxLines = 1) }
        Spacer(Modifier.weight(1f))
        StatusIcons(ui, act)
        Spacer(Modifier.width(8.dp))
        if (voiceLabel) VoicePill(act) else SRound(Icons.Rounded.AutoAwesome, tr("Ask me", "أمرني"), 48.dp, filled = true, onClick = act.onVoice)
        Spacer(Modifier.width(8.dp))
        SRound(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), 48.dp, onClick = act.onSettings)
    }
}

@Composable
private fun VoicePill(act: HomeActions, big: Boolean = false) {
    val k = LocalSkin.current
    Row(
        Modifier.height(if (big) 64.dp else 50.dp).clip(k.shape(32.dp)).background(k.accent).clickable(onClick = act.onVoice).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(if (big) 30.dp else 24.dp))
        Spacer(Modifier.width(8.dp))
        UiDirection { Text(tr("Ask me", "أمرني"), style = st(if (big) 22.sp else 17.sp, FontWeight.SemiBold, Color.White), maxLines = 1) }
    }
}

// ============================================================================ Classic

/**
 * «كلاسيك»: plain and official. Very large icons and text, wide spacing, no effects; light by
 * default with an optional dark mode. Everything important is one tap away.
 */
@Composable
private fun ClassicLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TopRow(ui, act, Modifier.fillMaxWidth().height(66.dp), timeSize = 38f, dateSize = 20f, voiceLabel = true)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SkinMap(ui, act, Modifier.weight(0.37f).fillMaxHeight())
            SCard(Modifier.weight(0.33f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                    MediaArt(ui, act, 118.dp, 16.dp)
                    UiDirection { MediaTitle(ui, act, 23.sp, Modifier.fillMaxWidth(), center = true) }
                    MediaBar(ui, Modifier.fillMaxWidth())
                    MediaButtons(ui, act, 74.dp, Modifier.fillMaxWidth())
                }
            }
            Column(Modifier.weight(0.30f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SCard(Modifier.weight(1f).fillMaxWidth(), onClick = act.onQuick) {
                    UiDirection {
                        Column(Modifier.align(Alignment.Center).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            WeatherLine(ui, 46.sp, 54.dp)
                            Spacer(Modifier.height(6.dp))
                            HiLo(ui, 17.sp)
                        }
                    }
                }
                SCard(Modifier.weight(0.8f).fillMaxWidth(), onClick = act.onPhone, fill = Color(0xFF16A34A)) {
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Phone, null, tint = Color.White, modifier = Modifier.size(54.dp))
                        Spacer(Modifier.width(14.dp))
                        UiDirection { Text(tr("Phone", "الهاتف"), style = st(30.sp, FontWeight.Bold, Color.White)) }
                    }
                }
            }
        }
        AppStrip(ui, act, Modifier.fillMaxWidth().height(156.dp), icon = 84.dp, label = 20.sp, tile = true, spacing = 12.dp)
    }
}

// ============================================================================ Mercedes

/** «Mercedes»: MBUX-inspired — ambient light glows, a large map and a floating "zero layer" bar. */
@Composable
private fun MercedesLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.CenterVertically) {
            // Voice orb ("Hey Mercedes"-style) leads the bar.
            Box(
                Modifier.size(60.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(k.accent, k.accent2, k.accent)))
                    .clickable(onClick = act.onVoice), contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.AutoAwesome, tr("Ask me", "أمرني"), tint = Color.White, modifier = Modifier.size(28.dp)) }
            Spacer(Modifier.width(18.dp))
            val clock = rememberClock(ui.use24h)
            UiDirection {
                Column(Modifier.clickable(onClick = act.onQuick)) {
                    Text(clock.time + if (clock.ampm.isNotEmpty()) " " + clock.ampm else "", style = st(36.sp, FontWeight.Light), maxLines = 1)
                    Text(clock.dateShort, style = st(15.sp, color = k.dim), maxLines = 1)
                }
            }
            Spacer(Modifier.weight(1f))
            StatusIcons(ui, act)
            Spacer(Modifier.width(10.dp))
            SRound(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), 50.dp, onClick = act.onSettings)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SkinMap(ui, act, Modifier.weight(0.58f).fillMaxHeight())
            Column(Modifier.weight(0.42f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SkinMedia(ui, act, Modifier.weight(0.56f).fillMaxWidth(), art = 116.dp)
                Row(Modifier.weight(0.44f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SCard(Modifier.weight(1f).fillMaxHeight(), onClick = act.onQuick) {
                        UiDirection { Column(Modifier.align(Alignment.Center).padding(10.dp)) { WeatherLine(ui, 34.sp, 40.dp); HiLo(ui, 13.sp) } }
                    }
                    SCard(Modifier.weight(1f).fillMaxHeight(), onClick = act.onVehicle) {
                        Box(Modifier.align(Alignment.Center)) { SpeedText(ui, 54.sp, act) }
                    }
                }
            }
        }
        SCard(Modifier.fillMaxWidth().height(116.dp), corner = 44.dp) {
            AppStrip(ui, act, Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp), icon = 60.dp, label = 13.sp)
        }
    }
}

// ============================================================================ Ford

/** «Ford»: SYNC-inspired — a labelled feature bar, flat panels, map with a card stack, climate-style controls. */
@Composable
private fun FordLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Row(Modifier.fillMaxSize()) {
        // Feature bar
        Column(
            Modifier.width(112.dp).fillMaxHeight().background(k.surface).padding(vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            menuItems(act).forEach { m ->
                Column(
                    Modifier.width(100.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (m.special) k.accent else Color.Transparent).clickable(onClick = m.onClick).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(m.icon, null, tint = if (m.special) Color.White else k.text, modifier = Modifier.size(30.dp))
                    UiDirection { Text(m.label, style = st(13.sp, FontWeight.Medium, if (m.special) Color.White else k.dim), maxLines = 1) }
                }
            }
        }
        Box(Modifier.width(3.dp).fillMaxHeight().background(k.accent))
        Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically) {
                val clock = rememberClock(ui.use24h)
                UiDirection { Text(clock.time + " " + clock.ampm, style = st(26.sp, FontWeight.SemiBold), modifier = Modifier.clickable(onClick = act.onQuick)) }
                Spacer(Modifier.width(16.dp))
                ui.weather?.takeIf { ui.showWeather }?.let { Text("${Math.round(it.tempC)}°", style = st(24.sp, FontWeight.Medium)) }
                Spacer(Modifier.weight(1f))
                StatusIcons(ui, act)
                Spacer(Modifier.width(12.dp))
                UiDirection { Text(clock.dateShort, style = st(17.sp, color = k.dim)) }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SkinMap(ui, act, Modifier.weight(0.64f).fillMaxHeight())
                Column(Modifier.weight(0.36f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkinMedia(ui, act, Modifier.weight(1.2f).fillMaxWidth(), art = 82.dp)
                    SCard(Modifier.weight(0.55f).fillMaxWidth(), onClick = act.onPhone) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF16A34A)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Phone, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            UiDirection { Text(tr("Phone", "الهاتف"), style = st(21.sp, FontWeight.SemiBold)) }
                        }
                    }
                    SCard(Modifier.weight(0.7f).fillMaxWidth(), onClick = act.onQuick) {
                        UiDirection { Column(Modifier.align(Alignment.CenterStart).padding(16.dp)) { WeatherLine(ui, 30.sp, 38.dp); HiLo(ui, 13.sp) } }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(98.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                SCard(Modifier.width(250.dp).fillMaxHeight()) { VolumeControl(act, Modifier.align(Alignment.Center).padding(horizontal = 10.dp), compact = true) }
                AppStrip(ui, act, Modifier.weight(1f).fillMaxHeight(), icon = 46.dp, label = 13.sp, tile = true, spacing = 8.dp)
                SCard(Modifier.width(250.dp).fillMaxHeight()) { BrightnessControl(act, Modifier.align(Alignment.Center).padding(horizontal = 10.dp), compact = true) }
            }
        }
    }
}

// ============================================================================ Apple Future Car

/** «Apple المستقبلية»: glass widgets on a soft gradient, a huge thin clock and a floating dock. */
@Composable
private fun AppleLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(Modifier.weight(0.34f).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) { StatusIcons(ui, act, 22.dp) }
                Spacer(Modifier.weight(0.3f))
                val clock = rememberClock(ui.use24h)
                UiDirection {
                    Column(Modifier.clickable(onClick = act.onQuick)) {
                        Text(clock.time, style = st(112.sp, FontWeight.Light), maxLines = 1)
                        Text(clock.dateShort, style = st(22.sp, FontWeight.Medium, k.dim), maxLines = 1)
                        Spacer(Modifier.height(14.dp))
                        WeatherLine(ui, 28.sp, 34.dp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VoicePill(act, big = true)
                    Spacer(Modifier.width(12.dp))
                    SRound(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), 60.dp, onClick = act.onSettings)
                }
            }
            Column(Modifier.weight(0.66f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SkinMap(ui, act, Modifier.weight(0.58f).fillMaxWidth())
                Row(Modifier.weight(0.42f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    SkinMedia(ui, act, Modifier.weight(1.35f).fillMaxHeight(), art = 104.dp)
                    SCard(Modifier.weight(0.65f).fillMaxHeight(), onClick = act.onVehicle) {
                        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            SpeedText(ui, 40.sp, act)
                            SkinCar(ui, act, Modifier.weight(1f).fillMaxWidth(), hint = false)
                        }
                    }
                }
            }
        }
        // Floating dock
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SCard(Modifier.fillMaxWidth(0.74f).height(104.dp), corner = 40.dp) {
                AppStrip(ui, act, Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), icon = 68.dp, labels = false)
            }
        }
    }
}

// ============================================================================ Jetour

/** «Jetour»: rugged-modern SUV — cut corners, a central speed arc around the car, side panels. */
@Composable
private fun JetourLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopRow(ui, act, Modifier.fillMaxWidth().height(58.dp), timeSize = 30f) {
            UiDirection { Text(carName(ui).uppercase(Locale.ROOT), style = st(18.sp, FontWeight.Bold, k.accent), maxLines = 1, modifier = Modifier.padding(end = 18.dp)) }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkinMedia(ui, act, Modifier.weight(0.27f).fillMaxHeight(), art = 120.dp, vertical = true)
            SCard(Modifier.weight(0.46f).fillMaxHeight(), onClick = act.onVehicle) {
                val speed = if (ui.hasLocation) ui.speedKmh ?: 0f else 0f
                Canvas(Modifier.fillMaxSize().padding(24.dp)) {
                    val d = minOf(size.width, size.height * 1.6f)
                    val tl = Offset((size.width - d) / 2, 8f)
                    val stroke = Stroke(10.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(k.text.copy(alpha = 0.10f), 160f, 220f, false, tl, Size(d, d), style = stroke)
                    drawArc(Brush.sweepGradient(listOf(k.accent2, k.accent)), 160f, 220f * (speed / 220f).coerceIn(0f, 1f), false, tl, Size(d, d), style = stroke)
                }
                Column(Modifier.align(Alignment.TopCenter).padding(top = 44.dp)) { SpeedText(ui, 76.sp, act) }
                SkinCar(ui, act, Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.8f).fillMaxHeight(0.5f).padding(bottom = 12.dp))
            }
            SkinMap(ui, act, Modifier.weight(0.27f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().height(104.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SCard(Modifier.weight(1f).fillMaxHeight()) {
                AppStrip(ui, act, Modifier.fillMaxSize().padding(8.dp), icon = 54.dp, label = 13.sp)
            }
            SCard(Modifier.width(230.dp).fillMaxHeight()) { VolumeControl(act, Modifier.align(Alignment.Center).padding(horizontal = 10.dp), compact = true) }
        }
    }
}

// ============================================================================ BYD

/** «BYD»: clean smart-EV look — big map, soft light cards and an app grid on the side. */
@Composable
private fun BydLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopRow(ui, act, Modifier.fillMaxWidth().height(58.dp), timeSize = 36f, dateSize = 17f)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(0.72f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkinMap(ui, act, Modifier.weight(0.6f).fillMaxWidth())
                Row(Modifier.weight(0.4f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkinMedia(ui, act, Modifier.weight(1.35f).fillMaxHeight(), art = 88.dp)
                    SCard(Modifier.weight(0.8f).fillMaxHeight(), onClick = act.onVehicle) {
                        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SpeedText(ui, 40.sp, act)
                            SkinCar(ui, act, Modifier.weight(1f).fillMaxHeight(), hint = false)
                        }
                    }
                    SCard(Modifier.weight(0.75f).fillMaxHeight(), onClick = act.onQuick) {
                        UiDirection { Column(Modifier.align(Alignment.Center).padding(8.dp)) { WeatherLine(ui, 30.sp, 34.dp); HiLo(ui, 12.sp) } }
                    }
                }
            }
            SCard(Modifier.weight(0.28f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.dock.chunked(2).forEach { pair ->
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { s -> AppItem(s, act, Modifier.weight(1f).fillMaxHeight(), 56.dp, 14.sp) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SCard(Modifier.weight(1f).fillMaxHeight()) { VolumeControl(act, Modifier.align(Alignment.Center).padding(horizontal = 12.dp), compact = true) }
            SCard(Modifier.weight(1f).fillMaxHeight()) { BrightnessControl(act, Modifier.align(Alignment.Center).padding(horizontal = 12.dp), compact = true) }
        }
    }
}

// ============================================================================ Haval

/** «Haval»: intelligent-drive cockpit — car and lane view, tall centre map, info stack, labelled bar. */
@Composable
private fun HavalLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            TopRow(ui, act, Modifier.fillMaxWidth().height(56.dp), timeSize = 28f) {
                UiDirection { Text(carName(ui), style = st(18.sp, FontWeight.Bold), maxLines = 1, modifier = Modifier.padding(end = 18.dp)) }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(listOf(k.accent, Color.Transparent))))
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SCard(Modifier.weight(0.30f).fillMaxHeight(), onClick = act.onVehicle) {
                // Lane lines in perspective around the car (scenery).
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    for (s in listOf(-1f, 1f)) drawLine(
                        Brush.verticalGradient(listOf(k.accent.copy(alpha = 0f), k.accent), startY = h * 0.35f, endY = h),
                        Offset(w / 2 + s * w * 0.08f, h * 0.35f), Offset(w / 2 + s * w * 0.45f, h), strokeWidth = 4.dp.toPx(),
                    )
                }
                Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SpeedText(ui, 70.sp, act)
                    SkinCar(ui, act, Modifier.weight(1f).fillMaxWidth(0.92f))
                }
            }
            SkinMap(ui, act, Modifier.weight(0.40f).fillMaxHeight())
            Column(Modifier.weight(0.30f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SCard(Modifier.weight(0.42f).fillMaxWidth(), onClick = act.onQuick) {
                    val clock = rememberClock(ui.use24h)
                    UiDirection {
                        Column(Modifier.align(Alignment.CenterStart).padding(16.dp)) {
                            Text(clock.time + " " + clock.ampm, style = st(36.sp, FontWeight.Bold), maxLines = 1)
                            Spacer(Modifier.height(6.dp))
                            WeatherLine(ui, 24.sp, 30.dp)
                        }
                    }
                }
                SkinMedia(ui, act, Modifier.weight(0.58f).fillMaxWidth(), art = 82.dp)
            }
        }
        SCard(Modifier.fillMaxWidth().height(108.dp)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ui.dock.forEachIndexed { i, s ->
                    if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight(0.55f).background(k.border))
                    AppItem(s, act, Modifier.weight(1f).fillMaxHeight(), 52.dp, 14.sp)
                }
            }
        }
    }
}

// ============================================================================ TANK

/** «TANK»: off-road — compass, GPS altitude and coordinates, topo-toned map, chunky controls. */
@Composable
private fun TankLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    val ar = LocalArabic.current
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopRow(ui, act, Modifier.fillMaxWidth().height(58.dp), timeSize = 30f) {
            Icon(Icons.Rounded.Terrain, null, tint = k.accent, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            UiDirection { Text(carName(ui).uppercase(Locale.ROOT), style = st(18.sp, FontWeight.Bold, k.accent), maxLines = 1, modifier = Modifier.padding(end = 18.dp)) }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(0.31f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SCard(Modifier.weight(0.62f).fillMaxWidth()) {
                    Compass(ui.bearing, Modifier.fillMaxSize().padding(14.dp))
                    UiDirection {
                        Text(ui.bearing?.let { "${it.toInt()}° · " + compassLabel(it, ar) } ?: tr("Heading: move to measure", "الاتجاه: يظهر أثناء الحركة"),
                            style = st(14.sp, FontWeight.SemiBold, k.dim), modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
                    }
                }
                Row(Modifier.weight(0.38f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SCard(Modifier.weight(1f).fillMaxHeight(), onClick = act.onVehicle) {
                        Box(Modifier.align(Alignment.Center)) { SpeedText(ui, 44.sp, act) }
                    }
                    SCard(Modifier.weight(1f).fillMaxHeight()) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(ui.altitudeM?.let { "${it.toInt()}" } ?: "--", style = st(40.sp, FontWeight.Bold), maxLines = 1)
                            UiDirection { Text(tr("altitude m · GPS", "الارتفاع م · GPS"), style = st(12.sp, color = k.dim)) }
                        }
                    }
                }
            }
            SkinMap(ui, act, Modifier.weight(0.41f).fillMaxHeight())
            Column(Modifier.weight(0.28f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SCard(Modifier.weight(0.34f).fillMaxWidth(), onClick = act.onQuick) {
                    Column(Modifier.align(Alignment.CenterStart).padding(14.dp)) {
                        UiDirection { Text(tr("Coordinates · GPS", "الإحداثيات · GPS"), style = st(13.sp, color = k.dim)) }
                        FixedLtr {
                            Text(ui.lat?.let { String.format(Locale.US, "%.5f° %s", abs(it), if (it >= 0) "N" else "S") } ?: "--", style = st(20.sp, FontWeight.SemiBold))
                            Text(ui.lon?.let { String.format(Locale.US, "%.5f° %s", abs(it), if (it >= 0) "E" else "W") } ?: "--", style = st(20.sp, FontWeight.SemiBold))
                        }
                    }
                }
                SkinMedia(ui, act, Modifier.weight(0.66f).fillMaxWidth(), art = 84.dp, vertical = true)
            }
        }
        AppStrip(ui, act, Modifier.fillMaxWidth().height(106.dp), icon = 54.dp, label = 14.sp, tile = true, spacing = 10.dp)
    }
}

/** Compass dial: needle points along the GPS direction of travel. */
@Composable
private fun Compass(bearing: Float?, modifier: Modifier) {
    val k = LocalSkin.current
    val ar = LocalArabic.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val d = minOf(maxWidth, maxHeight) * 0.86f
        Canvas(Modifier.size(d)) {
            val r = size.minDimension / 2
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(k.border, r, c, style = Stroke(3.dp.toPx()))
            for (i in 0 until 36) {
                val len = if (i % 9 == 0) 0.16f else if (i % 3 == 0) 0.1f else 0.05f
                rotate(i * 10f, c) { drawLine(k.dim, Offset(c.x, c.y - r), Offset(c.x, c.y - r * (1 - len)), strokeWidth = 2.dp.toPx()) }
            }
            if (bearing != null) rotate(bearing, c) {
                val p = Path().apply { moveTo(c.x, c.y - r * 0.78f); lineTo(c.x + r * 0.1f, c.y); lineTo(c.x - r * 0.1f, c.y); close() }
                drawPath(p, k.accent)
                val q = Path().apply { moveTo(c.x, c.y + r * 0.6f); lineTo(c.x + r * 0.1f, c.y); lineTo(c.x - r * 0.1f, c.y); close() }
                drawPath(q, k.dim.copy(alpha = 0.5f))
            }
            drawCircle(k.text, 5.dp.toPx(), c)
        }
        val labels = if (ar) listOf("ش", "ق", "ج", "غ") else listOf("N", "E", "S", "W")
        Box(Modifier.size(d)) {
            Text(labels[0], style = st(18.sp, FontWeight.Bold, Color(0xFFE53935)), modifier = Modifier.align(Alignment.TopCenter).padding(top = d * 0.2f))
            Text(labels[1], style = st(16.sp, FontWeight.Bold), modifier = Modifier.align(Alignment.CenterEnd).padding(end = d * 0.2f))
            Text(labels[2], style = st(16.sp, FontWeight.Bold), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = d * 0.2f))
            Text(labels[3], style = st(16.sp, FontWeight.Bold), modifier = Modifier.align(Alignment.CenterStart).padding(start = d * 0.2f))
        }
    }
}
