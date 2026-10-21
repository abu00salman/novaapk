package com.abu00salman.novadrive.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.ui.components.AppIcon
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.components.RoundButton
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr

/**
 * App library: search (parked only), favorites first, and favorite ordering. Long-press an app
 * to select it, then star/unstar or move it.
 */
@Composable
fun AppsScreen(
    apps: List<AppEntry>,
    favorites: List<String>,
    driving: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoveFavorite: (String, Int) -> Unit,
    onParkedOnly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalNova.current
    var query by rememberSaveable { mutableStateOf("") }
    var favOnly by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    val q = if (driving) "" else query.trim()
    val shown = remember(apps, q, favOnly, favorites) {
        val favs = favorites.mapNotNull { k -> apps.firstOrNull { it.key == k } }
        val base = if (favOnly) favs else favs + apps.filter { it.key !in favorites }
        if (q.isEmpty()) base else base.filter { it.label.contains(q, ignoreCase = true) }
    }

    Glass(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Applications", "التطبيقات"), style = NovaType.display.copy(fontSize = 30.sp), color = p.text)
                Spacer(Modifier.width(16.dp))
                Pill(tr("All", "الكل"), if (!favOnly) p.accent else p.textDim) { favOnly = false }
                Spacer(Modifier.width(8.dp))
                Pill(tr("Favorites", "المفضلة"), if (favOnly) p.accent else p.textDim, icon = Icons.Rounded.Star) { favOnly = true }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.width(340.dp).height(50.dp).clip(RoundedCornerShape(16.dp)).background(p.text.copy(alpha = 0.06f)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (driving) Icons.Rounded.Lock else Icons.Rounded.Search, null, tint = p.textDim)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (driving) Text(tr("Search is available when parked", "البحث متاح عند التوقف"), style = NovaType.label, color = p.textDim, maxLines = 1)
                        else {
                            if (query.isEmpty()) Text(tr("Search apps", "ابحث عن تطبيق"), style = NovaType.body, color = p.textDim)
                            BasicTextField(
                                query, { query = it }, singleLine = true,
                                textStyle = NovaType.body.copy(color = p.text),
                                cursorBrush = SolidColor(p.accent), modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            val sel = selected?.let { k -> apps.firstOrNull { it.key == k } }
            if (sel != null) {
                val isFav = sel.key in favorites
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.accent.copy(alpha = 0.14f)).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(sel.label, style = NovaType.title, color = p.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Pill(if (isFav) tr("Remove from favorites", "إزالة من المفضلة") else tr("Add to favorites", "إضافة للمفضلة"), p.accent,
                        icon = if (isFav) Icons.Rounded.StarBorder else Icons.Rounded.Star) { onToggleFavorite(sel.key) }
                    if (isFav) {
                        RoundButton(Icons.Rounded.ArrowBack, tr("Move earlier", "تقديم"), 44.dp) { onMoveFavorite(sel.key, -1) }
                        RoundButton(Icons.Rounded.ArrowForward, tr("Move later", "تأخير"), 44.dp) { onMoveFavorite(sel.key, 1) }
                    }
                    RoundButton(Icons.Rounded.Close, tr("Done", "تم"), 44.dp) { selected = null }
                }
            } else Text(
                if (driving) tr("Driving: tap an app to open it, or use voice.", "أثناء القيادة: اضغط على تطبيق لفتحه أو استخدم الصوت.")
                else tr("Tap to open · long-press to favorite or reorder", "اضغط للفتح · اضغط مطولاً للتفضيل أو الترتيب"),
                style = NovaType.label, color = p.textDim, modifier = Modifier.padding(top = 10.dp)
            )
            Spacer(Modifier.height(12.dp))
            if (shown.isEmpty()) Text(
                if (favOnly) tr("No favorites yet — long-press an app to add it.", "لا توجد مفضلة بعد — اضغط مطولاً على تطبيق لإضافته.")
                else tr("No apps match your search.", "لا توجد تطبيقات مطابقة."),
                style = NovaType.body, color = p.textDim, modifier = Modifier.padding(top = 24.dp)
            )
            LazyVerticalGrid(
                GridCells.Adaptive(if (driving) 136.dp else 116.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(shown, key = { it.key }) { app ->
                    AppIcon(
                        app, size = if (driving) 84.dp else 74.dp, starred = app.key in favorites,
                        onLongClick = { if (driving) onParkedOnly() else selected = app.key }
                    ) { onLaunch(app) }
                }
            }
        }
    }
}
