package com.archiekuo.radio.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.radio.R
import com.archiekuo.radio.model.Categories
import com.archiekuo.radio.model.Category
import com.archiekuo.radio.ui.components.AuthorLine
import com.archiekuo.radio.ui.components.CircleIconButton
import com.archiekuo.radio.ui.components.GoldText
import com.archiekuo.radio.ui.components.SectionHeader
import com.archiekuo.radio.ui.components.goldCard
import com.archiekuo.radio.ui.theme.RC
import com.archiekuo.radio.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onCategory: (String) -> Unit,
    onAllCategories: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // 標題列
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(painterResource(R.drawable.ic_logo), null, Modifier.size(56.dp))
            Column(Modifier.weight(1f)) {
                GoldText("網路收音機", 31.sp, letterSpacing = 2.sp)
                AuthorLine()
            }
            CircleIconButton(Icons.Rounded.Search, "搜尋", onSearch)
            CircleIconButton(Icons.Rounded.Settings, "設定", onSettings)
        }

        // 主視覺：點一下就開始播放
        Box(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)
                .fillMaxWidth()
                .aspectRatio(808f / 486f)
                .goldCard(RoundedCornerShape(20.dp))
                .clickable(role = Role.Button, onClickLabel = "開始播放") {
                    vm.playBanner()
                    onOpenPlayer()
                },
        ) {
            Image(
                painterResource(R.drawable.art_banner),
                contentDescription = "今晚，聽點好音樂。開始播放",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        SectionHeader(Icons.Rounded.LocalFireDepartment, "熱門電台", onAllCategories)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Categories.hotStations.forEach { c ->
                HotCard(c, Modifier.weight(1f)) { onCategory(c.key) }
            }
        }

        SectionHeader(Icons.Rounded.MusicNote, "音樂分類", onAllCategories)
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Categories.music.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { c -> CategoryCard(c, Modifier.weight(1f)) { onCategory(c.key) } }
                }
            }
        }
    }
}

@Composable
private fun HotCard(c: Category, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .goldCard(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Image(
            painterResource(c.art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF1B2457), Color(0xFF10173F)))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(c.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Clip)
            Icon(Icons.Rounded.GraphicEq, null, tint = RC.Gold, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CategoryCard(c: Category, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(100.dp)
            .goldCard(RoundedCornerShape(16.dp), Brush.verticalGradient(listOf(Color(c.tint), Color(0xFF121A46))))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        c.icon?.let { Image(painterResource(it), null, Modifier.size(46.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(c.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
    }
}
