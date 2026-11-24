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

@Composable
fun StationArt(station: Station, size: Dp, corner: Dp = 12.dp) {
    Image(
        painterResource(Categories.byKey(station.categoryKey).art),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .border(1.dp, RC.Gold.copy(alpha = 0.35f), RoundedCornerShape(corner)),
    )
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
            .height(86.dp)
            .clickable(onClick = onClick),
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
                    maxLines = 1,
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
