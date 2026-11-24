package com.archiekuo.carhome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 下面一排七個方塊的預設功能 */
enum class Role(val label: String, val icon: ImageVector) {
    SHIELD("神盾", Icons.Rounded.Shield),
    YOUTUBE("YouTube", Icons.Rounded.SmartDisplay),
    NAV("導航", Icons.Rounded.Navigation),
    PHONE("藍牙電話", Icons.Rounded.Phone),
    SETTINGS("設定", Icons.Rounded.Settings),
    APPS("所有 App", Icons.Rounded.Apps),
    OEM("原廠桌面", Icons.Rounded.Home),
}

data class TileUi(val role: Role, val label: String, val appIcon: ImageBitmap?)

@Composable
fun Dock(tiles: List<TileUi>, onTap: (Int) -> Unit, onLong: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        tiles.forEachIndexed { i, t ->
            Column(
                Modifier.weight(1f).fillMaxHeight()
                    .card(RoundedCornerShape(20.dp), tileBrush())
                    .clickAndHold(onClick = { onTap(i) }, onLong = { onLong(i) }),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                if (t.appIcon != null) Image(t.appIcon, null, Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)))
                else Icon(t.role.icon, null, tint = C.Gold, modifier = Modifier.size(46.dp))
                Spacer(Modifier.height(8.dp))
                Text(t.label, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
    }
}
