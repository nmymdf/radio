package com.archiekuo.carhome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role as SRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.carhome.data.AppInfo
import com.archiekuo.carhome.data.AppsRepo

@Composable
fun PanelFrame(
    title: String, onBack: () -> Unit, author: Boolean = true,
    right: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(panelColor()).clickable(enabled = true, onClick = {}).padding(horizontal = 28.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CircleBtn(Icons.Rounded.ArrowBackIosNew, "返回", 56.dp, 26.dp, onBack)
            GoldText(title, 32.sp, Modifier.weight(1f), weight = FontWeight.Black)
            right()
            if (author) AuthorLine()
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** 選一個 App（方塊、神盾、FM 都用這個） */
@Composable
fun AppPicker(title: String, repo: AppsRepo, apps: List<AppInfo>, allowReset: Boolean, onPick: (AppInfo?) -> Unit, onBack: () -> Unit) {
    PanelFrame(title, onBack) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (allowReset) item {
                Row(Modifier.fillMaxWidth().height(76.dp).card(RoundedCornerShape(16.dp)).clickable { onPick(null) }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Restore, null, tint = C.Gold, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(18.dp))
                    Text("恢復預設", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = C.Gold)
                }
            }
            items(apps, key = { it.pkg }) { a ->
                Row(Modifier.fillMaxWidth().height(76.dp).card(RoundedCornerShape(16.dp)).clickable { onPick(a) }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    repo.icon(a.pkg)?.let { Image(it, null, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))) }
                    Spacer(Modifier.width(18.dp))
                    Text(a.label, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(a.pkg, fontSize = 15.sp, color = C.Dim, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 380.dp))
                }
            }
        }
    }
}

@Composable
fun SwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 84.dp).clickable(role = SRole.Switch) { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(detail, fontSize = 17.sp, color = C.Sub, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = C.Gold, checkedBorderColor = C.Gold, uncheckedThumbColor = C.Sub, uncheckedTrackColor = Color(0x33FFFFFF), uncheckedBorderColor = Color(0x55FFFFFF)))
    }
}

@Composable
fun ActionRow(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(78.dp).clickable(role = SRole.Button, onClick = onClick).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
        Text(value, fontSize = 19.sp, color = C.Gold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 420.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = C.Dim, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, fontSize = 19.sp, color = C.Sub, modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().card(RoundedCornerShape(18.dp)), content = content)
}

data class SettingsModel(
    val autoRadio: Boolean, val autoShield: Boolean, val shieldLabel: String, val autoFm: Boolean, val fmLabel: String,
    val ytRestrict: Boolean, val viz: Boolean, val vizTempo: Boolean, val hideStatus: Boolean, val overlayOk: Boolean, val vizReal: Boolean,
    val black: Boolean = false,
    val shieldLeft: Boolean = false,
    val memText: String = "",
    val ytWin: String = "",
)

