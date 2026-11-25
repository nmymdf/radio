package com.archiekuo.radio.ui.screens

import android.media.AudioManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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

        StationArt(
            st, 240.dp, 26.dp,
            Modifier
                .padding(top = 22.dp, bottom = 20.dp)
                .shadow(36.dp, RoundedCornerShape(26.dp), ambientColor = RC.Gold, spotColor = RC.Gold),
        )

        Text(
            st.name,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.padding(horizontal = 24.dp)) { StatusLine(ps, fontSize = 17) }
        SourceCard(st)
        ps.nowPlaying?.let {
            Text(
                "♪ $it",
                fontSize = 17.sp,
                color = RC.Gold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp),
            )
        }

        Row(
            Modifier.padding(top = 24.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(34.dp),
        ) {
            CircleIconButton(Icons.Rounded.SkipPrevious, "上一台", vm::previous, size = 64.dp, iconSize = 34.dp)
            PlayRing(ps.isPlaying || ps.buffering && ps.error == null, vm::toggle, 96.dp)
            CircleIconButton(Icons.Rounded.SkipNext, "下一台", vm::next, size = 64.dp, iconSize = 34.dp)
        }

        VolumeRow()
        SleepCard(ps.sleepMinutes, ps.sleepEndAt, vm::setSleep)
        AuthorLine(Modifier.padding(top = 16.dp, bottom = 24.dp))
    }
}

/** 節目來源：電台、地區、語言、資料、音質 */
@Composable
private fun SourceCard(st: com.archiekuo.radio.model.Station) {
    val rows = listOf(
        Triple(Icons.Rounded.Radio, "電台", st.name),
        Triple(Icons.Rounded.Place, "地區", st.place),
        Triple(Icons.Rounded.Translate, "語言", st.languageZh),
        Triple(Icons.Rounded.Storage, "資料", st.sourceText),
        Triple(Icons.Rounded.GraphicEq, "音質", st.quality),
    ).filter { it.third.isNotBlank() }
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 14.dp)
            .fillMaxWidth()
            .goldCard(RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text("節目來源", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RC.Gold, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
        rows.forEach { (icon, k, v) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = RC.Gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(k, fontSize = 17.sp, color = RC.Sub, modifier = Modifier.width(48.dp))
                Spacer(Modifier.width(8.dp))
                Text(v, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
        if (st.homepage.startsWith("http")) {
            val ctx = LocalContext.current
            Text(
                "打開電台官方網站",
                fontSize = 17.sp,
                color = RC.Gold,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 4.dp)
                    .clickable {
                        runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(st.homepage))) }
                    }
                    .padding(vertical = 8.dp),
            )
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

@Composable
private fun SleepCard(minutes: Int, endAt: Long, onSet: (Int) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endAt) {
        while (endAt > 0) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth()
            .goldCard(RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bedtime, null, tint = RC.Gold, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("睡眠定時", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = RC.Gold)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0 to "關閉", 15 to "15分", 30 to "30分", 60 to "60分", 90 to "90分").forEach { (m, label) ->
                Pill(label, minutes == m, { onSet(m) }, Modifier.weight(1f), hPad = 0.dp)
            }
        }
        val left = if (endAt > 0) ((endAt - now + 59_999) / 60_000).coerceAtLeast(0) else 0
        Text(
            if (endAt > 0) "還有 $left 分鐘後自動停止" else "選好時間，時間到就會自動停止播放",
            fontSize = 16.sp,
            color = RC.Sub,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }
}
