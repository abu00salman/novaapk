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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.core.CarModel
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaFonts
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private const val START = 150f
private const val SWEEP = 240f
private const val MAX_KMH = 220f

/**
 * Circular speedometer driven by real GPS speed, with the chosen car on a small road in the
 * open lower sector. Lane dashes move only while the car moves — nothing animates when parked.
 */
@Composable
fun Speedometer(speedKmh: Float, car: CarModel, paint: Color, modifier: Modifier = Modifier) {
    val p = LocalNova.current
    val measurer = rememberTextMeasurer()
    val v by animateFloatAsState((speedKmh / MAX_KMH).coerceIn(0f, 1f), tween(600), label = "gauge")
    val phase = remember { mutableFloatStateOf(0f) }
    val speedRef = rememberUpdatedState(speedKmh)
    val moving = speedKmh > 0.5f
    LaunchedEffect(moving) {
        if (!moving) return@LaunchedEffect
        var last = 0L
        while (true) withFrameNanos { t ->
            if (last != 0L) phase.floatValue = (phase.floatValue + (t - last) / 1e9f * (speedRef.value / 70f)) % 1f
            last = t
        }
    }
    val labelStyle = TextStyle(fontFamily = NovaFonts.family, fontSize = 11.sp, color = p.textDim)

    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f - 6f
        val track = r * 0.055f
        // dial face
        drawCircle(Brush.radialGradient(listOf(p.accent.copy(alpha = if (p.dark) 0.10f else 0.06f), Color.Transparent), c, r), r, c)
        val arcTopLeft = Offset(c.x - r + track, c.y - r + track)
        val arcSize = Size((r - track) * 2, (r - track) * 2)
        drawArc(p.text.copy(alpha = 0.09f), START, SWEEP, false, arcTopLeft, arcSize, style = Stroke(track, cap = StrokeCap.Round))
        // ticks + labels
        val inner = r - track * 2.6f
        var kmh = 0
        while (kmh <= MAX_KMH.toInt()) {
            val a = Math.toRadians((START + SWEEP * kmh / MAX_KMH).toDouble())
            val major = kmh % 20 == 0
            val len = if (major) r * 0.075f else r * 0.035f
            val o = Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat())
            val i = Offset(c.x + (inner - len) * cos(a).toFloat(), c.y + (inner - len) * sin(a).toFloat())
            val lit = kmh / MAX_KMH <= v && v > 0f
            drawLine(
                if (lit) p.accent else p.text.copy(alpha = if (major) 0.55f else 0.22f), i, o,
                strokeWidth = if (major) 2.6f else 1.4f, cap = StrokeCap.Round
            )
            if (major && kmh % 40 == 0) {
                val lr = inner - len - r * 0.11f
                val m = measurer.measure("$kmh", labelStyle)
                drawText(m, topLeft = Offset(c.x + lr * cos(a).toFloat() - m.size.width / 2f, c.y + lr * sin(a).toFloat() - m.size.height / 2f))
            }
            kmh += 5
        }
        // live arc with a soft glow
        if (v > 0f) {
            drawArc(p.accent.copy(alpha = 0.18f), START, SWEEP * v, false, arcTopLeft, arcSize, style = Stroke(track * 2.6f, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(p.accent2, p.accent, Color(0xFF8FC3FF), p.accent2), c),
                START, SWEEP * v, false, arcTopLeft, arcSize, style = Stroke(track, cap = StrokeCap.Round)
            )
            val a = Math.toRadians((START + SWEEP * v).toDouble())
            val tip = Offset(c.x + (r - track) * cos(a).toFloat(), c.y + (r - track) * sin(a).toFloat())
            drawCircle(Color.White, track * 0.75f, tip)
            drawCircle(p.accent.copy(alpha = 0.35f), track * 1.6f, tip)
        }

        // road + car in the open lower sector
        val top = c.y + r * 0.40f
        val bottom = c.y + r * 0.98f
        val road = Path().apply {
            moveTo(c.x - r * 0.06f, top); lineTo(c.x + r * 0.06f, top)
            lineTo(c.x + r * 0.62f, bottom); lineTo(c.x - r * 0.62f, bottom); close()
        }
        drawPath(road, Brush.verticalGradient(listOf(p.accent.copy(alpha = 0f), p.accent.copy(alpha = 0.22f)), startY = top, endY = bottom))
        for (sx in listOf(-1f, 1f)) {
            drawLine(
                Brush.verticalGradient(listOf(p.accent.copy(alpha = 0f), p.accent), startY = top, endY = bottom),
                Offset(c.x + sx * r * 0.06f, top), Offset(c.x + sx * r * 0.62f, bottom), strokeWidth = 2.2f
            )
        }
        val ph = phase.floatValue
        for (k in 0 until 5) {
            val t0 = (k + ph) / 5f
            val t1 = t0 + 0.09f
            if (t1 > 1f) continue
            val y0 = top + (bottom - top) * t0.pow(1.8f)
            val y1 = top + (bottom - top) * t1.pow(1.8f)
            drawLine(p.text.copy(alpha = 0.15f + 0.5f * t0), Offset(c.x, y0), Offset(c.x, y1), strokeWidth = 1.5f + 4f * t0, cap = StrokeCap.Round)
        }
        drawCar(car, paint, c.x, c.y + r * 0.93f, r * 0.50f, p.dark, p.accent)
    }
}
