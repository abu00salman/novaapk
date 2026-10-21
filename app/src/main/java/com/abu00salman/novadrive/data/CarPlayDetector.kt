package com.abu00salman.novadrive.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Wired CarPlay detection: an Apple device (USB vendor 0x05AC) on the head unit's USB host.
 * Wireless CarPlay is negotiated privately inside ZLink/DiPlay and cannot be observed.
 */
class CarPlayDetector(private val ctx: Context) {

    fun iPhoneOnUsb(): Boolean = runCatching {
        val um = ctx.getSystemService(UsbManager::class.java) ?: return@runCatching false
        um.deviceList.values.any { it.vendorId == APPLE_VENDOR_ID }
    }.getOrDefault(false)

    fun connected(): Flow<Boolean> = callbackFlow {
        trySend(iPhoneOnUsb())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(iPhoneOnUsb())
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        val poll = launch {
            while (isActive) { delay(4_000); trySend(iPhoneOnUsb()) }
        }
        awaitClose {
            poll.cancel()
            runCatching { ctx.unregisterReceiver(receiver) }
        }
    }.distinctUntilChanged()

    companion object { const val APPLE_VENDOR_ID = 0x05AC }
}
