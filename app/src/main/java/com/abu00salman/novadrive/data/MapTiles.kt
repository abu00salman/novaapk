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
 * Raster map tiles from OpenStreetMap (free, no API key, local-language labels) for the map panels
 * of the Tesla and BMW interfaces; each interface recolours them (light grey / dark blue). Tiles
 * are cached in memory and on disk so a drive only downloads each area once, and the map keeps
 * working offline for places already seen.
 */
object MapTiles {
    /** Both looks share the same downloaded tiles; only the colour filter differs. */
    enum class Style(val path: String) { LIGHT("osm"), DARK("osm") }

    const val ATTRIBUTION = "© OpenStreetMap"
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
                cleanOld(ctx)
                val file = File(ctx.cacheDir, "tiles/${style.path}/$z/$wx/$y.png")
                val bytes = if (file.exists() && System.currentTimeMillis() - file.lastModified() < MAX_AGE_MS) file.readBytes()
                else fetch(style, z, wx, y)?.also { b -> runCatching { file.parentFile?.mkdirs(); file.writeBytes(b) } }
                    ?: file.takeIf { it.exists() }?.readBytes()
                bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.asImageBitmap()?.also { memory.put(key, it) }
            }
        }
    }

    @Volatile private var cleaned = false

    /** Removes tiles from the previous provider (it began answering with "API key required" images). */
    private fun cleanOld(ctx: Context) {
        if (cleaned) return
        cleaned = true
        listOf("light_all", "dark_all").forEach { runCatching { File(ctx.cacheDir, "tiles/$it").deleteRecursively() } }
    }

    private fun fetch(@Suppress("UNUSED_PARAMETER") style: Style, z: Int, x: Int, y: Int): ByteArray? = runCatching {
        val conn = (URL("https://tile.openstreetmap.org/$z/$x/$y.png").openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("User-Agent", "NovaDrive/2.0 (Android car launcher; +https://github.com/abu00salman/novaapk)")
        }
        try {
            if (conn.responseCode == 200) conn.inputStream.use { it.readBytes() } else null
        } finally { conn.disconnect() }
    }.getOrNull()

    private fun key(style: Style, z: Int, x: Int, y: Int) = "${style.path}/$z/$x/$y"
}
