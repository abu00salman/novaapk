package com.abu00salman.novadrive.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType

/**
 * Glass panel without real-time blur: a translucent navy gradient, a faint diagonal sheen and
 * a lit hairline edge. Real blur (RenderEffect) is far too expensive for the A133's Mali-G31.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = LocalNova.current
    val shape = RoundedCornerShape(corner)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), label = "press"
    )
    val haptics = LocalHapticFeedback.current
    val clickable = onClick != null || onLongClick != null
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(
                if (tint != null) Brush.verticalGradient(listOf(tint.copy(alpha = 0.34f), tint.copy(alpha = 0.16f)))
                else Brush.verticalGradient(listOf(p.glassTop, p.glassBottom))
            )
            .background(Brush.linearGradient(0f to Color.White.copy(alpha = if (p.dark) 0.06f else 0.30f), 0.5f to Color.Transparent))
            .border(1.dp, Brush.verticalGradient(listOf(if (tint != null) tint.copy(alpha = 0.7f) else p.strokeHi, p.stroke)), shape)
            .then(
                if (clickable) Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onClick?.invoke() },
                    onLongClick = onLongClick?.let { lc -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); lc() } },
                ) else Modifier
            ),
        content = content,
    )
}

@Composable
fun IconTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    badge: Color? = null,
    size: Dp = 64.dp,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val p = LocalNova.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Glass(Modifier.size(size), corner = size / 3, tint = if (active) p.accent else null, onClick = onClick, onLongClick = onLongClick) {
            Icon(icon, label, tint = if (active) p.accent else p.text, modifier = Modifier.align(Alignment.Center).size(size * 0.46f))
            if (badge != null) Box(
                Modifier.align(Alignment.TopEnd).padding(9.dp).size(10.dp).clip(CircleShape).background(badge)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(label, style = NovaType.label, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun AppIcon(
    app: AppEntry,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showLabel: Boolean = true,
    starred: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val p = LocalNova.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Glass(Modifier.size(size), corner = size * 0.28f, onClick = onClick, onLongClick = onLongClick) {
            Image(app.icon, app.label, Modifier.align(Alignment.Center).size(size * 0.72f))
            if (starred) Box(
                Modifier.align(Alignment.TopEnd).padding(7.dp).size(9.dp).clip(CircleShape).background(p.accent)
            )
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(app.label, style = NovaType.label, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    Glass(modifier.height(36.dp), corner = 18.dp, tint = color, onClick = onClick) {
        Row(
            Modifier.align(Alignment.Center).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = NovaType.label, color = LocalNova.current.text, maxLines = 1)
        }
    }
}

/** Round transport / action button with an optional accent fill. */
@Composable
fun RoundButton(icon: ImageVector, description: String?, size: Dp, primary: Boolean = false, onClick: () -> Unit) {
    val p = LocalNova.current
    Glass(Modifier.size(size), corner = size / 2, tint = if (primary) p.accent else null, onClick = onClick) {
        Icon(icon, description, tint = if (primary) Color.White else p.text, modifier = Modifier.align(Alignment.Center).size(size * 0.48f))
    }
}
