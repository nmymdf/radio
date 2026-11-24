package com.archiekuo.radio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.ui.components.CircleIconButton
import com.archiekuo.radio.ui.components.FavButton
import com.archiekuo.radio.ui.components.GoldPlayButton
import com.archiekuo.radio.ui.components.LoadingBox
import com.archiekuo.radio.ui.components.MessageBox
import com.archiekuo.radio.ui.components.Pill
import com.archiekuo.radio.ui.components.ScreenHeader
import com.archiekuo.radio.ui.components.StationRow
import com.archiekuo.radio.ui.theme.GoldButtonBrush
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.viewmodel.Load
import com.archiekuo.radio.viewmodel.MainViewModel

@Composable
fun CategoryScreen(vm: MainViewModel, initialKey: String, onSearch: () -> Unit, onOpenPlayer: () -> Unit) {
    var key by rememberSaveable(initialKey) { mutableStateOf(initialKey) }
    var group by rememberSaveable(initialKey) { mutableStateOf(Categories.byKey(initialKey).group) }
    LaunchedEffect(key) { vm.load(key) }
    val cats by vm.cats.collectAsStateWithLifecycle()
    val ps by vm.playerState.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val cat = Categories.byKey(key)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("全部分類") { CircleIconButton(Icons.Rounded.Search, "搜尋", onSearch) }
        LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
            // 音樂／講話節目／依地區
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Categories.groups.forEach { (g, label) ->
                        val on = g == group
                        val shape = RoundedCornerShape(16.dp)
                        Row(
                            Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(shape)
                                .then(
                                    if (on) Modifier.background(GoldButtonBrush)
                                    else Modifier.background(Color(0x0FFFFFFF)).border(1.dp, RC.Gold.copy(alpha = 0.38f), shape),
                                )
                                .clickable(role = Role.Tab) {
                                    group = g
                                    if (Categories.byKey(key).group != g) Categories.inGroup(g).firstOrNull()?.let { key = it.key }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            val fg = if (on) Color(0xFF1A1405) else Color.White
                            Icon(
                                when (g) { "talk" -> Icons.Rounded.Mic; "region" -> Icons.Rounded.Place; else -> Icons.Rounded.MusicNote },
                                null, tint = fg, modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(label, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
                        }
                    }
                }
            }
            // 該組的分類（每排三個，對齊排列）
            item {
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Categories.inGroup(group).chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { c -> Pill(c.title, c.key == key, { key = c.key }, Modifier.weight(1f), hPad = 0.dp) }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            when (val l = cats[key]) {
                is Load.Ok -> {
                    item {
                        Text("${cat.title} ・ 共 ${l.list.size} 台", fontSize = 17.sp, color = RC.Sub, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                    }
                    itemsIndexed(l.list, key = { i, s -> "$i-${s.id}" }) { _, s ->
                        val play = { vm.play(s, l.list); onOpenPlayer() }
                        StationRow(s, ps.station?.id == s.id, play) {
                            FavButton(favs.any { it.id == s.id }) { vm.toggleFavorite(s) }
                            GoldPlayButton(play)
                        }
                    }
                }
                is Load.Err -> item { MessageBox(l.msg, action = "再試一次", onAction = { vm.load(key, force = true) }) }
                else -> item { LoadingBox() }
            }
        }
    }
}
