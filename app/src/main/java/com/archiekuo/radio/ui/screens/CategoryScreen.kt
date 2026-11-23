package com.archiekuo.radio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.viewmodel.Load
import com.archiekuo.radio.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryScreen(vm: MainViewModel, initialKey: String, onSearch: () -> Unit, onOpenPlayer: () -> Unit) {
    var key by rememberSaveable(initialKey) { mutableStateOf(initialKey) }
    LaunchedEffect(key) { vm.load(key) }
    val cats by vm.cats.collectAsStateWithLifecycle()
    val ps by vm.playerState.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val cat = Categories.byKey(key)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("音樂分類") { CircleIconButton(Icons.Rounded.Search, "搜尋", onSearch) }
        LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
            item {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Categories.all.forEach { c -> Pill(c.title, c.key == key, { key = c.key }) }
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
