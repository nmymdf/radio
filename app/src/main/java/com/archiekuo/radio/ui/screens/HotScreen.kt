package com.archiekuo.radio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.ui.components.FavButton
import com.archiekuo.radio.ui.components.GoldPlayButton
import com.archiekuo.radio.ui.components.LoadingBox
import com.archiekuo.radio.ui.components.MessageBox
import com.archiekuo.radio.ui.components.ScreenHeader
import com.archiekuo.radio.ui.components.StationRow
import com.archiekuo.radio.ui.theme.GoldButtonBrush
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.viewmodel.Load
import com.archiekuo.radio.viewmodel.MainViewModel

/**
 * 熱門電台：上面選國家（台灣、美國、英國、韓國、日本、香港），
 * 下面是該國排行：先是實測可播的精選大台，再依實際收聽次數排。
 */
@Composable
fun HotScreen(vm: MainViewModel, initialCc: String, onBack: (() -> Unit)?, onOpenPlayer: () -> Unit) {
    var cc by rememberSaveable(initialCc) { mutableStateOf(initialCc) }
    LaunchedEffect(cc) { vm.loadHot(cc) }
    val cats by vm.cats.collectAsStateWithLifecycle()
    val ps by vm.playerState.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(cc) { listState.scrollToItem(0) }
    val country = Categories.hotCountries.firstOrNull { it.first == cc }?.second ?: cc

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("熱門電台", onBack)
        LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
            // 國家按鈕：一排三個，對齊排列
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Categories.hotCountries.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (code, name) ->
                                val on = code == cc
                                val shape = RoundedCornerShape(16.dp)
                                Box(
                                    Modifier.weight(1f).height(56.dp).clip(shape)
                                        .then(if (on) Modifier.background(GoldButtonBrush) else Modifier.background(Color(0x0FFFFFFF)).border(1.dp, RC.Gold.copy(alpha = 0.38f), shape))
                                        .clickable(role = Role.Tab) { cc = code },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(name, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = if (on) Color(0xFF1A1405) else Color.White)
                                }
                            }
                        }
                    }
                }
            }
            when (val l = cats["hot-$cc"]) {
                is Load.Ok -> {
                    val feat = l.list.count { it.curated }
                    item {
                        Text(
                            "$country ・ 共 ${l.list.size} 台" + if (feat > 0) " ・ 前 $feat 台為實測精選大台" else "",
                            fontSize = 17.sp, color = RC.Sub, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    }
                    itemsIndexed(l.list, key = { i, s -> "$cc-$i-${s.id}" }) { i, s ->
                        val play = { vm.play(s, l.list); onOpenPlayer() }
                        StationRow(
                            s, ps.station?.id == s.id, play,
                            leading = {
                                Text(
                                    "${i + 1}", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                    color = if (i < 3) RC.Gold else RC.Sub, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp),
                                )
                            },
                        ) {
                            FavButton(favs.any { it.id == s.id }) { vm.toggleFavorite(s) }
                            GoldPlayButton(play)
                        }
                    }
                }
                is Load.Err -> item { MessageBox(l.msg, action = "再試一次", onAction = { vm.loadHot(cc, force = true) }) }
                else -> item { LoadingBox() }
            }
        }
    }
}
