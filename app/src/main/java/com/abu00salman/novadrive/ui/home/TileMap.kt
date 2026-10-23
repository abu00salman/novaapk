package com.abu00salman.novadrive.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.abu00salman.novadrive.data.MapTiles
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/** The live map position marker drawn at the car's position. */
enum class MapMarker { TESLA, BMW }

/**
 * A real street map around the current GPS position, built from cached raster tiles. It follows
 * the car (heading-up when [headingUp] and the receiver reports a direction), supports pinch and
 * double-tap zoom, and a single tap hands off to the navigation app.
 */
@Composable
fun TileMap(
    lat: Double?,
    lon: Double?,
    bearing: Float?,
    style: MapTiles.Style,
    marker: MapMarker,
    headingUp: Boolean,
    zoom: Float,
    onZoom: (Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    tint: ColorFilter? = null,
    background: Color,
) {
    val ctx = LocalContext.current
    val tiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val zoomRef = rememberUpdatedState(zoom)
    BoxWithConstraints(
        modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, _, change, _ -> if (change != 1f) onZoom((zoomRef.value + ln(change) / ln(2f)).coerceIn(3f, 18f)) }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() }, onDoubleTap = { onZoom((zoomRef.value + 1f).coerceAtMost(18f)) })
            }
    ) {
        val d = LocalDensity.current
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        if (lat == null || lon == null) {
            Canvas(Modifier.fillMaxSize()) { drawRect(background) }
            return@BoxWithConstraints
        }
        val z = floor(zoom).toInt().coerceIn(3, 18)
        val tilePx = with(d) { 256.dp.toPx() } * 2f.pow(zoom - z)
        val n = 1 shl z
        val latR = Math.toRadians(lat.coerceIn(-85.0, 85.0))
        val cx = ((lon + 180.0) / 360.0 * n * tilePx).toFloat()
        val cy = ((1.0 - ln(tan(latR) + 1.0 / cos(latR)) / PI) / 2.0 * n * tilePx).toFloat()
        val r = hypot(w, h) / 2f + tilePx * 0.1f
        val x0 = floor((cx - r) / tilePx).toInt(); val x1 = floor((cx + r) / tilePx).toInt()
        val y0 = floor((cy - r) / tilePx).toInt(); val y1 = floor((cy + r) / tilePx).toInt()
        val needed = remember(z, x0, x1, y0, y1, style) {
            buildList { for (x in x0..x1) for (y in y0..y1) if (y in 0 until n) add(Triple(z, x, y)) }
        }
        LaunchedEffect(needed) {
            for ((tz, tx, ty) in needed) {
                val k = "${style.path}/$tz/$tx/$ty"
                if (k in tiles) continue
                val hit = MapTiles.cached(style, tz, ((tx % n) + n) % n, ty)
                if (hit != null) tiles[k] = hit
                else launch { MapTiles.load(ctx, style, tz, tx, ty)?.let { tiles[k] = it } }
            }
            // Keep memory bounded to what is on screen.
            val keep = needed.map { (tz, tx, ty) -> "${style.path}/$tz/$tx/$ty" }.toSet()
            tiles.keys.filter { it !in keep }.forEach { tiles.remove(it) }
        }
        val rotation = if (headingUp && bearing != null) -bearing else 0f
        Canvas(Modifier.fillMaxSize()) {
            drawRect(background)
            val center = Offset(w / 2f, h / 2f)
            rotate(rotation, center) {
                for ((tz, tx, ty) in needed) {
                    val img = tiles["${style.path}/$tz/$tx/$ty"] ?: continue
                    val left = tx * tilePx - cx + center.x
                    val top = ty * tilePx - cy + center.y
                    drawImage(
                        img, dstOffset = IntOffset(left.toInt(), top.toInt()),
                        dstSize = IntSize((left + tilePx).toInt() - left.toInt(), (top + tilePx).toInt() - top.toInt()),
                        colorFilter = tint,
                    )
                }
            }
            // Position marker: points along the direction of travel (straight up in heading-up mode).
            val heading = if (bearing == null) 0f else bearing + rotation
            val s = 26.dp.toPx()
            rotate(heading, center) {
                when (marker) {
                    MapMarker.TESLA -> {
                        val p = Path().apply {
                            moveTo(center.x, center.y - s); lineTo(center.x + s * 0.62f, center.y + s * 0.62f)
                            lineTo(center.x, center.y + s * 0.3f); lineTo(center.x - s * 0.62f, center.y + s * 0.62f); close()
                        }
                        drawPath(p, Color(0xFFE82127))
                        drawPath(p, Color.White, style = Stroke(2.dp.toPx()))
                    }
                    MapMarker.BMW -> {
                        drawCircle(Color(0xFF2F8BFF).copy(alpha = 0.22f), s * 1.5f, center)
                        val p = Path().apply {
                            moveTo(center.x, center.y - s); lineTo(center.x + s * 0.8f, center.y + s * 0.55f)
                            lineTo(center.x, center.y + s * 0.15f); lineTo(center.x - s * 0.8f, center.y + s * 0.55f); close()
                        }
                        drawPath(p, Color(0xFF2F8BFF))
                        drawPath(p, Color.White, style = Stroke(3.dp.toPx()))
                    }
                }
            }
        }
    }
}

/** Metres per screen pixel at the map centre, used for the scale bar. */
fun mapMetresPerPx(lat: Double, zoom: Float, density: Float): Double =
    156543.03392 * cos(Math.toRadians(lat)) / 2.0.pow(zoom.toDouble()) / density

/** A round distance for a scale bar about [targetPx] long, and its length in pixels. */
fun mapScale(metresPerPx: Double, targetPx: Float): Pair<Int, Float> {
    val want = metresPerPx * targetPx
    val nice = listOf(20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000).lastOrNull { it <= want } ?: 20
    return nice to (nice / metresPerPx).toFloat()
}

/** Heading in degrees → 8-point compass label. */
fun compassLabel(bearing: Float, arabic: Boolean): String {
    val i = (((bearing % 360) + 360) % 360 / 45f + 0.5f).toInt() % 8
    return if (arabic) listOf("شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب")[i]
    else listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[i]
}
