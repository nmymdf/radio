package com.archiekuo.carhome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.carhome.nav.NavGuide
import com.archiekuo.carhome.nav.distText
import com.archiekuo.carhome.nav.remainText

/** 高德的轉彎代碼 → 箭頭圖示 */
fun turnIcon(icon: Int): ImageVector = when (icon) {
    2 -> Icons.Rounded.TurnLeft
    3 -> Icons.Rounded.TurnRight
    4 -> Icons.Rounded.TurnSlightLeft
    5 -> Icons.Rounded.TurnSlightRight
    6 -> Icons.Rounded.TurnSharpLeft
    7 -> Icons.Rounded.TurnSharpRight
    8 -> Icons.Rounded.UTurnLeft
    9 -> Icons.Rounded.Straight
    11, 12 -> Icons.Rounded.RoundaboutRight
    10, 15 -> Icons.Rounded.Flag
    13 -> Icons.Rounded.LocalGasStation
    14 -> Icons.Rounded.Toll
    16 -> Icons.Rounded.Landscape
    else -> Icons.Rounded.Navigation
}

/**
 * 導航小卡：大箭頭、距離、下一條路、剩下多遠多久。
 * compact＝分割畫面用的小一號版本。
 */
@Composable
fun NavCard(g: NavGuide, modifier: Modifier, compact: Boolean = false, tiny: Boolean = false, onClick: () -> Unit) {
    val (d, u) = distText(g.segDist)
    val arrow = if (compact) 64 else 96
    Row(
        modifier.card().clickable(onClick = onClick).padding(horizontal = if (compact) 14.dp else 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp),
    ) {
        Box(Modifier.size(arrow.dp).clip(RoundedCornerShape(if (compact) 16.dp else 20.dp)).background(GoldBrush), contentAlignment = Alignment.Center) {
            Icon(turnIcon(g.icon), g.action, tint = C.Ink, modifier = Modifier.size((arrow * 0.68f).dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(d, fontSize = (if (compact) 36 else 40).sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = (if (compact) 38 else 42).sp, maxLines = 1)
                Text(" $u", fontSize = (if (compact) 20 else 22).sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(g.actionLine, fontSize = (if (compact) 19 else 20).sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = C.Gold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            val rest = if (tiny) remainText(g, twoLines = true).substringBefore('\n') else remainText(g, twoLines = compact)
            if (rest.isNotEmpty()) Text(rest, fontSize = (if (compact) 15 else 16).sp, color = C.Sub, maxLines = if (compact) 2 else 1, lineHeight = (if (compact) 19 else 21).sp, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** 沒在導航時：三個大按鈕「說目的地／回家／去公司」 */
@Composable
fun NavQuick(modifier: Modifier, onVoice: () -> Unit, onHome: () -> Unit, onWork: () -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickBtn(Icons.Rounded.Mic, "說目的地", true, onVoice)
        QuickBtn(Icons.Rounded.Home, "回家", false, onHome)
        QuickBtn(Icons.Rounded.Work, "去公司", false, onWork)
    }
}

@Composable
private fun RowScope.QuickBtn(icon: ImageVector, label: String, primary: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.weight(1f).fillMaxHeight()
            .then(if (primary) Modifier.clip(shape).background(GoldBrush) else Modifier.card(shape, tileBrush(), alpha = 0.45f))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (primary) C.Ink else C.Gold, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = if (primary) C.Ink else Color.White, maxLines = 1)
    }
}
