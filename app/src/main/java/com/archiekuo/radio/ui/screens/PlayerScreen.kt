package com.archiekuo.radio.ui.screens

import android.media.AudioManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.ui.components.StationArt
import com.archiekuo.radio.ui.components.AuthorLine
import com.archiekuo.radio.ui.components.CircleIconButton
import com.archiekuo.radio.ui.components.MessageBox
import com.archiekuo.radio.ui.components.Pill
import com.archiekuo.radio.ui.components.PlayRing
import com.archiekuo.radio.ui.components.StatusLine
import com.archiekuo.radio.ui.components.goldCard
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ps by vm.playerState.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val st = ps.station
    val fav = st != null && favs.any { it.id == st.id }

    // 剛好一整頁：內容依螢幕高度平均分配；螢幕太矮（或字很大）才可以往下滑
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageH = maxHeight
        val cover = (pageH * 0.26f).coerceIn(150.dp, 240.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = pageH),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(Icons.Rounded.ArrowBackIosNew, "返回", onBack, iconSize = 22.dp)
                Text("正在播放", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                CircleIconButton(
                    if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (fav) "取消收藏" else "收藏",
                    { st?.let { vm.toggleFavorite(it) } },
                    tint = RC.Gold,
                )
            }

            if (st == null) {
                MessageBox("還沒有選電台", "回到首頁，挑一個喜歡的電台吧", "回首頁", onBack)
                return@Column
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
                    StationArt(st, cover, 26.dp, Modifier.shadow(36.dp, RoundedCornerShape(26.dp), ambientColor = RC.Gold, spotColor = RC.Gold))
                    Text(
                        st.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 34.sp,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp),
                    )
                    Box(Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp)) { StatusLine(ps, fontSize = 17) }
                    ps.nowPlaying?.let {
                        Text(
                            "♪ $it", fontSize = 17.sp, color = RC.Gold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
                        )
                    }
                }
                SourceCard(st)
                Row(
                    Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(34.dp),
                ) {
                    CircleIconButton(Icons.Rounded.SkipPrevious, "上一台", vm::previous, size = 64.dp, iconSize = 34.dp)
                    PlayRing(ps.isPlaying || ps.buffering && ps.error == null, vm::toggle, 92.dp)
                    CircleIconButton(Icons.Rounded.SkipNext, "下一台", vm::next, size = 64.dp, iconSize = 34.dp)
                }
                VolumeRow()
                SleepRow(ps.sleepMinutes, ps.sleepEndAt, vm::setSleep)
                AuthorLine()
            }
        }
    }
}

/** 節目來源：兩欄排列（地區、語言、音質、資料），官方網站是右上角的小按鈕 */
@Composable
private fun SourceCard(st: com.archiekuo.radio.model.Station) {
    val ctx = LocalContext.current
    val cells = listOf("地區" to st.place, "語言" to st.languageZh, "音質" to st.quality, "資料" to (if (st.curated) "精選" else "公開資料庫"))
        .filter { it.second.isNotBlank() }
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .goldCard(RoundedCornerShape(18.dp))
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("節目來源", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RC.Gold, modifier = Modifier.weight(1f))
            if (st.homepage.startsWith("http")) {
                Text(
                    "官方網站 ›", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RC.Gold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, RC.Gold.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .clickable { runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(st.homepage))) } }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
        cells.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                row.forEach { (k, v) ->
                    Column(Modifier.weight(1f)) {
                        Text(k, fontSize = 14.sp, color = RC.Sub)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(v, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (k == "資料" && st.curated) {
                                Text(
                                    "已實測", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RC.GreenText,
                                    modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(8.dp)).background(RC.GreenText.copy(alpha = 0.15f)).padding(horizontal = 7.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VolumeRow() {
    val ctx = LocalContext.current
    val am = remember { ctx.getSystemService(AudioManager::class.java) }
    val max = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var vol by remember { mutableFloatStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.VolumeDown, "音量", tint = RC.Sub, modifier = Modifier.size(30.dp))
        Slider(
            value = vol,
            onValueChange = {
                vol = it
                am.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0)
            },
            valueRange = 0f..max,
            colors = SliderDefaults.colors(
                thumbColor = RC.GoldLight,
                activeTrackColor = RC.Gold,
                inactiveTrackColor = Color(0x26FFFFFF),
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Icon(Icons.Rounded.VolumeUp, null, tint = RC.Sub, modifier = Modifier.size(30.dp))
    }
}

/** 睡眠定時：一排（設定後在按鈕上顯示剩幾分鐘） */
@Composable
private fun SleepRow(minutes: Int, endAt: Long, onSet: (Int) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endAt) {
        while (endAt > 0) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val left = if (endAt > 0) ((endAt - now + 59_999) / 60_000).coerceAtLeast(0) else 0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Bedtime, null, tint = RC.Gold, modifier = Modifier.size(22.dp))
        Text(if (endAt > 0) "剩${left}分" else "睡眠", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RC.Gold, maxLines = 1, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to "關閉", 15 to "15分", 30 to "30分", 60 to "60分", 90 to "90分").forEach { (m, label) ->
                Pill(label, minutes == m, { onSet(m) }, Modifier.weight(1f), hPad = 0.dp)
            }
        }
    }
}
