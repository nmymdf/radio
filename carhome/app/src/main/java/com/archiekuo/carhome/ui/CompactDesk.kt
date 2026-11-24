package com.archiekuo.carhome.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archiekuo.carhome.CarApp
import com.archiekuo.carhome.R
import com.archiekuo.carhome.data.SystemInfo
import com.archiekuo.carhome.media.RadioRemote

/**
 * 分割畫面時的精簡桌面。依寬度自動選：
 * 寬（桌面佔 2/3）、中（1/2）、窄（1/3）。
 */
@Composable
fun CompactDesk(app: CarApp, width: Float, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val p = app.prefs
    val now = rememberNow()
    val heading by app.location.heading.collectAsStateWithLifecycle()
    val road by app.location.road.collectAsStateWithLifecycle()
    val fix by app.location.fix.collectAsStateWithLifecycle()
    val district by app.location.district.collectAsStateWithLifecycle()
    val online by app.net.online.collectAsStateWithLifecycle()
    val weather by app.weather.weather.collectAsStateWithLifecycle()
    val guide by app.nav.guide.collectAsStateWithLifecycle()
    val radio by app.radio.state.collectAsStateWithLifecycle()
    val local by app.local.state.collectAsStateWithLifecycle()
    val mode by p.mode.flow.collectAsStateWithLifecycle()
    val circleMode by p.circleMode.flow.collectAsStateWithLifecycle()
    val black by p.black.flow.collectAsStateWithLifecycle()
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    Look.black = black
    var drawer by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { app.radio.connect() }
    BackHandler(enabled = drawer) { drawer = false }

    val size = when { width >= 760 -> 2; width >= 540 -> 1; else -> 0 }
    fun shield() { val s = app.shieldPackage(); if (s != null) app.apps.launch(s) else app.say("找不到神盾 App，請到設定選擇") }
    val circle: @Composable (Int) -> Unit = { d ->
        ClockCompass(now, heading, circleMode, d, onTap = { p.circleMode.set((circleMode + 1) % 3) }, onLong = ::shield)
    }
    val navOpen = { app.navPackage()?.let { app.apps.launch(it) }; Unit }
    val infoCard: @Composable (Modifier, Boolean) -> Unit = { m, small ->
        val g = guide
        if (g != null) NavCard(g, m, compact = true, onClick = navOpen)
        else if (!small) MiniWeather(weather, district ?: (if (fix == null) "新店" else null), online, m)
    }

    Box(
        Modifier.fillMaxSize().background(
            if (black) Brush.verticalGradient(listOf(Color.Black, Color.Black))
            else Brush.verticalGradient(listOf(C.Navy2, C.Navy1, C.Navy0)),
        ),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 路名＋車速
            Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, null, tint = C.Gold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(road ?: "正在找路名…", fontSize = (if (size == 0) 21 else 24).sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(GoldBrush).padding(horizontal = 12.dp, vertical = 2.dp)) {
                    Text(fix?.speedKmh?.let { "${it.toInt()} km/h" } ?: "-- km/h", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Ink, maxLines = 1)
                }
                if (size == 2) { Spacer(Modifier.width(12.dp)); AuthorLine(16.sp) }
            }
            when (size) {
                2 -> {
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.width(250.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            circle(250)
                            infoCard(Modifier.width(250.dp).height(150.dp), false)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MiniPlayer(app, mode, radio, local, cover = 130, nameSize = 28, ctrlScale = 0.9f)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Pill("網路收音機", mode == "radio", Modifier.weight(1f), 48.dp) { app.switchMode("radio") }
                                Pill("內部音樂", mode == "local", Modifier.weight(1f), 48.dp) { app.switchMode("local") }
                                Pill("FM", mode == "fm", Modifier.weight(1f), 48.dp) {
                                    app.switchMode("fm"); app.fmPackage()?.let { app.apps.launch(it) }
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CTile(Icons.Rounded.Fullscreen, "導航全畫面", 100) { app.navFull() }
                        CTile(Icons.Rounded.Shield, "神盾", 100, ::shield)
                        CTile(Icons.Rounded.SmartDisplay, "YouTube", 100) { app.openYouTubeOnDesk() }
                        CTile(Icons.Rounded.Apps, "所有 App", 100) { drawer = true }
                        CTile(Icons.Rounded.Close, "結束分割", 100) { app.endSplit(); onClose() }
                    }
                }
                1 -> {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        circle(190)
                        infoCard(Modifier.weight(1f).height(190.dp), false)
                    }
                    MiniPlayer(app, mode, radio, local, cover = 110, nameSize = 25, ctrlScale = 0.8f)
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CTile(Icons.Rounded.Fullscreen, "導航全畫面", 96) { app.navFull() }
                        CTile(Icons.Rounded.Shield, "神盾", 96, ::shield)
                        CTile(Icons.Rounded.Apps, "所有 App", 96) { drawer = true }
                        CTile(Icons.Rounded.Close, "結束分割", 96) { app.endSplit(); onClose() }
                    }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AuthorLine(15.sp) }
                }
                else -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { circle(150) }
                    infoCard(Modifier.fillMaxWidth().height(120.dp), true)
                    MiniPlayer(app, mode, radio, local, cover = 84, nameSize = 22, ctrlScale = 0.72f)
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CTile(Icons.Rounded.Fullscreen, "導航全畫面", 84) { app.navFull() }
                        CTile(Icons.Rounded.Close, "結束分割", 84) { app.endSplit(); onClose() }
                    }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AuthorLine(15.sp) }
                }
            }
        }
        if (drawer) {
            val tileLabels = Role.entries.mapIndexed { i, r -> p.tile(i).value.takeIf { it.isNotBlank() }?.let { app.apps.find(it)?.label } ?: r.label }
            AppsPage(
                app.apps, apps, tileLabels, columns = (width / 180f).toInt().coerceIn(2, 5), compact = true,
                onLaunch = { a -> drawer = false; app.apps.launch(a.pkg) },
                onAssign = { a, i -> p.tile(i).set(a.pkg); app.say("已放到下方第 ${i + 1} 個方塊") },
                onInfo = { a -> SystemInfo.openAppInfo(ctx, a.pkg) },
                onUninstall = { a -> uninstallOrInfo(app, a.pkg, a.system) },
            ) { drawer = false }
        }
        val msg by app.message.collectAsStateWithLifecycle()
        MessageBanner(msg, Modifier.align(Alignment.TopCenter).padding(top = 64.dp, start = 12.dp, end = 12.dp))
    }
}

