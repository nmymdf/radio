package com.archiekuo.radio.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.radio.R
import com.archiekuo.radio.ui.components.goldCard
import com.archiekuo.radio.ui.theme.GoldButtonBrush
import com.archiekuo.radio.ui.theme.GoldTextBrush
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.ui.theme.SerifTitle
import kotlinx.coroutines.delay

data class Banner(
    @DrawableRes val image: Int,
    val line1: String,
    val line2: String,
    val gold: String,
    val sub: String,
    /** 要播哪一類；null 代表接著播上次的電台 */
    val categoryKey: String?,
)

val banners = listOf(
    Banner(R.drawable.banner_taipei, "今晚，", "聽點", "好音樂", "陪伴每一段安靜時光", null),
    Banner(R.drawable.banner_lake, "早安，", "聽見", "好心情", "用音樂迎接新的一天", "easy"),
    Banner(R.drawable.banner_cafe, "雨天，", "聽首", "好歌", "窗外下雨，心裡放晴", "jazz"),
    Banner(R.drawable.banner_zen, "靜心，", "聽見", "自在", "讓心慢下來", "zen"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarousel(onPlay: (Banner) -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState { banners.size }
    // 每 7 秒自動換下一張；手指滑動時暫停
    LaunchedEffect(pager) {
        while (true) {
            delay(7000)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % banners.size)
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(808f / 486f)
            .goldCard(RoundedCornerShape(20.dp)),
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { i ->
            BannerPage(banners[i]) { onPlay(banners[i]) }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(banners.size) { i ->
                val on = i == pager.currentPage
                Box(
                    Modifier
                        .height(7.dp)
                        .width(if (on) 22.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (on) RC.Gold else Color.White.copy(alpha = 0.6f)),
                )
            }
        }
    }
}

@Composable
private fun BannerPage(b: Banner, onPlay: () -> Unit) {
    val shadow = Shadow(Color(0x99000000), Offset(0f, 3f), 10f)
    Box(
        Modifier
            .fillMaxSize()
            .clickable(role = Role.Button, onClickLabel = "開始播放", onClick = onPlay),
    ) {
        Image(painterResource(b.image), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xE0070C26),
                        0.45f to Color(0x8C070C26),
                        0.75f to Color(0x00070C26),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp, bottom = 12.dp),
        ) {
            Text(
                buildAnnotatedString {
                    append(b.line1)
                    append("\n")
                    append(b.line2)
                    withStyle(SpanStyle(brush = GoldTextBrush)) { append(b.gold) }
                },
                style = TextStyle(
                    fontFamily = SerifTitle,
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                    lineHeight = 38.sp,
                    color = Color.White,
                    shadow = shadow,
                ),
            )
            Text(
                "— ${b.sub} —",
                style = TextStyle(fontFamily = SerifTitle, fontSize = 15.sp, color = Color(0xFFE8E2CF), letterSpacing = 1.sp, shadow = shadow),
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .height(48.dp)
                    .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = RC.Gold, spotColor = RC.Gold)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GoldButtonBrush)
                    .clickable(role = Role.Button, onClick = onPlay)
                    .padding(start = 16.dp, end = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF1A1405), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(4.dp))
                Text("開始播放", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1405))
            }
        }
    }
}
