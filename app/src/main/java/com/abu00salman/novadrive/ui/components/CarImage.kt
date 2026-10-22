package com.abu00salman.novadrive.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abu00salman.novadrive.core.CarImageStore
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The user's car picture, scaled to fit (never cropped or stretched). [version] changes whenever a
 * new image is chosen in Settings › Car. Without one, a simple placeholder is shown.
 */
@Composable
fun CarImage(version: Long, modifier: Modifier = Modifier, hint: Boolean = true, preview: ImageBitmap? = null) {
    val ctx = LocalContext.current
    val image by produceState(preview, version) {
        if (preview == null) value = if (version == 0L) null else withContext(Dispatchers.IO) { CarImageStore.load(ctx) }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val img = image
        if (img != null) Image(img, tr("My car", "سيارتي"), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        else CarPlaceholder(hint)
    }
}

@Composable
private fun CarPlaceholder(hint: Boolean) {
    val p = LocalNova.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(p.accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DirectionsCar, null, tint = p.accent, modifier = Modifier.size(56.dp))
        }
        if (hint) {
            Spacer(Modifier.height(8.dp))
            Text(
                tr("Add your car photo in Settings › Car", "أضف صورة سيارتك من الإعدادات › السيارة"),
                style = NovaType.small, color = p.textDim, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
