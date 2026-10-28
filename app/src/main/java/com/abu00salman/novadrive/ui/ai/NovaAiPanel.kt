package com.abu00salman.novadrive.ui.ai

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.data.voice.VoiceState
import com.abu00salman.novadrive.data.voice.VoiceUi
import com.abu00salman.novadrive.data.voice.busy
import com.abu00salman.novadrive.ui.components.UiDirection
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import kotlinx.coroutines.delay

/** What the panel's buttons do (all handled by the voice session). */
class NovaAiActions(
    val onListen: () -> Unit,
    val onStop: () -> Unit,
    val onRetry: () -> Unit,
    val onReplay: () -> Unit,
    val onAsk: (String) -> Unit,
    val onKeyboard: (Boolean) -> Unit,
    val onClose: () -> Unit,
)

private val QUICK_AR = listOf("افتح الخرائط", "شغّل الموسيقى", "أوقف الموسيقى", "التالي", "ارفع الصوت", "اخفض الصوت", "افتح يوتيوب", "الشاشة الرئيسية", "الوضع الليلي", "الوضع النهاري", "الإعدادات")
private val QUICK_EN = listOf("Open maps", "Play music", "Pause", "Next", "Volume up", "Volume down", "Open YouTube", "Home screen", "Night mode", "Day mode", "Settings")

/**
 * Nova AI's conversation card: listening / thinking / doing / speaking animations, the question and
 * the answer, offline indicator, and — when speech can't be used — typing and quick local commands.
 * It sits low and narrow above the dock so maps and controls stay visible, and hides itself after a
 * while when idle (the conversation is kept for follow-ups).
 */
@Composable
fun NovaAiPanel(state: VoiceUi, actions: NovaAiActions, modifier: Modifier = Modifier) {
    if (!state.visible) return
    LaunchedEffect(state.state, state.session, state.showKeyboard, state.reply) {
        if (state.state == VoiceState.IDLE && !state.showKeyboard) { delay(25_000); actions.onClose() }
    }
    Box(modifier.fillMaxSize().padding(bottom = 118.dp), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.widthIn(max = 780.dp).clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xF2121A33), Color(0xF20B1022))))
                .border(1.dp, Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))), RoundedCornerShape(28.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Orb(state.state, state.level)
                Spacer(Modifier.width(16.dp))
                UiDirection {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Nova AI" + (state.provider?.let { " · $it" } ?: tr(" · local", " · محلي")) + "  —  " + statusText(state),
                                style = NovaType.label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Color(0xFFB9C4FF), maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                            )
                            if (!state.online) {
                                Spacer(Modifier.width(8.dp))
                                Row(
                                    Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x33F59E0B)).padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.CloudOff, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("Offline", "دون اتصال"), style = NovaType.small, color = Color(0xFFFBBF24))
                                }
                            }
                        }
                        val heard = if (state.state == VoiceState.LISTENING) state.partial else state.heard
                        heard?.let { Text("«$it»", style = NovaType.body.copy(fontSize = 17.sp), color = Color(0xFFDCE3FF), maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        state.reply?.let {
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.heightIn(max = 112.dp).verticalScroll(rememberScrollState())) {
                                Text(it, style = NovaType.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium), color = Color.White)
                            }
                        }
                        state.notice?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = NovaType.body.copy(fontSize = 15.sp),
                                color = if (state.state == VoiceState.ERROR || state.state == VoiceState.TIMEOUT) Color(0xFFFF8A9A) else Color(0xFFFBBF24), maxLines = 3)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.state.busy) PanelButton(Icons.Rounded.Stop, tr("Stop", "إيقاف"), Color(0xFFEF4444), actions.onStop)
                    if (state.canRetry && !state.state.busy) PanelButton(Icons.Rounded.Refresh, tr("Retry", "إعادة"), Color(0x33FFFFFF), actions.onRetry)
                    if (state.reply != null && !state.state.busy) PanelButton(Icons.AutoMirrored.Rounded.VolumeUp, tr("Repeat", "أعد النطق"), Color(0x33FFFFFF), actions.onReplay)
                    // Always available: pressing it while Nova speaks interrupts and listens again.
                    PanelButton(Icons.Rounded.Mic, tr("Talk", "تحدّث"), Color(0xFF7C3AED), actions.onListen)
                    PanelButton(Icons.Rounded.Keyboard, tr("Type", "كتابة"), if (state.showKeyboard) Color(0xFF3B82F6) else Color(0x33FFFFFF)) { actions.onKeyboard(!state.showKeyboard) }
                    PanelButton(Icons.Rounded.Close, tr("Close", "إغلاق"), Color(0x33FFFFFF), actions.onClose)
                }
            }
            if (state.showKeyboard) {
                Spacer(Modifier.height(10.dp))
                TypeRow(actions.onAsk)
                Spacer(Modifier.height(8.dp))
                val ar = LocalArabic.current
                val quick = if (ar) QUICK_AR else QUICK_EN
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quick.size) { i ->
                        Text(
                            quick[i], style = NovaType.label, color = Color.White,
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x334F46E5))
                                .clickable { actions.onAsk(quick[i]) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeRow(onAsk: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val send = { if (text.isNotBlank()) { onAsk(text); text = "" } }
    UiDirection {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, singleLine = true, modifier = Modifier.weight(1f),
                placeholder = { Text(tr("Type a command or a question", "اكتب أمرًا أو سؤالًا"), color = Color(0x99DCE3FF)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF8B5CF6),
                    unfocusedBorderColor = Color(0x553B82F6), cursorColor = Color.White,
                ),
            )
            Spacer(Modifier.width(10.dp))
            PanelButton(Icons.AutoMirrored.Rounded.Send, tr("Send", "إرسال"), Color(0xFF7C3AED)) { send() }
        }
    }
}

