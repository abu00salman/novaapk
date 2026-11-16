package com.abu00salman.novadrive.ui.home

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
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.RoundButton
import com.abu00salman.novadrive.ui.components.UiDirection
import com.abu00salman.novadrive.ui.styles.rememberClock
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr

/**
 * NovaDrive's side of a split screen (Google Maps, Waze or YouTube on the other side): time, GPS speed,
 * the navigation app's own turn instruction, the player and large shortcuts — in one tall column that
 * stays readable at half width.
 */
@Composable
fun CompactLayout(ui: HomeUi, act: HomeActions, modifier: Modifier = Modifier) {
    val p = LocalNova.current
    val clock = rememberClock(ui.use24h)
    UiDirection {
        Column(modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Time · weather · speed
            Glass(Modifier.fillMaxWidth().height(132.dp), onClick = act.onQuick) {
                Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(clock.time, style = NovaType.display.copy(fontSize = 46.sp, fontWeight = FontWeight.SemiBold), color = p.text, maxLines = 1)
                            if (clock.ampm.isNotEmpty()) Text(" " + clock.ampm, style = NovaType.title, color = p.textDim, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Text(clock.dateShort + (ui.weather?.takeIf { ui.showWeather }?.let { "  ·  ${Math.round(it.tempC)}°" } ?: ""),
                            style = NovaType.label, color = p.textDim, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (ui.hasLocation) ui.speedKmh?.toInt()?.toString() ?: "--" else "--", style = NovaType.display.copy(fontSize = 52.sp, fontWeight = FontWeight.Bold), color = p.text, maxLines = 1)
                        Text(tr("km/h · GPS", "كم/س · GPS"), style = NovaType.small, color = p.textDim)
                    }
                }
            }
            // The navigation app's own next turn (from its notification), or a way back to it.
            val nav = ui.nav
            Glass(Modifier.fillMaxWidth().height(126.dp), onClick = if (nav != null) act.onNavCard else act.onOpenNav) {
                Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    val icon = nav?.maneuver
                    if (icon != null) Image(icon, null, Modifier.size(58.dp))
                    else Icon(if (nav != null) Icons.Rounded.TurnRight else Icons.Rounded.Map, null, tint = p.accent, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(nav?.primary ?: tr("No active route", "لا يوجد توجيه نشط"), style = NovaType.title.copy(fontSize = if (nav != null) 28.sp else 20.sp, fontWeight = FontWeight.Bold),
                            color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(nav?.secondary ?: tr("Open ", "افتح ") + (ui.navApp?.label ?: tr("maps", "الخرائط")), style = NovaType.body, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        nav?.eta?.let { Text(it, style = NovaType.small, color = p.accent, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
            // Player
            val m = ui.media
            Glass(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Artwork(m, 96.dp, 18.dp, act.onOpenMedia)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m?.title ?: tr("Nothing playing", "لا يوجد تشغيل"), style = NovaType.title.copy(fontSize = 22.sp), color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(m?.artist ?: tr("Tap to open music", "اضغط لفتح الموسيقى"), style = NovaType.body, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    com.abu00salman.novadrive.ui.components.FixedLtr {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            RoundButton(Icons.Rounded.SkipPrevious, tr("Previous", "السابق"), 64.dp, onClick = act.onPrev)
                            RoundButton(if (m?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, tr("Play / pause", "تشغيل / إيقاف"), 80.dp, primary = true, onClick = act.onPlayPause)
                            RoundButton(Icons.Rounded.SkipNext, tr("Next", "التالي"), 64.dp, onClick = act.onNext)
                        }
                    }
                }
            }
            // Large shortcuts
            com.abu00salman.novadrive.ui.components.FixedLtr {
                Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactAction(Icons.Rounded.Mic, com.abu00salman.novadrive.ui.components.voiceName(), Modifier.weight(1f), act.onVoice)
                    CompactAction(Icons.Rounded.Phone, tr("Phone", "الهاتف"), Modifier.weight(1f), act.onPhone)
                    CompactAction(Icons.Rounded.MusicNote, tr("Music", "الموسيقى"), Modifier.weight(1f), act.onOpenMedia)
                    CompactAction(Icons.Rounded.Apps, tr("Apps", "التطبيقات"), Modifier.weight(1f), act.onApps)
                }
            }
        }
    }
}

@Composable
private fun CompactAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalNova.current
    Glass(modifier.fillMaxSize(), corner = 20.dp, onClick = onClick) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, tint = p.text, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(4.dp))
            UiDirection { Text(label, style = NovaType.label, color = p.text, maxLines = 1) }
        }
    }
}
