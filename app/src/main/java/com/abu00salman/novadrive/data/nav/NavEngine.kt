package com.abu00salman.novadrive.data.nav

import android.content.Context
import androidx.compose.runtime.Immutable
import com.abu00salman.novadrive.data.GeoFix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Live in-app guidance. Everything is derived from the real route and the latest GPS fix. */
@Immutable
data class NavState(
    val destination: Place,
    val route: Route?,
    /** Index of the next manoeuvre in [Route.steps]. */
    val stepIndex: Int = 1,
    val toNextM: Double = 0.0,
    val remainingM: Double = 0.0,
    val remainingS: Double = 0.0,
    val status: Status = Status.ROUTING,
    /** Index into [Route.points] the car has reached (the part already driven is drawn faded). */
    val progressIndex: Int = 0,
) {
    enum class Status { ROUTING, GUIDING, REROUTING, ARRIVED, NO_ROUTE, NO_GPS }
    val nextStep: RouteStep? get() = route?.steps?.getOrNull(stepIndex)
}

/**
 * Turn-by-turn navigation inside NovaDrive: routes with OSRM, follows the car on GPS, recalculates
 * after leaving the route and announces manoeuvres by voice. It lives in the app container, so it
 * keeps running across interface changes and while other apps are open; the destination is saved
 * so a restart resumes the trip.
 */
