package com.abu00salman.novadrive.ui.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.NavInfo
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.home.NavCard
import com.abu00salman.novadrive.ui.home.ScenicBackdrop
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr

/**
 * Navigation hub. Routing itself always happens in a real navigation app; NOVA mirrors its live
 * guidance and offers one-tap destinations.
 */
@Composable
fun NavigationScreen(
    nav: NavInfo?,
    hasAccess: Boolean,
    navApp: AppEntry?,
    navApps: List<AppEntry>,
    homeAddress: String,
    workAddress: String,
    driving: Boolean,
    onCard: () -> Unit,
    onOpenNav: () -> Unit,
    onGrantAccess: () -> Unit,
    onNavigate: (String) -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onSetPlaces: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalNova.current
    Row(modifier.fillMaxSize()) {
        Box(
            Modifier.weight(0.60f).fillMaxHeight().clip(RoundedCornerShape(24.dp))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.04f))), RoundedCornerShape(24.dp))
        ) {
            ScenicBackdrop(Modifier.fillMaxSize())
            NavCard(
                nav, hasAccess, navApp?.label, homeAddress.isNotBlank(), workAddress.isNotBlank(),
                onCard = onCard, onOpenNav = onOpenNav, onGrantAccess = onGrantAccess,
                onHome = { onNavigate(homeAddress) }, onWork = { onNavigate(workAddress) },
                modifier = Modifier.align(Alignment.TopCenter).padding(18.dp).fillMaxWidth(), large = true,
            )
            Glass(
                Modifier.align(Alignment.BottomCenter).padding(18.dp).fillMaxWidth().height(64.dp),
                corner = 22.dp, tint = p.accent, onClick = if (nav != null) onCard else onOpenNav,
            ) {
                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                    navApp?.let { Image(it.icon, null, Modifier.size(34.dp)); Spacer(Modifier.width(10.dp)) }
                        ?: Icon(Icons.Rounded.Map, null, tint = Color.White, modifier = Modifier.padding(end = 10.dp).size(28.dp))
                    Text(
                        if (nav != null) tr("Return to ", "العودة إلى ") + (navApp?.label ?: tr("navigation", "الملاحة"))
                        else tr("Open ", "افتح ") + (navApp?.label ?: tr("navigation", "الملاحة")),
                        style = NovaType.title, color = Color.White, maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(0.40f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Glass(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("Destinations", "الوجهات"), style = NovaType.title, color = p.text)
                    Spacer(Modifier.height(10.dp))
                    Place(Icons.Rounded.Home, tr("Home", "المنزل"), homeAddress, { onNavigate(homeAddress) }, onSetPlaces)
                    Spacer(Modifier.height(8.dp))
                    Place(Icons.Rounded.Work, tr("Work", "العمل"), workAddress, { onNavigate(workAddress) }, onSetPlaces)
                    Spacer(Modifier.height(12.dp))
                    SearchBox(driving, onNavigate)
                }
            }
            Glass(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(tr("Navigation apps", "تطبيقات الملاحة"), style = NovaType.title, color = p.text)
                    Spacer(Modifier.height(8.dp))
                    if (navApps.isEmpty()) Text(
                        tr("No navigation app is installed. Install Google Maps or Waze.", "لا يوجد تطبيق ملاحة مثبت. ثبّت خرائط Google أو Waze."),
                        style = NovaType.label, color = p.textDim
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(navApps, key = { it.key }) { app ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onLaunchApp(app) }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(app.icon, null, Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(app.label, style = NovaType.body, color = p.text, modifier = Modifier.weight(1f), maxLines = 1)
                                if (app.packageName == navApp?.packageName) Box(Modifier.size(10.dp).clip(CircleShape).background(p.accent))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Place(icon: ImageVector, title: String, address: String, onGo: () -> Unit, onSet: () -> Unit) {
    val p = LocalNova.current
    val set = address.isNotBlank()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.text.copy(alpha = 0.05f))
            .clickable(onClick = if (set) onGo else onSet).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(p.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = p.accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = NovaType.body, color = p.text)
            Text(if (set) address else tr("Not set — tap to add in Settings", "غير محدد — اضغط للإضافة من الإعدادات"),
                style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (set) Icon(Icons.Rounded.ArrowForward, null, tint = p.textDim)
    }
}

@Composable
private fun SearchBox(driving: Boolean, onNavigate: (String) -> Unit) {
    val p = LocalNova.current
    var q by rememberSaveable { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(p.text.copy(alpha = 0.06f)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (driving) Icons.Rounded.Lock else Icons.Rounded.Search, null, tint = p.textDim)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (driving) Text(tr("Search is available when parked", "البحث متاح عند التوقف"), style = NovaType.label, color = p.textDim)
            else {
                if (q.isEmpty()) Text(tr("Search a destination", "ابحث عن وجهة"), style = NovaType.body, color = p.textDim)
                BasicTextField(
                    q, { q = it }, singleLine = true, textStyle = NovaType.body.copy(color = p.text),
                    cursorBrush = SolidColor(p.accent), modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (q.isNotBlank()) onNavigate(q) }),
                )
            }
        }
        if (!driving && q.isNotBlank()) Box(
            Modifier.size(38.dp).clip(CircleShape).background(p.accent).clickable { onNavigate(q) },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.ArrowForward, tr("Go", "انطلق"), tint = Color.White) }
    }
}
