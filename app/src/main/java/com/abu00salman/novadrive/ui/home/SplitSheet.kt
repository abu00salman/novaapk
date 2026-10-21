package com.abu00salman.novadrive.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.ui.components.AppPickerDialog
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import com.abu00salman.novadrive.ui.toast

/** Pairs two apps side by side using Android multi-window (when the unit supports it). */
@Composable
fun SplitSheet(c: AppContainer, s: NovaSettings, onClose: () -> Unit) {
    val p = LocalNova.current
    val ctx = LocalContext.current
    val apps by c.apps.apps.collectAsStateWithLifecycle()
    val sc = rememberShortcuts(c, s, apps)
    var left by remember { mutableStateOf(sc.nav) }
    var right by remember { mutableStateOf(sc.music ?: sc.carPlay) }
    var picking by remember { mutableStateOf(0) }

    Box(
        Modifier.fillMaxSize().background(p.bgBottom.copy(alpha = 0.6f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
    ) {
        Glass(
            Modifier.align(Alignment.Center).width(720.dp)
                .clickable(remember { MutableInteractionSource() }, null) { },
            corner = 32.dp
        ) {
            Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Split screen", "تقسيم الشاشة"), style = NovaType.display, color = p.text)
                Text(
                    tr("Run two apps side by side — e.g. navigation + music.", "شغّل تطبيقين جنباً إلى جنب — مثل الخرائط + الموسيقى."),
                    style = NovaType.body, color = p.textDim
                )
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Slot(left, tr("Left", "اليسار")) { picking = 1 }
                    Slot(right, tr("Right", "اليمين")) { picking = 2 }
                }
                Spacer(Modifier.height(22.dp))
                Glass(Modifier.fillMaxWidth().height(60.dp), corner = 20.dp, tint = p.accent, onClick = {
                    val a = left; val b = right
                    if (a == null || b == null) toast(ctx, if (s.arabic) "اختر تطبيقين" else "Pick two apps")
                    else {
                        c.controls.launchSplit(c.apps.intentFor(a), c.apps.intentFor(b))
                        onClose()
                    }
                }) {
                    Text(tr("Launch split", "تشغيل التقسيم"), style = NovaType.title, color = p.text, modifier = Modifier.align(Alignment.Center))
                }
                Text(
                    tr("Uses your unit's Android multi-window. If it isn't supported, the second app opens full screen.",
                        "يعتمد على دعم النوافذ المتعددة في جهازك. إذا لم يكن مدعوماً يفتح التطبيق الثاني بملء الشاشة."),
                    style = NovaType.label, color = p.textDim, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
    if (picking != 0) AppPickerDialog(
        tr("Choose app", "اختر التطبيق"), apps, allowNone = false,
        onPick = { a: AppEntry? -> if (picking == 1) left = a else right = a; picking = 0 },
        onDismiss = { picking = 0 }
    )
}

@Composable
private fun Slot(app: AppEntry?, label: String, onClick: () -> Unit) {
    val p = LocalNova.current
    Glass(Modifier.size(300.dp, 170.dp), corner = 24.dp, onClick = onClick) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (app != null) Image(app.icon, null, Modifier.size(64.dp))
            else Icon(Icons.Rounded.Add, null, tint = p.accent, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(10.dp))
            Text(app?.label ?: label, style = NovaType.title, color = p.text)
            Text(label, style = NovaType.label, color = p.textDim)
        }
    }
}