/** 移除 App；原廠內建的不能移除，改成打開 App 資訊讓使用者停用 */
fun uninstallOrInfo(app: CarApp, pkg: String, system: Boolean) {
    if (system) {
        app.say("原廠內建 App 不能移除，請按「停用」")
        SystemInfo.openAppInfo(app, pkg)
    } else if (!SystemInfo.uninstall(app, pkg)) {
        SystemInfo.openAppInfo(app, pkg)
    }
}

@Composable
private fun RowScope.CTile(icon: ImageVector, label: String, h: Int, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).height(h.dp).card(RoundedCornerShape(18.dp), tileBrush()).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = C.Gold, modifier = Modifier.size((h * 0.38f).dp))
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun MiniWeather(w: com.archiekuo.carhome.location.Weather?, place: String?, online: Boolean, modifier: Modifier) {
    Row(modifier.card().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (w == null) { Text(if (online) "天氣載入中…" else "沒有網路", fontSize = 19.sp, color = C.Sub); return@Row }
        val (icon, tint) = weatherIcon(w.kind)
        Icon(icon, w.text, tint = tint, modifier = Modifier.size(56.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("${w.temp}° ${w.text}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            Text(listOfNotNull(place, w.rain?.let { "降雨 $it%" }).joinToString(" ・ "), fontSize = 17.sp, color = C.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 精簡播放器：封面、名稱、狀態、上一首／播放／下一首 */
@Composable
private fun MiniPlayer(app: CarApp, mode: String, radio: com.archiekuo.carhome.media.RadioState, local: com.archiekuo.carhome.media.LocalState, cover: Int, nameSize: Int, ctrlScale: Float) {
    val title: String
    val sub: String
    val subColor: Color
    val art: Int
    val playing: Boolean
    when (mode) {
        "local" -> { title = local.title ?: "內部音樂"; sub = local.artist?.takeIf { it.isNotBlank() } ?: (if (local.playing) "播放中" else "已暫停"); subColor = C.Sub; art = R.drawable.art_cafe; playing = local.playing }
        "fm" -> { title = "FM 收音機"; sub = "用原廠收音機播放"; subColor = C.Sub; art = R.drawable.art_lake; playing = false }
        else -> {
            title = radio.title ?: "網路收音機"
            sub = when { radio.buffering -> "連線中…"; radio.playing -> "直播中" + (radio.subtitle?.let { " ・ $it" } ?: ""); else -> "已暫停" }
            subColor = if (radio.playing) C.GreenText else C.Sub
            art = R.drawable.art_radio; playing = radio.playing || radio.buffering
        }
    }
    Column(Modifier.fillMaxWidth().card(RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Image(painterResource(art), null, Modifier.size(cover.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = nameSize.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = (nameSize + 6).sp)
                Text(sub, fontSize = 18.sp, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (mode != "fm") {
            val toggle = { if (mode == "local") app.local.toggle() else app.radio.toggle() }
            val prev = { if (mode == "local") app.local.previous() else app.radio.previous() }
            val next = { if (mode == "local") app.local.next() else app.radio.next() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy((30 * ctrlScale).dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                CircleBtn(Icons.Rounded.SkipPrevious, "上一首", (76 * ctrlScale).dp, (40 * ctrlScale).dp, { prev() })
                PlayRing(playing, (100 * ctrlScale).dp) { toggle() }
                CircleBtn(Icons.Rounded.SkipNext, "下一首", (76 * ctrlScale).dp, (40 * ctrlScale).dp, { next() })
            }
        } else {
            GhostButton("打開 FM 收音機", Modifier.fillMaxWidth(), Icons.Rounded.Radio) { app.fmPackage()?.let { app.apps.launch(it) } }
        }
    }
}
