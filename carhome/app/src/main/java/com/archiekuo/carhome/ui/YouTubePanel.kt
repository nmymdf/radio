package com.archiekuo.carhome.ui

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/** 建立 YouTube 用的網頁畫面（整個桌面只建一個，切換放大時不會重新載入） */
@SuppressLint("SetJavaScriptEnabled")
fun createYouTubeWebView(ctx: Context, startUrl: String, onUrl: (String) -> Unit): WebView = WebView(ctx).apply {
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
        override fun onPageFinished(view: WebView, url: String) { if (url.contains("youtube.com")) onUrl(url) }
    }
    loadUrl(startUrl)
}

@Composable
fun YouTubePanel(
    web: WebView,
    modifier: Modifier,
    driving: Boolean,
    restricted: Boolean,
    passenger: Boolean,
    full: Boolean,
    onPassenger: () -> Unit,
    onFull: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val hideVideo = driving && restricted && !passenger
    Column(modifier.card(RoundedCornerShape(24.dp), if (full) androidx.compose.ui.graphics.SolidColor(panelColor()) else cardBrush()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SmartDisplay, null, tint = C.Gold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text("YouTube", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            val (txt, col) = when {
                hideVideo -> "行駛中 ・ 只播聲音" to C.Gold
                driving && passenger -> "副駕駛觀看中" to Color(0xFFFFB27A)
                else -> "停車中 ・ 可看畫面" to C.GreenText
            }
            Box(Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, col.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text(txt, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = col)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.Black)) {
            AndroidView(factory = { web.also { (it.parent as? android.view.ViewGroup)?.removeView(it) } }, modifier = Modifier.fillMaxSize())
            if (hideVideo) {
                Column(Modifier.fillMaxSize().background(if (Look.black) Color(0xF5000000) else Color(0xF2070C26)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.DirectionsCar, null, tint = C.Gold, modifier = Modifier.size(56.dp))
                    Text("行駛中，只播聲音", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    Text("停車後會自動顯示畫面", fontSize = 17.sp, color = C.Sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (driving && restricted && !passenger) GhostButton("副駕駛觀看", Modifier.weight(1f), Icons.Rounded.Visibility, onClick = onPassenger)
            else GhostButton(if (full) "縮小" else "放大", Modifier.weight(1f), if (full) Icons.Rounded.CloseFullscreen else Icons.Rounded.OpenInFull, onClick = onFull)
            GhostButton("打開 YouTube", Modifier.weight(1f), onClick = onOpenApp)
        }
    }
}
