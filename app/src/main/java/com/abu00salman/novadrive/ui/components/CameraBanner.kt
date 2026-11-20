package com.abu00salman.novadrive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.data.nav.Geo

/** Top banner while a speed camera (ساهر) is ahead: distance and the camera's limit when mapped. */
@Composable
fun CameraBanner(c: AppContainer) {
    val alert by c.cameraAlert.collectAsState()
    val a = alert ?: return
    val ar = c.settings.current.arabic
    val speed = c.fix.collectAsState().value?.speedKmh ?: 0f
    val over = a.camera.maxKmh != null && speed > a.camera.maxKmh + 2
    Box(Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier.background(if (over) Color(0xFFD7262E) else Color(0xEE15181D), RoundedCornerShape(30.dp))
                .border(2.dp, Color(0xFFFFC233), RoundedCornerShape(30.dp))
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.PhotoCamera, null, tint = Color(0xFFFFC233), modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                (if (ar) "ساهر بعد " else "Speed camera in ") + Geo.formatDistance(a.distanceM, ar),
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            )
            a.camera.maxKmh?.let { lim ->
                Spacer(Modifier.width(14.dp))
                Box(Modifier.size(46.dp).background(Color.White, CircleShape).border(5.dp, Color(0xFFD7262E), CircleShape), contentAlignment = Alignment.Center) {
                    Text("$lim", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
