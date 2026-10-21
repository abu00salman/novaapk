package com.abu00salman.novadrive.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.abu00salman.novadrive.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

data class Accent(val color: Color, val second: Color, val en: String, val ar: String)

val Accents = listOf(
    Accent(Color(0xFF3DDCFF), Color(0xFF7C5CFF), "Nova Cyan", "سماوي نوفا"),
    Accent(Color(0xFF8F7CFF), Color(0xFFFF5CC8), "Aurora", "شفق"),
    Accent(Color(0xFF2EF2A0), Color(0xFF3DDCFF), "Electric Mint", "نعناع كهربائي"),
    Accent(Color(0xFFFF9A3D), Color(0xFFFF4D6D), "Desert Sunset", "غروب الصحراء"),
    Accent(Color(0xFFE8C872), Color(0xFFB98A2E), "Royal Gold", "ذهبي ملكي"),
    Accent(Color(0xFFFF4D6D), Color(0xFFFF9A3D), "Racing Red", "أحمر السباق"),
)

@Immutable
data class NovaPalette(
    val dark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val stroke: Color,
    val strokeHi: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val accent2: Color,
    val danger: Color = Color(0xFFFF5470),
    val ok: Color = Color(0xFF2EF2A0),
)

val LocalNova = staticCompositionLocalOf {
    NovaPalette(true, Color.Black, Color.Black, Color.Transparent, Color.Transparent, Color.Gray,
        Color.Gray, Color.White, Color.Gray, Accents[0].color, Accents[0].second)
}
val LocalArabic = staticCompositionLocalOf { false }

/** Inline bilingual string. */
@Composable
@ReadOnlyComposable
fun tr(en: String, ar: String): String = if (LocalArabic.current) ar else en

@Composable
private fun anim(c: Color) = animateColorAsState(c, tween(700), label = "palette").value

@Composable
fun NovaTheme(dark: Boolean, accentIndex: Int, arabic: Boolean, content: @Composable () -> Unit) {
    NovaFonts.init(LocalContext.current)
    val accent = Accents.getOrElse(accentIndex) { Accents[0] }
    val p = NovaPalette(
        dark = dark,
        bgTop = anim(if (dark) Color(0xFF10151F) else Color(0xFFF4F6FA)),
        bgBottom = anim(if (dark) Color(0xFF030509) else Color(0xFFD9E0EC)),
        glassTop = anim(if (dark) Color(0x2BFFFFFF) else Color(0xD9FFFFFF)),
        glassBottom = anim(if (dark) Color(0x0FFFFFFF) else Color(0xA6FFFFFF)),
        stroke = anim(if (dark) Color(0x14FFFFFF) else Color(0x140B1430)),
        strokeHi = anim(if (dark) Color(0x4DFFFFFF) else Color(0xFFFFFFFF)),
        text = anim(if (dark) Color(0xFFEFF4FF) else Color(0xFF0B1220)),
        textDim = anim(if (dark) Color(0xFF8C98B2) else Color(0xFF5B6680)),
        accent = anim(accent.color),
        accent2 = anim(accent.second),
    )
    val scheme = if (dark) darkColorScheme(
        primary = p.accent, secondary = p.accent2, background = p.bgBottom, surface = Color(0xFF111827),
        onPrimary = Color(0xFF02040A), onSurface = p.text, onBackground = p.text,
    ) else lightColorScheme(
        primary = p.accent, secondary = p.accent2, background = p.bgBottom, surface = Color.White,
        onPrimary = Color.White, onSurface = p.text, onBackground = p.text,
    )
    CompositionLocalProvider(
        LocalNova provides p,
        LocalArabic provides arabic,
        LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/**
 * Typeface: Thmanyah (Sans) when its .ttf files are placed in app/src/main/assets/fonts/,
 * otherwise IBM Plex Sans Arabic (bundled, OFL).
 */
object NovaFonts {
    var family: FontFamily = FontFamily.Default
        private set
    private var ready = false

    fun init(ctx: Context) {
        if (ready) return
        ready = true
        family = runCatching { thmanyah(ctx) }.getOrNull() ?: plex()
    }

    private fun plex() = FontFamily(
        Font(R.font.plex_light, FontWeight.Light),
        Font(R.font.plex_regular, FontWeight.Normal),
        Font(R.font.plex_medium, FontWeight.Medium),
        Font(R.font.plex_semibold, FontWeight.SemiBold),
        Font(R.font.plex_bold, FontWeight.Bold),
    )

    private fun thmanyah(ctx: Context): FontFamily? {
        val files = ctx.assets.list("fonts")?.filter { it.endsWith(".ttf", true) || it.endsWith(".otf", true) }
            ?.filter { it.contains("thmanyah", true) || it.contains("ثمانية") } ?: return null
        if (files.isEmpty()) return null
        val sans = files.filter { it.contains("sans", true) }.ifEmpty { files }
        fun weight(n: String): FontWeight = n.lowercase().let {
            when {
                "thin" in it -> FontWeight.Thin
                "extralight" in it || "ultralight" in it -> FontWeight.ExtraLight
                "light" in it -> FontWeight.Light
                "medium" in it -> FontWeight.Medium
                "semibold" in it || "demibold" in it -> FontWeight.SemiBold
                "extrabold" in it -> FontWeight.ExtraBold
                "black" in it || "heavy" in it -> FontWeight.Black
                "bold" in it -> FontWeight.Bold
                else -> FontWeight.Normal
            }
        }
        return FontFamily(sans.map { Font("fonts/$it", ctx.assets, weight(it)) })
    }
}

object NovaType {
    private val f get() = NovaFonts.family
    val hero get() = TextStyle(fontFamily = f, fontSize = 96.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp)
    val display get() = TextStyle(fontFamily = f, fontSize = 44.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp)
    val title get() = TextStyle(fontFamily = f, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val body get() = TextStyle(fontFamily = f, fontSize = 16.sp, fontWeight = FontWeight.Normal)
    val label get() = TextStyle(fontFamily = f, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val caps get() = TextStyle(fontFamily = f, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
}
