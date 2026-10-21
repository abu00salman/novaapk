package com.abu00salman.novadrive.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class Link { OFF, ON, CONNECTED }

@Immutable
data class NetState(val wifi: Link = Link.OFF, val cellular: Boolean = false)

/** Real radio state for the status bar. Every query degrades to OFF instead of throwing. */
class StatusRepository(private val ctx: Context) {

    private fun btNow(): Link = runCatching {
        val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: return@runCatching Link.OFF
        if (!adapter.isEnabled) return@runCatching Link.OFF
        val canQuery = Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (!canQuery) return@runCatching Link.ON
        val connected = listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET).any {
            adapter.getProfileConnectionState(it) == BluetoothProfile.STATE_CONNECTED
        }
        if (connected) Link.CONNECTED else Link.ON
    }.getOrDefault(Link.OFF)

    fun bluetooth(): Flow<Link> = callbackFlow {
        trySend(btNow())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { trySend(btNow()) }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        awaitClose { runCatching { ctx.unregisterReceiver(receiver) } }
    }.distinctUntilChanged()

    fun network(): Flow<NetState> = callbackFlow {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        if (cm == null) { trySend(NetState()); awaitClose { }; return@callbackFlow }
        fun now(): NetState = runCatching {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            val wifiOn = runCatching {
                @Suppress("DEPRECATION")
                (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager)?.isWifiEnabled == true
            }.getOrDefault(false)
            NetState(
                wifi = when {
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> Link.CONNECTED
                    wifiOn -> Link.ON
                    else -> Link.OFF
                },
                cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true,
            )
        }.getOrDefault(NetState())
        trySend(now())
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(now()) }
            override fun onLost(network: Network) { trySend(now()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(now()) }
        }
        runCatching { cm.registerDefaultNetworkCallback(cb) }
        awaitClose { runCatching { cm.unregisterNetworkCallback(cb) } }
    }.distinctUntilChanged()

    fun gpsEnabled(): Boolean = runCatching {
        ctx.getSystemService(LocationManager::class.java)?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    }.getOrDefault(false)
}
