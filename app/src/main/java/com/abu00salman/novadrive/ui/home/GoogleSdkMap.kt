package com.abu00salman.novadrive.ui.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.abu00salman.novadrive.BuildConfig
import com.abu00salman.novadrive.data.nav.LatLon
import androidx.compose.ui.platform.LocalDensity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * The real Google map (Maps SDK for Android), used when the build carries a Maps key and the unit has
 * Google Play services. Draws NovaDrive's own route, destination and car position on top.
 */
object GoogleSdkMap {
    /** Set when the SDK never finished loading (key not authorised, no billing, ...): fall back for this run. */
    @Volatile var failed = false
    val failedState = androidx.compose.runtime.mutableStateOf(false)
    fun available(ctx: Context): Boolean = BuildConfig.HAS_MAPS_KEY && !failed && runCatching {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx) == ConnectionResult.SUCCESS
    }.getOrDefault(false)
}

@Composable
fun GoogleSdkMap(
    lat: Double, lon: Double, bearing: Float?, headingUp: Boolean, zoom: Float,
    onZoom: (Float) -> Unit, onTap: () -> Unit, modifier: Modifier, background: Color,
    route: List<LatLon>?, routeProgress: Int, routeColor: Color, destination: LatLon?, markerColor: Color,
) {
    val density = LocalDensity.current.density
    val night = background.luminance() < 0.3f
    val camera = rememberCameraPositionState { position = CameraPosition(LatLng(lat, lon), zoom, 0f, 0f) }
    val onZoomRef = rememberUpdatedState(onZoom)
    val tilt = if (headingUp) 45f else 0f
    val rot = if (headingUp && bearing != null) bearing else 0f
    LaunchedEffect(lat, lon, rot, zoom, tilt) {
        runCatching { camera.animate(CameraUpdateFactory.newCameraPosition(CameraPosition(LatLng(lat, lon), zoom, tilt, rot)), 900) }
    }
    // Pinch zoom on the map is reported back so the app's own zoom stays in step.
    LaunchedEffect(Unit) {
        snapshotFlow { camera.isMoving }.collect { moving ->
            if (!moving && camera.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
                onZoomRef.value(camera.position.zoom.coerceIn(3f, 19f))
            }
        }
    }
    val loaded = remember { androidx.compose.runtime.mutableStateOf(false) }
    // A key without Maps SDK / billing enabled shows an empty beige map and never reports "loaded".
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(10_000)
        if (!loaded.value) { GoogleSdkMap.failed = true; GoogleSdkMap.failedState.value = true }
    }
    val props = remember(night) {
        MapProperties(mapStyleOptions = if (night) MapStyleOptions(NIGHT_STYLE) else null, isBuildingEnabled = true, isTrafficEnabled = true)
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = props,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false, compassEnabled = false, myLocationButtonEnabled = false,
            mapToolbarEnabled = false, rotationGesturesEnabled = false, tiltGesturesEnabled = false,
        ),
        onMapClick = { onTap() },
        onMapLoaded = { loaded.value = true },
    ) {
        if (route != null && route.size >= 2) {
            val split = routeProgress.coerceIn(0, route.size - 1)
            if (split > 0) Polyline(route.subList(0, split + 1).map { LatLng(it.lat, it.lon) }, color = routeColor.copy(alpha = 0.35f), width = 14f)
            val ahead = route.subList(split, route.size).map { LatLng(it.lat, it.lon) }
            Polyline(ahead, color = Color.White, width = 26f, zIndex = 1f)
            Polyline(ahead, color = routeColor, width = 18f, zIndex = 2f)
        }
        destination?.let { Marker(state = MarkerState(LatLng(it.lat, it.lon))) }
        Circle(center = LatLng(lat, lon), radius = mapMetresPerPx(lat, camera.position.zoom, density) * 11.0,
            fillColor = markerColor, strokeColor = Color.White, strokeWidth = 6f, zIndex = 3f)
    }
}

private const val NIGHT_STYLE = """[
{"elementType":"geometry","stylers":[{"color":"#1d2128"}]},
{"elementType":"labels.text.fill","stylers":[{"color":"#9aa3ad"}]},
{"elementType":"labels.text.stroke","stylers":[{"color":"#1d2128"}]},
{"featureType":"road","elementType":"geometry","stylers":[{"color":"#2c333d"}]},
{"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#3d4654"}]},
{"featureType":"water","elementType":"geometry","stylers":[{"color":"#0e1a26"}]},
{"featureType":"poi","elementType":"geometry","stylers":[{"color":"#22282f"}]}
]"""
