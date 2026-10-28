package com.abu00salman.novadrive.ui.ai

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.data.ai.AiStatus
import com.abu00salman.novadrive.data.ai.AiUi
import com.abu00salman.novadrive.ui.components.UiDirection
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr

/** What Nova AI is doing right now: listening, thinking, acting, speaking — with stop and close. */
@Composable
fun NovaAiPanel(state: AiUi, onListen: () -> Unit, onStop: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    if (!state.visible) return
    val busy = state.status == AiStatus.THINKING || state.status == AiStatus.ACTING || state.status == AiStatus.SPEAKING
    Box(modifier.fillMaxSize().padding(bottom = 26.dp), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier.widthIn(max = 820.dp).clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(Color(0xF2121A33), Color(0xF20B1022))))
                .border(1.dp, Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))), RoundedCornerShape(30.dp))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Orb(state.status)
            Spacer(Modifier.width(18.dp))
            UiDirection {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Nova AI" + (state.engine?.let { " · $it" } ?: "") + "  —  " + statusText(state),
                        style = NovaType.label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Color(0xFFB9C4FF), maxLines = 1,
                    )
                    state.heard?.let { Text("«$it»", style = NovaType.body.copy(fontSize = 17.sp), color = Color(0xFFDCE3FF), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    state.reply?.let {
                        Spacer(Modifier.padding(top = 4.dp))
                        Text(it, style = NovaType.title.copy(fontSize = 21.sp, fontWeight = FontWeight.Medium),
                            color = if (state.status == AiStatus.ERROR) Color(0xFFFF8A9A) else Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (busy) PanelButton(Icons.Rounded.Stop, tr("Stop", "إيقاف"), Color(0xFFEF4444), onStop)
                else PanelButton(Icons.Rounded.Mic, tr("Talk", "تحدّث"), Color(0xFF7C3AED), onListen)
                PanelButton(Icons.Rounded.Close, tr("Close", "إغلاق"), Color(0x33FFFFFF), onClose)
            }
        }
    }
}

@Composable
private fun statusText(s: AiUi): String = when (s.status) {
    AiStatus.LISTENING -> tr("listening…", "يستمع…")
    AiStatus.THINKING -> tr("thinking…", "يفكّر…")
    AiStatus.ACTING -> tr("doing: ", "ينفّذ: ") + toolLabel(s.acting)
    AiStatus.SPEAKING -> tr("speaking", "يتحدث")
    AiStatus.ERROR -> tr("problem", "مشكلة")
    AiStatus.IDLE -> tr("ready", "جاهز")
}

@Composable
private fun toolLabel(name: String?): String = when (name) {
    "open_app" -> tr("opening an app", "فتح تطبيق")
    "media_control", "play_media", "now_playing" -> tr("music", "الموسيقى")
    "set_volume" -> tr("volume", "الصوت")
    "set_brightness" -> tr("brightness", "السطوع")
    "navigate_to", "navigate_nearby" -> tr("starting navigation", "بدء الملاحة")
    "navigation_status", "stop_navigation" -> tr("navigation", "الملاحة")
    "switch_interface", "set_theme", "open_screen" -> tr("NovaDrive", "NovaDrive")
    "get_weather" -> tr("weather", "الطقس")
    "get_news" -> tr("news", "الأخبار")
    "call_contact" -> tr("contacts", "جهات الاتصال")
    else -> name ?: ""
}

@Composable
private fun Orb(status: AiStatus) {
    val t = rememberInfiniteTransition(label = "orb")
    val pulse by t.animateFloat(0.9f, 1.08f, infiniteRepeatable(tween(if (status == AiStatus.LISTENING) 500 else 900), RepeatMode.Reverse), label = "pulse")
    val active = status != AiStatus.IDLE && status != AiStatus.ERROR
    Box(
        Modifier.size(66.dp).scale(if (active) pulse else 1f).clip(CircleShape)
            .background(Brush.sweepGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF22D3EE), Color(0xFF8B5CF6)))),
        contentAlignment = Alignment.Center,
    ) { Icon(if (status == AiStatus.LISTENING) Icons.Rounded.Mic else Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
}

@Composable
private fun PanelButton(icon: ImageVector, label: String, bg: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(60.dp).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Text(label, style = NovaType.small, color = Color(0xFFDCE3FF), maxLines = 1)
    }
}
