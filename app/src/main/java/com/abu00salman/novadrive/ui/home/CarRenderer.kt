package com.abu00salman.novadrive.ui.home

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.abu00salman.novadrive.core.Body
import com.abu00salman.novadrive.core.CarModel
import com.abu00salman.novadrive.core.Lights

private class Shape(val roof: Float, val belt: Float, val gwB: Float, val gwT: Float, val bodyTop: Float, val clear: Float)

private fun shapeOf(b: Body) = when (b) {
    Body.SEDAN -> Shape(.76f, .52f, .33f, .20f, .37f, .09f)
    Body.SUV -> Shape(.88f, .60f, .41f, .37f, .47f, .14f)
    Body.HATCH -> Shape(.80f, .55f, .38f, .30f, .45f, .10f)
    Body.COUPE -> Shape(.66f, .46f, .34f, .22f, .38f, .08f)
    Body.PICKUP -> Shape(.90f, .64f, .38f, .34f, .50f, .17f)
}

private val Brake = Color(0xFFFF2D4F)

/**
 * Rear view of the chosen car, drawn as layered vector shapes in car-width units
 * (x: -0.5..0.5 across, y: 0 at the road up). [w] is the on-screen width of the car.
 */
fun DrawScope.drawCar(model: CarModel, paint: Color, cx: Float, ground: Float, w: Float, dark: Boolean, glow: Color, braking: Boolean = false) {
    val s = shapeOf(model.body)
    fun pt(x: Float, y: Float) = Offset(cx + x * w, ground - y * w)
    fun poly(vararg p: Float): Path = Path().apply {
        for (i in p.indices step 2) { val o = pt(p[i], p[i + 1]); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
        close()
    }
    val soft = Stroke(w * 0.012f, join = StrokeJoin.Round)
    fun fill(path: Path, brush: Brush) { drawPath(path, brush); }
    fun fillSoft(path: Path, color: Color) { drawPath(path, color); drawPath(path, color, style = soft) }

    val hi = lerp(paint, Color.White, 0.30f)
    val lo = lerp(paint, Color.Black, if (paint.luminance() > 0.6f) 0.28f else 0.45f)
    val roofY = s.roof; val belt = s.belt; val c = s.clear

    // ground contact shadow + accent underglow
    drawOval(Color.Black.copy(alpha = if (dark) 0.75f else 0.30f), pt(-.56f, .035f), Size(1.12f * w, .07f * w))
    drawOval(Brush.radialGradient(listOf(glow.copy(alpha = 0.50f), Color.Transparent), pt(0f, 0f), w * 0.62f), w * 0.62f, pt(0f, 0f), alpha = 0.55f)

    // tyres
    for (sx in listOf(-1f, 1f)) {
        val tl = pt(sx * .385f - .058f, .30f)
        drawRoundRect(Color(0xFF07080B), tl, Size(.116f * w, .30f * w), CornerRadius(.02f * w))
        drawRoundRect(Color.White.copy(alpha = 0.06f), tl, Size(.116f * w, .30f * w), CornerRadius(.02f * w), style = Stroke(1.2f))
    }
    // mirrors
    for (sx in listOf(-1f, 1f)) {
        fillSoft(poly(sx * (s.gwB - .01f), belt + .015f, sx * (s.gwB + .075f), belt + .035f, sx * (s.gwB + .07f), belt + .085f, sx * (s.gwB - .01f), belt + .075f), lo)
    }
    // roof / greenhouse
    val green = poly(-s.gwB, belt, -s.gwT, roofY - .035f, -s.gwT + .05f, roofY, s.gwT - .05f, roofY, s.gwT, roofY - .035f, s.gwB, belt)
    fill(green, Brush.verticalGradient(listOf(hi, paint), pt(0f, roofY).y, pt(0f, belt).y))
    drawPath(green, hi.copy(alpha = 0.5f), style = soft)
    // rear glass with sky reflection
    val ins = .035f
    val glass = poly(-s.gwB + ins, belt + .015f, -s.gwT + ins, roofY - .045f, -s.gwT + .06f, roofY - .016f, s.gwT - .06f, roofY - .016f, s.gwT - ins, roofY - .045f, s.gwB - ins, belt + .015f)
    fill(glass, Brush.verticalGradient(listOf(Color(0xFF2A3348), Color(0xFF0A0D14)), pt(0f, roofY).y, pt(0f, belt).y))
    fill(glass, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.20f), Color.Transparent), pt(-s.gwB, roofY), pt(0f, belt)))
    drawPath(glass, Color.Black.copy(alpha = 0.5f), style = Stroke(2f, join = StrokeJoin.Round))

    // body
    val bt = s.bodyTop
    val body = poly(-bt, belt + .02f, bt, belt + .02f, .485f, belt - .07f, .5f, belt - .22f, .495f, c + .10f, .47f, c, -.47f, c, -.495f, c + .10f, -.5f, belt - .22f, -.485f, belt - .07f)
    fill(body, Brush.verticalGradient(listOf(hi, paint, lo), pt(0f, belt + .02f).y, pt(0f, c).y))
    drawPath(body, Color.White.copy(alpha = 0.35f), style = Stroke(1.6f, join = StrokeJoin.Round))
    // shoulder crease + lit edge
    drawLine(Color.White.copy(alpha = 0.40f), pt(-bt + .02f, belt + .012f), pt(bt - .02f, belt + .012f), w * 0.006f)
    drawLine(Color.Black.copy(alpha = 0.22f), pt(-.47f, belt - .27f), pt(.47f, belt - .27f), w * 0.01f)
    // lower diffuser
    fillSoft(poly(-.40f, c, .40f, c, .38f, c + .075f, -.38f, c + .075f), Color(0xFF15171D))

    // tail lights
    val bl = if (braking) 1f else 0.82f
    when (model.lights) {
        Lights.WRAP -> for (sx in listOf(-1f, 1f)) {
            val l = poly(sx * .49f, belt - .075f, sx * .20f, belt - .045f, sx * .18f, belt - .10f, sx * .485f, belt - .165f)
            drawPath(l, Brake.copy(alpha = 0.22f), style = Stroke(w * 0.03f, join = StrokeJoin.Round))
            fill(l, Brush.horizontalGradient(if (sx < 0) listOf(Brake, Brake.copy(alpha = .55f)) else listOf(Brake.copy(alpha = .55f), Brake)))
            drawPath(l, Color.White.copy(alpha = 0.35f * bl), style = Stroke(1.2f))
        }
        Lights.BAR -> {
            val l = poly(-.49f, belt - .07f, .49f, belt - .07f, .49f, belt - .105f, -.49f, belt - .105f)
            drawPath(l, Brake.copy(alpha = 0.22f), style = Stroke(w * 0.035f, join = StrokeJoin.Round))
            fill(l, Brush.horizontalGradient(listOf(Brake, Brake.copy(alpha = 0.7f), Brake.copy(alpha = 0.7f), Brake)))
        }
        Lights.VERT -> for (sx in listOf(-1f, 1f)) {
            val l = poly(sx * .495f, belt + .06f, sx * .415f, belt + .06f, sx * .42f, belt - .17f, sx * .497f, belt - .17f)
            drawPath(l, Brake.copy(alpha = 0.22f), style = Stroke(w * 0.03f, join = StrokeJoin.Round))
            fill(l, Brush.verticalGradient(listOf(Brake, Brake.copy(alpha = .6f)), pt(0f, belt + .06f).y, pt(0f, belt - .17f).y))
        }
    }
    // plate + badge
    drawRoundRect(Color(0xFFE9EBF0), pt(-.105f, c + .165f), Size(.21f * w, .085f * w), CornerRadius(w * .01f))
    drawRoundRect(Color.Black.copy(alpha = 0.25f), pt(-.105f, c + .165f), Size(.21f * w, .085f * w), CornerRadius(w * .01f), style = Stroke(1.2f))
    drawCircle(Color.White.copy(alpha = 0.55f), w * 0.016f, pt(0f, belt - .02f))
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
