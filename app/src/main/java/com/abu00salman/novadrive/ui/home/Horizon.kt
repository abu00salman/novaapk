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
import com.abu00salman.novadrive.core.CarModel
import com.abu00salman.novadrive.ui.theme.LocalNova
import kotlin.math.pow

/**
 * NOVA "Horizon": a stylised perspective road whose lane markings flow at a rate driven by
 * real GPS speed. Everything is vector drawing in a single Canvas; the per-frame phase is read
 * only inside the draw lambda, so animation never triggers recomposition.
 */
@Composable
fun HorizonCanvas(speedKmh: Float, car: CarModel, paint: Color, modifier: Modifier = Modifier) {
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
                    // slow idle drift when parked, otherwise proportional to speed
                    val rate = 0.05f + speedRef.value / 60f
                    phase.floatValue = (phase.floatValue + dt * rate) % 1f
                }
                last = t
            }
        }
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.36f
        val cx = w * 0.5f
        val ph = phase.floatValue

        // Horizon bloom
        drawCircle(
            Brush.radialGradient(
                listOf(p.accent.copy(alpha = if (p.dark) 0.38f else 0.26f), p.accent2.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(cx, horizon), radius = w * 0.62f
            ),
            radius = w * 0.62f, center = Offset(cx, horizon)
        )
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, p.accent.copy(alpha = 0.8f), Color.Transparent)),
            Offset(0f, horizon), Offset(w, horizon), strokeWidth = 1.5f
        )

        // Ground rows flowing towards the viewer
        val rows = 10
        val grid = p.text.copy(alpha = if (p.dark) 0.07f else 0.10f)
        for (i in 0 until rows) {
            val t = ((i + ph) / rows)
            val y = horizon + (h - horizon) * t.pow(2.2f)
            drawLine(grid.copy(alpha = grid.alpha * t), Offset(0f, y), Offset(w, y), strokeWidth = 1f + 1.2f * t)
        }

        // Road
        val topHalf = w * 0.010f
        val bottomHalf = w * 0.46f
        val road = Path().apply {
            moveTo(cx - topHalf, horizon); lineTo(cx + topHalf, horizon)
            lineTo(cx + bottomHalf, h); lineTo(cx - bottomHalf, h); close()
        }
        drawPath(
            road,
            Brush.verticalGradient(
                listOf(p.text.copy(alpha = 0f), p.text.copy(alpha = if (p.dark) 0.10f else 0.14f)),
                startY = horizon, endY = h
            )
        )
        // Glowing road edges
        val edgeBrush = Brush.verticalGradient(listOf(p.accent.copy(alpha = 0f), p.accent.copy(alpha = 0.9f)), startY = horizon, endY = h)
        for (sx in listOf(-1f, 1f)) {
            val e = Path().apply { moveTo(cx + sx * topHalf, horizon); lineTo(cx + sx * bottomHalf, h) }
            drawPath(e, p.accent.copy(alpha = 0.12f), style = Stroke(10f, cap = StrokeCap.Round))
            drawPath(e, edgeBrush, style = Stroke(2.5f, cap = StrokeCap.Round))
        }
        // Lane dashes
        val dashes = 9
        for (i in 0 until dashes) {
            val t0 = ((i + ph) / dashes)
            val t1 = t0 + 0.42f / dashes
            if (t1 > 1f) continue
            val y0 = horizon + (h - horizon) * t0.pow(2.2f)
            val y1 = horizon + (h - horizon) * t1.pow(2.2f)
            drawLine(p.text.copy(alpha = 0.10f + 0.50f * t0), Offset(cx, y0), Offset(cx, y1), strokeWidth = 1.5f + 8f * t0, cap = StrokeCap.Round)
        }

        val carW = minOf(w * 0.52f, h * 0.62f)
        drawCar(car, paint, cx, h * 0.90f, carW, p.dark, p.accent)
    }
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
