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
fun NavCard(g: NavGuide, modifier: Modifier, compact: Boolean = false, onClick: () -> Unit) {
    val (d, u) = distText(g.segDist)
    val arrow = if (compact) 64 else 96
    Row(
        modifier.card().clickable(onClick = onClick).padding(horizontal = if (compact) 14.dp else 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp),
    ) {
        Box(Modifier.size(arrow.dp).clip(RoundedCornerShape(if (compact) 16.dp else 20.dp)).background(GoldBrush), contentAlignment = Alignment.Center) {
            Icon(turnIcon(g.icon), g.action, tint = C.Ink, modifier = Modifier.size((arrow * 0.68f).dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(d, fontSize = (if (compact) 36 else 44).sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = (if (compact) 38 else 46).sp, maxLines = 1)
                Text(" $u", fontSize = (if (compact) 20 else 24).sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(g.actionLine, fontSize = (if (compact) 19 else 21).sp, fontWeight = FontWeight.Bold, color = C.Gold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            val rest = remainText(g)
            if (rest.isNotEmpty()) Text(rest, fontSize = (if (compact) 15 else 17).sp, color = C.Sub, maxLines = if (compact) 2 else 1, lineHeight = (if (compact) 19 else 22).sp, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
