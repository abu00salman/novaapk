package com.abu00salman.novadrive.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.abu00salman.novadrive.ui.theme.LocalArabic

/**
 * Lays the UI out on a fixed 1280×720 design grid and scales it to the real display, so the
 * composition is identical on every head-unit density (160/213/240 dpi, 1024×600, 1280×720…).
 * The longer axis keeps any extra room, which the weighted layouts absorb.
 */
@Composable
fun DesignViewport(designW: Dp = 1280.dp, designH: Dp = 720.dp, textScale: Float = 1f, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = minOf(maxWidth / designW, maxHeight / designH).coerceIn(0.4f, 3f)
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density * scale, d.fontScale.coerceIn(0.85f, 1.15f) * textScale.coerceIn(0.8f, 1.6f))) {
            Box(Modifier.requiredSize(maxWidth / scale, maxHeight / scale)) { content() }
        }
    }
}

/**
 * The dashboard keeps one physical arrangement in both languages (driver-side controls on the
 * left, as in left-hand-drive cars); text inside each panel still follows the UI language.
 */
@Composable
fun FixedLtr(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)

@Composable
fun UiDirection(content: @Composable () -> Unit) =
    CompositionLocalProvider(
        LocalLayoutDirection provides if (LocalArabic.current) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )

/** User icon-size multiplier (Settings › Appearance) for app icons in the dock, library and rail. */
val LocalIconScale = androidx.compose.runtime.staticCompositionLocalOf { 1f }

/** Optional colour theme for app icons (null = the apps' original colours). */
val LocalIconTint = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.ui.graphics.Color?> { null }

/** Icon colour choices offered in Settings › Appearance (index 0 = original colours). */
val IconTints = listOf(
    null,
    androidx.compose.ui.graphics.Color(0xFF3D8BFF),
    androidx.compose.ui.graphics.Color(0xFF2EE6C0),
    androidx.compose.ui.graphics.Color(0xFF8B5CFF),
    androidx.compose.ui.graphics.Color(0xFFE8C872),
    androidx.compose.ui.graphics.Color(0xFFFF4D6D),
    androidx.compose.ui.graphics.Color(0xFFECEFF5),
)

/**
 * Duotone filter: keeps each icon's shape and detail (luminance) but recolours it with the chosen
 * tint, so every app icon matches. Null when original colours are selected.
 */
@Composable
fun iconFilter(): androidx.compose.ui.graphics.ColorFilter? {
    val t = LocalIconTint.current ?: return null
    val r = t.red; val g = t.green; val b = t.blue
    val k = 1.25f
    return androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
        0.299f * r * k, 0.587f * r * k, 0.114f * r * k, 0f, 0f,
        0.299f * g * k, 0.587f * g * k, 0.114f * g * k, 0f, 0f,
        0.299f * b * k, 0.587f * b * k, 0.114f * b * k, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )))
}