@Composable
fun SettingsPanel(
    m: SettingsModel,
    onAutoRadio: (Boolean) -> Unit, onAutoShield: (Boolean) -> Unit, onPickShield: () -> Unit,
    onAutoFm: (Boolean) -> Unit, onPickFm: () -> Unit, onYt: (Boolean) -> Unit, onViz: (Boolean) -> Unit, onVizTempo: (Boolean) -> Unit,
    onHideStatus: (Boolean) -> Unit, onBlack: (Boolean) -> Unit = {}, onOverlay: () -> Unit, onPermissions: () -> Unit, onDefaultHome: () -> Unit, onResetTiles: () -> Unit,
    onShieldLeft: (Boolean) -> Unit = {}, onMemory: () -> Unit = {},
    onYtWin: () -> Unit = {},
    onBack: () -> Unit,
) {
    PanelFrame("車機桌面設定", onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 120.dp)) {
            Group("發動車子後") {
                SwitchRow("自動播網路收音機", "接著播上次的電台（會先等網路連上）", m.autoRadio, onAutoRadio)
                SwitchRow("自動打開神盾", "預設關閉：要用時按下方「神盾」方塊自己打開（神盾說話會讓 YouTube 暫停）", m.autoShield, onAutoShield)
                ActionRow("神盾 App", m.shieldLabel, onPickShield)
            }
            Group("YouTube") {
                ActionRow("小窗開法（點一下切換）", m.ytWin, onYtWin)
            }
            Group("收音機") {
                SwitchRow("沒網路時自動切 FM", "網路收音機斷網超過 10 秒，改開原廠 FM；網路恢復再切回來", m.autoFm, onAutoFm)
                ActionRow("FM 收音機 App", m.fmLabel, onPickFm)
            }
            Group("畫面") {
                SwitchRow("全黑桌面", "關閉＝午夜藍加金色；打開＝全黑背景，晚上開車更不刺眼", m.black, onBlack)
                SwitchRow("導航區和 YouTube 放左邊", "打開＝導航區、YouTube 在左邊（靠駕駛座），時鐘和天氣換到右邊", m.shieldLeft, onShieldLeft)
                SwitchRow("跳動光條", if (m.vizReal) "下方彩虹光條跟著音樂跳動（已讀到聲音）" else "下方彩虹光條（讀不到聲音時會柔和起伏）", m.viz, onViz)
                SwitchRow("光條流動跟著節奏", "快歌流得快，慢歌流得慢", m.vizTempo, onVizTempo)
                SwitchRow("隱藏系統狀態列", "畫面更乾淨（日期和網路狀態桌面上都有）", m.hideStatus, onHideStatus)
                ActionRow("下方方塊全部恢復預設", "恢復", onResetTiles)
            }
            Group("系統") {
                ActionRow("記憶體與背景 App", m.memText.ifBlank { "查看" }, onMemory)
            }
            Group("權限與桌面") {
                ActionRow("允許顯示在其他 App 上層", if (m.overlayOk) "已允許" else "尚未允許（自動開神盾需要）", onOverlay)
                ActionRow("位置、錄音、音樂檔案權限", "重新詢問", onPermissions)
                ActionRow("設為預設桌面", "打開系統設定", onDefaultHome)
            }
            Group("關於") {
                Row(Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("版本", fontSize = 22.sp, color = Color.White, modifier = Modifier.weight(1f))
                    Text("v${com.archiekuo.carhome.BuildConfig.VERSION_NAME}", fontSize = 21.sp, color = C.Sub)
                }
                Row(Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("作者", fontSize = 22.sp, color = Color.White, modifier = Modifier.weight(1f))
                    GoldText("ArchieKUO", 22.sp, italic = true)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun ConfirmBox(title: String, text: String, ok: String, onOk: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Color(0xFF1C2558),
        title = { Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White) },
        text = { Text(text, fontSize = 21.sp, color = C.Sub, lineHeight = 30.sp) },
        confirmButton = { TextButton(onClick = onOk) { Text(ok, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.Gold) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消", fontSize = 22.sp, color = Color.White) } },
    )
}

/** 選 YouTube 平常顯示的照片 */
@Composable
fun PhotoPicker(current: String, onPick: (String) -> Unit, onUsb: () -> Unit, onBack: () -> Unit) {
    PanelFrame("YouTube 平常顯示的照片", onBack) {
        val items = ytPhotos.map { it.first to it.second } + listOf("black" to "全黑", "usb" to "從隨身碟選照片")
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            items(items, key = { it.first }) { (k, label) ->
                val on = k == current || (k == "usb" && current.startsWith("file:"))
                Column(
                    Modifier.height(250.dp).card(RoundedCornerShape(20.dp), alpha = if (on) 1f else 0.38f)
                        .clickable { if (k == "usb") onUsb() else onPick(k) }.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                        when {
                            k == "usb" && current.startsWith("file:") -> YtPhoto(current, Modifier.fillMaxSize())
                            k == "usb" -> Icon(Icons.Rounded.Usb, null, tint = C.Gold, modifier = Modifier.size(64.dp))
                            else -> YtPhoto(k, Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (on) { Icon(Icons.Rounded.CheckCircle, null, tint = C.Gold, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(6.dp)) }
                        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (on) C.Gold else Color.White)
                    }
                }
            }
        }
    }
}

/** 點「導航」方塊：說目的地、回家、去公司、打開導航 */
@Composable
fun NavMenu(appLabel: String?, onVoice: () -> Unit, onHome: () -> Unit, onWork: () -> Unit, onOpen: () -> Unit, onBack: () -> Unit) {
    PanelFrame("導航 ・ ${appLabel ?: "尚未選擇導航 App"}", onBack) {
        Column(Modifier.fillMaxSize().padding(horizontal = 40.dp), verticalArrangement = Arrangement.Center) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                BigNavButton(Icons.Rounded.Mic, "說目的地", "說完自動在導航搜尋", true, onVoice)
                BigNavButton(Icons.Rounded.Home, "回家", "一鍵開始導航", false, onHome)
                BigNavButton(Icons.Rounded.Work, "去公司", "一鍵開始導航", false, onWork)
                BigNavButton(Icons.Rounded.Map, "打開導航", appLabel ?: "選擇導航 App", false, onOpen)
            }
            Spacer(Modifier.height(28.dp))
            Text("「回家」、「去公司」請先在導航 App 裡設好地址 ・ 長按「導航」方塊可以更換導航 App", fontSize = 19.sp, color = C.Sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 畫面上方的大字提示 */
@Composable
fun MessageBanner(text: String?, modifier: Modifier) {
    if (text == null) return
    Box(modifier.clip(RoundedCornerShape(30.dp)).background(Color(0xF0101838)).border(1.5.dp, C.Gold, RoundedCornerShape(30.dp)).padding(horizontal = 30.dp, vertical = 14.dp)) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun RowScope.BigNavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, sub: String, primary: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.weight(1f).height(230.dp)
            .then(if (primary) Modifier.clip(shape).background(GoldBrush) else Modifier.card(shape, alpha = 0.45f))
            .clickable(role = SRole.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (primary) C.Ink else C.Gold, modifier = Modifier.size(76.dp))
        Spacer(Modifier.height(14.dp))
        Text(label, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (primary) C.Ink else Color.White, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(sub, fontSize = 17.sp, color = if (primary) C.Ink.copy(alpha = 0.75f) else C.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp))
    }
}
