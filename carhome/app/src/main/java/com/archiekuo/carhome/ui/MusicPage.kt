package com.archiekuo.carhome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.carhome.media.LocalState
import com.archiekuo.carhome.media.Song

private data class Folder(val key: String, val name: String, val songs: List<Song>)

/** 點內部音樂的封面：全畫面選歌（資料夾／全部歌曲／最近播放） */
@Composable
fun MusicPage(
    state: LocalState, library: List<Song>, recent: List<String>,
    onPlay: (String) -> Unit, onShuffle: () -> Unit, onRescan: () -> Unit, onBack: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val folders = remember(library) {
        val groups = library.groupBy { it.folder }
        val names = groups.keys.associateWith { it.substringAfterLast('/').ifBlank { it } }
        val dup = names.values.groupingBy { it }.eachCount()
        groups.map { (k, v) ->
            val n = names[k]!!
            // 同名資料夾加上上一層名稱，才分得出來
            val shown = if ((dup[n] ?: 0) > 1) k.substringBeforeLast('/').substringAfterLast('/') + " / " + n else n
            Folder(k, shown, v)
        }
    }
    var folderKey by rememberSaveable { mutableStateOf<String?>(null) }
    // 預設選正在播的歌所在的資料夾
    LaunchedEffect(folders, state.id) {
        if (folderKey == null || folders.none { it.key == folderKey }) {
            folderKey = library.firstOrNull { it.id == state.id }?.folder ?: folders.firstOrNull()?.key
        }
    }
    val byId = remember(library) { library.associateBy { it.id } }

    PanelFrame("內部音樂", onBack) {
        if (state.scanning || (!state.scanned && library.isEmpty())) { Centered(Icons.Rounded.LibraryMusic, "正在找車機和隨身碟裡的歌…", null, null); return@PanelFrame }
        if (library.isEmpty()) { Centered(Icons.Rounded.MusicOff, "找不到歌曲", "請插入有歌曲的隨身碟，再按重新尋找", onRescan); return@PanelFrame }

        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // 左邊：分頁＋資料夾
            Column(Modifier.width(420.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabPill(Icons.Rounded.Folder, "資料夾", tab == 0, Modifier.weight(1f)) { tab = 0 }
                    TabPill(Icons.Rounded.MusicNote, "全部歌曲", tab == 1, Modifier.weight(1f)) { tab = 1 }
                    TabPill(Icons.Rounded.History, "最近播放", tab == 2, Modifier.weight(1f)) { tab = 2 }
                }
                Column(Modifier.fillMaxWidth().weight(1f).card(RoundedCornerShape(22.dp)).padding(10.dp)) {
                    if (tab == 0) {
                        val ls = rememberLazyListState()
                        LazyColumn(Modifier.weight(1f), state = ls) {
                            itemsIndexed(folders, key = { _, f -> f.key }) { _, f ->
                                val on = f.key == folderKey
                                Row(
                                    Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(16.dp))
                                        .then(if (on) Modifier.background(C.Gold.copy(alpha = 0.14f)).border(1.dp, C.Gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)) else Modifier)
                                        .clickable { folderKey = f.key }.padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.Folder, null, tint = C.Gold, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Text(f.name, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = if (on) C.Gold else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text("${f.songs.size} 首", fontSize = 18.sp, color = C.Sub)
                                }
                            }
                        }
                    } else {
                        // 全部歌曲／最近播放：左邊放播放方式
                        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("共 ${library.size} 首 ・ ${folders.size} 個資料夾", fontSize = 21.sp, color = Color.White)
                            GhostButton(if (state.shuffle) "隨機播放：開" else "依序播放", Modifier.fillMaxWidth(), if (state.shuffle) Icons.Rounded.Shuffle else Icons.Rounded.Repeat, height = 60.dp, onClick = onShuffle)
                            GhostButton("重新尋找歌曲", Modifier.fillMaxWidth(), Icons.Rounded.Refresh, height = 60.dp, onClick = onRescan)
                            if (state.title != null) {
                                Spacer(Modifier.weight(1f))
                                Text("正在播放", fontSize = 18.sp, color = C.Sub)
                                Text(state.title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = C.Gold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            // 右邊：歌曲清單
            val (title, songs) = when (tab) {
                0 -> folders.firstOrNull { it.key == folderKey }.let { (it?.name ?: "資料夾") to (it?.songs ?: emptyList()) }
                1 -> "全部歌曲" to library
                else -> "最近播放" to recent.mapNotNull { byId[it] }
            }
            Column(Modifier.weight(1f).fillMaxHeight().card(RoundedCornerShape(22.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$title ・ ${songs.size} 首", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (songs.isNotEmpty()) GhostButton("從第一首播放", Modifier.padding(start = 12.dp), Icons.Rounded.PlayArrow, height = 50.dp) { onPlay(songs.first().id) }
                }
                if (songs.isEmpty()) {
                    Text(if (tab == 2) "還沒有播放紀錄" else "這裡沒有歌", fontSize = 21.sp, color = C.Sub, modifier = Modifier.padding(16.dp))
                } else {
                    val ls = rememberLazyListState()
                    // 打開時捲到正在播的那首
                    LaunchedEffect(title) {
                        val i = songs.indexOfFirst { it.id == state.id }
                        if (i > 2) ls.scrollToItem(i - 2)
                    }
                    LazyColumn(Modifier.fillMaxSize(), state = ls) {
                        itemsIndexed(songs, key = { i, s -> "$i|${s.id}" }) { i, s ->
                            val now = s.id == state.id
                            Row(
                                Modifier.fillMaxWidth().height(76.dp).clickable { onPlay(s.id) }.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                                    if (now) Icon(if (state.playing) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote, null, tint = C.Gold, modifier = Modifier.size(28.dp))
                                    else Text("${i + 1}", fontSize = 19.sp, color = C.Dim, textAlign = TextAlign.Center)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(s.title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = if (now) C.Gold else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (s.artist.isNotBlank()) Text(s.artist, fontSize = 17.sp, color = C.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Box(Modifier.size(50.dp).clip(CircleShape).background(GoldBrush), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.PlayArrow, "播放", tint = C.Ink, modifier = Modifier.size(28.dp))
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                        }
                    }
                }
            }
        }
        Text("點歌就播 ・ 按「首頁」回桌面，歌會繼續播", fontSize = 17.sp, color = C.Sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun TabPill(icon: ImageVector, text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier.height(52.dp).clip(shape)
            .then(if (selected) Modifier.background(GoldBrush) else Modifier.background(Color(0x0FFFFFFF)).border(1.dp, C.Gold.copy(alpha = 0.38f), shape))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (selected) C.Ink else C.Gold, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = if (selected) C.Ink else Color.White, maxLines = 1)
    }
}

@Composable
private fun ColumnScope.Centered(icon: ImageVector, title: String, sub: String?, onRescan: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = C.Gold, modifier = Modifier.size(90.dp))
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 14.dp))
        if (sub != null) Text(sub, fontSize = 20.sp, color = C.Sub, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp))
        if (onRescan != null) GhostButton("重新尋找", Modifier.width(200.dp), Icons.Rounded.Refresh, height = 56.dp, onClick = onRescan)
    }
}
