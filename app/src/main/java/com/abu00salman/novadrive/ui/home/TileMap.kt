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
import com.abu00salman.novadrive.data.nav.LatLon
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/** The live map position marker drawn at the car's position. */
enum class MapMarker { TESLA, BMW, DOT }

/**
 * A real street map around the current GPS position, built from cached raster tiles. It follows
 * the car (heading-up when [headingUp] and the receiver reports a direction), supports pinch and
 * double-tap zoom, and a single tap hands off to the navigation view. With [onViewChange] the map
 * can also be dragged ([view] is then the centre instead of the car); [route] draws the active
 * route, with the part already driven faded.
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
    /** Vertical position of the car on the map (0 = top, 1 = bottom), below overlaid cards. */
    focusY: Float = 0.5f,
    route: List<LatLon>? = null,
    routeProgress: Int = 0,
    routeColor: Color = Color(0xFF2F8BFF),
    destination: LatLon? = null,
    /** Colour of the [MapMarker.DOT] marker. */
    markerColor: Color = Color(0xFF2F8BFF),
    view: LatLon? = null,
    onViewChange: ((LatLon) -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val tiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val zoomRef = rememberUpdatedState(zoom)
    val onTapRef = rememberUpdatedState(onTap)
    val onViewRef = rememberUpdatedState(onViewChange)
    // Current centre in world pixels and tile size, read by the drag handler.
    val worldRef = remember { floatArrayOf(0f, 0f, 256f, 1f) }
    BoxWithConstraints(
        modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, change, _ ->
                    if (change != 1f) onZoom((zoomRef.value + ln(change) / ln(2f)).coerceIn(3f, 18f))
                    val cb = onViewRef.value
                    if (cb != null && pan != Offset.Zero && worldRef[3] > 0f) {
                        cb(worldToLatLon(worldRef[0] - pan.x, worldRef[1] - pan.y, worldRef[2] * worldRef[3]))
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTapRef.value() }, onDoubleTap = { onZoom((zoomRef.value + 1f).coerceAtMost(18f)) })
            }
    ) {
        val d = LocalDensity.current
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        val centre = view ?: if (lat != null && lon != null) LatLon(lat, lon) else null
        if (centre == null) {
            Canvas(Modifier.fillMaxSize()) { drawRect(background) }
            return@BoxWithConstraints
        }
        val z = floor(zoom).toInt().coerceIn(3, 18)
        val tilePx = with(d) { 256.dp.toPx() } * 2f.pow(zoom - z)
        val n = 1 shl z
        val cx = worldX(centre.lon, n * tilePx)
        val cy = worldY(centre.lat, n * tilePx)
        worldRef[0] = cx; worldRef[1] = cy; worldRef[2] = tilePx; worldRef[3] = n.toFloat()
        val r = hypot(w, h) + tilePx * 0.1f
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
        val following = view == null
        val rotation = if (following && headingUp && bearing != null) -bearing else 0f
        val world = n * tilePx
        Canvas(Modifier.fillMaxSize()) {
            drawRect(background)
            val center = Offset(w / 2f, if (following) h * focusY else h / 2f)
            fun screen(p: LatLon) = Offset(worldX(p.lon, world) - cx + center.x, worldY(p.lat, world) - cy + center.y)
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
                if (route != null && route.size >= 2) {
                    val split = routeProgress.coerceIn(0, route.size - 1)
                    fun path(from: Int, to: Int) = Path().apply {
                        val a = screen(route[from]); moveTo(a.x, a.y)
                        for (i in from + 1..to) { val b = screen(route[i]); lineTo(b.x, b.y) }
                    }
                    if (split > 0) drawPath(path(0, split), routeColor.copy(alpha = 0.35f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    val ahead = path(split, route.size - 1)
                    drawPath(ahead, Color.White.copy(alpha = 0.9f), style = Stroke(11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(ahead, routeColor, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                destination?.let { dst ->
                    val c = screen(dst)
                    drawCircle(Color.White, 11.dp.toPx(), c)
                    drawCircle(Color(0xFFE82127), 8.dp.toPx(), c)
                }
            }
            if (lat == null || lon == null) return@Canvas
            // Position marker: points along the direction of travel (straight up in heading-up mode).
            val carAt = if (following) center else screen(LatLon(lat, lon))
            val heading = if (bearing == null) 0f else bearing + rotation
            val s = 26.dp.toPx()
            rotate(heading, carAt) {
                when (marker) {
                    MapMarker.TESLA -> {
                        val p = Path().apply {
                            moveTo(carAt.x, carAt.y - s); lineTo(carAt.x + s * 0.62f, carAt.y + s * 0.62f)
                            lineTo(carAt.x, carAt.y + s * 0.3f); lineTo(carAt.x - s * 0.62f, carAt.y + s * 0.62f); close()
                        }
                        drawPath(p, Color(0xFFE82127))
                        drawPath(p, Color.White, style = Stroke(2.dp.toPx()))
                    }
                    MapMarker.BMW -> {
                        drawCircle(Color(0xFF2F8BFF).copy(alpha = 0.22f), s * 1.5f, carAt)
                        val p = Path().apply {
                            moveTo(carAt.x, carAt.y - s); lineTo(carAt.x + s * 0.8f, carAt.y + s * 0.55f)
                            lineTo(carAt.x, carAt.y + s * 0.15f); lineTo(carAt.x - s * 0.8f, carAt.y + s * 0.55f); close()
                        }
                        drawPath(p, Color(0xFF2F8BFF))
                        drawPath(p, Color.White, style = Stroke(3.dp.toPx()))
                    }
                    MapMarker.DOT -> {
                        drawCircle(markerColor.copy(alpha = 0.22f), s * 1.4f, carAt)
                        drawCircle(Color.White, s * 0.62f, carAt)
                        val p = Path().apply {
                            moveTo(carAt.x, carAt.y - s * 0.48f); lineTo(carAt.x + s * 0.36f, carAt.y + s * 0.34f)
                            lineTo(carAt.x, carAt.y + s * 0.14f); lineTo(carAt.x - s * 0.36f, carAt.y + s * 0.34f); close()
                        }
                        drawPath(p, markerColor)
                    }
                }
            }
        }
    }
}

private fun worldX(lon: Double, world: Float): Float = ((lon + 180.0) / 360.0 * world).toFloat()

private fun worldY(lat: Double, world: Float): Float {
    val latR = Math.toRadians(lat.coerceIn(-85.0, 85.0))
    return ((1.0 - ln(tan(latR) + 1.0 / cos(latR)) / PI) / 2.0 * world).toFloat()
}

private fun worldToLatLon(x: Float, y: Float, world: Float): LatLon {
    val lon = x / world * 360.0 - 180.0
    val nY = PI - 2.0 * PI * y / world
    val lat = Math.toDegrees(kotlin.math.atan(kotlin.math.sinh(nY)))
    return LatLon(lat.coerceIn(-85.0, 85.0), ((lon + 540) % 360) - 180)
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
