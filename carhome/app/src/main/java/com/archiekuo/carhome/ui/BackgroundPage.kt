package com.archiekuo.carhome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.archiekuo.carhome.data.AppsRepo
import com.archiekuo.carhome.data.MemInfo
import com.archiekuo.carhome.data.RecentApp
import com.archiekuo.carhome.data.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** 每 3 秒更新一次的記憶體用量 */
@Composable
fun rememberMemory(): MemInfo? {
    val ctx = LocalContext.current
    var mem by remember { mutableStateOf<MemInfo?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            mem = withContext(Dispatchers.Default) { runCatching { SystemInfo.memory(ctx) }.getOrNull() }
            delay(3000)
        }
    }
    return mem
}

fun memColor(p: Int): Color = when {
    p < 70 -> C.GreenText
    p < 85 -> C.Gold
    else -> C.Red
}

/**
 * 設定 → 記憶體與背景 App。
 * 只是列出來，要不要停止由使用者自己在「App 資訊」決定（比較安全）。
 */
@Composable
fun BackgroundPage(repo: AppsRepo, keep: Set<String>, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val mem = rememberMemory()
    val apps by repo.apps.collectAsState()
    var allowed by remember { mutableStateOf(SystemInfo.hasUsageAccess(ctx)) }
    var recent by remember { mutableStateOf<List<RecentApp>>(emptyList()) }
    val life = LocalLifecycleOwner.current
    // 回到這頁時（例如剛去強制停止）重新整理
    LaunchedEffect(apps) {
        life.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                allowed = SystemInfo.hasUsageAccess(ctx)
                if (allowed) {
                    val set = apps.map { it.pkg }.toSet()
                    recent = withContext(Dispatchers.Default) { SystemInfo.recentApps(ctx, set) }
                }
                delay(5000)
            }
        }
    }
    PanelFrame("記憶體與背景 App", onBack) {
        Column(Modifier.fillMaxSize().padding(horizontal = 100.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // 記憶體
            Column(Modifier.fillMaxWidth().card(RoundedCornerShape(20.dp)).padding(horizontal = 24.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Memory, null, tint = C.Gold, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("目前記憶體使用量", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                    Text(mem?.text ?: "讀取中…", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = mem?.let { memColor(it.percent) } ?: C.Sub)
                }
                Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color(0x22FFFFFF))) {
                    val p = (mem?.percent ?: 0) / 100f
                    Box(Modifier.fillMaxHeight().fillMaxWidth(p.coerceIn(0f, 1f)).clip(RoundedCornerShape(7.dp)).background(memColor(mem?.percent ?: 0)))
                }
                Text("Android 會自己管理記憶體，不夠時會先關掉最久沒用的 App，平常不用手動清理。", fontSize = 17.sp, color = C.Sub, modifier = Modifier.padding(top = 10.dp))
            }
            if (!allowed) {
                Column(Modifier.fillMaxWidth().card(RoundedCornerShape(20.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("要列出背景 App，需要允許「使用情況存取權」", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Text("請在打開的系統設定裡找到「車機桌面」，把它打開，再回來這裡", fontSize = 19.sp, color = C.Sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp))
                    Box(Modifier.width(260.dp).height(60.dp).clip(RoundedCornerShape(30.dp)).background(GoldBrush).clickable { SystemInfo.openUsageAccess(ctx) }, contentAlignment = Alignment.Center) {
                        Text("前往允許", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                    }
                }
                return@Column
            }
            Text("最近 6 小時用過的 App（可能還在背景執行）・ 點「停止」會打開 App 資訊，請按「強制停止」", fontSize = 18.sp, color = C.Sub, modifier = Modifier.padding(start = 6.dp))
            if (recent.isEmpty()) Text("最近沒有用過其他 App", fontSize = 21.sp, color = C.Sub, modifier = Modifier.padding(start = 6.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recent, key = { it.pkg }) { r ->
                    val info = apps.firstOrNull { it.pkg == r.pkg }
                    val important = r.pkg in keep
                    Row(Modifier.fillMaxWidth().height(80.dp).card(RoundedCornerShape(16.dp)).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        repo.icon(r.pkg)?.let { Image(it, null, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))) }
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(info?.label ?: r.pkg, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(SystemInfo.ago(r.lastUsed), fontSize = 17.sp, color = C.Sub)
                        }
                        if (important) {
                            Box(Modifier.padding(end = 14.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, C.GreenText, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                                Text("建議保留", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.GreenText)
                            }
                        }
                        Row(
                            Modifier.height(52.dp).clip(RoundedCornerShape(14.dp)).border(1.5.dp, C.Red.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                                .clickable { SystemInfo.openAppInfo(ctx, r.pkg) }.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.StopCircle, null, tint = C.Red, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("停止", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Red)
                        }
                    }
                }
            }
        }
    }
}
