package com.archiekuo.carhome.ui

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.carhome.R
import com.archiekuo.carhome.media.LocalState
import com.archiekuo.carhome.media.RadioState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun rememberArt(uri: Uri?, embedded: Boolean): ImageBitmap? {
    val ctx = LocalContext.current
    var bmp by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bmp = if (uri == null) null else withContext(Dispatchers.IO) {
            runCatching {
                if (embedded) {
                    val r = MediaMetadataRetriever()
                    try {
                        r.setDataSource(ctx, uri)
                        r.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
                    } finally { r.release() }
                } else ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    return bmp
}

@Composable
private fun Cover(bmp: ImageBitmap?, fallback: Int) {
    val mod = Modifier.size(140.dp).shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = C.Gold, spotColor = C.Gold)
        .clip(RoundedCornerShape(20.dp)).border(1.dp, C.Gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    if (bmp != null) Image(bmp, "封面", mod, contentScale = ContentScale.Crop)
    else Image(painterResource(fallback), "封面", mod, contentScale = ContentScale.Crop)
}

@Composable
private fun StatusDot(color: Color, text: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 19.sp, color = textColor, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 25.sp)
    }
}

@Composable
private fun Controls(playing: Boolean, onPrev: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        CircleBtn(Icons.Rounded.SkipPrevious, "上一首", 76.dp, 40.dp, onPrev)
        PlayRing(playing, 108.dp, onToggle)
        CircleBtn(Icons.Rounded.SkipNext, "下一首", 76.dp, 40.dp, onNext)
    }
}

