package com.archiekuo.radio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.radio.BuildConfig
import com.archiekuo.radio.ui.theme.CardBrush
import com.archiekuo.radio.ui.theme.GoldButtonBrush
import com.archiekuo.radio.ui.theme.GoldTextBrush
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.ui.theme.SerifTitle
import kotlin.random.Random

/** 深藍星空背景 */
@Composable
fun Backdrop(content: @Composable BoxScope.() -> Unit) {
    val stars = remember {
        val r = Random(7)
        List(70) { Triple(r.nextFloat(), r.nextFloat() * 0.55f, 0.6f + r.nextFloat() * 1.4f) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(RC.Navy2, RC.Navy1, RC.Navy0)))
                drawRect(
                    Brush.radialGradient(
                        listOf(RC.Glow, Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 1.1f,
                    ),
                )
                stars.forEachIndexed { i, (x, y, s) ->
                    drawCircle(
                        Color.White.copy(alpha = if (i % 3 == 0) 0.75f else 0.4f),
                        radius = s * density,
                        center = Offset(x * size.width, y * size.height),
                    )
                }
            },
        content = content,
    )
}

/** 精緻圓角卡片：細金邊 + 陰影 */
fun Modifier.goldCard(shape: Shape, brush: Brush = CardBrush, elevation: Dp = 8.dp, borderAlpha: Float = 0.38f): Modifier =
    this
        .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
        .clip(shape)
        .background(brush)
        .border(1.dp, RC.Gold.copy(alpha = borderAlpha), shape)

@Composable
fun GoldText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    family: FontFamily = SerifTitle,
    weight: FontWeight = FontWeight.Black,
    italic: Boolean = false,
    letterSpacing: TextUnit = 0.sp,
    align: TextAlign? = null,
) {
    Text(
        text,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        style = TextStyle(
            brush = GoldTextBrush,
            fontSize = size,
            fontFamily = family,
            fontWeight = weight,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            letterSpacing = letterSpacing,
        ),
    )
}

/** 作者：ArchieKUO（金色斜體）＋ 版本 */
@Composable
fun AuthorLine(modifier: Modifier = Modifier) {
    GoldText(
        "作者：ArchieKUO　v${BuildConfig.VERSION_NAME}",
        14.sp,
        modifier,
        family = FontFamily.Default,
        weight = FontWeight.Bold,
        italic = true,
    )
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
    size: Dp = 48.dp,
    tint: Color = Color.White,
    iconSize: Dp = size * 0.5f,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0x8C080E2C))
            .border(1.5.dp, RC.Gold.copy(alpha = 0.6f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** 頁面標題列：返回鍵（可省略）＋ 金色標題 ＋ 作者 ＋ 右側按鈕 */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) CircleIconButton(Icons.Rounded.ArrowBackIosNew, "返回", onBack, iconSize = 22.dp)
        Column(Modifier.weight(1f)) {
            GoldText(title, 30.sp, letterSpacing = 2.sp)
            AuthorLine()
        }
        actions()
    }
}

@Composable
fun SectionHeader(icon: ImageVector, title: String, onMore: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = RC.Gold, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
        if (onMore != null) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onMore)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("查看更多", fontSize = 17.sp, color = RC.Gold, fontWeight = FontWeight.Medium)
                Icon(Icons.Rounded.ChevronRight, null, tint = RC.Gold, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Row(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(if (enabled) GoldButtonBrush else Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (enabled) Color(0xFF1A1405) else RC.Dim
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, hPad: Dp = 18.dp) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .height(48.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.background(GoldButtonBrush)
                else Modifier.background(Color(0x0FFFFFFF)).border(1.dp, RC.Gold.copy(alpha = 0.38f), shape),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = hPad),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color(0xFF1A1405) else Color.White,
            maxLines = 1,
        )
    }
}

@Composable
fun MessageBox(title: String, detail: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        if (detail != null) Text(detail, fontSize = 17.sp, color = RC.Sub, textAlign = TextAlign.Center)
        if (action != null && onAction != null) GoldButton(action, onAction)
    }
}

@Composable
fun LoadingBox() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            androidx.compose.material3.CircularProgressIndicator(color = RC.Gold, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
            Text("正在找電台…", fontSize = 18.sp, color = RC.Sub)
        }
    }
}