@Composable
private fun statusText(s: VoiceUi): String = when (s.state) {
    VoiceState.LISTENING -> tr("listening…", "يستمع…")
    VoiceState.PROCESSING -> tr("thinking…", "يفكّر…")
    VoiceState.ACTING -> tr("doing: ", "ينفّذ: ") + toolLabel(s.acting)
    VoiceState.SPEAKING -> tr("speaking", "يتحدث")
    VoiceState.ERROR -> tr("problem", "مشكلة")
    VoiceState.TIMEOUT -> tr("timed out", "انتهت المهلة")
    VoiceState.CANCELLED -> tr("stopped", "توقف")
    VoiceState.IDLE -> tr("ready", "جاهز")
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

/** Pulses with the voice while listening, spins while thinking, waves while speaking. */
@Composable
private fun Orb(state: VoiceState, level: Float) {
    val t = rememberInfiniteTransition(label = "orb")
    val pulse by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(if (state == VoiceState.SPEAKING) 380 else 900), RepeatMode.Reverse), label = "pulse")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "spin")
    val voice by animateFloatAsState(level, tween(120), label = "level")
    val scale = when (state) {
        VoiceState.LISTENING -> 1f + voice * 0.22f
        VoiceState.SPEAKING -> pulse
        VoiceState.PROCESSING, VoiceState.ACTING -> 0.96f
        else -> 1f
    }
    val colors = when (state) {
        VoiceState.ERROR, VoiceState.TIMEOUT -> listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFEF4444))
        VoiceState.IDLE, VoiceState.CANCELLED -> listOf(Color(0xFF6366F1), Color(0xFF3B82F6), Color(0xFF6366F1))
        else -> listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF22D3EE), Color(0xFF8B5CF6))
    }
    Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(66.dp).scale(scale)
                .rotate(if (state == VoiceState.PROCESSING || state == VoiceState.ACTING) spin else 0f)
                .clip(CircleShape).background(Brush.sweepGradient(colors)),
        )
        Icon(
            when (state) {
                VoiceState.LISTENING -> Icons.Rounded.Mic
                VoiceState.SPEAKING -> Icons.AutoMirrored.Rounded.VolumeUp
                else -> Icons.Rounded.AutoAwesome
            }, null, tint = Color.White, modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun PanelButton(icon: ImageVector, label: String, bg: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(62.dp)) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Text(label, style = NovaType.small, color = Color(0xFFDCE3FF), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
