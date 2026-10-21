package com.abu00salman.novadrive.ui.home

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.CarRepair
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.core.CarModel
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.MediaInfo
import com.abu00salman.novadrive.data.NavInfo
import com.abu00salman.novadrive.data.VehicleData
import com.abu00salman.novadrive.data.Weather
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr

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

@Immutable
data class DashUi(
    /** Null until the first location fix. */
    val speedKmh: Float?,
    val hasLocation: Boolean,
    val vehicle: VehicleData,
    val obdEnabled: Boolean,
    val car: CarModel,
    val paint: Color,
    val weather: Weather?,
    val city: String?,
    val showWeather: Boolean,
    val media: MediaInfo?,
    val notificationAccess: Boolean,
    val nav: NavInfo?,
    val navAppLabel: String?,
    val homeSet: Boolean,
    val workSet: Boolean,
    val dock: List<DockSlot>,
    val use24h: Boolean,
)

class DashActions(
    val onEnableLocation: () -> Unit = {},
    val onVehicle: () -> Unit = {},
    val onOpenNav: () -> Unit = {},
    val onNavCard: () -> Unit = {},
    val onHome: () -> Unit = {},
    val onWork: () -> Unit = {},
    val onGrantAccess: () -> Unit = {},
    val onOpenMedia: () -> Unit = {},
    val onPrev: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onDock: (DockSlot) -> Unit = {},
    val onDockLong: (DockSlot) -> Unit = {},
)

/** Home dashboard: speed · scenic guidance · weather/clock/media, over the app dock. */
@Composable
fun Dashboard(ui: DashUi, act: DashActions, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            SpeedPanel(ui, act, Modifier.weight(0.29f).fillMaxHeight())
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(0.37f).fillMaxHeight().clip(RoundedCornerShape(24.dp))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.04f))), RoundedCornerShape(24.dp))
            ) {
                ScenicBackdrop(Modifier.fillMaxSize())
                NavCard(
                    ui.nav, ui.notificationAccess, ui.navAppLabel, ui.homeSet, ui.workSet,
                    onCard = act.onNavCard, onOpenNav = act.onOpenNav, onGrantAccess = act.onGrantAccess,
                    onHome = act.onHome, onWork = act.onWork,
                    modifier = Modifier.align(Alignment.TopCenter).padding(14.dp).fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(0.34f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().weight(0.36f)) {
                    WeatherCard(ui.weather, ui.city, ui.showWeather, Modifier.weight(0.52f).fillMaxHeight())
                    Spacer(Modifier.width(12.dp))
                    ClockCard(ui.use24h, Modifier.weight(0.48f).fillMaxHeight())
                }
                Spacer(Modifier.height(12.dp))
                MediaCard(
                    ui.media, ui.notificationAccess,
                    onOpenApp = act.onOpenMedia, onGrantAccess = act.onGrantAccess,
                    onPrev = act.onPrev, onPlayPause = act.onPlayPause, onNext = act.onNext,
                    modifier = Modifier.weight(0.64f).fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Dock(ui.dock, act, Modifier.fillMaxWidth().height(128.dp))
    }
}

@Composable
private fun SpeedPanel(ui: DashUi, act: DashActions, modifier: Modifier) {
    val p = LocalNova.current
    val v = ui.vehicle
    val live = ui.obdEnabled && v.state == VehicleData.State.LIVE
    Glass(modifier) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val dial = minOf(maxWidth, maxHeight)
                Box(Modifier.size(dial), contentAlignment = Alignment.Center) {
                    Speedometer(ui.speedKmh ?: 0f, ui.car, ui.paint, Modifier.fillMaxSize())
                    Column(Modifier.offset(y = -dial * 0.10f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            ui.speedKmh?.toInt()?.toString() ?: "--",
                            style = NovaType.hero.copy(fontSize = (dial.value * 0.25f).sp), color = p.text, maxLines = 1
                        )
                        Text("km/h", style = NovaType.title.copy(fontWeight = FontWeight.Medium), color = p.textDim)
                    }
                    if (!ui.hasLocation) Pill(
                        tr("Enable GPS", "فعّل الموقع"), p.danger, icon = Icons.Rounded.GpsNotFixed,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = dial * 0.18f), onClick = act.onEnableLocation
                    )
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = act.onVehicle).padding(vertical = 4.dp)) {
                Metric(Icons.Rounded.LocalGasStation, if (live) v.fuelPct?.let { "$it%" } ?: "—" else "—", tr("Fuel level", "مستوى الوقود"), Modifier.weight(1f))
                Metric(
                    Icons.Rounded.CarRepair,
                    if (!live) "—" else when (v.mil) { true -> tr("Check", "تحقق"); false -> tr("Good", "سليم"); null -> "—" },
                    tr("Engine", "حالة المحرك"), Modifier.weight(1f),
                    valueColor = if (live && v.mil == true) p.warn else if (live && v.mil == false) p.ok else null,
                )
                Metric(Icons.Rounded.Thermostat, if (live) v.coolantC?.let { "$it°C" } ?: "—" else "—", tr("Coolant", "حرارة المحرك"), Modifier.weight(1f))
            }
            if (!live) Text(
                if (ui.obdEnabled) tr("OBD-II connecting…", "جارٍ الاتصال بـ OBD-II…") else tr("Connect an OBD-II adapter for engine data", "صِل محول OBD-II لعرض بيانات المحرك"),
                style = NovaType.small, color = p.textDim, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun Metric(icon: ImageVector, value: String, label: String, modifier: Modifier, valueColor: Color? = null) {
    val p = LocalNova.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = p.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text(value, style = NovaType.title.copy(fontSize = 18.sp), color = valueColor ?: p.text, maxLines = 1)
        }
        Text(label, style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Dock(slots: List<DockSlot>, act: DashActions, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        slots.forEach { s -> DockTile(s, { act.onDock(s) }, { act.onDockLong(s) }, Modifier.weight(1f).fillMaxHeight()) }
    }
}

@Composable
private fun DockTile(s: DockSlot, onClick: () -> Unit, onLong: () -> Unit, modifier: Modifier) {
    val p = LocalNova.current
    val available = s.app != null || s.builtIn
    Glass(modifier, corner = 22.dp, onClick = onClick, onLongClick = onLong) {
        Column(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val app = s.app
            if (app != null) {
                Image(app.icon, null, Modifier.size(58.dp))
            } else {
                Box(
                    Modifier.size(58.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (available) Brush.linearGradient(listOf(Color(0xFF5B7DB8), Color(0xFF2B3E68))) else Brush.linearGradient(listOf(p.text.copy(alpha = 0.10f), p.text.copy(alpha = 0.05f))))
                        .alpha(if (available) 1f else 0.55f),
                    contentAlignment = Alignment.Center
                ) { Icon(s.fallback, null, tint = if (available) Color.White else p.textDim, modifier = Modifier.size(32.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(s.title, style = NovaType.label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = if (available) p.text else p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    app != null -> app.label
                    s.builtIn -> " "
                    s.storePackage != null -> tr("Tap to install", "اضغط للتثبيت")
                    else -> tr("Not installed", "غير مثبت")
                },
                style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}
