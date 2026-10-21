package com.abu00salman.novadrive.data

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@Immutable
data class VehicleData(
    val state: State = State.OFF,
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val coolantC: Int? = null,
    val voltage: Float? = null,
    val fuelPct: Int? = null,
    val loadPct: Int? = null,
    /** Malfunction indicator (check-engine) lamp, from Mode 01 PID 01. */
    val mil: Boolean? = null,
    val dtcCount: Int? = null,
    val error: String? = null,
) {
    enum class State { OFF, CONNECTING, LIVE, ERROR }
}

/**
 * Optional, read-only OBD-II reader for a paired ELM327 Bluetooth adapter.
 * Sends only AT setup commands and standard SAE J1979 Mode 01 queries — it never writes,
 * clears codes, or talks to any proprietary vehicle module.
 */
class ObdClient(private val ctx: Context, private val scope: CoroutineScope) {
    private val _data = MutableStateFlow(VehicleData())
    val data: StateFlow<VehicleData> = _data
    private var job: Job? = null
    @Volatile private var socket: BluetoothSocket? = null

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    /** Paired devices as (name, MAC). Pair the ELM327 in Android Bluetooth settings first. */
    fun pairedDevices(): List<Pair<String, String>> {
        if (!hasPermission()) return emptyList()
        return try {
            val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
            adapter.bondedDevices.orEmpty().map { (it.name ?: it.address) to it.address }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun start(address: String) {
        stop()
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                _data.value = VehicleData(VehicleData.State.CONNECTING)
                try {
                    session(address)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _data.value = VehicleData(VehicleData.State.ERROR, error = e.message ?: e.javaClass.simpleName)
                } finally {
                    runCatching { socket?.close() }
                    socket = null
                }
                delay(5_000) // reconnect backoff
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
        runCatching { socket?.close() }
        socket = null
        _data.value = VehicleData()
    }

    private suspend fun session(address: String) {
        if (!hasPermission()) throw IllegalStateException("Bluetooth permission required")
        val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter
            ?: throw IllegalStateException("No Bluetooth adapter")
        val device = adapter.getRemoteDevice(address)
        val sock = try {
            device.createRfcommSocketToServiceRecord(SPP)
        } catch (e: SecurityException) {
            throw IllegalStateException("Bluetooth permission required")
        }
        socket = sock
        try { sock.connect() } catch (e: SecurityException) { throw IllegalStateException("Bluetooth permission required") }
        val input = sock.inputStream
        val out = sock.outputStream
        for (c in listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")) {
            command(out, input, c, if (c == "ATZ") 4000 else 2000)
        }
        while (currentCoroutineContext().isActive && sock.isConnected) {
            val rpm = bytes(command(out, input, "010C"), "0C")?.let { if (it.size >= 2) (it[0] * 256 + it[1]) / 4 else null }
            val speed = bytes(command(out, input, "010D"), "0D")?.firstOrNull()
            val coolant = bytes(command(out, input, "0105"), "05")?.firstOrNull()?.minus(40)
            val volt = command(out, input, "ATRV").filter { it.isDigit() || it == '.' }.toFloatOrNull()
            // Optional PIDs: many cars don't report them, in which case they stay null ("unavailable").
            val fuel = bytes(command(out, input, "012F"), "2F")?.firstOrNull()?.let { it * 100 / 255 }
            val load = bytes(command(out, input, "0104"), "04")?.firstOrNull()?.let { it * 100 / 255 }
            val status = bytes(command(out, input, "0101"), "01")?.firstOrNull()
            _data.value = VehicleData(
                VehicleData.State.LIVE, rpm, speed, coolant, volt,
                fuelPct = fuel, loadPct = load,
                mil = status?.let { it and 0x80 != 0 }, dtcCount = status?.let { it and 0x7F },
            )
            delay(1_000)
        }
    }

    private fun command(out: OutputStream, input: InputStream, cmd: String, timeoutMs: Long = 2500): String {
        out.write((cmd + "\r").toByteArray()); out.flush()
        val sb = StringBuilder()
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            if (input.available() > 0) {
                val c = input.read()
                if (c < 0) break
                if (c.toChar() == '>') return sb.toString()
                sb.append(c.toChar())
            } else Thread.sleep(8)
        }
        return sb.toString()
    }

    /** Extracts data bytes from a Mode 01 reply such as "41 0C 1A F8". */
    private fun bytes(resp: String, pid: String): List<Int>? {
        val line = resp.split('\r', '\n')
            .map { it.replace(" ", "").trim().uppercase() }
            .firstOrNull { it.startsWith("41$pid") } ?: return null
        return line.substring(4).chunked(2).mapNotNull { it.toIntOrNull(16) }.takeIf { it.isNotEmpty() }
    }

    companion object {
        private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
