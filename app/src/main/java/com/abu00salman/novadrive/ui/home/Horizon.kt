package com.abu00salman.novadrive.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.abu00salman.novadrive.ui.theme.LocalNova
import kotlin.math.pow

/**
 * NOVA "Horizon": a stylised perspective road whose lane markings flow at a rate driven by
 * real GPS speed. Everything is vector drawing in a single Canvas; the per-frame phase is read
 * only inside the draw lambda, so animation never triggers recomposition.
 */
@Composable
fun HorizonCanvas(speedKmh: Float, modifier: Modifier = Modifier) {
    val p = LocalNova.current
    val smoothSpeed by animateFloatAsState(speedKmh, tween(900), label = "speed")
    val speedRef = rememberUpdatedState(smoothSpeed)
    val phase = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (last != 0L) {
                    val dt = (t - last) / 1_000_000_000f
                    // idle drift when parked, otherwise proportional to speed
                    val rate = 0.12f + speedRef.value / 55f
                    phase.floatValue = (phase.floatValue + dt * rate) % 1f
                }
                last = t
            }
        }
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.40f
        val cx = w * 0.5f
        val ph = phase.floatValue

        // Sky
        drawRect(Brush.verticalGradient(0f to p.bgTop, 1f to p.bgTop.copy(alpha = 0f), startY = 0f, endY = horizon))
        // Horizon glow
        drawCircle(
            Brush.radialGradient(
                listOf(p.accent.copy(alpha = if (p.dark) 0.45f else 0.30f), p.accent2.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(cx, horizon), radius = w * 0.45f
            ),
            radius = w * 0.45f, center = Offset(cx, horizon)
        )
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, p.accent.copy(alpha = 0.9f), Color.Transparent)),
            Offset(0f, horizon), Offset(w, horizon), strokeWidth = 2f
        )

        // Ground grid (perspective): horizontal rows flowing towards the viewer
        val rows = 12
        val grid = if (p.dark) p.accent.copy(alpha = 0.22f) else p.accent2.copy(alpha = 0.18f)
        for (i in 0 until rows) {
            val t = ((i + ph) / rows)
            val y = horizon + (h - horizon) * t.pow(2.2f)
            drawLine(grid.copy(alpha = grid.alpha * t), Offset(0f, y), Offset(w, y), strokeWidth = 1f + 1.5f * t)
        }
        // Converging verticals
        for (k in -9..9) {
            if (k == 0) continue
            val xb = cx + k * w * 0.16f
            drawLine(grid.copy(alpha = grid.alpha * 0.8f), Offset(cx + k * w * 0.006f, horizon), Offset(xb, h), strokeWidth = 1f)
        }

        // Road surface
        val topHalf = w * 0.012f
        val bottomHalf = w * 0.30f
        val road = Path().apply {
            moveTo(cx - topHalf, horizon); lineTo(cx + topHalf, horizon)
            lineTo(cx + bottomHalf, h); lineTo(cx - bottomHalf, h); close()
        }
        drawPath(
            road,
            Brush.verticalGradient(
                listOf(p.accent.copy(alpha = 0.10f), p.accent.copy(alpha = if (p.dark) 0.32f else 0.22f)),
                startY = horizon, endY = h
            )
        )
        // Road edges
        val edge = Stroke(width = 3f, cap = StrokeCap.Round)
        val edgeBrush = Brush.verticalGradient(listOf(p.accent.copy(alpha = 0.2f), p.accent), startY = horizon, endY = h)
        drawPath(Path().apply { moveTo(cx - topHalf, horizon); lineTo(cx - bottomHalf, h) }, edgeBrush, style = edge)
        drawPath(Path().apply { moveTo(cx + topHalf, horizon); lineTo(cx + bottomHalf, h) }, edgeBrush, style = edge)

        // Flowing centre dashes
        val dashes = 9
        for (i in 0 until dashes) {
            val t0 = ((i + ph) / dashes)
            val t1 = t0 + 0.45f / dashes
            if (t1 > 1f) continue
            val y0 = horizon + (h - horizon) * t0.pow(2.2f)
            val y1 = horizon + (h - horizon) * t1.pow(2.2f)
            val wd = 1.5f + 9f * t0
            drawLine(p.text.copy(alpha = 0.25f + 0.65f * t0), Offset(cx, y0), Offset(cx, y1), strokeWidth = wd, cap = StrokeCap.Round)
        }

        drawVehicle(cx, h, w, p.text, p.accent, p.dark)
    }
}

