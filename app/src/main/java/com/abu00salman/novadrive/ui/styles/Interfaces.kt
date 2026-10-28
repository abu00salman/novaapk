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
internal fun TopRow(
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
        if (voiceLabel) VoicePill(act) else SRound(Icons.Rounded.AutoAwesome, "Nova AI", 48.dp, filled = true, onClick = act.onVoice)
        Spacer(Modifier.width(8.dp))
        SRound(Icons.Rounded.Settings, tr("Settings", "الإعدادات"), 48.dp, onClick = act.onSettings)
    }
}

@Composable
internal fun VoicePill(act: HomeActions, big: Boolean = false) {
    val k = LocalSkin.current
    Row(
        Modifier.height(if (big) 64.dp else 50.dp).clip(k.shape(32.dp)).background(k.accent).clickable(onClick = act.onVoice).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(if (big) 30.dp else 24.dp))
        Spacer(Modifier.width(8.dp))
        UiDirection { Text("Nova AI", style = st(if (big) 22.sp else 17.sp, FontWeight.SemiBold, Color.White), maxLines = 1) }
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
                BigShortcut(ui, act, Modifier.weight(0.8f).fillMaxWidth())
            }
        }
        AppStrip(ui, act, Modifier.fillMaxWidth().height(156.dp), icon = 84.dp, label = 20.sp, tile = true, spacing = 12.dp)
    }
}

/** Compass dial: needle points along the GPS direction of travel. */
@Composable
internal fun Compass(bearing: Float?, modifier: Modifier) {
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
