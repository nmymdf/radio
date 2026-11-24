package com.archiekuo.carhome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.carhome.data.AppInfo
import com.archiekuo.carhome.data.AppsRepo

/**
 * 所有 App：上面搜尋，點一下打開，長按出現選單（放到下方方塊／App 資訊／移除）。
 * columns：一排幾個（分割畫面時比較少）。
 */
@Composable
fun AppsPage(
    repo: AppsRepo, apps: List<AppInfo>, tileLabels: List<String>, columns: Int = 6, compact: Boolean = false,
    onLaunch: (AppInfo) -> Unit, onAssign: (AppInfo, Int) -> Unit, onInfo: (AppInfo) -> Unit, onUninstall: (AppInfo) -> Unit,
    onVoice: ((((String) -> Unit)) -> Unit)? = null, onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppInfo?>(null) }
    var pickSlot by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val shown = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) || it.pkg.contains(q, ignoreCase = true) }
    }
    Box(Modifier.fillMaxSize()) {
        PanelFrame("所有 App", onBack, author = !compact, right = {
            if (!compact) SearchBox(query, { query = it }, Modifier.width(380.dp), onVoice?.let { v -> { v { query = it } } }) { focus.clearFocus() }
        }) {
            if (compact) {
                SearchBox(query, { query = it }, Modifier.fillMaxWidth(), onVoice?.let { v -> { v { query = it } } }) { focus.clearFocus() }
                Spacer(Modifier.height(12.dp))
            }
            when {
                apps.isEmpty() -> Text("正在讀取 App…", fontSize = 22.sp, color = C.Sub)
                shown.isEmpty() -> Text("找不到「${query.trim()}」", fontSize = 23.sp, color = C.Sub, modifier = Modifier.padding(top = 20.dp))
                else -> LazyVerticalGrid(
                    GridCells.Fixed(columns), Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(shown, key = { it.pkg }) { a ->
                        Column(
                            Modifier.height(150.dp).card(RoundedCornerShape(18.dp))
                                .clickAndHold(onClick = { focus.clearFocus(); onLaunch(a) }, onLong = { focus.clearFocus(); pickSlot = false; menuFor = a })
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            repo.icon(a.pkg)?.let { Image(it, null, Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))) }
                            Spacer(Modifier.height(10.dp))
                            Text(a.label, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 23.sp)
                        }
                    }
                }
            }
            if (!compact) Text("點一下打開 ・ 長按出現選單 ・ 原廠內建 App 會打開「App 資訊」讓你停用", fontSize = 18.sp, color = C.Sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }

        // 長按選單（小卡片，不是大視窗）
        val a = menuFor
        if (a != null) {
            Box(Modifier.fillMaxSize().background(Color(0x8C000000)).clickable { menuFor = null }, contentAlignment = Alignment.Center) {
                Column(
                    Modifier.width(if (pickSlot) 440.dp else 360.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF1C2558))
                        .border(1.5.dp, C.Gold, RoundedCornerShape(22.dp)).clickable(enabled = true, onClick = {}).padding(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        repo.icon(a.pkg)?.let { Image(it, null, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))) }
                        Spacer(Modifier.width(12.dp))
                        Text(if (pickSlot) "放到哪一個方塊？" else a.label, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1FFFFFFF)))
                    if (!pickSlot) {
                        MenuRow(Icons.Rounded.Apps, "放到下方方塊", Color.White) { pickSlot = true }
                        MenuRow(Icons.Rounded.Info, "App 資訊", Color.White) { menuFor = null; onInfo(a) }
                        MenuRow(Icons.Rounded.Delete, if (a.system) "停用（原廠內建）" else "移除", C.Red) { menuFor = null; onUninstall(a) }
                    } else {
                        tileLabels.forEachIndexed { i, label ->
                            MenuRow(null, "第 ${i + 1} 個　（現在：$label）", Color.White) { menuFor = null; pickSlot = false; onAssign(a, i) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector?, text: String, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, null, tint = if (color == Color.White) C.Gold else color, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(14.dp)) }
        Text(text, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier, onVoice: (() -> Unit)?, onDone: () -> Unit) {
    val fr = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    Row(
        modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).background(Color(0x99080E2C)).border(1.5.dp, C.Gold, RoundedCornerShape(26.dp))
            .clickable { runCatching { fr.requestFocus() }; kb?.show() }.padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = C.Gold, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text("搜尋 App", fontSize = 20.sp, color = C.Dim)
            BasicTextField(
                value, onChange, singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(fr),
                textStyle = TextStyle(fontSize = 20.sp, color = Color.White), cursorBrush = SolidColor(C.Gold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onDone() }),
            )
        }
        if (value.isNotEmpty()) Icon(Icons.Rounded.Close, "清除", tint = C.Sub, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable { onChange("") }.padding(8.dp))
        if (onVoice != null) Icon(Icons.Rounded.Mic, "語音搜尋", tint = C.Gold, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onVoice).padding(7.dp))
    }
}
