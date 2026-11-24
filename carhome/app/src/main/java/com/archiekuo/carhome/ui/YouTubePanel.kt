package com.archiekuo.carhome.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.archiekuo.carhome.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 建立 YouTube 用的網頁畫面；平常是空白，點了照片才載入 */
@SuppressLint("SetJavaScriptEnabled")
fun createYouTubeWebView(ctx: Context, onUrl: (String) -> Unit): WebView = WebView(ctx).apply {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.loadWithOverviewMode = true
    settings.useWideViewPort = true
    setBackgroundColor(android.graphics.Color.BLACK)
    webChromeClient = WebChromeClient()
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val s = request.url.scheme ?: return true
            return s != "http" && s != "https"
        }
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !(url.startsWith("http://") || url.startsWith("https://"))
        override fun onPageFinished(view: WebView, url: String) { if (url.contains("youtube.com")) onUrl(url) }
    }
}

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

@Composable
fun YouTubePanel(
    web: WebView,
    modifier: Modifier,
    active: Boolean,
    photo: String,
    driving: Boolean,
    restricted: Boolean,
    passenger: Boolean,
    full: Boolean,
    onActivate: () -> Unit,
    onClose: () -> Unit,
    onVoice: () -> Unit,
    onPassenger: () -> Unit,
    onFull: () -> Unit,
    onChangePhoto: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val hideVideo = active && driving && restricted && !passenger
    Column(
        modifier.card(RoundedCornerShape(24.dp), if (full) SolidColor(panelColor()) else cardBrush()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SmartDisplay, null, tint = C.Gold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text("YouTube", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            // 語音搜尋
            Row(
                Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(C.Gold.copy(alpha = 0.15f)).border(1.5.dp, C.Gold, RoundedCornerShape(17.dp))
                    .clickable(onClick = onVoice).padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Mic, "語音搜尋", tint = C.Gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(4.dp))
                Text("語音搜尋", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Gold)
            }
            Spacer(Modifier.width(8.dp))
            val (txt, col) = when {
                !active -> "點一下開啟" to C.Sub
                hideVideo -> "行駛中 ・ 只播聲音" to C.Gold
                driving && passenger -> "副駕駛觀看中" to Color(0xFFFFB27A)
                else -> "停車中" to C.GreenText
            }
            Box(Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, col.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text(txt, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = col, maxLines = 1)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.Black)) {
            // 網頁一直放在這裡（不會重新建立），平常被照片蓋住
            AndroidView(factory = { web.also { (it.parent as? android.view.ViewGroup)?.removeView(it) } }, modifier = Modifier.fillMaxSize())
            if (!active) {
                Box(Modifier.fillMaxSize().clickable(onClick = onActivate), contentAlignment = Alignment.Center) {
                    YtPhoto(photo, Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(72.dp).clip(CircleShape).background(Color(0xA6070C26)).border(2.dp, C.Gold, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, "開啟 YouTube", tint = C.Gold, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("點一下開始看 YouTube", style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White, shadow = Shadow(Color.Black, blurRadius = 10f)))
                    }
                }
            } else if (hideVideo) {
                Column(Modifier.fillMaxSize().background(if (Look.black) Color(0xF5000000) else Color(0xF2070C26)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.DirectionsCar, null, tint = C.Gold, modifier = Modifier.size(56.dp))
                    Text("行駛中，只播聲音", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    Text("停車後會自動顯示畫面", fontSize = 17.sp, color = C.Sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!active) {
                GhostButton("更換照片", Modifier.weight(1f), Icons.Rounded.Image, onClick = onChangePhoto)
                GhostButton("打開 YouTube", Modifier.weight(1f), onClick = onOpenApp)
            } else {
                if (hideVideo) GhostButton("副駕駛觀看", Modifier.weight(1f), Icons.Rounded.Visibility, onClick = onPassenger)
                else GhostButton(if (full) "縮小" else "放大", Modifier.weight(1f), if (full) Icons.Rounded.CloseFullscreen else Icons.Rounded.OpenInFull, onClick = onFull)
                GhostButton("關閉", Modifier.weight(1f), Icons.Rounded.Close, onClick = onClose)
            }
        }
    }
}
