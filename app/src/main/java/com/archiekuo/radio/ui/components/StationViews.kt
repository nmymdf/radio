package com.archiekuo.radio.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.model.Station
import com.archiekuo.radio.player.PlayerState
import com.archiekuo.radio.ui.theme.GoldButtonBrush
import com.archiekuo.radio.ui.theme.RC

/**
 * 電台封面：有台標就顯示台標（白底），沒有或太小就顯示「名稱標章」
 * （電台名稱前兩個字，每台顏色不同），一眼就分得出是哪一台。
 */
@Composable
fun StationArt(station: Station, size: Dp, corner: Dp = 12.dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(corner)
    var logoOk by remember(station.favicon) { mutableStateOf(false) }
    Box(
        modifier
            .size(size)
            .clip(shape)
            .border(1.dp, RC.Gold.copy(alpha = 0.45f), shape),
    ) {
        StationBadge(station.name, Modifier.fillMaxSize(), size)
        if (station.favicon.startsWith("http")) {
            val ctx = LocalContext.current
            Box(Modifier.fillMaxSize().then(if (logoOk) Modifier.background(Color(0xFFF8F4EA)) else Modifier)) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(station.favicon).crossfade(true).size(256).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    onSuccess = { st ->
                        // 太小的圖示（例如 16×16）放大會糊，改用名稱標章
                        val d = st.result.drawable
                        logoOk = d.intrinsicWidth >= 48 && d.intrinsicHeight >= 48
                    },
                    onError = { logoOk = false },
                    alpha = if (logoOk) 1f else 0f,
                    modifier = Modifier.fillMaxSize().padding(size * 0.08f),
                )
            }
        }
    }
}

private val badgeColors = listOf(
    0xFF2B3A8C to 0xFF141B4D, 0xFF7A2E3B to 0xFF3A1220, 0xFF1F6B5C to 0xFF0C2E28, 0xFF6B4A1F to 0xFF2E1E0A,
    0xFF4B2E7A to 0xFF1F1238, 0xFF1F557A to 0xFF0B2236, 0xFF7A5A1F to 0xFF362508, 0xFF5C2E5C to 0xFF26102A,
)

/** 名稱標章上的字：中文取前兩個字；英文取縮寫（BBC、NPR、ICRT） */
fun badgeText(name: String): String {
    val n = name.trim()
    if (n.isEmpty()) return "♪"
    val isHan = { c: Char -> Character.UnicodeScript.of(c.code) == Character.UnicodeScript.HAN }
    if (isHan(n.first())) return n.filter(isHan).take(2)
    val words = n.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotBlank() }
    // 開頭的單一數字或符號（例如「0 - Classic Rock」的 0）不要拿來當標章
    val ws = words.dropWhile { it.length == 1 && it[0].isDigit() }.ifEmpty { words }
    val first = ws.firstOrNull() ?: return n.filter(isHan).take(2).ifEmpty { "♪" }
    return when {
        first.length in 2..5 && first.all { it.isUpperCase() || it.isDigit() } -> first
        first.length <= 4 -> first.uppercase()
        ws.size >= 2 -> ws.take(3).joinToString("") { it.first().uppercase() }
        else -> first.take(3).uppercase()
    }
}

@Composable
fun StationBadge(name: String, modifier: Modifier, size: Dp) {
    val (a, b) = badgeColors[(name.lowercase().hashCode() and 0x7fffffff) % badgeColors.size]
    val t = badgeText(name)
    val k = when (t.length) { 1, 2 -> 0.34f; 3 -> 0.27f; 4 -> 0.22f; else -> 0.19f }
    val fs = with(LocalDensity.current) { (size * k).toSp() }
    Box(modifier.background(Brush.linearGradient(listOf(Color(a), Color(b)))), contentAlignment = Alignment.Center) {
        Text(t, fontSize = fs, fontWeight = FontWeight.Black, color = RC.Gold, maxLines = 1, softWrap = false)
    }
}

