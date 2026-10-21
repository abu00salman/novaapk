package com.abu00salman.novadrive.ui.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.MediaInfo
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.RoundButton
import com.abu00salman.novadrive.ui.home.Artwork
import com.abu00salman.novadrive.ui.home.EqBars
import com.abu00salman.novadrive.ui.home.MediaProgress
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr

@Composable
fun MusicScreen(
    info: MediaInfo?,
    hasAccess: Boolean,
    sourceApp: AppEntry?,
    musicApps: List<AppEntry>,
    onOpenSource: () -> Unit,
    onGrantAccess: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onQueueItem: (Long) -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalNova.current
    Row(modifier.fillMaxSize()) {
        Glass(Modifier.weight(0.58f).fillMaxHeight()) {
            Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(info, 280.dp, 28.dp, onOpenSource)
                Spacer(Modifier.width(26.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(p.text.copy(alpha = 0.06f)).clickable(onClick = onOpenSource)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sourceApp?.let { Image(it.icon, null, Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)) }
                        Text(sourceApp?.label ?: tr("Music", "الموسيقى"), style = NovaType.label, color = p.text, maxLines = 1)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(info?.title ?: tr("Nothing playing", "لا يوجد تشغيل"), style = NovaType.display.copy(fontSize = 32.sp), color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(info?.artist ?: tr("Start music in any app — it appears here.", "شغّل الموسيقى من أي تطبيق وستظهر هنا."),
                        style = NovaType.title.copy(fontSize = 18.sp), color = p.textDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!hasAccess) Text(
                        tr("Allow access to show track details ›", "اسمح بالوصول لعرض تفاصيل المقطع ›"), style = NovaType.label, color = p.accent,
                        modifier = Modifier.padding(top = 6.dp).clickable(onClick = onGrantAccess)
                    )
                    Spacer(Modifier.height(22.dp))
                    MediaProgress(info, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        RoundButton(Icons.Rounded.SkipPrevious, tr("Previous", "السابق"), 64.dp, onClick = onPrev)
                        RoundButton(if (info?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, tr("Play / pause", "تشغيل / إيقاف"), 84.dp, primary = true, onClick = onPlayPause)
                        RoundButton(Icons.Rounded.SkipNext, tr("Next", "التالي"), 64.dp, onClick = onNext)
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 30.dp)) { if (info?.playing == true) EqBars(Modifier.fillMaxSize()) }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(0.42f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Glass(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.QueueMusic, null, tint = p.accent)
                        Spacer(Modifier.width(8.dp))
                        Text(info?.queueTitle ?: tr("Up next", "قائمة التشغيل"), style = NovaType.title, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(8.dp))
                    val queue = info?.queue.orEmpty()
                    if (queue.isEmpty()) Text(
                        tr("The playing app doesn't share its queue.", "التطبيق الحالي لا يشارك قائمة التشغيل."),
                        style = NovaType.label, color = p.textDim
                    ) else LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(queue, key = { it.id }) { q ->
                            val current = q.id == info?.activeQueueId
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(if (current) p.accent.copy(alpha = 0.16f) else p.accent.copy(alpha = 0f))
                                    .clickable { onQueueItem(q.id) }.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(q.title, style = NovaType.body, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    q.subtitle?.let { Text(it, style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                }
                                if (current) Icon(Icons.Rounded.GraphicEq, null, tint = p.accent)
                            }
                        }
                    }
                }
            }
            Glass(Modifier.fillMaxWidth().height(148.dp)) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(tr("Sources", "مصادر الصوت"), style = NovaType.title, color = p.text)
                    Spacer(Modifier.height(10.dp))
                    if (musicApps.isEmpty()) Text(tr("No music apps found", "لا توجد تطبيقات موسيقى"), style = NovaType.label, color = p.textDim)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(musicApps, key = { it.key }) { app ->
                            Column(Modifier.width(72.dp).clickable { onLaunchApp(app) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(app.icon, null, Modifier.size(52.dp))
                                Text(app.label, style = NovaType.small, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
