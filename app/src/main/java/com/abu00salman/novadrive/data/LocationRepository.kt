package com.abu00salman.novadrive.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Immutable
data class GeoFix(
    val lat: Double,
    val lon: Double,
    val speedKmh: Float,
    val fromGps: Boolean,
    val accuracyM: Float,
)

class LocationRepository(private val ctx: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Real GPS speed (Location.speed). Falls back to distance/time when a fix lacks speed. */
    fun fixes(): Flow<GeoFix?> = callbackFlow {
        val lm = ctx.getSystemService(LocationManager::class.java)
        if (lm == null || !hasPermission()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        var previous: Location? = null
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val prev = previous
                var kmh = if (location.hasSpeed()) location.speed * 3.6f else {
                    if (prev != null && location.time > prev.time) {
                        location.distanceTo(prev) / ((location.time - prev.time) / 1000f) * 3.6f
                    } else 0f
                }
                if (kmh < 2f) kmh = 0f
                previous = location
                trySend(
                    GeoFix(
                        lat = location.latitude,
                        lon = location.longitude,
                        speedKmh = kmh,
                        fromGps = location.provider == LocationManager.GPS_PROVIDER,
                        accuracyM = if (location.hasAccuracy()) location.accuracy else -1f,
                    )
                )
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        try {
            val providers = lm.allProviders
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .mapNotNull { p -> if (p in providers) lm.getLastKnownLocation(p) else null }
                .maxByOrNull { it.time }
                ?.let { trySend(GeoFix(it.latitude, it.longitude, 0f, false, it.accuracy)) }
            if (LocationManager.GPS_PROVIDER in providers) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
            }
            if (LocationManager.NETWORK_PROVIDER in providers) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30_000L, 50f, listener, Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
            trySend(null)
        } catch (e: IllegalArgumentException) {
            trySend(null)
        }
        awaitClose { runCatching { lm.removeUpdates(listener) } }
    }
}
