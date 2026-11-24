package com.archiekuo.carhome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object C {
    val Navy0 = Color(0xFF070C26)
    val Navy1 = Color(0xFF0B1336)
    val Navy2 = Color(0xFF0E1844)
    val Glow = Color(0xFF26357A)
    val Gold = Color(0xFFF2C86A)
    val GoldLight = Color(0xFFFFE8AD)
    val GoldDark = Color(0xFFC78C2E)
    val Sub = Color(0xFFB9C0DC)
    val Dim = Color(0xFF8A93B8)
    val Green = Color(0xFF4ADE80)
    val GreenText = Color(0xFF5EE08A)
    val Red = Color(0xFFFF8A8A)
    val Ink = Color(0xFF1A1405)
}

val GoldTextBrush = Brush.verticalGradient(listOf(Color(0xFFFFF3C8), C.Gold, Color(0xFFC58C2C)))
val GoldBrush = Brush.verticalGradient(listOf(C.GoldLight, Color(0xFFE8B65C), C.GoldDark))
/** 桌面顏色：false＝午夜藍、true＝全黑 */
object Look { var black by androidx.compose.runtime.mutableStateOf(false) }

val NavyCard = Brush.verticalGradient(listOf(Color(0xEB202A60), Color(0xEB141C48)))
val BlackCard = Brush.verticalGradient(listOf(Color(0xF0161616), Color(0xF00A0A0A)))
fun cardBrush(): Brush = if (Look.black) BlackCard else NavyCard
fun tileBrush(): Brush = if (Look.black) Brush.verticalGradient(listOf(Color(0x99181818), Color(0x990A0A0A))) else Brush.verticalGradient(listOf(Color(0x8C1C2558), Color(0x8C0E163E)))
fun panelColor(): Color = if (Look.black) Color(0xFF000000) else Color(0xFF080D2A)

@Composable
fun CarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = C.Gold, onPrimary = C.Ink, background = C.Navy0, surface = Color(0xFF1C2558), onSurface = Color.White), content = content)
}

fun Modifier.card(shape: Shape = RoundedCornerShape(22.dp), brush: Brush = cardBrush(), alpha: Float = 0.38f): Modifier =
    this.shadow(10.dp, shape, ambientColor = Color.Black, spotColor = Color.Black).clip(shape).background(brush).border(1.dp, C.Gold.copy(alpha = alpha), shape)

@Composable
fun GoldText(text: String, size: TextUnit, modifier: Modifier = Modifier, italic: Boolean = false, weight: FontWeight = FontWeight.Bold, family: FontFamily = FontFamily.Default) {
    Text(text, modifier, maxLines = 1, overflow = TextOverflow.Ellipsis,
        style = TextStyle(brush = GoldTextBrush, fontSize = size, fontWeight = weight, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal, fontFamily = family))
}

@Composable
fun CircleBtn(icon: ImageVector, desc: String, size: Dp, iconSize: Dp, onClick: () -> Unit, tint: Color = Color.White) {
    Box(
        Modifier.size(size).clip(CircleShape).background(if (Look.black) Color(0xCC111111) else Color(0x99080E2C)).border(1.5.dp, C.Gold.copy(alpha = 0.6f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(iconSize)) }
}

@Composable
fun PlayRing(playing: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).shadow(size / 4, CircleShape, ambientColor = C.Gold, spotColor = C.Gold).clip(CircleShape)
            .background(if (Look.black) Color(0xF0000000) else Color(0xE6080C24)).border(3.dp, C.Gold, CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (playing) "暫停" else "播放", tint = C.Gold, modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, height: Dp = 50.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier.height(height).clip(shape)
            .then(if (selected) Modifier.background(GoldBrush) else Modifier.background(Color(0x0FFFFFFF)).border(1.dp, C.Gold.copy(alpha = 0.38f), shape))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = if (selected) C.Ink else Color.White, maxLines = 1) }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, height: Dp = 48.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.height(height).clip(shape).background(Color(0x0FFFFFFF)).border(1.dp, C.Gold.copy(alpha = 0.38f), shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = C.Gold, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.clickAndHold(onClick: () -> Unit, onLong: () -> Unit): Modifier =
    this.combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLong)

/** 作者：ArchieKUO（金色斜體）＋ 版本 */
@Composable
fun AuthorLine(size: TextUnit = 18.sp) {
    GoldText("作者：ArchieKUO　v${com.archiekuo.carhome.BuildConfig.VERSION_NAME}", size, italic = true)
}
