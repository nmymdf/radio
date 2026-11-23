package com.archiekuo.radio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archiekuo.radio.ui.components.CircleIconButton
import com.archiekuo.radio.ui.components.FavButton
import com.archiekuo.radio.ui.components.GoldPlayButton
import com.archiekuo.radio.ui.components.LoadingBox
import com.archiekuo.radio.ui.components.MessageBox
import com.archiekuo.radio.ui.components.StationRow
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.viewmodel.Load
import com.archiekuo.radio.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(vm: MainViewModel, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    val q by vm.query.collectAsStateWithLifecycle()
    val result by vm.search.collectAsStateWithLifecycle()
    val recent by vm.recentSearches.collectAsStateWithLifecycle()
    val ps by vm.playerState.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val fm = LocalFocusManager.current
    LaunchedEffect(Unit) { if (result == null) runCatching { focus.requestFocus() } }

    fun go(text: String = q) { fm.clearFocus(); vm.runSearch(text) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.Rounded.ArrowBackIosNew, "返回", onBack, iconSize = 22.dp)
            Row(
                Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(Color(0x99080E2C))
                    .border(1.5.dp, RC.Gold, RoundedCornerShape(27.dp))
                    .padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = RC.Gold, modifier = Modifier.size(26.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (q.isEmpty()) Text("輸入電台名稱或音樂類型", fontSize = 19.sp, color = RC.Dim, maxLines = 1)
                    BasicTextField(
                        value = q,
                        onValueChange = vm::setQuery,
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                        cursorBrush = SolidColor(RC.Gold),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { go() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                }
                if (q.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "清除") { vm.setQuery(""); runCatching { focus.requestFocus() } },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Close, "清除", tint = RC.Sub, modifier = Modifier.size(24.dp)) }
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
            if (recent.isNotEmpty()) {
                item {
                    Text("最近搜尋", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        recent.forEach { r ->
                            Row(
                                Modifier
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(23.dp))
                                    .background(Color(0x0FFFFFFF))
                                    .border(1.dp, RC.Gold.copy(alpha = 0.38f), RoundedCornerShape(23.dp))
                                    .clickable(role = Role.Button) { vm.setQuery(r); go(r) }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.History, null, tint = RC.Sub, modifier = Modifier.size(20.dp))
                                Text(r, fontSize = 18.sp, color = Color.White, maxLines = 1)
                            }
                        }
                    }
                }
            }
            when (val r = result) {
                null -> if (recent.isEmpty()) item { MessageBox("想聽什麼？", "可以輸入「爵士」、「古典」、「鋼琴」，或電台名稱") }
                is Load.Loading -> item { LoadingBox() }
                is Load.Err -> item { MessageBox(r.msg, action = "再試一次", onAction = { go() }) }
                is Load.Ok -> {
                    item { Text("「$q」的搜尋結果 ・ 共 ${r.list.size} 台", fontSize = 17.sp, color = RC.Sub, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)) }
                    itemsIndexed(r.list, key = { i, s -> "$i-${s.id}" }) { _, s ->
                        val play = { vm.play(s, r.list); onOpenPlayer() }
                        StationRow(s, ps.station?.id == s.id, play) {
                            FavButton(favs.any { it.id == s.id }) { vm.toggleFavorite(s) }
                            GoldPlayButton(play)
                        }
                    }
                }
            }
        }
    }
}
