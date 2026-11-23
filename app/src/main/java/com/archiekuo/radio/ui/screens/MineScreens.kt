package com.archiekuo.radio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archiekuo.radio.BuildConfig
import com.archiekuo.radio.data.HistoryEntry
import com.archiekuo.radio.ui.components.GoldPlayButton
import com.archiekuo.radio.ui.components.GoldText
import com.archiekuo.radio.ui.components.MessageBox
import com.archiekuo.radio.ui.components.ScreenHeader
import com.archiekuo.radio.ui.components.StationRow
import com.archiekuo.radio.ui.components.goldCard
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
private fun CardGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp)) {
        Text(title, fontSize = 17.sp, color = RC.Sub, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .goldCard(RoundedCornerShape(18.dp)),
            content = content,
        )
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(RC.Line),
    )
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = RC.Gold, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (value != null) Text(value, fontSize = 18.sp, color = RC.Sub)
        Icon(Icons.Rounded.ChevronRight, null, tint = RC.Dim, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun InfoRow(title: String, content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(66.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 20.sp, color = Color.White, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun AboutCard() {
    CardGroup("關於") {
        InfoRow("版本") { Text("v${BuildConfig.VERSION_NAME}", fontSize = 19.sp, color = RC.Sub) }
        Divider()
        InfoRow("作者") { GoldText("ArchieKUO", 20.sp, family = FontFamily.Default, weight = FontWeight.Bold, italic = true) }
        Divider()
        InfoRow("電台資料") { Text("Radio Browser", fontSize = 18.sp, color = RC.Sub) }
    }
}

@Composable
fun MineScreen(vm: MainViewModel, onHistory: () -> Unit, onSettings: () -> Unit, onFavorites: () -> Unit) {
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val hist by vm.history.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader("我的")
        CardGroup("我的電台") {
            MenuRow(Icons.Rounded.Favorite, "我的收藏", "${favs.size} 台", onFavorites)
            Divider()
            MenuRow(Icons.Rounded.History, "播放紀錄", "${hist.size} 筆", onHistory)
            Divider()
            MenuRow(Icons.Rounded.Settings, "設定", null, onSettings)
        }
        AboutCard()
        Spacer(Modifier.height(20.dp))
    }
}

private fun dayLabel(t: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = t }
    val today = Calendar.getInstance()
    val y = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    fun same(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    return when {
        same(c, today) -> "今天"
        same(c, y) -> "昨天"
        else -> SimpleDateFormat("M 月 d 日", Locale.TAIWAN).format(Date(t))
    }
}

@Composable
fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    val hist by vm.history.collectAsStateWithLifecycle()
    val ps by vm.playerState.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.TAIWAN) }
    val groups: List<Pair<String, List<HistoryEntry>>> = remember(hist) {
        hist.groupBy { dayLabel(it.time) }.toList()
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("播放紀錄", onBack) {
            if (hist.isNotEmpty()) ToolbarButton("清除", { confirm = true }, danger = true)
        }
        if (hist.isEmpty()) {
            MessageBox("還沒有播放紀錄", "聽過的電台會自動記在這裡")
            return@Column
        }
        val stations = hist.map { it.station }.distinctBy { it.id }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            groups.forEach { (label, list) ->
                item(key = "h-$label") {
                    Text(label, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = RC.Gold, modifier = Modifier.padding(top = 18.dp, bottom = 2.dp))
                }
                items(list, key = { "${it.time}-${it.station.id}" }) { e ->
                    val play = { vm.play(e.station, stations); onOpenPlayer() }
                    StationRow(e.station, ps.station?.id == e.station.id, play) {
                        Text(timeFmt.format(Date(e.time)), fontSize = 18.sp, color = RC.Sub)
                        GoldPlayButton(play)
                    }
                }
            }
        }
    }
    if (confirm) {
        ConfirmDialog("清除播放紀錄", "確定要清除全部 ${hist.size} 筆播放紀錄嗎？", "清除",
            onConfirm = { vm.clearHistory(); confirm = false }, onDismiss = { confirm = false })
    }
}

@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Text(detail, fontSize = 16.sp, color = RC.Sub, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RC.Gold,
                checkedBorderColor = RC.Gold,
                uncheckedThumbColor = RC.Sub,
                uncheckedTrackColor = Color(0x33FFFFFF),
                uncheckedBorderColor = Color(0x55FFFFFF),
            ),
        )
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val wifi by vm.wifiOnly.collectAsStateWithLifecycle()
    val auto by vm.autoPlay.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader("設定", onBack)
        CardGroup("播放") {
            SwitchRow("打開就播放", "打開 App 時，自動接著播上次的電台", auto, vm::setAutoPlay)
            Divider()
            SwitchRow("只用 Wi‑Fi 播放", "關閉時，用行動網路也能收聽", wifi, vm::setWifiOnly)
        }
        AboutCard()
        Spacer(Modifier.height(20.dp))
    }
}
