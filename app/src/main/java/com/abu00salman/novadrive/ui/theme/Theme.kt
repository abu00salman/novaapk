package com.abu00salman.novadrive.ui.theme

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.R

data class Accent(val color: Color, val second: Color, val en: String, val ar: String)

val Accents = listOf(
    Accent(Color(0xFF3D8BFF), Color(0xFF8B5CFF), "Electric Blue", "أزرق كهربائي"),
    Accent(Color(0xFF8F7CFF), Color(0xFFFF5CC8), "Aurora", "شفق"),
    Accent(Color(0xFF2EE6C0), Color(0xFF3D8BFF), "Electric Mint", "نعناع كهربائي"),
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
    val ok: Color = Color(0xFF34D399),
    val warn: Color = Color(0xFFFFB547),
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
private fun anim(c: Color) = animateColorAsState(c, tween(250), label = "palette").value

/** Night: deep navy glass. Day: bright frosted panels with navy text for sunlight legibility. */
fun novaPalette(dark: Boolean, accent: Accent) = NovaPalette(
    dark = dark,
    bgTop = if (dark) Color(0xFF0B1534) else Color(0xFFEFF3FB),
    bgBottom = if (dark) Color(0xFF03060F) else Color(0xFFD5DEEE),
    glassTop = if (dark) Color(0x33416AC8) else Color(0xF2FFFFFF),
    glassBottom = if (dark) Color(0x1A16244D) else Color(0xCCFFFFFF),
    stroke = if (dark) Color(0x1FFFFFFF) else Color(0x1A0B1630),
    strokeHi = if (dark) Color(0x4D9CC2FF) else Color(0xFFFFFFFF),
    text = if (dark) Color(0xFFF2F6FF) else Color(0xFF0B1630),
    textDim = if (dark) Color(0xFF93A3C4) else Color(0xFF4A5878),
    accent = accent.color,
    accent2 = accent.second,
)

@Composable
fun NovaTheme(dark: Boolean, accentIndex: Int, arabic: Boolean, content: @Composable () -> Unit) {
    NovaFonts.init(LocalContext.current)
    val target = novaPalette(dark, Accents.getOrElse(accentIndex) { Accents[0] })
    val p = target.copy(
        bgTop = anim(target.bgTop), bgBottom = anim(target.bgBottom),
        glassTop = anim(target.glassTop), glassBottom = anim(target.glassBottom),
        stroke = anim(target.stroke), strokeHi = anim(target.strokeHi),
        text = anim(target.text), textDim = anim(target.textDim),
        accent = anim(target.accent), accent2 = anim(target.accent2),
    )
    val scheme = if (dark) darkColorScheme(
        primary = p.accent, secondary = p.accent2, background = p.bgBottom, surface = Color(0xFF0E1630),
        onPrimary = Color.White, onSurface = p.text, onBackground = p.text,
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
    val hero get() = TextStyle(fontFamily = f, fontSize = 84.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-2).sp)
    val display get() = TextStyle(fontFamily = f, fontSize = 40.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp)
    val title get() = TextStyle(fontFamily = f, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    val body get() = TextStyle(fontFamily = f, fontSize = 16.sp, fontWeight = FontWeight.Normal)
    val label get() = TextStyle(fontFamily = f, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val small get() = TextStyle(fontFamily = f, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val caps get() = TextStyle(fontFamily = f, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
}
