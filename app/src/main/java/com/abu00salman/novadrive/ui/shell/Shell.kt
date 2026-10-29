package com.abu00salman.novadrive.ui.shell

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.R
import com.abu00salman.novadrive.core.HomeMode
import com.abu00salman.novadrive.core.ThemeMode
import com.abu00salman.novadrive.data.Link
import com.abu00salman.novadrive.data.NetState
import com.abu00salman.novadrive.ui.components.DesignViewport
import com.abu00salman.novadrive.ui.components.FixedLtr
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.LocalIconScale
import com.abu00salman.novadrive.ui.components.LocalIconTint
import androidx.compose.runtime.CompositionLocalProvider
import com.abu00salman.novadrive.ui.components.UiDirection
import com.abu00salman.novadrive.ui.home.uiLocale
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date

enum class Screen { HOME, NAV, MUSIC, VEHICLE, APPS, SETTINGS }

/** Real-time "now", ticking on the minute boundary (no per-second recomposition). */
@Composable
fun rememberMinuteClock(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(60_000 - value % 60_000) }
    }
    return now
}

/** Rail + status bar + routed content on the scaled 1280×720 grid, with overlays on top. */
@Composable
fun ShellLayout(
    rail: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
    overlay: @Composable () -> Unit = {},
    railRight: Boolean = false,
    textScale: Float = 1f,
    iconScale: Float = 1f,
    iconTint: Color? = null,
    /** False for interfaces that draw their own menus and status bar edge to edge (Tesla). */
    chrome: Boolean = true,
    content: @Composable () -> Unit,
) {
    DesignViewport(textScale = textScale) {
      CompositionLocalProvider(LocalIconScale provides iconScale.coerceIn(0.8f, 1.6f), LocalIconTint provides iconTint) {
        FixedLtr {
            if (!chrome) Box(Modifier.fillMaxSize()) { UiDirection(content) }
            else Row(Modifier.fillMaxSize().padding(14.dp)) {
                if (!railRight) { rail(); Spacer(Modifier.width(12.dp)) }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    topBar()
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.weight(1f)) { UiDirection(content) }
                }
                if (railRight) { Spacer(Modifier.width(12.dp)); rail() }
            }
            UiDirection(overlay)
        }
      }
    }
}

// ---------------------------------------------------------------- side rail

@Composable
fun Rail(
    current: Screen,
    mode: HomeMode,
    onSelect: (Screen) -> Unit,
    onMode: (HomeMode) -> Unit,
    onPhone: () -> Unit,
    onVoice: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val home = current == Screen.HOME
    Glass(modifier.width(100.dp).fillMaxHeight(), corner = 28.dp) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 10.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RailItem(Icons.Rounded.Home, tr("Home", "الرئيسية"), home && (mode == HomeMode.CAR || mode == HomeMode.SPLIT)) { onMode(HomeMode.CAR) }
            RailItem(Icons.Rounded.Navigation, tr("Navigation", "الملاحة"), home && mode == HomeMode.NAVIGATION || current == Screen.NAV) { onMode(HomeMode.NAVIGATION) }
            RailItem(Icons.Rounded.MusicNote, tr("Music", "الموسيقى"), home && mode == HomeMode.MUSIC || current == Screen.MUSIC) { onMode(HomeMode.MUSIC) }
            RailItem(Icons.Rounded.Phone, tr("Phone", "الهاتف"), false, onClick = onPhone)
            RailItem(com.abu00salman.novadrive.ui.components.voiceIcon(), com.abu00salman.novadrive.ui.components.voiceName(), false, special = true, onClick = onVoice)
            RailItem(Icons.Rounded.DirectionsCar, tr("Vehicle", "السيارة"), current == Screen.VEHICLE) { onSelect(Screen.VEHICLE) }
            RailItem(Icons.Rounded.Apps, tr("Apps", "التطبيقات"), current == Screen.APPS) { onSelect(Screen.APPS) }
            RailItem(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), current == Screen.SETTINGS) { onSelect(Screen.SETTINGS) }
        }
    }
}

