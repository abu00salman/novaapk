package com.abu00salman.novadrive.data

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Raster map tiles (OpenStreetMap data, CARTO basemaps) for the map panels of the Tesla and BMW
 * interfaces. Tiles are cached in memory and on disk so a drive only downloads each area once,
 * and the map keeps working offline for places already seen.
 */
object MapTiles {
    enum class Style(val path: String) { LIGHT("light_all"), DARK("dark_all") }

    const val ATTRIBUTION = "© OpenStreetMap © CARTO"
    private const val MAX_AGE_MS = 30L * 24 * 3600_000
    private val memory = LruCache<String, ImageBitmap>(48)
    private val gate = Semaphore(4)

    fun cached(style: Style, z: Int, x: Int, y: Int): ImageBitmap? = memory.get(key(style, z, x, y))

    suspend fun load(ctx: Context, style: Style, z: Int, x: Int, y: Int): ImageBitmap? {
        val n = 1 shl z
        if (y !in 0 until n) return null
        val wx = ((x % n) + n) % n
        val key = key(style, z, wx, y)
        memory.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            gate.withPermit {
                memory.get(key)?.let { return@withPermit it }
                val file = File(ctx.cacheDir, "tiles/${style.path}/$z/$wx/$y.png")
                val bytes = if (file.exists() && System.currentTimeMillis() - file.lastModified() < MAX_AGE_MS) file.readBytes()
                else fetch(style, z, wx, y)?.also { b -> runCatching { file.parentFile?.mkdirs(); file.writeBytes(b) } }
                    ?: file.takeIf { it.exists() }?.readBytes()
                bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.asImageBitmap()?.also { memory.put(key, it) }
            }
        }
    }

    private fun fetch(style: Style, z: Int, x: Int, y: Int): ByteArray? = runCatching {
        val sub = "abcd"[(x + y) % 4]
        val conn = (URL("https://$sub.basemaps.cartocdn.com/${style.path}/$z/$x/$y@2x.png").openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("User-Agent", "NovaDrive/2.0 (Android car launcher)")
        }
        try {
            if (conn.responseCode == 200) conn.inputStream.use { it.readBytes() } else null
        } finally { conn.disconnect() }
    }.getOrNull()

    private fun key(style: Style, z: Int, x: Int, y: Int) = "${style.path}/$z/$x/$y"
}
