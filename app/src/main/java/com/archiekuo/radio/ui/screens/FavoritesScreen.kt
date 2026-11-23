package com.archiekuo.radio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archiekuo.radio.ui.components.GoldPlayButton
import com.archiekuo.radio.ui.components.MessageBox
import com.archiekuo.radio.ui.components.ScreenHeader
import com.archiekuo.radio.ui.components.StationRow
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.viewmodel.MainViewModel

@Composable
fun CheckBox(checked: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(if (checked) Modifier.background(RC.Gold) else Modifier.border(2.dp, RC.Dim, RoundedCornerShape(8.dp))),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Rounded.Check, "已選", tint = Color(0xFF1A1405), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun ToolbarButton(text: String, onClick: () -> Unit, enabled: Boolean = true, danger: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    val fg = when {
        !enabled -> RC.Dim
        danger -> RC.Red
        else -> Color.White
    }
    Row(
        Modifier
            .clip(shape)
            .background(if (danger && enabled) Color(0x26EF4444) else Color(0x0FFFFFFF))
            .border(1.dp, if (danger && enabled) Color(0x80FF7878) else Color(0x33FFFFFF), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (danger) Icon(Icons.Rounded.Delete, null, tint = fg, modifier = Modifier.size(22.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RC.CardTop,
        title = { Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White) },
        text = { Text(text, fontSize = 18.sp, color = RC.Sub) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, fontSize = 18.sp, color = RC.Red, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", fontSize = 18.sp, color = Color.White) } },
    )
}

@Composable
fun FavoritesScreen(vm: MainViewModel, onOpenPlayer: () -> Unit) {
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val ps by vm.playerState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirm by remember { mutableStateOf(false) }
    val sel = selected.filter { id -> favs.any { it.id == id } }.toSet()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("我的收藏")
        if (favs.isEmpty()) {
            MessageBox("還沒有收藏的電台", "在電台旁邊按一下愛心，就會收藏到這裡")
            return@Column
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (sel.isEmpty()) "共 ${favs.size} 台" else "已選 ${sel.size} 台", fontSize = 17.sp, color = RC.Sub, modifier = Modifier.weight(1f))
            ToolbarButton(if (sel.size == favs.size) "全不選" else "全選", {
                selected = if (sel.size == favs.size) emptySet() else favs.map { it.id }.toSet()
            })
            ToolbarButton("移除收藏", { confirm = true }, enabled = sel.isNotEmpty(), danger = true)
        }
        LazyColumn(contentPadding = PaddingValues(start = 4.dp, end = 16.dp, bottom = 16.dp)) {
            itemsIndexed(favs, key = { _, s -> s.id }) { _, s ->
                val checked = s.id in sel
                val play = { vm.play(s, favs); onOpenPlayer() }
                StationRow(
                    s, ps.station?.id == s.id, play,
                    leading = { CheckBox(checked) { selected = if (checked) selected - s.id else selected + s.id } },
                ) { GoldPlayButton(play) }
            }
        }
    }

    if (confirm) {
        ConfirmDialog(
            "移除收藏",
            "確定要把選取的 ${sel.size} 個電台從收藏移除嗎？",
            "移除",
            onConfirm = { vm.removeFavorites(sel); selected = emptySet(); confirm = false },
            onDismiss = { confirm = false },
        )
    }
}
