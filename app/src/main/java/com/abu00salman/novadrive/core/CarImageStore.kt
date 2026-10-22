package com.abu00salman.novadrive.core

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * The user's own car picture (PNG/WebP/JPEG, transparency kept). It is copied into app storage
 * so it survives the source file being moved or deleted, and decoded down-sampled for the GPU.
 */
object CarImageStore {
    private const val NAME = "car_image"
    private const val MAX_BYTES = 20L * 1024 * 1024

    fun file(ctx: Context) = File(ctx.filesDir, NAME)

    /** Copies [uri] into app storage. Returns false if it isn't a decodable image. */
    fun import(ctx: Context, uri: Uri): Boolean = runCatching {
        val tmp = File(ctx.filesDir, "$NAME.tmp")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BYTES) error("too large")
                    out.write(buf, 0, n)
                }
            }
        } ?: return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(tmp.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) { tmp.delete(); return false }
        tmp.renameTo(file(ctx))
    }.getOrDefault(false)

    fun clear(ctx: Context) { file(ctx).delete() }

    /** Decodes with power-of-two down-sampling so the long side stays near [maxPx]. Call off the main thread. */
    fun load(ctx: Context, maxPx: Int = 1280): ImageBitmap? = runCatching {
        val f = file(ctx)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
    }.getOrNull()
}
