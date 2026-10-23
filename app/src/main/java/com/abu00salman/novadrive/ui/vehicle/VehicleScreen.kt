package com.abu00salman.novadrive.ui.vehicle

import com.abu00salman.novadrive.core.carDisplayName
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CarRepair
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.core.CarBrand
import com.abu00salman.novadrive.core.CarModel
import com.abu00salman.novadrive.core.CarPaint
import com.abu00salman.novadrive.data.VehicleData
import com.abu00salman.novadrive.ui.components.CarImage
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import java.util.Locale

/**
 * Vehicle information. Every number comes from the OBD-II adapter; anything the adapter or
 * car doesn't report is shown as unavailable rather than estimated.
 */
@Composable
fun VehicleScreen(
    brand: CarBrand,
    model: CarModel,
    paint: CarPaint,
    obdEnabled: Boolean,
    data: VehicleData,
    onSetup: () -> Unit,
    carImageVersion: Long = 0,
    modifier: Modifier = Modifier,
) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val live = obdEnabled && data.state == VehicleData.State.LIVE
    val unavailable = tr("Unavailable", "غير متاح")
    val unsupported = tr("Not reported", "غير مدعوم")
    fun v(x: Any?, fmt: (Any) -> String = { it.toString() }) = if (!live) unavailable else x?.let(fmt) ?: unsupported

    Row(modifier.fillMaxSize()) {
        Column(Modifier.weight(0.28f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(Icons.Rounded.Speed, tr("Speed (OBD)", "السرعة (OBD)"), v(data.speedKmh) { "$it" }, "km/h", live && data.speedKmh != null, Modifier.weight(1f))
            MetricCard(Icons.Rounded.TrendingUp, tr("Engine speed", "دوران المحرك"), v(data.rpm) { "$it" }, "RPM", live && data.rpm != null, Modifier.weight(1f))
            MetricCard(Icons.Rounded.CarRepair, tr("Engine load", "حمل المحرك"), v(data.loadPct) { "$it%" }, null, live && data.loadPct != null, Modifier.weight(1f))
        }
        Spacer(Modifier.width(12.dp))
        Glass(Modifier.weight(0.44f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(carDisplayName(brand, model, ar), style = NovaType.title.copy(fontSize = 22.sp), color = p.text, maxLines = 1)
                Text(if (ar) paint.ar else paint.en, style = NovaType.label, color = p.textDim)
                Spacer(Modifier.height(6.dp))
                StatusChip(obdEnabled, data)
                CarImage(carImageVersion, Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp))
                if (!live) Pill(tr("Set up OBD-II", "إعداد OBD-II"), p.accent, icon = Icons.Rounded.Settings, onClick = onSetup)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(0.28f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(Icons.Rounded.Thermostat, tr("Coolant temperature", "حرارة المحرك"), v(data.coolantC) { "$it°" }, "C", live && data.coolantC != null, Modifier.weight(1f))
            MetricCard(Icons.Rounded.BatteryChargingFull, tr("Battery", "البطارية"), v(data.voltage) { String.format(Locale.US, "%.1f", it as Float) }, "V", live && data.voltage != null, Modifier.weight(1f))
            MetricCard(Icons.Rounded.LocalGasStation, tr("Fuel level", "مستوى الوقود"), v(data.fuelPct) { "$it%" }, null, live && data.fuelPct != null, Modifier.weight(1f))
            val diag = when {
                !live -> unavailable
                data.mil == null -> unsupported
                data.mil == true -> tr("Check engine", "فحص المحرك") + (data.dtcCount?.takeIf { it > 0 }?.let { " · $it" } ?: "")
                else -> tr("No faults", "لا أعطال")
            }
            MetricCard(
                Icons.Rounded.CarRepair, tr("Diagnostics", "التشخيص"), diag, null, live && data.mil != null, Modifier.weight(1f),
                valueColor = if (live && data.mil == true) p.warn else if (live && data.mil == false) p.ok else null,
            )
        }
    }
}

@Composable
private fun StatusChip(enabled: Boolean, d: VehicleData) {
    val p = LocalNova.current
    val (color, text) = when {
        !enabled -> p.textDim to tr("OBD-II off", "OBD-II متوقف")
        d.state == VehicleData.State.LIVE -> p.ok to tr("OBD-II connected", "OBD-II متصل")
        d.state == VehicleData.State.CONNECTING -> p.warn to tr("Connecting…", "جارٍ الاتصال…")
        d.state == VehicleData.State.ERROR -> p.danger to (d.error ?: tr("Connection error", "خطأ في الاتصال"))
        else -> p.textDim to tr("OBD-II off", "OBD-II متوقف")
    }
    Row(
        Modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Bluetooth, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = NovaType.small, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector, label: String, value: String, unit: String?, real: Boolean, modifier: Modifier, valueColor: Color? = null,
) {
    val p = LocalNova.current
    Glass(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(p.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = p.accent, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = NovaType.label, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value, style = if (real) NovaType.display.copy(fontSize = 28.sp) else NovaType.title.copy(fontSize = 17.sp),
                        color = valueColor ?: if (real) p.text else p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (real && unit != null) Text(" $unit", style = NovaType.label, color = p.textDim, modifier = Modifier.padding(bottom = 5.dp))
                }
            }
        }
    }
}
