package com.abu00salman.novadrive.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr

@Composable
fun AppPickerDialog(title: String, apps: List<AppEntry>, allowNone: Boolean = true, onPick: (AppEntry?) -> Unit, onDismiss: () -> Unit, noneLabel: String? = null) {
    val p = LocalNova.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.foundation.layout.Box(
            Modifier.width(560.dp).fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(28.dp))
                .background(if (p.dark) Color(0xFF0E1426) else Color(0xFFF7F9FD))
        ) {
            Glass(Modifier.fillMaxWidth().fillMaxHeight(), corner = 28.dp) {
                Column(Modifier.padding(20.dp)) {
                    Text(title, style = NovaType.title, color = p.text)
                    Spacer(Modifier.height(12.dp))
                    LazyColumn {
                        if (allowNone) item {
                            Text(
                                noneLabel ?: tr("Automatic (detect)", "تلقائي (اكتشاف)"), style = NovaType.body, color = p.accent,
                                modifier = Modifier.fillMaxWidth().clickable { onPick(null) }.padding(vertical = 14.dp, horizontal = 8.dp)
                            )
                        }
                        items(apps, key = { it.key }) { app ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onPick(app) }.padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(app.icon, null, Modifier.size(40.dp))
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(app.label, style = NovaType.body, color = p.text)
                                    Text(app.packageName, style = NovaType.label, color = p.textDim)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
