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
private fun PanelFrame(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(panelColor()).clickable(enabled = true, onClick = {}).padding(horizontal = 28.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CircleBtn(Icons.Rounded.ArrowBackIosNew, "返回", 56.dp, 26.dp, onBack)
            GoldText(title, 32.sp, Modifier.weight(1f), weight = FontWeight.Black)
            AuthorLine()
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** 所有 App */
@Composable
fun AppDrawer(repo: AppsRepo, apps: List<AppInfo>, onLaunch: (AppInfo) -> Unit, onBack: () -> Unit) {
    PanelFrame("所有 App", onBack) {
        if (apps.isEmpty()) { Text("正在讀取 App…", fontSize = 22.sp, color = C.Sub); return@PanelFrame }
        LazyVerticalGrid(GridCells.Fixed(6), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(apps, key = { it.pkg }) { a ->
                Column(
                    Modifier.height(150.dp).card(RoundedCornerShape(18.dp)).clickable(role = SRole.Button) { onLaunch(a) }.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    repo.icon(a.pkg)?.let { Image(it, null, Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))) }
                    Spacer(Modifier.height(10.dp))
                    Text(a.label, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 23.sp)
                }
            }
        }
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
)

@Composable
fun SettingsPanel(
    m: SettingsModel,
    onAutoRadio: (Boolean) -> Unit, onAutoShield: (Boolean) -> Unit, onPickShield: () -> Unit,
    onAutoFm: (Boolean) -> Unit, onPickFm: () -> Unit, onYt: (Boolean) -> Unit, onViz: (Boolean) -> Unit, onVizTempo: (Boolean) -> Unit,
    onHideStatus: (Boolean) -> Unit, onBlack: (Boolean) -> Unit = {}, onOverlay: () -> Unit, onPermissions: () -> Unit, onDefaultHome: () -> Unit, onResetTiles: () -> Unit,
    onBack: () -> Unit,
) {
    PanelFrame("車機桌面設定", onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 120.dp)) {
            Group("發動車子後") {
                SwitchRow("自動播網路收音機", "接著播上次的電台（會先等網路連上）", m.autoRadio, onAutoRadio)
                SwitchRow("自動打開神盾", "打開後自動回到桌面，神盾變成懸浮視窗；你隨時可以自己關掉", m.autoShield, onAutoShield)
                ActionRow("神盾 App", m.shieldLabel, onPickShield)
            }
            Group("收音機") {
                SwitchRow("沒網路時自動切 FM", "網路收音機斷網超過 10 秒，改開原廠 FM；網路恢復再切回來", m.autoFm, onAutoFm)
                ActionRow("FM 收音機 App", m.fmLabel, onPickFm)
            }
            Group("YouTube") {
                SwitchRow("行駛中只播聲音", "車子在動時遮住畫面；副駕駛要看時，按「副駕駛觀看」", m.ytRestrict, onYt)
            }
            Group("畫面") {
                SwitchRow("全黑桌面", "關閉＝午夜藍加金色；打開＝全黑背景，晚上開車更不刺眼", m.black, onBlack)
                SwitchRow("跳動光條", if (m.vizReal) "下方彩虹光條跟著音樂跳動（已讀到聲音）" else "下方彩虹光條（讀不到聲音時會柔和起伏）", m.viz, onViz)
                SwitchRow("光條流動跟著節奏", "快歌流得快，慢歌流得慢", m.vizTempo, onVizTempo)
                SwitchRow("隱藏系統狀態列", "畫面更乾淨（日期和網路狀態桌面上都有）", m.hideStatus, onHideStatus)
                ActionRow("下方方塊全部恢復預設", "恢復", onResetTiles)
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
