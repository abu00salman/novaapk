package com.abu00salman.novadrive.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.ui.components.AppIcon
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import com.abu00salman.novadrive.ui.toast

@Composable
fun AppDrawer(c: AppContainer, s: NovaSettings, driving: Boolean, onClose: () -> Unit) {
    val p = LocalNova.current
    val ctx = LocalContext.current
    val apps by c.apps.apps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var favOnly by remember { mutableStateOf(false) }
    val shown = remember(apps, query, favOnly, s.favorites) {
        apps.filter { a ->
            (!favOnly || a.key in s.favorites) &&
                (query.isBlank() || a.label.contains(query.trim(), ignoreCase = true))
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(p.bgBottom.copy(alpha = 0.94f))
            .clickable(remember { MutableInteractionSource() }, null) { }
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("App Library", "مكتبة التطبيقات"), style = NovaType.display, color = p.text)
                Spacer(Modifier.width(18.dp))
                Pill(tr("All", "الكل"), if (!favOnly) p.accent else p.textDim) { favOnly = false }
                Spacer(Modifier.width(8.dp))
                Pill(tr("Favorites", "المفضلة"), if (favOnly) p.accent else p.textDim) { favOnly = true }
                Spacer(Modifier.weight(1f))
                if (!driving) {
                    Glass(Modifier.width(320.dp).height(48.dp), corner = 16.dp) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Search, null, tint = p.textDim)
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) Text(tr("Search apps", "ابحث عن تطبيق"), style = NovaType.body, color = p.textDim)
                                BasicTextField(
                                    query, { query = it }, singleLine = true,
                                    textStyle = NovaType.body.copy(color = p.text),
                                    cursorBrush = SolidColor(p.accent), modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Glass(Modifier.size(48.dp), corner = 16.dp, onClick = onClose) {
                    Icon(Icons.Rounded.Close, tr("Close", "إغلاق"), tint = p.text, modifier = Modifier.align(Alignment.Center))
                }
            }
            Text(
                if (driving) tr("Driving: search is disabled. Pick an app or use voice.", "أثناء القيادة: البحث متوقف. اختر تطبيقاً أو استخدم الصوت.")
                else tr("Tap to open · long-press to pin to the dock", "اضغط للفتح · اضغط مطولاً للتثبيت في الشريط"),
                style = NovaType.label, color = p.textDim, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
            LazyVerticalGrid(
                GridCells.Adaptive(if (driving) 132.dp else 112.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(shown, key = { it.key }) { app ->
                    AppIcon(
                        app, size = if (driving) 84.dp else 72.dp, starred = app.key in s.favorites,
                        onLongClick = {
                            if (driving) toast(ctx, if (s.arabic) "متاح عند التوقف" else "Available when parked")
                            else c.settings.toggleFavorite(app.key)
                        }
                    ) {
                        if (c.apps.launch(app)) onClose()
                    }
                }
            }
        }
    }
}
