package com.archiekuo.carhome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.carhome.nav.NavGuide
import com.archiekuo.carhome.nav.Trip
import com.archiekuo.carhome.nav.distText
import java.util.Locale

/** 導航中：右邊整欄的大字導航卡 */
@Composable
fun NavBig(g: NavGuide, modifier: Modifier, onClick: () -> Unit) {
    val (d, u) = distText(g.segDist)
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.clip(shape).background(Brush.verticalGradient(listOf(Color(0xFF1F5A3A), Color(0xFF113322))))
            .border(1.5.dp, C.GreenText, shape).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Box(Modifier.size(132.dp).clip(RoundedCornerShape(26.dp)).background(GoldBrush), contentAlignment = Alignment.Center) {
            Icon(turnIcon(g.icon), g.action, tint = C.Ink, modifier = Modifier.size(100.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(d, fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 66.sp, maxLines = 1)
            Text(" $u", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(g.actionLine, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, color = C.Gold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (g.remainDist > 0) {
            val (rd, ru) = distText(g.remainDist)
            val arrive = remember(g.remainSec / 60) {
                java.text.SimpleDateFormat("HH:mm", Locale.TAIWAN).format(java.util.Date(System.currentTimeMillis() + g.remainSec * 1000L))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.Bottom) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("剩 ", fontSize = 24.sp, color = C.Sub); Text(rd, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(" $ru", fontSize = 24.sp, color = C.Sub)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("約 ", fontSize = 24.sp, color = C.Sub); Text(arrive, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(" 到", fontSize = 24.sp, color = C.Sub)
                }
            }
        }
    }
}

/** 平時：這一趟開了多久、多遠、平均時速 */
@Composable
fun TripCard(t: Trip, modifier: Modifier) {
    Column(modifier.card(RoundedCornerShape(24.dp)).padding(horizontal = 26.dp, vertical = 14.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        Text("這一趟", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = C.Gold)
        TripRow("開了", "${t.minutes}", "分鐘")
        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
        TripRow("里程", String.format(Locale.US, "%.1f", t.km), "公里")
        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
        TripRow("平均", if (t.km < 0.1 || t.minutes < 1) "--" else "${t.avgKmh}", "km/h")
    }
}

@Composable
private fun TripRow(label: String, value: String, unit: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 28.sp, color = C.Sub, modifier = Modifier.weight(1f))
        Text(value, fontSize = 54.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 58.sp, maxLines = 1)
        Text(" $unit", fontSize = 24.sp, color = Color.White, maxLines = 1, modifier = Modifier.width(92.dp))
    }
}

/** 一顆選擇按鈕 */
data class Choice(val label: String, val icon: ImageVector, val style: Int = 0, val onClick: () -> Unit)

/** 跳出兩顆大按鈕（點旁邊空白處取消） */
@Composable
fun ChoiceBox(title: String, choices: List<Choice>, dot: Color? = null, note: String? = null, onCancel: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(30.dp)
        Column(
            Modifier.width(680.dp).clip(shape).background(Color(0xFF141C4A)).border(2.dp, C.Gold, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dot != null) { Box(Modifier.size(16.dp).clip(CircleShape).background(dot)); Spacer(Modifier.width(10.dp)) }
                Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                choices.forEach { c ->
                    val s = RoundedCornerShape(26.dp)
                    val bg = when (c.style) {
                        1 -> Modifier.background(GoldBrush)
                        2 -> Modifier.background(Color(0xFF7A2330)).border(2.dp, Color(0xFFFF8A8A), s)
                        else -> Modifier.background(Color(0xCC28378A)).border(2.dp, C.Gold.copy(alpha = 0.6f), s)
                    }
                    Column(
                        Modifier.weight(1f).height(150.dp).clip(s).then(bg).clickable(onClick = c.onClick),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(c.icon, null, tint = if (c.style == 1) C.Ink else Color.White, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(c.label, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (c.style == 1) C.Ink else Color.White, maxLines = 1)
                    }
                }
            }
            if (note != null) Text(note, fontSize = 22.sp, color = C.Sub, textAlign = TextAlign.Center, lineHeight = 30.sp)
            Text("點旁邊空白處取消", fontSize = 22.sp, color = C.Sub)
        }
    }
}

/** 高德資料記錄：看高德送了哪些資料（巡航時有沒有限速、測速照相） */
@Composable
fun AmapLogPage(items: List<com.archiekuo.carhome.nav.NavInfo.LogItem>, onClear: () -> Unit, onBack: () -> Unit) {
    PanelFrame("高德資料記錄", onBack, right = {
        GhostButton("清除", Modifier.width(120.dp), onClick = onClear)
    }) {
        if (items.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("還沒有收到高德的資料", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("開著高德開一段路（有導航、沒導航都可以），再回來看這一頁", fontSize = 22.sp, color = C.Sub, modifier = Modifier.padding(top = 12.dp))
            }
            return@PanelFrame
        }
        val fmt = remember { java.text.SimpleDateFormat("M/d HH:mm:ss", Locale.TAIWAN) }
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().padding(horizontal = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items.size) { i ->
                val it = items[i]
                Column(Modifier.fillMaxWidth().card(RoundedCornerShape(18.dp)).padding(18.dp)) {
                    Text("類型 ${it.type} ・ 收到 ${it.count} 次 ・ 最後 ${fmt.format(java.util.Date(it.time))}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = C.Gold)
                    Text(it.text, fontSize = 19.sp, color = Color.White, lineHeight = 26.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
