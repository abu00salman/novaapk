package com.abu00salman.novadrive.ui.styles

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.ui.components.FixedLtr
import com.abu00salman.novadrive.ui.components.UiDirection
import com.abu00salman.novadrive.ui.home.DockIcon
import com.abu00salman.novadrive.ui.home.HomeActions
import com.abu00salman.novadrive.ui.home.HomeUi
import com.abu00salman.novadrive.ui.home.compassLabel
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.tr
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ============================================================================ Mercedes — MBUX

/**
 * «Mercedes» (MBUX-inspired): the map is the "zero layer" filling the screen; a slim left menu,
 * the status bar and floating glass tiles sit on top of it, lit by ambient colour.
 */
@Composable
internal fun MercedesLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Box(Modifier.fillMaxSize()) {
        SkinMap(ui, act, Modifier.fillMaxSize(), focusY = 0.48f, guidance = false, frame = false)
        // Vignette so overlays stay readable over the map.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to k.bg[0].copy(alpha = 0.85f), 0.18f to Color.Transparent, 0.62f to Color.Transparent, 1f to k.bg[0].copy(alpha = 0.92f))))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to k.bg[0].copy(alpha = 0.9f), 0.14f to Color.Transparent)))
        Row(Modifier.fillMaxSize()) {
            // MBUX-style left menu
            Column(
                Modifier.width(104.dp).fillMaxHeight().padding(vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MbuxItem(Icons.Rounded.Home, tr("Home", "الرئيسية"), true) {}
                menuItems(act).forEach { m -> MbuxItem(m.icon, m.label, false, m.special, m.onClick) }
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(end = 22.dp, top = 14.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val clock = rememberClock(ui.use24h)
                    UiDirection {
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.clickable(onClick = act.onQuick)) {
                            Text(clock.time, style = st(40.sp, FontWeight.Light), maxLines = 1)
                            Text(" " + clock.ampm + "   " + clock.dateShort, style = st(16.sp, color = k.dim), maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 7.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    WeatherLine(ui, 24.sp, 28.dp)
                    Spacer(Modifier.width(14.dp))
                    StatusIcons(ui, act)
                }
                ui.nav?.let { nav ->
                    Spacer(Modifier.height(10.dp))
                    SCard(Modifier.width(420.dp), onClick = act.onNavCard, corner = 22.dp) {
                        UiDirection {
                            Column(Modifier.padding(14.dp)) {
                                Text(nav.primary, style = st(26.sp, FontWeight.SemiBold), maxLines = 1)
                                nav.secondary?.let { Text(it, style = st(15.sp, color = k.dim), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                nav.eta?.let { Text(it, style = st(13.sp, color = k.accent), maxLines = 1) }
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().height(176.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SkinMedia(ui, act, Modifier.weight(1.25f).fillMaxHeight(), art = 120.dp)
                    SCard(Modifier.weight(0.5f).fillMaxHeight(), onClick = act.onVehicle) { Box(Modifier.align(Alignment.Center)) { SpeedText(ui, 50.sp, act) } }
                    SCard(Modifier.weight(1f).fillMaxHeight(), corner = 26.dp) {
                        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ui.dock.chunked(3).take(2).forEach { row ->
                                Row(Modifier.weight(1f).fillMaxWidth()) { row.forEach { s -> AppItem(s, act, Modifier.weight(1f).fillMaxHeight(), 44.dp, 12.sp) } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MbuxItem(icon: ImageVector, label: String, active: Boolean, special: Boolean = false, onClick: () -> Unit) {
    val k = LocalSkin.current
    Column(Modifier.width(92.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(50.dp).clip(CircleShape)
                .background(if (special) Brush.sweepGradient(listOf(k.accent, k.accent2, k.accent)) else if (active) Brush.linearGradient(listOf(k.accent.copy(alpha = 0.5f), k.accent2.copy(alpha = 0.4f))) else Brush.linearGradient(listOf(k.surface, k.surface)))
                .border(1.dp, if (active) k.accent else k.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = if (active || special) Color.White else k.text, modifier = Modifier.size(26.dp)) }
        UiDirection { Text(label, style = st(12.sp, FontWeight.Medium, if (active) k.accent else k.dim), maxLines = 1) }
    }
}

// ============================================================================ Ford — SYNC

/**
 * «Ford» (SYNC-inspired): a three-tile home — Navigation large, Audio and Phone on the right — under
 * a thin status strip, and SYNC's bottom feature bar with labelled buttons and the user's apps.
 */
@Composable
internal fun FordLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Column(Modifier.fillMaxSize()) {
        // Status strip
        Row(Modifier.fillMaxWidth().height(48.dp).background(k.surface).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            val clock = rememberClock(ui.use24h)
            ui.weather?.takeIf { ui.showWeather }?.let { Text("${Math.round(it.tempC)}°", style = st(20.sp, FontWeight.Medium)) }
            Spacer(Modifier.weight(1f))
            UiDirection { Text(clock.time + " " + clock.ampm, style = st(22.sp, FontWeight.SemiBold), modifier = Modifier.clickable(onClick = act.onQuick)) }
            Spacer(Modifier.weight(1f))
            StatusIcons(ui, act, 20.dp)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(k.accent))
        Row(Modifier.weight(1f).fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SyncTile(tr("Navigation", "الملاحة"), Icons.Rounded.Navigation, act.onNavDetails, Modifier.weight(0.6f).fillMaxHeight()) {
                SkinMap(ui, act, Modifier.fillMaxSize(), frame = false)
            }
            Column(Modifier.weight(0.4f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SyncTile(tr("Audio", "الصوت"), Icons.Rounded.MusicNote, act.onMusicDetails, Modifier.weight(1.25f).fillMaxWidth()) {
                    Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        MediaArt(ui, act, 96.dp, 4.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UiDirection { MediaTitle(ui, act, 18.sp, Modifier.fillMaxWidth()) }
                            MediaBar(ui, Modifier.fillMaxWidth())
                            MediaButtons(ui, act, 50.dp, Modifier.fillMaxWidth())
                        }
                    }
                }
                SyncTile(tr("Phone", "الهاتف"), Icons.Rounded.Phone, act.onPhone, Modifier.weight(0.75f).fillMaxWidth()) {
                    BigShortcut(ui, act, Modifier.fillMaxSize().padding(8.dp), icon = 40.dp, label = 22.sp, phoneFill = Color(0xFF15803D))
                }
            }
        }
        // SYNC feature bar
        Row(Modifier.fillMaxWidth().height(96.dp).background(k.surface).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            SyncBarItem(Icons.Rounded.Home, tr("Home", "الرئيسية"), true) {}
            ui.dock.forEach { s -> Box(Modifier.weight(1f).fillMaxHeight()) { AppItem(s, act, Modifier.fillMaxSize(), 42.dp, 12.sp) } }
            SyncBarItem(Icons.Rounded.AutoAwesome, "Nova AI", false, act.onVoice)
            SyncBarItem(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), false, act.onSettings)
        }
    }
}

@Composable
private fun SyncTile(title: String, icon: ImageVector, onTitle: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    val k = LocalSkin.current
    SCard(modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(40.dp).background(k.surfaceHi).clickable(onClick = onTitle).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = k.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                UiDirection { Text(title, style = st(16.sp, FontWeight.SemiBold), maxLines = 1) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}

@Composable
private fun SyncBarItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val k = LocalSkin.current
    Column(Modifier.width(92.dp).fillMaxHeight().clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, label, tint = if (active) k.accent else k.text, modifier = Modifier.size(30.dp))
        UiDirection { Text(label, style = st(12.sp, FontWeight.Medium, if (active) k.accent else k.dim), maxLines = 1) }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(36.dp).height(3.dp).background(if (active) k.accent else Color.Transparent))
    }
}

// ============================================================================ Apple Future Car

/**
 * «Apple المستقبلية» (a concept inspired by CarPlay's dashboard, not an Apple product): a status
 * sidebar with the user's apps, a large map and stacked widgets on a soft wallpaper.
 */
@Composable
internal fun AppleLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Sidebar
        SCard(Modifier.width(112.dp).fillMaxHeight(), corner = 32.dp) {
            Column(Modifier.fillMaxSize().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val clock = rememberClock(ui.use24h)
                Text(clock.time, style = st(24.sp, FontWeight.SemiBold), maxLines = 1, modifier = Modifier.clickable(onClick = act.onQuick))
                StatusIcons(ui, act, 16.dp)
                Spacer(Modifier.height(6.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                    ui.dock.take(5).forEach { s -> AppItem(s, act, Modifier.size(78.dp), 62.dp, 0.sp, labels = false) }
                }
                SRound(Icons.Rounded.AutoAwesome, "Nova AI", 58.dp, filled = true, onClick = act.onVoice)
            }
        }
        // Dashboard
        Row(Modifier.weight(1f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SkinMap(ui, act, Modifier.weight(0.6f).fillMaxHeight(), corner = 32.dp)
            Column(Modifier.weight(0.4f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SCard(Modifier.weight(0.5f).fillMaxWidth(), corner = 32.dp) {
                    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MediaArt(ui, act, 86.dp, 16.dp)
                            Spacer(Modifier.width(14.dp))
                            UiDirection { MediaTitle(ui, act, 19.sp, Modifier.weight(1f)) }
                        }
                        MediaBar(ui, Modifier.fillMaxWidth(), times = false)
                        MediaButtons(ui, act, 58.dp, Modifier.fillMaxWidth())
                    }
                }
                Row(Modifier.weight(0.5f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SCard(Modifier.weight(1f).fillMaxHeight(), onClick = act.onQuick, corner = 32.dp) {
                        val clock = rememberClock(ui.use24h)
                        UiDirection {
                            Column(Modifier.align(Alignment.CenterStart).padding(16.dp)) {
                                Text(clock.dateShort, style = st(15.sp, FontWeight.SemiBold, k.accent), maxLines = 1)
                                Text(clock.time, style = st(44.sp, FontWeight.Light), maxLines = 1)
                                HiLo(ui, 12.sp)
                            }
                        }
                    }
                    SCard(Modifier.weight(1f).fillMaxHeight(), onClick = act.onVehicle, corner = 32.dp) {
                        UiDirection { Column(Modifier.align(Alignment.Center).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            WeatherLine(ui, 30.sp, 34.dp)
                            Spacer(Modifier.height(6.dp))
                            SpeedText(ui, 30.sp, act)
                        } }
                    }
                }
            }
        }
    }
}

// ============================================================================ Jetour — Modern SUV

/**
 * «Jetour» (modern SUV): tab bar on top, the car in the middle inside a speed arc, map and player
 * on the left, time/weather and controls on the right, and an icon dock.
 */
@Composable
internal fun JetourLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Tab bar (each tab opens its screen)
        Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
            UiDirection { Text(carName(ui).uppercase(Locale.ROOT), style = st(18.sp, FontWeight.Bold, k.accent), maxLines = 1) }
            Spacer(Modifier.width(18.dp))
            JetourTab(tr("Home", "الرئيسية"), true) {}
            JetourTab(tr("Navigation", "الملاحة"), false, act.onNavDetails)
            JetourTab(tr("Media", "الوسائط"), false, act.onMusicDetails)
            JetourTab(tr("Vehicle", "السيارة"), false, act.onVehicle)
            JetourTab(tr("Apps", "التطبيقات"), false, act.onApps)
            Spacer(Modifier.weight(1f))
            StatusIcons(ui, act)
            SRound(Icons.Rounded.AutoAwesome, "Nova AI", 46.dp, filled = true, onClick = act.onVoice)
            Spacer(Modifier.width(8.dp))
            SRound(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), 46.dp, onClick = act.onSettings)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(0.3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkinMap(ui, act, Modifier.weight(0.55f).fillMaxWidth())
                SkinMedia(ui, act, Modifier.weight(0.45f).fillMaxWidth(), art = 70.dp)
            }
            SCard(Modifier.weight(0.42f).fillMaxHeight(), onClick = act.onVehicle) {
                val speed = if (ui.hasLocation) ui.speedKmh ?: 0f else 0f
                Canvas(Modifier.fillMaxSize().padding(22.dp)) {
                    val d = minOf(size.width, size.height * 1.5f)
                    val tl = Offset((size.width - d) / 2, 6f)
                    val stroke = Stroke(9.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(k.text.copy(alpha = 0.10f), 150f, 240f, false, tl, Size(d, d), style = stroke)
                    drawArc(Brush.sweepGradient(listOf(k.accent2, k.accent)), 150f, 240f * (speed / 220f).coerceIn(0f, 1f), false, tl, Size(d, d), style = stroke)
                }
                Column(Modifier.align(Alignment.TopCenter).padding(top = 40.dp)) { SpeedText(ui, 72.sp, act) }
                SkinCar(ui, act, Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.82f).fillMaxHeight(0.5f).padding(bottom = 10.dp))
            }
            Column(Modifier.weight(0.28f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SCard(Modifier.weight(0.5f).fillMaxWidth(), onClick = act.onQuick) {
                    val clock = rememberClock(ui.use24h)
                    UiDirection { Column(Modifier.align(Alignment.CenterStart).padding(16.dp)) {
                        Text(clock.time + " " + clock.ampm, style = st(38.sp, FontWeight.Bold), maxLines = 1)
                        Text(clock.dateShort, style = st(14.sp, color = k.dim), maxLines = 1)
                        Spacer(Modifier.height(8.dp))
                        WeatherLine(ui, 22.sp, 28.dp)
                    } }
                }
                SCard(Modifier.weight(0.25f).fillMaxWidth()) { VolumeControl(act, Modifier.align(Alignment.Center).padding(horizontal = 8.dp), compact = true) }
                SCard(Modifier.weight(0.25f).fillMaxWidth()) { BrightnessControl(act, Modifier.align(Alignment.Center).padding(horizontal = 8.dp), compact = true) }
            }
        }
        SCard(Modifier.fillMaxWidth().height(98.dp)) {
            AppStrip(ui, act, Modifier.fillMaxSize().padding(6.dp), icon = 50.dp, label = 13.sp)
        }
    }
}

@Composable
private fun JetourTab(label: String, active: Boolean, onClick: () -> Unit) {
    val k = LocalSkin.current
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        UiDirection { Text(label, style = st(17.sp, if (active) FontWeight.Bold else FontWeight.Medium, if (active) k.text else k.dim), maxLines = 1) }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(28.dp).height(3.dp).clip(CircleShape).background(if (active) k.accent else Color.Transparent))
    }
}

// ============================================================================ BYD — DiLink

/**
 * «BYD» (DiLink-inspired smart EV): a round quick bar on the left, the big map, a column of cards
 * and a wide app dock — clean and light.
 */
@Composable
internal fun BydLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Row(Modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        // Quick bar
        Column(
            Modifier.width(84.dp).fillMaxHeight().clip(RoundedCornerShape(42.dp)).background(k.surface).border(1.dp, k.border, RoundedCornerShape(42.dp)).padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            val clock = rememberClock(ui.use24h)
            Text(clock.time, style = st(18.sp, FontWeight.Bold), maxLines = 1, modifier = Modifier.clickable(onClick = act.onQuick))
            menuItems(act).forEach { m -> SRound(m.icon, m.label, 54.dp, filled = m.special, onClick = m.onClick) }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SkinMap(ui, act, Modifier.weight(0.64f).fillMaxHeight())
                Column(Modifier.weight(0.36f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SkinMedia(ui, act, Modifier.weight(0.42f).fillMaxWidth(), art = 78.dp)
                    SCard(Modifier.weight(0.28f).fillMaxWidth(), onClick = act.onQuick) {
                        UiDirection { Row(Modifier.align(Alignment.CenterStart).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            WeatherLine(ui, 28.sp, 32.dp); Spacer(Modifier.width(12.dp)); HiLo(ui, 12.sp)
                        } }
                    }
                    SCard(Modifier.weight(0.3f).fillMaxWidth(), onClick = act.onVehicle) {
                        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SpeedText(ui, 34.sp, act)
                            SkinCar(ui, act, Modifier.weight(1f).fillMaxHeight(), hint = false)
                        }
                    }
                }
            }
            SCard(Modifier.fillMaxWidth().height(104.dp)) {
                Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppStrip(ui, act, Modifier.weight(1f).fillMaxHeight(), icon = 52.dp, label = 13.sp)
                    Box(Modifier.width(1.dp).fillMaxHeight(0.6f).background(k.border))
                    VolumeControl(act, Modifier.width(220.dp).padding(horizontal = 10.dp), compact = true)
                }
            }
        }
    }
}

// ============================================================================ Haval — Coffee OS

/**
 * «Haval» (Coffee-OS-inspired): the car with lane lines on the left, a tall map, a large player card
 * on the right and a labelled dock with an active indicator.
 */
@Composable
internal fun HavalLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            TopRow(ui, act, Modifier.fillMaxWidth().height(54.dp), timeSize = 28f) {
                UiDirection { Text(carName(ui), style = st(18.sp, FontWeight.Bold), maxLines = 1, modifier = Modifier.padding(end = 18.dp)) }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(listOf(k.accent, Color.Transparent))))
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SCard(Modifier.weight(0.28f).fillMaxHeight(), onClick = act.onVehicle) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    for (s in listOf(-1f, 1f)) drawLine(
                        Brush.verticalGradient(listOf(k.accent.copy(alpha = 0f), k.accent), startY = h * 0.35f, endY = h),
                        Offset(w / 2 + s * w * 0.08f, h * 0.35f), Offset(w / 2 + s * w * 0.45f, h), strokeWidth = 4.dp.toPx(),
                    )
                }
                Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SpeedText(ui, 66.sp, act)
                    SkinCar(ui, act, Modifier.weight(1f).fillMaxWidth(0.92f))
                }
            }
            SkinMap(ui, act, Modifier.weight(0.44f).fillMaxHeight())
            SCard(Modifier.weight(0.28f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                    MediaArt(ui, act, 150.dp, 18.dp)
                    UiDirection { MediaTitle(ui, act, 20.sp, Modifier.fillMaxWidth(), center = true) }
                    MediaBar(ui, Modifier.fillMaxWidth())
                    MediaButtons(ui, act, 58.dp, Modifier.fillMaxWidth())
                }
            }
        }
        SCard(Modifier.fillMaxWidth().height(104.dp)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ui.dock.forEachIndexed { i, s ->
                    if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight(0.5f).background(k.border))
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        AppItem(s, act, Modifier.weight(1f).fillMaxWidth(), 50.dp, 13.sp)
                        Box(Modifier.width(24.dp).height(3.dp).clip(CircleShape).background(if (i == 0) k.accent else Color.Transparent))
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

// ============================================================================ TANK — Off-Road

/**
 * «TANK» (off-road): compass, GPS altitude and speed, the unit's own tilt sensor, coordinates, a
 * topo-toned map and chunky controls.
 */
@Composable
internal fun TankLayout(ui: HomeUi, act: HomeActions) {
    val k = LocalSkin.current
    val ar = LocalArabic.current
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopRow(ui, act, Modifier.fillMaxWidth().height(56.dp), timeSize = 30f) {
            Icon(Icons.Rounded.Terrain, null, tint = k.accent, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            UiDirection { Text(tr("OFF-ROAD", "الطرق الوعرة") + " · " + carName(ui), style = st(18.sp, FontWeight.Bold, k.accent), maxLines = 1, modifier = Modifier.padding(end = 18.dp)) }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(0.3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SCard(Modifier.weight(0.6f).fillMaxWidth()) {
                    Compass(ui.bearing, Modifier.fillMaxSize().padding(12.dp))
                    UiDirection {
                        Text(ui.bearing?.let { "${it.toInt()}° · " + compassLabel(it, ar) } ?: tr("Heading: move to measure", "الاتجاه: يظهر أثناء الحركة"),
                            style = st(13.sp, FontWeight.SemiBold, k.dim), modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp))
                    }
                }
                Row(Modifier.weight(0.4f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TankGauge(Modifier.weight(1f).fillMaxHeight(), ui.altitudeM?.let { "${it.toInt()}" } ?: "--", tr("altitude m · GPS", "الارتفاع م · GPS"))
                    SCard(Modifier.weight(1f).fillMaxHeight(), onClick = act.onVehicle) { Box(Modifier.align(Alignment.Center)) { SpeedText(ui, 40.sp, act) } }
                }
            }
            SkinMap(ui, act, Modifier.weight(0.4f).fillMaxHeight())
            Column(Modifier.weight(0.3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Inclinometer(Modifier.weight(0.5f).fillMaxWidth())
                SCard(Modifier.weight(0.18f).fillMaxWidth(), onClick = act.onQuick) {
                    FixedLtr {
                        Text(
                            if (ui.lat != null && ui.lon != null) String.format(Locale.US, "%.5f° %s  %.5f° %s", abs(ui.lat), if (ui.lat >= 0) "N" else "S", abs(ui.lon), if (ui.lon >= 0) "E" else "W") else "--",
                            style = st(15.sp, FontWeight.SemiBold), maxLines = 1, modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
                SkinMedia(ui, act, Modifier.weight(0.32f).fillMaxWidth(), art = 60.dp)
            }
        }
        AppStrip(ui, act, Modifier.fillMaxWidth().height(100.dp), icon = 52.dp, label = 13.sp, tile = true, spacing = 10.dp)
    }
}

@Composable
private fun TankGauge(modifier: Modifier, value: String, label: String) {
    val k = LocalSkin.current
    SCard(modifier) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = st(38.sp, FontWeight.Bold), maxLines = 1)
            UiDirection { Text(label, style = st(12.sp, color = k.dim), textAlign = TextAlign.Center) }
        }
    }
}

/**
 * Pitch and roll from the head unit's own accelerometer (it is mounted in the dashboard, so this
 * is the car's tilt up to the mounting angle — "zero" stores the level reference). Hidden values
 * ("--") when the unit has no such sensor.
 */
@Composable
private fun Inclinometer(modifier: Modifier) {
    val k = LocalSkin.current
    val ctx = LocalContext.current
    var pitch by remember { mutableStateOf<Float?>(null) }
    var roll by remember { mutableStateOf<Float?>(null) }
    var zeroPitch by rememberSaveable { mutableFloatStateOf(0f) }
    var zeroRoll by rememberSaveable { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val acc = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var fx = 0f; var fy = 0f; var fz = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                fx += 0.15f * (e.values[0] - fx); fy += 0.15f * (e.values[1] - fy); fz += 0.15f * (e.values[2] - fz)
                pitch = Math.toDegrees(atan2(-fx.toDouble(), sqrt((fy * fy + fz * fz).toDouble()))).toFloat()
                roll = Math.toDegrees(atan2(fy.toDouble(), fz.toDouble())).toFloat()
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (acc != null) sm?.registerListener(listener, acc, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }
    val p = pitch?.minus(zeroPitch); val r = roll?.minus(zeroRoll)
    SCard(modifier, onLongClick = { pitch?.let { zeroPitch = it }; roll?.let { zeroRoll = it } }) {
        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TiltDial(r, tr("Roll", "الميلان الجانبي"), Modifier.weight(1f).fillMaxHeight())
            TiltDial(p, tr("Pitch", "الميلان الأمامي"), Modifier.weight(1f).fillMaxHeight())
        }
        UiDirection { Text(tr("unit sensor · hold to zero", "حساس الجهاز · اضغط مطولًا للتصفير"), style = st(10.sp, color = k.dim), modifier = Modifier.align(Alignment.BottomCenter).padding(2.dp)) }
    }
}

@Composable
private fun TiltDial(deg: Float?, label: String, modifier: Modifier) {
    val k = LocalSkin.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(k.border, size.minDimension / 2, style = Stroke(2.dp.toPx()))
                drawLine(k.dim, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1.dp.toPx())
            }
            Box(Modifier.width(54.dp).height(6.dp).rotate(deg ?: 0f).clip(CircleShape).background(if (deg != null && abs(deg) > 20) Color(0xFFE53935) else k.accent))
        }
        Text(deg?.let { "${it.roundToInt()}°" } ?: "--", style = st(20.sp, FontWeight.Bold), maxLines = 1)
        UiDirection { Text(label, style = st(11.sp, color = k.dim), maxLines = 1) }
    }
}