@Composable
fun GoldPlayButton(onClick: () -> Unit, size: Dp = 48.dp, playing: Boolean = false) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(GoldButtonBrush)
            .clickable(role = Role.Button, onClickLabel = if (playing) "暫停" else "播放", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (playing) "暫停" else "播放",
            tint = Color(0xFF1A1405),
            modifier = Modifier.size(size * 0.58f),
        )
    }
}

@Composable
fun FavButton(fav: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = if (fav) "取消收藏" else "收藏", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (fav) "已收藏" else "收藏",
            tint = if (fav) RC.Gold else RC.Dim,
            modifier = Modifier.size(28.dp),
        )
    }
}

/** 一列電台：封面、名稱、說明、右側按鈕 */
@Composable
fun StationRow(
    station: Station,
    current: Boolean,
    onClick: () -> Unit,
    subtitle: String = station.subtitle,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 86.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        StationArt(station, 62.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (current) {
                    Icon(Icons.Rounded.GraphicEq, "正在播放", tint = RC.Gold, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    station.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (current) RC.Gold else Color.White,
                    maxLines = 2,
                    lineHeight = 25.sp,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(subtitle, fontSize = 15.sp, color = RC.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
        }
        trailing()
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(RC.Line),
    )
}

@Composable
fun StatusLine(state: PlayerState, fontSize: Int = 15) {
    val (dot, text, color) = when {
        state.error != null -> Triple(RC.Red, state.error, RC.Red)
        state.buffering -> Triple(RC.Gold, "連線中…", RC.Gold)
        state.isPlaying -> Triple(RC.Green, state.station?.liveLine ?: "直播中", RC.GreenText)
        else -> Triple(RC.Dim, "已暫停", RC.Sub)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = fontSize.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PlayRing(playing: Boolean, onClick: () -> Unit, size: Dp) {
    Box(
        Modifier
            .size(size)
            .shadow(size / 4, CircleShape, ambientColor = RC.Gold, spotColor = RC.Gold)
            .clip(CircleShape)
            .background(Color(0xE6080C24))
            .border(if (size > 70.dp) 3.dp else 2.5.dp, RC.Gold, CircleShape)
            .clickable(role = Role.Button, onClickLabel = if (playing) "暫停" else "播放", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (playing) "暫停" else "播放",
            tint = RC.Gold,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
private fun PlainIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Color.White, modifier = Modifier.size(32.dp)) }
}

/** 底部迷你播放器 */
@Composable
fun MiniPlayer(state: PlayerState, onOpen: () -> Unit, onToggle: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit) {
    val s = state.station ?: return
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(84.dp)
            .goldCard(shape, Brush.horizontalGradient(listOf(Color(0xF2202A60), Color(0xF2141C48))))
            .clickable(onClickLabel = "打開播放頁", onClick = onOpen)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StationArt(s, 60.dp)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(s.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            StatusLine(state)
        }
        PlainIconButton(Icons.Rounded.SkipPrevious, "上一台", onPrev)
        PlayRing(state.isPlaying || state.buffering && state.error == null, onToggle, 58.dp)
        PlainIconButton(Icons.Rounded.SkipNext, "下一台", onNext)
    }
}

/** 底部導覽列 */
@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("首頁" to Icons.Rounded.Home, "分類" to Icons.Rounded.GridView, "收藏" to Icons.Rounded.Favorite, "我的" to Icons.Rounded.Person)
    Row(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xF50E163E), Color(0xFA080C26))))
            .border(1.dp, RC.Gold.copy(alpha = 0.3f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
            .height(80.dp),
    ) {
        items.forEachIndexed { i, (label, icon) ->
            val on = i == selected
            val color = if (on) RC.Gold else Color(0xFFC3C9E4)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable(role = Role.Tab, onClick = { onSelect(i) }),
                contentAlignment = Alignment.Center,
            ) {
                if (on) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .width(44.dp)
                            .height(3.dp)
                            .shadow(6.dp, RoundedCornerShape(2.dp), ambientColor = RC.Gold, spotColor = RC.Gold)
                            .clip(RoundedCornerShape(2.dp))
                            .background(RC.Gold),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(30.dp))
                    Text(label, fontSize = 17.sp, color = color, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}
