package com.archiekuo.carhome.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.carhome.R
import com.archiekuo.carhome.yt.YtController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 內建照片 */
val ytPhotos = listOf(
    Triple("lake", "清晨湖畔", R.drawable.banner_lake),
    Triple("cafe", "雨天咖啡館", R.drawable.banner_cafe),
    Triple("taipei", "台北夜景", R.drawable.banner_taipei),
    Triple("zen", "月光禪園", R.drawable.banner_zen),
)

@Composable
fun YtPhoto(key: String, modifier: Modifier) {
    if (key == "black") { Box(modifier.background(Color.Black)); return }
    if (key.startsWith("file:")) {
        var bmp by remember(key) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(key) {
            bmp = withContext(Dispatchers.IO) {
                runCatching {
                    val opt = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(key.removePrefix("file:"), opt)?.asImageBitmap()
                }.getOrNull()
            }
        }
        val b = bmp
        if (b != null) Image(b, null, modifier, contentScale = ContentScale.Crop) else Box(modifier.background(Color.Black))
        return
    }
    val res = ytPhotos.firstOrNull { it.first == key }?.third ?: R.drawable.banner_lake
    Image(painterResource(res), null, modifier, contentScale = ContentScale.Crop)
}

/** 放播放器的位置（Compose 裡的一塊原生畫面） */
@Composable
fun YtHost(modifier: Modifier, register: (android.widget.FrameLayout?) -> Unit) {
    var holder by remember { mutableStateOf<android.widget.FrameLayout?>(null) }
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { c -> android.widget.FrameLayout(c).apply { setBackgroundColor(android.graphics.Color.BLACK) }.also { holder = it; register(it) } },
        modifier = modifier,
    )
    DisposableEffect(Unit) { onDispose { register(null) } }
}

/**
 * YouTube 卡：平常是一張照片；點一下就在這一格裡看 YouTube。
 * 可以放大成全畫面，或浮動到其他 App 上面。
 */
@Composable
fun YouTubeCard(
    photo: String, mode: YtController.Mode, modifier: Modifier,
    register: (android.widget.FrameLayout?) -> Unit,
    onStart: () -> Unit, onVoice: () -> Unit, onChangePhoto: () -> Unit,
    onHome: () -> Unit, onBack: () -> Unit, onFull: () -> Unit, onFloat: () -> Unit, onClose: () -> Unit, onPullBack: () -> Unit,
) {
    val idle = mode == YtController.Mode.IDLE
    Column(modifier.card(RoundedCornerShape(24.dp)).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SmartDisplay, null, tint = C.Gold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text("YouTube", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            if (!idle) {
                SmallGoldPill("首頁", Icons.Rounded.Home, onHome)
                Spacer(Modifier.width(8.dp))
            }
            SmallGoldPill("語音搜尋", Icons.Rounded.Mic, onVoice)
        }
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
            when (mode) {
                YtController.Mode.IDLE -> {
                    Box(Modifier.fillMaxSize().clickable(onClick = onStart), contentAlignment = Alignment.Center) {
                        YtPhoto(photo, Modifier.fillMaxSize())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xA6070C26)).border(2.dp, C.Gold, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PlayArrow, "開始看 YouTube", tint = C.Gold, modifier = Modifier.size(36.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("點一下在這裡看 YouTube", style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White, shadow = Shadow(Color.Black, blurRadius = 10f)))
                        }
                    }
                }
                YtController.Mode.CARD -> YtHost(Modifier.fillMaxSize(), register)
                else -> Column(Modifier.fillMaxSize().clickable(onClick = onPullBack), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(if (mode == YtController.Mode.FLOAT) Icons.Rounded.PictureInPicture else Icons.Rounded.Fullscreen, null, tint = C.Gold, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(if (mode == YtController.Mode.FLOAT) "正在浮動小視窗播放" else "正在全畫面播放", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text("點一下拉回這裡", fontSize = 17.sp, color = C.Gold)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (idle) {
                GhostButton("更換照片", Modifier.weight(1f), Icons.Rounded.Image, onClick = onChangePhoto)
                GhostButton("開始看", Modifier.weight(1f), Icons.Rounded.PlayArrow, onClick = onStart)
            } else {
                GhostButton("上一頁", Modifier.weight(1f), onClick = onBack)
                GhostButton("放大", Modifier.weight(1f), onClick = onFull)
                GhostButton("浮動", Modifier.weight(1f), onClick = onFloat)
                GhostButton("關閉", Modifier.weight(1f), onClick = onClose)
            }
        }
    }
}

@Composable
private fun SmallGoldPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(C.Gold.copy(alpha = 0.15f)).border(1.5.dp, C.Gold, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = C.Gold, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Gold)
    }
}

/** YouTube 全畫面：上面一排大按鈕，下面整片是影片 */
@Composable
fun YouTubeFull(
    register: (android.widget.FrameLayout?) -> Unit,
    onSmall: () -> Unit, onBack: () -> Unit, onHome: () -> Unit, onVoice: () -> Unit, onFloat: () -> Unit, onClose: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color.Black).clickable(enabled = true, onClick = {})) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).background(panelColor()).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GhostButton("縮小", Modifier.width(130.dp), Icons.Rounded.FullscreenExit, onClick = onSmall)
            GhostButton("上一頁", Modifier.width(130.dp), Icons.Rounded.ArrowBackIosNew, onClick = onBack)
            GhostButton("首頁", Modifier.width(120.dp), Icons.Rounded.Home, onClick = onHome)
            GhostButton("語音搜尋", Modifier.width(160.dp), Icons.Rounded.Mic, onClick = onVoice)
            GhostButton("浮動", Modifier.width(120.dp), Icons.Rounded.PictureInPicture, onClick = onFloat)
            GhostButton("關閉", Modifier.width(120.dp), Icons.Rounded.Close, onClick = onClose)
            Spacer(Modifier.weight(1f))
            AuthorLine()
        }
        YtHost(Modifier.fillMaxWidth().weight(1f), register)
    }
}
