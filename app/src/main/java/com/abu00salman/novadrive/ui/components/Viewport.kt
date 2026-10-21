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
fun DesignViewport(designW: Dp = 1280.dp, designH: Dp = 720.dp, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = minOf(maxWidth / designW, maxHeight / designH).coerceIn(0.4f, 3f)
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density * scale, d.fontScale.coerceIn(0.85f, 1.15f))) {
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
