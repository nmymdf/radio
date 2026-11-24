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

/**
 * YouTube 卡：平常是一張照片，點一下打開車機上的 YouTube App
 * （車機的網頁元件太舊，網頁版 YouTube 打不開）。
 */
@Composable
fun YouTubeCard(photo: String, modifier: Modifier, onOpen: () -> Unit, onVoice: () -> Unit, onChangePhoto: () -> Unit) {
    Column(modifier.card(RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SmartDisplay, null, tint = C.Gold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text("YouTube", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            Row(
                Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(C.Gold.copy(alpha = 0.15f)).border(1.5.dp, C.Gold, RoundedCornerShape(17.dp))
                    .clickable(onClick = onVoice).padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Mic, "語音搜尋", tint = C.Gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(4.dp))
                Text("語音搜尋", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Gold)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.Black).clickable(onClick = onOpen), contentAlignment = Alignment.Center) {
            YtPhoto(photo, Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xA6070C26)).border(2.dp, C.Gold, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, "打開 YouTube", tint = C.Gold, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("點一下打開 YouTube", style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White, shadow = Shadow(Color.Black, blurRadius = 10f)))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("更換照片", Modifier.weight(1f), Icons.Rounded.Image, onClick = onChangePhoto)
            GhostButton("打開 YouTube", Modifier.weight(1f), onClick = onOpen)
        }
    }
}
