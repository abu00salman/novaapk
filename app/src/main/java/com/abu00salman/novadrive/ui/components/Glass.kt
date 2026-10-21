package com.abu00salman.novadrive.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType

/**
 * "Glass" without real-time blur: layered translucent gradients + a lit top edge.
 * Real blur (RenderEffect) is far too expensive for the Mali GPU in the A133.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    corner: Dp = 26.dp,
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
        if (pressed) 0.955f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "press"
    )
    val haptics = LocalHapticFeedback.current
    val clickable = onClick != null || onLongClick != null
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(
                if (tint != null) Brush.verticalGradient(listOf(tint.copy(alpha = 0.32f), tint.copy(alpha = 0.14f)))
                else Brush.verticalGradient(listOf(p.glassTop, p.glassBottom))
            )
            .background(Brush.linearGradient(0f to Color.White.copy(alpha = if (p.dark) 0.10f else 0.35f), 0.45f to Color.Transparent, 1f to Color.Transparent))
            .border(1.dp, Brush.verticalGradient(listOf(p.strokeHi, p.stroke)), shape)
            .then(
                if (clickable) Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
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
        Glass(Modifier.size(size), corner = size / 3, onClick = onClick, onLongClick = onLongClick) {
            Image(app.icon, app.label, Modifier.align(Alignment.Center).size(size * 0.7f))
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
    Glass(modifier.height(34.dp), corner = 17.dp, tint = color, onClick = onClick) {
        androidx.compose.foundation.layout.Row(
            Modifier.align(Alignment.Center).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
            }
            Text(text, style = NovaType.label, color = LocalNova.current.text, maxLines = 1)
        }
    }
}

/** iOS-style app glyph: gradient squircle, white symbol, lit top edge. */
@Composable
fun GlyphTile(
    icon: ImageVector,
    label: String,
    from: Color,
    to: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    badge: Color? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val p = LocalNova.current
    val shape = RoundedCornerShape(size * 0.30f)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.90f else 1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "glyph")
    val haptics = LocalHapticFeedback.current
    Column(modifier.graphicsLayer { scaleX = scale; scaleY = scale }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(size).clip(shape)
                .background(Brush.linearGradient(listOf(from, to)))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), endY = size.value * 2.2f))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.06f))), shape)
                .combinedClickable(
                    interactionSource = interaction, indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick?.let { lc -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); lc() } },
                )
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(size * 0.50f))
            if (badge != null) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(9.dp).clip(CircleShape).background(badge).border(1.5.dp, Color.White, CircleShape))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, style = NovaType.label.copy(fontSize = 11.sp), color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