@Composable
fun PlayerCard(
    mode: String,
    radio: RadioState,
    local: LocalState,
    fmLabel: String?,
    autoFm: Boolean,
    onMode: (String) -> Unit,
    onRadioToggle: () -> Unit, onRadioPrev: () -> Unit, onRadioNext: () -> Unit, onOpenRadioApp: () -> Unit,
    onLocalToggle: () -> Unit, onLocalPrev: () -> Unit, onLocalNext: () -> Unit, onShuffle: () -> Unit, onRescan: () -> Unit,
    onOpenFm: () -> Unit, onPickFm: () -> Unit,
) {
    Column(Modifier.width(440.dp).fillMaxHeight().card(RoundedCornerShape(24.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("網路收音機", mode == "radio", Modifier.weight(1f)) { onMode("radio") }
            Pill("內部音樂", mode == "local", Modifier.weight(1f)) { onMode("local") }
            Pill("FM 收音機", mode == "fm", Modifier.weight(1f)) { onMode("fm") }
        }
        when (mode) {
            "local" -> LocalBody(local, onLocalToggle, onLocalPrev, onLocalNext, onShuffle, onRescan)
            "fm" -> FmBody(fmLabel, autoFm, onOpenFm, onPickFm)
            else -> RadioBody(radio, onRadioToggle, onRadioPrev, onRadioNext, onOpenRadioApp, autoFm)
        }
    }
}

@Composable
private fun ColumnScope.RadioBody(s: RadioState, onToggle: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit, onOpenApp: () -> Unit, autoFm: Boolean) {
    if (!s.installed) {
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Radio, null, tint = C.Gold, modifier = Modifier.size(80.dp))
            Text("請先安裝「網路收音機」App", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 14.dp))
            Text("裝好後回到桌面，這裡就能直接控制", fontSize = 18.sp, color = C.Sub, modifier = Modifier.padding(top = 6.dp))
        }
        return
    }
    val art = rememberArt(s.artwork, false)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Cover(art, R.drawable.art_radio)
        Column(Modifier.weight(1f)) {
            Text(s.title ?: "網路收音機", fontSize = 31.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 38.sp)
            Spacer(Modifier.height(10.dp))
            when {
                s.error && !s.playing -> StatusDot(C.Red, "連線不穩，重新連線中…", C.Red)
                s.buffering -> StatusDot(C.Gold, "連線中…", C.Gold)
                s.playing -> StatusDot(C.Green, "直播中" + (s.subtitle?.let { " ・ $it" } ?: ""), C.GreenText)
                s.title == null -> StatusDot(C.Dim, "按播放，接著聽上次的電台", C.Sub)
                else -> StatusDot(C.Dim, "已暫停", C.Sub)
            }
        }
    }
    Controls(s.playing || s.buffering, onPrev, onToggle, onNext)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GhostButton("選電台", icon = Icons.Rounded.QueueMusic, modifier = Modifier.width(140.dp), onClick = onOpenApp)
        Text(if (autoFm) "沒有網路時，自動切到 FM" else "沒網路自動切 FM：已關閉", fontSize = 17.sp, color = C.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ColumnScope.LocalBody(s: LocalState, onToggle: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit, onShuffle: () -> Unit, onRescan: () -> Unit) {
    if (s.scanning) {
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.LibraryMusic, null, tint = C.Gold, modifier = Modifier.size(72.dp))
            Text("正在找車機和隨身碟裡的歌…", fontSize = 21.sp, color = Color.White, modifier = Modifier.padding(top = 14.dp))
        }
        return
    }
    if (s.scanned && s.count == 0) {
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.MusicOff, null, tint = C.Gold, modifier = Modifier.size(72.dp))
            Text("找不到歌曲", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 12.dp))
            Text("請插入有歌曲的隨身碟，再按重新尋找", fontSize = 18.sp, color = C.Sub, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            GhostButton("重新尋找", icon = Icons.Rounded.Refresh, modifier = Modifier.width(180.dp), onClick = onRescan)
        }
        return
    }
    val art = rememberArt(s.uri, true)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Cover(art, R.drawable.art_cafe)
        Column(Modifier.weight(1f)) {
            Text(s.title ?: "內部音樂", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(s.artist?.takeIf { it.isNotBlank() } ?: (if (s.scanned) "共 ${s.count} 首" else "按播放開始聽"), fontSize = 19.sp, color = C.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (s.scanned && s.count > 0 && s.title != null) Text("第 ${s.index + 1} 首 ・ 共 ${s.count} 首", fontSize = 17.sp, color = C.Sub, modifier = Modifier.padding(top = 4.dp))
        }
    }
    Controls(s.playing, onPrev, onToggle, onNext)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GhostButton(if (s.shuffle) "隨機播放：開" else "依序播放", icon = if (s.shuffle) Icons.Rounded.Shuffle else Icons.Rounded.Repeat, modifier = Modifier.weight(1f), onClick = onShuffle)
        GhostButton("重新尋找歌曲", icon = Icons.Rounded.Refresh, modifier = Modifier.weight(1f), onClick = onRescan)
    }
}

@Composable
private fun ColumnScope.FmBody(fmLabel: String?, autoFm: Boolean, onOpen: () -> Unit, onPick: () -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.Radio, null, tint = C.Gold, modifier = Modifier.size(88.dp))
        Text(fmLabel ?: "還沒選擇 FM 收音機 App", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        Text("FM 用車機原廠的收音機播放，不需要網路", fontSize = 18.sp, color = C.Sub, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(220.dp).height(56.dp).clip(RoundedCornerShape(28.dp)).background(GoldBrush)
                .clickAndHold(onClick = { if (fmLabel == null) onPick() else onOpen() }, onLong = onPick), contentAlignment = Alignment.Center) {
                Text(if (fmLabel == null) "選擇 FM App" else "打開 FM 收音機", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Ink)
            }
            GhostButton("更換", modifier = Modifier.width(100.dp), height = 56.dp, onClick = onPick)
        }
        if (autoFm) Text("網路恢復時，會自動切回網路收音機", fontSize = 17.sp, color = C.Sub, modifier = Modifier.padding(top = 14.dp))
    }
}