/** Minimal rear-view vehicle silhouette with a light-bar signature (original design). */
private fun DrawScope.drawVehicle(cx: Float, h: Float, w: Float, body: Color, accent: Color, dark: Boolean) {
    val cw = w * 0.20f
    val ch = cw * 0.42f
    val top = h - ch - h * 0.06f
    // shadow
    drawOval(Color.Black.copy(alpha = if (dark) 0.55f else 0.20f), Offset(cx - cw * 0.62f, top + ch * 0.80f), Size(cw * 1.24f, ch * 0.42f))
    // cabin
    val cabin = Path().apply {
        moveTo(cx - cw * 0.30f, top + ch * 0.05f)
        lineTo(cx + cw * 0.30f, top + ch * 0.05f)
        lineTo(cx + cw * 0.40f, top + ch * 0.42f)
        lineTo(cx - cw * 0.40f, top + ch * 0.42f); close()
    }
    drawPath(cabin, Brush.verticalGradient(listOf(Color(0xFF1B2335), Color(0xFF0A0F1A)), startY = top, endY = top + ch * 0.42f))
    // body
    drawRoundRect(
        Brush.verticalGradient(
            listOf(if (dark) Color(0xFFE9EEF7) else Color(0xFFFFFFFF), if (dark) Color(0xFF9AA6BC) else Color(0xFFC9D2E2)),
            startY = top + ch * 0.38f, endY = top + ch
        ),
        topLeft = Offset(cx - cw * 0.5f, top + ch * 0.38f), size = Size(cw, ch * 0.55f), cornerRadius = CornerRadius(ch * 0.22f)
    )
    // light bar
    drawRoundRect(
        Brush.horizontalGradient(listOf(Color(0xFFFF2D55).copy(alpha = 0.3f), Color(0xFFFF2D55), Color(0xFFFF2D55).copy(alpha = 0.3f))),
        topLeft = Offset(cx - cw * 0.44f, top + ch * 0.50f), size = Size(cw * 0.88f, ch * 0.07f), cornerRadius = CornerRadius(ch * 0.04f)
    )
    drawRoundRect(Color(0xFFFF2D55).copy(alpha = 0.18f), Offset(cx - cw * 0.5f, top + ch * 0.44f), Size(cw, ch * 0.19f), CornerRadius(ch * 0.1f))
    // accent underglow
    drawRoundRect(accent.copy(alpha = 0.55f), Offset(cx - cw * 0.36f, top + ch * 0.93f), Size(cw * 0.72f, ch * 0.035f), CornerRadius(4f))
    // wheels
    drawRoundRect(Color(0xFF05070D), Offset(cx - cw * 0.47f, top + ch * 0.86f), Size(cw * 0.14f, ch * 0.14f), CornerRadius(5f))
    drawRoundRect(Color(0xFF05070D), Offset(cx + cw * 0.33f, top + ch * 0.86f), Size(cw * 0.14f, ch * 0.14f), CornerRadius(5f))
}

/** Speed arc gauge: 240° sweep, 0–200 km/h, warns above 120. */
@Composable
fun SpeedArc(speedKmh: Float, modifier: Modifier = Modifier) {
    val p = LocalNova.current
    val v by animateFloatAsState((speedKmh / 200f).coerceIn(0f, 1f), tween(700), label = "arc")
    val warn = speedKmh > 120f
    Canvas(modifier) {
        val stroke = size.minDimension * 0.07f
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(p.text.copy(alpha = 0.10f), 150f, 240f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(
            Brush.sweepGradient(listOf(p.accent2, p.accent, if (warn) Color(0xFFFF9A3D) else p.accent, p.accent2)),
            150f, 240f * v, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
        )
    }
}
