package com.abu00salman.novadrive.ui.quick

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.Lang
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.core.ThemeMode
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.IconTile
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr

@Composable
fun QuickPanel(c: AppContainer, s: NovaSettings, onClose: () -> Unit, onSettings: () -> Unit, onSplit: () -> Unit, onVoice: () -> Unit) {
    val p = LocalNova.current
    val activity = LocalContext.current as? Activity
    var volume by remember { mutableFloatStateOf(c.controls.volume()) }
    var bright by remember { mutableFloatStateOf(c.controls.brightness()) }
    val canWrite = remember { c.controls.canWriteSettings() }

    Box(
        Modifier.fillMaxSize().background(p.bgBottom.copy(alpha = 0.55f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
    ) {
        Glass(
            Modifier.align(Alignment.TopCenter).padding(top = 14.dp).widthIn(max = 860.dp).fillMaxWidth(0.82f)
                .clickable(remember { MutableInteractionSource() }, null) { },
            corner = 30.dp
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(tr("Quick controls", "التحكم السريع"), style = NovaType.title, color = p.text)
                Spacer(Modifier.height(16.dp))
                SliderRow(Icons.Rounded.VolumeUp, tr("Media volume", "صوت الوسائط"), volume) {
                    volume = it; c.controls.setVolume(it)
                }
                Spacer(Modifier.height(8.dp))
                SliderRow(Icons.Rounded.BrightnessMedium, tr("Brightness", "السطوع"), bright) {
                    bright = it; c.controls.setBrightness(it, activity?.window)
                }
                if (!canWrite) Text(
                    tr("Tap to allow system-wide brightness ›", "اضغط للسماح بالتحكم في سطوع النظام ›"),
                    style = NovaType.label, color = p.accent,
                    modifier = Modifier.padding(start = 44.dp, bottom = 6.dp).clickable { c.controls.requestWriteSettings() }
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconTile(Icons.Rounded.Wifi, "Wi-Fi") { c.controls.openWifi() }
                    IconTile(Icons.Rounded.Bluetooth, tr("Bluetooth", "بلوتوث")) { c.controls.openBluetooth() }
                    val (themeIcon: ImageVector, themeLabel: String) = when (s.themeMode) {
                        ThemeMode.AUTO -> Icons.Rounded.Brightness4 to tr("Auto", "تلقائي")
                        ThemeMode.LIGHT -> Icons.Rounded.LightMode to tr("Day", "نهاري")
                        ThemeMode.DARK -> Icons.Rounded.DarkMode to tr("Night", "ليلي")
                        ThemeMode.SYSTEM -> Icons.Rounded.Settings to tr("System", "النظام")
                    }
                    IconTile(themeIcon, themeLabel, active = true) {
                        val next = when (s.themeMode) {
                            ThemeMode.AUTO -> ThemeMode.LIGHT; ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.SYSTEM; ThemeMode.SYSTEM -> ThemeMode.AUTO
                        }
                        c.settings.update { it.copy(themeMode = next) }
                    }
                    IconTile(Icons.Rounded.DirectionsCar, tr("Driving mode", "وضع القيادة"), active = s.drivingMode) {
                        c.settings.update { it.copy(drivingMode = !it.drivingMode) }
                    }
                    IconTile(Icons.Rounded.Language, if (s.language == Lang.AR) "English" else "العربية") {
                        c.settings.update { it.copy(language = if (it.language == Lang.AR) Lang.EN else Lang.AR) }
                    }
                    IconTile(Icons.Rounded.Settings, tr("System", "النظام")) { c.controls.openSystemSettings() }
                    IconTile(Icons.Rounded.VerticalSplit, tr("Split", "تقسيم"), onClick = onSplit)
                    IconTile(Icons.Rounded.Mic, tr("Voice", "صوت"), onClick = onVoice)
                    IconTile(Icons.Rounded.Tune, tr("All settings", "كل الإعدادات"), onClick = onSettings)
                }
            }
        }
    }
}

@Composable
private fun SliderRow(icon: ImageVector, label: String, value: Float, onChange: (Float) -> Unit) {
    val p = LocalNova.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, tint = p.accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Slider(
            value = value, onValueChange = onChange, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.text.copy(alpha = 0.12f))
        )
        Spacer(Modifier.width(12.dp))
        Text("${(value * 100).toInt()}%", style = NovaType.body, color = p.textDim, modifier = Modifier.width(52.dp))
    }
}
