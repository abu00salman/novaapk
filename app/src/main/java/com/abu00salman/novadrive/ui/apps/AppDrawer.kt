package com.abu00salman.novadrive.ui.apps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.core.AppTab
import com.abu00salman.novadrive.core.filterApps
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.LocalIconScale
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.components.RoundButton
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr

/**
 * App library: category tabs, large icons with two-line names, 4–6 columns by width, search when
 * parked, favorites with ordering, and pre-installed extras hidden unless asked for. Apps open
 * through PackageManager in their own window — Android and the app decide full-screen behaviour.
 */
@Composable
fun AppsScreen(
    apps: List<AppEntry>,
    favorites: List<String>,
    showSystem: Boolean,
    driving: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoveFavorite: (String, Int) -> Unit,
    onShowSystem: (Boolean) -> Unit,
    onParkedOnly: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: AppTab? = null,
) {
    val p = LocalNova.current
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(initialTab ?: if (favorites.isEmpty()) AppTab.ALL else AppTab.FAVORITES) }
    var selected by remember { mutableStateOf<String?>(null) }
    val q = if (driving) "" else query
    val shown = remember(apps, tab, favorites, q, showSystem) { filterApps(apps, tab, favorites, q, showSystem) }

    Glass(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Applications", "التطبيقات"), style = NovaType.display.copy(fontSize = 28.sp), color = p.text)
                Spacer(Modifier.width(12.dp))
                Text("${shown.size}", style = NovaType.label, color = p.textDim)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.width(320.dp).height(52.dp).clip(RoundedCornerShape(16.dp)).background(p.text.copy(alpha = 0.07f)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (driving) Icons.Rounded.Lock else Icons.Rounded.Search, null, tint = p.textDim)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (driving) Text(tr("Search is available when parked", "البحث متاح عند التوقف"), style = NovaType.label, color = p.textDim, maxLines = 1)
                        else {
                            if (query.isEmpty()) Text(tr("Search apps", "ابحث عن تطبيق"), style = NovaType.body, color = p.textDim)
                            BasicTextField(query, { query = it }, singleLine = true, textStyle = NovaType.body.copy(color = p.text), cursorBrush = SolidColor(p.accent), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Row(
                    Modifier.height(52.dp).clip(RoundedCornerShape(16.dp)).clickable { if (driving) onParkedOnly() else onShowSystem(!showSystem) }.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("System apps", "تطبيقات النظام"), style = NovaType.label, color = p.textDim)
                    Spacer(Modifier.width(8.dp))
                    Switch(showSystem, { if (driving) onParkedOnly() else onShowSystem(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = p.accent, checkedThumbColor = Color.White))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(AppTab.FAVORITES, Icons.Rounded.Star, tr("Favorites", "المفضلة")),
                    Triple(AppTab.NAVIGATION, Icons.Rounded.Navigation, tr("Navigation", "الملاحة")),
                    Triple(AppTab.MEDIA, Icons.Rounded.MusicNote, tr("Media", "الوسائط")),
                    Triple(AppTab.CAR, Icons.Rounded.DirectionsCar, tr("Car", "السيارة")),
                    Triple(AppTab.TOOLS, Icons.Rounded.Build, tr("Tools", "الأدوات")),
                    Triple(AppTab.ALL, Icons.Rounded.Apps, tr("All", "الكل")),
                ).forEach { (t, icon, label) -> TabButton(icon, label, t == tab, Modifier.weight(1f)) { tab = t; selected = null } }
            }
            val sel = selected?.let { k -> apps.firstOrNull { it.key == k } }
            if (sel != null) {
                val isFav = sel.key in favorites
                Row(
                    Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.accent.copy(alpha = 0.14f)).padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(sel.label, style = NovaType.title, color = p.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Pill(if (isFav) tr("Remove from favorites", "إزالة من المفضلة") else tr("Add to favorites", "إضافة للمفضلة"), p.accent,
                        icon = if (isFav) Icons.Rounded.StarBorder else Icons.Rounded.Star) { onToggleFavorite(sel.key) }
                    if (isFav) {
                        RoundButton(Icons.Rounded.ArrowBack, tr("Move earlier", "تقديم"), 48.dp) { onMoveFavorite(sel.key, -1) }
                        RoundButton(Icons.Rounded.ArrowForward, tr("Move later", "تأخير"), 48.dp) { onMoveFavorite(sel.key, 1) }
                    }
                    RoundButton(Icons.Rounded.Close, tr("Done", "تم"), 48.dp) { selected = null }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (shown.isEmpty()) Text(
                when {
                    tab == AppTab.FAVORITES -> tr("No favorites yet — long-press an app in any tab to add it.", "لا توجد مفضلة بعد — اضغط مطولاً على أي تطبيق لإضافته.")
                    q.isNotBlank() -> tr("No apps match your search.", "لا توجد تطبيقات مطابقة للبحث.")
                    else -> tr("No installed apps in this category.", "لا توجد تطبيقات مثبتة في هذا التصنيف.")
                },
                style = NovaType.body, color = p.textDim, modifier = Modifier.padding(top = 24.dp)
            )
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val cols = (maxWidth / 170.dp).toInt().coerceIn(4, 6)
                LazyVerticalGrid(
                    GridCells.Fixed(cols),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shown, key = { it.key }) { app ->
                        AppTile(app, app.key in favorites, app.key == selected,
                            onLong = { selected = app.key }) { onLaunch(app) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(icon: ImageVector, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalNova.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.height(52.dp).clip(shape)
            .background(if (active) Brush.verticalGradient(listOf(p.accent, p.accent.copy(alpha = 0.75f))) else Brush.verticalGradient(listOf(p.text.copy(alpha = 0.06f), p.text.copy(alpha = 0.04f))))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (active) Color.White else p.textDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = NovaType.label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = if (active) Color.White else p.text, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(app: AppEntry, favorite: Boolean, selected: Boolean, onLong: () -> Unit, onClick: () -> Unit) {
    val p = LocalNova.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().height(78.dp * LocalIconScale.current + 72.dp).clip(shape)
            .background(if (selected) p.accent.copy(alpha = 0.18f) else p.text.copy(alpha = if (p.dark) 0.04f else 0.35f))
            .border(1.dp, if (selected) p.accent else p.stroke, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(app.icon, app.label, Modifier.size(72.dp * LocalIconScale.current))
            if (favorite) Box(Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(p.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Star, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(app.label, style = NovaType.label.copy(fontSize = 15.sp, lineHeight = 18.sp), color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
