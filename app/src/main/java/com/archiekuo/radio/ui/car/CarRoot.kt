package com.archiekuo.radio.ui.car

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.model.Category
import com.archiekuo.radio.model.Station
import com.archiekuo.radio.player.PlayerState
import com.archiekuo.radio.ui.components.AuthorLine
import com.archiekuo.radio.ui.components.Backdrop
import com.archiekuo.radio.ui.components.CircleIconButton
import com.archiekuo.radio.ui.components.GoldText
import com.archiekuo.radio.ui.components.PlayRing
import com.archiekuo.radio.ui.components.StatusLine
import com.archiekuo.radio.ui.components.goldCard
import com.archiekuo.radio.ui.screens.CategoryScreen
import com.archiekuo.radio.ui.screens.FavoritesScreen
import com.archiekuo.radio.ui.screens.HistoryScreen
import com.archiekuo.radio.ui.screens.MineScreen
import com.archiekuo.radio.ui.screens.SearchScreen
import com.archiekuo.radio.ui.screens.SettingsScreen
import com.archiekuo.radio.ui.screens.talkIcon
import com.archiekuo.radio.ui.theme.GoldButtonBrush
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.viewmodel.MainViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("首頁", Icons.Rounded.Home),
    CATS("分類", Icons.Rounded.GridView),
    FAV("收藏", Icons.Rounded.Favorite),
    SEARCH("搜尋", Icons.Rounded.Search),
    MINE("我的", Icons.Rounded.Person),
}

/** 網路收音機 車機版（16:9 橫式，照 1280×720 設計） */
@Composable
fun CarRoot(vm: MainViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var catKey by rememberSaveable { mutableStateOf("classical") }
    var mineSub by rememberSaveable { mutableStateOf("") }
    val ps by vm.playerState.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    BackHandler(enabled = tab != Tab.HOME || mineSub.isNotEmpty()) {
        if (mineSub.isNotEmpty()) mineSub = "" else tab = Tab.HOME
    }

    Backdrop {
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Rail(tab) { t -> tab = t; mineSub = "" }
            NowPlaying(ps, ps.station?.let { s -> favs.any { it.id == s.id } } == true, vm, Modifier.width(470.dp).fillMaxHeight())
            Box(Modifier.weight(1f).fillMaxHeight().goldCard(RoundedCornerShape(24.dp)).padding(vertical = 4.dp)) {
                when (tab) {
                    Tab.HOME -> CarHome { k -> catKey = k; tab = Tab.CATS }
                    Tab.CATS -> CategoryScreen(vm, catKey, onSearch = { tab = Tab.SEARCH }, onOpenPlayer = {})
                    Tab.FAV -> FavoritesScreen(vm, onOpenPlayer = {})
                    Tab.SEARCH -> SearchScreen(vm, onBack = { tab = Tab.HOME }, onOpenPlayer = {})
                    Tab.MINE -> when (mineSub) {
                        "hist" -> HistoryScreen(vm, onBack = { mineSub = "" }, onOpenPlayer = {})
                        "settings" -> SettingsScreen(vm, onBack = { mineSub = "" })
                        else -> MineScreen(vm, onHistory = { mineSub = "hist" }, onSettings = { mineSub = "settings" }, onFavorites = { tab = Tab.FAV })
                    }
                }
            }
        }
    }
}

