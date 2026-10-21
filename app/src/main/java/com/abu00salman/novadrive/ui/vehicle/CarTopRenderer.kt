package com.abu00salman.novadrive.ui.vehicle

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.abu00salman.novadrive.core.Body
import com.abu00salman.novadrive.core.CarModel

private fun widthRatio(b: Body) = when (b) {
    Body.SEDAN -> 0.41f; Body.SUV -> 0.44f; Body.HATCH -> 0.45f; Body.COUPE -> 0.43f; Body.PICKUP -> 0.39f
}

/** Top-down view of the chosen car (front at the top), [len] pixels long, centred on [c]. */
fun DrawScope.drawCarTop(model: CarModel, paint: Color, c: Offset, len: Float, glow: Color) {
    val w = len * widthRatio(model.body)
    val top = c.y - len / 2f
    val bottom = c.y + len / 2f
    val left = c.x - w / 2f
    val right = c.x + w / 2f
    val hi = lerp(paint, Color.White, 0.35f)
    val lo = lerp(paint, Color.Black, 0.40f)

    // floor glow + shadow
    drawOval(Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent), c, len * 0.62f),
        Offset(c.x - len * 0.62f, c.y - len * 0.62f), Size(len * 1.24f, len * 1.24f))
    drawRoundRect(Color.Black.copy(alpha = 0.45f), Offset(left - w * 0.05f, top + len * 0.02f), Size(w * 1.1f, len * 1.0f), CornerRadius(w * 0.35f))

    // tyres
    val tw = w * 0.13f
    val th = len * 0.14f
    for (y in listOf(top + len * 0.17f, bottom - len * 0.30f)) {
        drawRoundRect(Color(0xFF07080C), Offset(left - tw * 0.30f, y), Size(tw, th), CornerRadius(tw * 0.3f))
        drawRoundRect(Color(0xFF07080C), Offset(right - tw * 0.70f, y), Size(tw, th), CornerRadius(tw * 0.3f))
    }

    // body
    val body = Path().apply {
        moveTo(c.x, top)
        cubicTo(right - w * 0.06f, top, right, top + len * 0.05f, right, top + len * 0.15f)
        lineTo(right, bottom - len * 0.10f)
        cubicTo(right, bottom - len * 0.015f, right - w * 0.10f, bottom, c.x, bottom)
        cubicTo(left + w * 0.10f, bottom, left, bottom - len * 0.015f, left, bottom - len * 0.10f)
        lineTo(left, top + len * 0.15f)
        cubicTo(left, top + len * 0.05f, left + w * 0.06f, top, c.x, top)
        close()
    }
    drawPath(body, Brush.horizontalGradient(listOf(lo, paint, hi, paint, lo), startX = left, endX = right))
    drawPath(body, Color.White.copy(alpha = 0.30f), style = Stroke(1.5f))

    // mirrors
    for (sx in listOf(-1f, 1f)) {
        val mx = if (sx < 0) left - w * 0.09f else right - w * 0.01f
        drawRoundRect(lo, Offset(mx, top + len * 0.29f), Size(w * 0.10f, len * 0.035f), CornerRadius(w * 0.03f))
    }

    val glassBrush = Brush.verticalGradient(listOf(Color(0xFF2B3550), Color(0xFF0A0E18)))
    val pickup = model.body == Body.PICKUP
    val cabinEnd = if (pickup) c.y + len * 0.02f else bottom - len * 0.24f
    // windscreen
    val ws = Path().apply {
        moveTo(left + w * 0.08f, top + len * 0.36f); lineTo(right - w * 0.08f, top + len * 0.36f)
        lineTo(right - w * 0.15f, top + len * 0.26f); lineTo(left + w * 0.15f, top + len * 0.26f); close()
    }
    drawPath(ws, glassBrush)
    drawPath(ws, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), Offset(left, top + len * 0.26f), Offset(c.x, top + len * 0.36f)))
    // roof
    drawRoundRect(
        Brush.horizontalGradient(listOf(paint, hi, paint), startX = left, endX = right),
        Offset(left + w * 0.10f, top + len * 0.37f), Size(w * 0.80f, cabinEnd - (top + len * 0.37f) - len * 0.07f), CornerRadius(w * 0.10f)
    )
    // rear glass
    val rg = Path().apply {
        moveTo(left + w * 0.11f, cabinEnd - len * 0.065f); lineTo(right - w * 0.11f, cabinEnd - len * 0.065f)
        lineTo(right - w * 0.16f, cabinEnd); lineTo(left + w * 0.16f, cabinEnd); close()
    }
    drawPath(rg, glassBrush)
    if (pickup) {
        val bedTop = cabinEnd + len * 0.025f
        drawRoundRect(lo, Offset(left + w * 0.07f, bedTop), Size(w * 0.86f, bottom - len * 0.035f - bedTop), CornerRadius(w * 0.05f))
        drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(left + w * 0.12f, bedTop + len * 0.02f), Size(w * 0.76f, bottom - len * 0.075f - bedTop), CornerRadius(w * 0.04f))
    } else {
        drawLine(Color.Black.copy(alpha = 0.18f), Offset(c.x, top + len * 0.04f), Offset(c.x, top + len * 0.24f), strokeWidth = 1.5f)
    }
    // lights
    for (sx in listOf(-1f, 1f)) {
        val hx = if (sx < 0) left + w * 0.06f else right - w * 0.30f
        drawRoundRect(Color.White.copy(alpha = 0.30f), Offset(hx - 3f, top + len * 0.035f - 3f), Size(w * 0.24f + 6f, len * 0.022f + 6f), CornerRadius(8f))
        drawRoundRect(Color(0xFFEAF4FF), Offset(hx, top + len * 0.035f), Size(w * 0.24f, len * 0.022f), CornerRadius(6f))
        val tx = if (sx < 0) left + w * 0.05f else right - w * 0.27f
        drawRoundRect(Color(0xFFFF2D4F).copy(alpha = 0.35f), Offset(tx - 3f, bottom - len * 0.04f - 3f), Size(w * 0.22f + 6f, len * 0.018f + 6f), CornerRadius(8f))
        drawRoundRect(Color(0xFFFF2D4F), Offset(tx, bottom - len * 0.04f), Size(w * 0.22f, len * 0.018f), CornerRadius(6f))
    }
}