class NavEngine(
    ctx: Context,
    private val scope: CoroutineScope,
    private val fixes: Flow<GeoFix?>,
    private val arabic: () -> Boolean,
    private val speak: (String) -> Unit,
) {
    private val prefs = ctx.getSharedPreferences("nav", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow<NavState?>(null)
    val state: StateFlow<NavState?> = _state

    private val _voice = MutableStateFlow(prefs.getBoolean("voice", true))
    val voice: StateFlow<Boolean> = _voice

    private var follow: Job? = null
    private var last: GeoFix? = null
    private var offRouteCount = 0
    private var lastReroute = 0L
    private val announced = mutableSetOf<String>()

    init {
        // Resume a trip that was in progress (e.g. after the unit restarted), if it's recent.
        val lat = prefs.getFloat("lat", Float.NaN); val lon = prefs.getFloat("lon", Float.NaN)
        val at = prefs.getLong("at", 0L)
        if (!lat.isNaN() && !lon.isNaN() && System.currentTimeMillis() - at < 6 * 3600_000L) {
            val p = Place(prefs.getString("name", "") ?: "", prefs.getString("addr", "") ?: "", LatLon(lat.toDouble(), lon.toDouble()))
            scope.launch { delay(1500); start(p, announce = false) }
        }
    }

    fun setVoice(on: Boolean) { _voice.value = on; prefs.edit().putBoolean("voice", on).apply() }

    fun start(place: Place, announce: Boolean = true) {
        prefs.edit().putFloat("lat", place.point.lat.toFloat()).putFloat("lon", place.point.lon.toFloat())
            .putString("name", place.name).putString("addr", place.address).putLong("at", System.currentTimeMillis()).apply()
        announced.clear(); offRouteCount = 0
        _state.value = NavState(place, null, status = NavState.Status.ROUTING)
        follow?.cancel()
        follow = scope.launch {
            launch { fixes.filterNotNull().collect(::onFix) }
            val from = waitForFix() ?: run { _state.value = _state.value?.copy(status = NavState.Status.NO_GPS); return@launch }
            calculate(from, place, announce)
        }
    }

    fun stop() {
        follow?.cancel(); follow = null
        _state.value = null
        prefs.edit().clear().putBoolean("voice", _voice.value).apply()
    }

    /** Retries routing (e.g. after "no route" with no connection). */
    fun retry() { _state.value?.destination?.let { start(it, announce = false) } }

    private suspend fun waitForFix(): LatLon? {
        repeat(40) {
            last?.let { return LatLon(it.lat, it.lon) }
            delay(500)
        }
        return null
    }

    private suspend fun calculate(from: LatLon, place: Place, announce: Boolean) {
        val route = RouteService.route(from, place.point)
        val cur = _state.value ?: return
        if (route == null) { _state.value = cur.copy(status = NavState.Status.NO_ROUTE); return }
        _state.value = cur.copy(route = route, status = NavState.Status.GUIDING, stepIndex = 1,
            remainingM = route.distanceM, remainingS = route.durationS, progressIndex = 0)
        announced.clear()
        last?.let { update(it) }
        if (announce && _voice.value) {
            val ar = arabic()
            speak((if (ar) "بدء الملاحة إلى ${place.name}. " else "Starting navigation to ${place.name}. ") +
                Geo.formatDuration(route.durationS, ar) + "، " + Geo.formatDistance(route.distanceM, ar))
        }
    }

    private fun onFix(fix: GeoFix) {
        last = fix
        if (_state.value?.status == NavState.Status.GUIDING) update(fix)
    }

    private fun update(fix: GeoFix) {
        val s = _state.value ?: return
        val route = s.route ?: return
        if (route.steps.size < 2 || route.points.size < 2) return
        val pos = LatLon(fix.lat, fix.lon)
        val pts = route.points

        // Nearest point on the route, searching forward from the last progress (cheap, and it
        // doesn't jump back onto the opposite carriageway of the same road).
        var bestI = s.progressIndex; var bestD = Double.MAX_VALUE; var bestT = 0.0
        val end = minOf(pts.size - 1, s.progressIndex + 400)
        for (i in maxOf(0, s.progressIndex - 2) until end) {
            val (d, t) = Geo.toSegment(pos, pts[i], pts[i + 1])
            if (d < bestD) { bestD = d; bestI = i; bestT = t }
        }

        val tolerance = maxOf(45.0, (if (fix.accuracyM > 0) fix.accuracyM.toDouble() else 20.0) * 1.6)
        if (bestD > tolerance) {
            offRouteCount++
            if (offRouteCount >= 3 && System.currentTimeMillis() - lastReroute > 15_000) reroute(pos, s)
            return
        }
        offRouteCount = 0

        // Remaining distance along the route from the projected position.
        val seg = Geo.distance(pts[bestI], pts[bestI + 1])
        var remaining = seg * (1 - bestT)
        for (i in bestI + 1 until pts.size - 1) remaining += Geo.distance(pts[i], pts[i + 1])

        // Next manoeuvre: the first step whose location lies ahead of the car.
        var next = s.stepIndex.coerceIn(1, route.steps.size - 1)
        while (next < route.steps.size - 1 && indexOf(pts, route.steps[next].location, bestI) <= bestI) next++
        val toNext = Geo.distance(pos, route.steps[next].location)
        val remainingS = if (route.distanceM > 0) route.durationS * remaining / route.distanceM else 0.0

        val arrived = remaining < 30 || (next == route.steps.size - 1 && toNext < 25)
        _state.value = s.copy(
            stepIndex = next, toNextM = toNext, remainingM = remaining, remainingS = remainingS, progressIndex = bestI,
            status = if (arrived) NavState.Status.ARRIVED else NavState.Status.GUIDING,
        )
        announce(route.steps[next], next, toNext, fix.speedKmh, arrived)
        if (arrived) scope.launch { delay(20_000); if (_state.value?.status == NavState.Status.ARRIVED) stop() }
    }

    private fun indexOf(pts: List<LatLon>, p: LatLon, from: Int): Int {
        var best = from; var bd = Double.MAX_VALUE
        for (i in maxOf(0, from - 5) until minOf(pts.size, from + 600)) {
            val d = Geo.distance(pts[i], p)
            if (d < bd) { bd = d; best = i }
            if (d < 3) break
        }
        return best
    }

    private fun reroute(pos: LatLon, s: NavState) {
        lastReroute = System.currentTimeMillis(); offRouteCount = 0
        _state.value = s.copy(status = NavState.Status.REROUTING)
        if (_voice.value) speak(if (arabic()) "إعادة حساب المسار" else "Recalculating")
        scope.launch {
            val r = RouteService.route(pos, s.destination.point)
            val cur = _state.value ?: return@launch
            _state.value = if (r == null) cur.copy(status = NavState.Status.GUIDING)
            else cur.copy(route = r, stepIndex = 1, progressIndex = 0, remainingM = r.distanceM, remainingS = r.durationS, status = NavState.Status.GUIDING)
            announced.clear()
        }
    }

    /** Speaks each manoeuvre once early (~speed-dependent) and once just before it. */
    private fun announce(step: RouteStep, index: Int, toNext: Double, speedKmh: Float, arrived: Boolean) {
        if (!_voice.value) return
        val ar = arabic()
        if (arrived) {
            if (announced.add("arrive")) speak(if (ar) "لقد وصلت إلى وجهتك" else "You have arrived")
            return
        }
        val early = if (speedKmh > 80) 1500.0 else if (speedKmh > 50) 700.0 else 300.0
        val text = Instructions.text(step, ar)
        when {
            toNext < 60 && announced.add("$index-now") -> speak(text)
            toNext in 60.0..early && announced.add("$index-early") ->
                speak((if (ar) "بعد " else "In ") + Geo.formatDistance(toNext, ar) + "، " + text)
        }
    }
}