/** 最左邊的直式選單 */
@Composable
private fun Rail(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(
        Modifier.width(112.dp).fillMaxHeight().goldCard(RoundedCornerShape(24.dp)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Tab.entries.forEach { t ->
            val on = t == selected
            Column(
                Modifier.size(96.dp, 104.dp).clip(RoundedCornerShape(20.dp))
                    .then(if (on) Modifier.background(GoldButtonBrush) else Modifier)
                    .clickable(role = Role.Tab) { onSelect(t) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Icon(t.icon, null, tint = if (on) Color(0xFF1A1405) else RC.Gold, modifier = Modifier.size(38.dp))
                Spacer(Modifier.height(6.dp))
                Text(t.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (on) Color(0xFF1A1405) else Color.White)
            }
        }
    }
}

/** 左半：正在播放 */
@Composable
private fun NowPlaying(ps: PlayerState, fav: Boolean, vm: MainViewModel, modifier: Modifier) {
    val st = ps.station
    Column(modifier.goldCard(RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoldText("網路收音機", 28.sp, Modifier.weight(1f), letterSpacing = 2.sp)
            AuthorLine()
        }
        if (st == null) {
            Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.Radio, null, tint = RC.Gold, modifier = Modifier.size(88.dp))
                Text("還沒有選電台", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 12.dp))
                Text("在右邊挑一個分類，點電台就開始播", fontSize = 19.sp, color = RC.Sub, modifier = Modifier.padding(top = 6.dp))
            }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Image(
                painterResource(Categories.byKey(st.categoryKey).art), "電台封面", contentScale = ContentScale.Crop,
                modifier = Modifier.size(170.dp).shadow(20.dp, RoundedCornerShape(22.dp), ambientColor = RC.Gold, spotColor = RC.Gold).goldCard(RoundedCornerShape(22.dp), borderAlpha = 0.5f),
            )
            Column(Modifier.weight(1f)) {
                Text(st.name, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 37.sp)
                Spacer(Modifier.height(10.dp))
                StatusLine(ps, fontSize = 19)
            }
        }
        // 節目來源
        Column(Modifier.fillMaxWidth().goldCard(RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf(
                Triple(Icons.Rounded.Place, "地區", st.place),
                Triple(Icons.Rounded.Translate, "語言", st.languageZh),
                Triple(Icons.Rounded.GraphicEq, "音質", st.quality),
                Triple(Icons.Rounded.Storage, "資料", st.sourceText),
            ).filter { it.third.isNotBlank() }.forEach { (icon, k, v) ->
                Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = RC.Gold, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(k, fontSize = 18.sp, color = RC.Sub, modifier = Modifier.width(52.dp))
                    Text(v, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Rounded.SkipPrevious, "上一台", vm::previous, size = 78.dp, iconSize = 42.dp)
            PlayRing(ps.isPlaying || ps.buffering && ps.error == null, vm::toggle, 108.dp)
            CircleIconButton(Icons.Rounded.SkipNext, "下一台", vm::next, size = 78.dp, iconSize = 42.dp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton(if (fav) "已收藏" else "加入收藏", if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, fav, Modifier.weight(1f)) { vm.toggleFavorite(st) }
            val sleepLabel = if (ps.sleepMinutes > 0) "睡眠 ${ps.sleepMinutes} 分" else "睡眠定時"
            BigButton(sleepLabel, Icons.Rounded.Bedtime, ps.sleepMinutes > 0, Modifier.weight(1f)) {
                val next = when (ps.sleepMinutes) { 0 -> 15; 15 -> 30; 30 -> 60; 60 -> 90; else -> 0 }
                vm.setSleep(next)
            }
        }
    }
}

@Composable
private fun BigButton(text: String, icon: ImageVector, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.height(56.dp).clip(shape)
            .then(if (on) Modifier.background(GoldButtonBrush) else Modifier.goldCard(shape))
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (on) Color(0xFF1A1405) else Color.White
        Icon(icon, null, tint = if (on) fg else RC.Gold, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
    }
}

/** 右半首頁：大方塊分類 */
@Composable
private fun CarHome(onCategory: (String) -> Unit) {
    val sections = listOf("熱門電台" to Categories.hotStations, "音樂分類" to Categories.music, "講話節目" to Categories.talkHome)
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        sections.forEach { (title, list) ->
            if (list.isEmpty()) return@forEach
            item(span = { GridItemSpan(3) }) {
                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }
            items(list, key = { "${title}-${it.key}" }) { c -> CarTile(c, title == "講話節目") { onCategory(c.key) } }
        }
    }
}

@Composable
private fun CarTile(c: Category, talk: Boolean, onClick: () -> Unit) {
    Box(Modifier.height(130.dp).goldCard(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClick = onClick)) {
        if (talk) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(c.tint), Color(0xFF121A46)))))
            Icon(talkIcon(c.key), null, tint = RC.Gold, modifier = Modifier.size(52.dp).align(Alignment.TopCenter).padding(top = 18.dp))
        } else {
            Image(painterResource(c.art), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xE6070C26))))
        }
        Text(c.title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp))
    }
}