@Composable
private fun RailItem(icon: ImageVector, label: String, active: Boolean, special: Boolean = false, onClick: () -> Unit) {
    val p = LocalNova.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.width(84.dp).height(78.dp).clip(shape)
            .then(
                if (special && !active) Modifier
                    .background(Brush.linearGradient(listOf(p.accent2.copy(alpha = 0.85f), p.accent.copy(alpha = 0.85f))))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
                else if (active) Modifier
                    .background(Brush.verticalGradient(listOf(p.accent.copy(alpha = 0.95f), p.accent.copy(alpha = 0.55f))))
                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), p.accent.copy(alpha = 0.2f))), shape)
                else Modifier
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (active || special) Color.White else p.text.copy(alpha = 0.92f), modifier = Modifier.size(30.dp * LocalIconScale.current.coerceAtMost(1.25f)))
        Spacer(Modifier.height(6.dp))
        Text(label, style = NovaType.small.copy(fontSize = 13.sp), color = if (active || special) Color.White else p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------- status bar

enum class GpsState { OFF, SEARCHING, FIX }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopBar(
    bt: Link,
    net: NetState,
    gps: GpsState,
    dark: Boolean,
    themeMode: ThemeMode,
    use24h: Boolean,
    onBluetooth: () -> Unit,
    onWifi: () -> Unit,
    onGps: () -> Unit,
    onToggleTheme: () -> Unit,
    onAutoTheme: () -> Unit,
    onQuick: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val now = rememberMinuteClock()
    val loc = uiLocale(ar)
    val time = remember(now, use24h, ar) {
        SimpleDateFormat(if (use24h) "HH:mm" else "h:mm", loc).format(Date(now)) +
            if (use24h) "" else if (ar) (if (SimpleDateFormat("a", java.util.Locale.US).format(Date(now)) == "PM") " م" else " ص")
            else " " + SimpleDateFormat("a", java.util.Locale.US).format(Date(now))
    }
    val date = remember(now, ar) { SimpleDateFormat(if (ar) "EEEE d MMMM yyyy" else "EEEE, d MMMM yyyy", loc).format(Date(now)) }

    Row(modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.nova_logo), null, Modifier.size(40.dp, 34.dp).clickable(onClick = onQuick))
        Spacer(Modifier.width(12.dp))
        Text("NovaDrive", style = NovaType.title.copy(fontSize = 25.sp, fontWeight = FontWeight.SemiBold), color = p.text)
        Spacer(Modifier.weight(1f))
        StatusGlyph(
            when (bt) { Link.CONNECTED -> Icons.Rounded.BluetoothConnected; Link.ON -> Icons.Rounded.Bluetooth; Link.OFF -> Icons.Rounded.BluetoothDisabled },
            if (bt == Link.CONNECTED) p.accent else if (bt == Link.ON) p.text else p.textDim.copy(alpha = 0.6f),
            tr("Bluetooth", "بلوتوث"), onBluetooth,
        )
        StatusGlyph(
            if (net.wifi == Link.OFF) Icons.Rounded.WifiOff else Icons.Rounded.Wifi,
            if (net.wifi == Link.CONNECTED) p.text else p.textDim.copy(alpha = 0.6f),
            "Wi-Fi", onWifi,
        )
        if (net.cellular) StatusGlyph(Icons.Rounded.SignalCellularAlt, p.text, tr("Mobile data", "بيانات الجوال"), onWifi)
        StatusGlyph(
            when (gps) { GpsState.FIX -> Icons.Rounded.GpsFixed; GpsState.SEARCHING -> Icons.Rounded.GpsNotFixed; GpsState.OFF -> Icons.Rounded.LocationOff },
            when (gps) { GpsState.FIX -> p.ok; GpsState.SEARCHING -> p.text; GpsState.OFF -> p.danger },
            "GPS", onGps,
        )
        Spacer(Modifier.width(10.dp))
        Text(time, style = NovaType.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium), color = p.text, maxLines = 1)
        Spacer(Modifier.width(14.dp))
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(p.text.copy(alpha = 0.08f))
                .combinedClickable(remember { MutableInteractionSource() }, null, onClick = onToggleTheme, onLongClick = onAutoTheme),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                when { themeMode == ThemeMode.AUTO -> Icons.Rounded.BrightnessAuto; dark -> Icons.Rounded.DarkMode; else -> Icons.Rounded.LightMode },
                tr("Day / night", "نهاري / ليلي"), tint = p.text, modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        StatusGlyph(Icons.Rounded.Mic, p.text, tr("Voice", "صوت"), onVoice)
        StatusGlyph(Icons.Rounded.Tune, p.text, tr("Quick controls", "التحكم السريع"), onQuick)
        Spacer(Modifier.weight(1f))
        Text(date, style = NovaType.title.copy(fontSize = 19.sp, fontWeight = FontWeight.Medium), color = p.text, maxLines = 1)
    }
}

@Composable
private fun StatusGlyph(icon: ImageVector, tint: Color, description: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(24.dp))
    }
}
