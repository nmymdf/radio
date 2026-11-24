package com.archiekuo.carhome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.carhome.location.Weather
import com.archiekuo.carhome.location.aqiText
import com.archiekuo.carhome.location.uvText
import com.archiekuo.carhome.location.weatherKind
import com.archiekuo.carhome.location.windLevel
import com.archiekuo.carhome.location.windText

private val RainBlue = Color(0xFF8FB4FF)

/** 點天氣卡：詳細天氣（全畫面） */
@Composable
fun WeatherPage(w: Weather?, place: String?, online: Boolean, onBack: () -> Unit) {
    PanelFrame("天氣" + (place?.let { " ・ $it" } ?: ""), onBack) {
        if (w == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.Cloud, null, tint = C.Sub, modifier = Modifier.size(90.dp))
                Text(if (online) "天氣載入中…" else "沒有網路，暫時看不到天氣", fontSize = 26.sp, color = C.Sub, modifier = Modifier.padding(top = 14.dp))
            }
            return@PanelFrame
        }
        val d = w.detail
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.width(760.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 現在
                Row(Modifier.fillMaxWidth().card(RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = weatherIcon(w.kind)
                    Icon(icon, w.text, tint = tint, modifier = Modifier.size(96.dp))
                    Spacer(Modifier.width(26.dp))
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${w.temp}°", fontSize = 76.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 78.sp)
                            Spacer(Modifier.width(14.dp))
                            Text(w.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 10.dp))
                        }
                        Text(
                            listOfNotNull(d?.feels?.let { "體感 $it°" }, w.max?.let { "最高 $it°" }, w.min?.let { "最低 $it°" }, w.rain?.let { "降雨 $it%" }).joinToString(" ・ "),
                            fontSize = 21.sp, color = C.Sub, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                // 接下來 12 小時
                if (d != null && d.hours.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().card(RoundedCornerShape(24.dp)).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("接下來 12 小時（可左右滑）", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Gold, modifier = Modifier.padding(start = 6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(d.hours) { i, h ->
                                val now = i == 0
                                Column(
                                    Modifier.width(85.dp).clip(RoundedCornerShape(18.dp))
                                        .then(if (now) Modifier.background(C.Gold.copy(alpha = 0.14f)).border(1.dp, C.Gold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)) else Modifier)
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(h.label, fontSize = 20.sp, color = if (now) C.Gold else C.Sub, fontWeight = if (now) FontWeight.Bold else FontWeight.Normal)
                                    val (ic, tint) = weatherIcon(weatherKind(h.code))
                                    Icon(ic, null, tint = tint, modifier = Modifier.size(42.dp))
                                    Text("${h.temp}°", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.WaterDrop, null, tint = RainBlue, modifier = Modifier.size(16.dp))
                                        Text("${h.rain ?: 0}%", fontSize = 17.sp, color = RainBlue)
                                    }
                                }
                            }
                        }
                    }
                }
                // 濕度、風、紫外線、空氣品質、日出、日落
                if (d != null) {
                    val stats = listOf(
                        Stat(Icons.Rounded.WaterDrop, "濕度", d.humidity?.let { "$it%" } ?: "--", null),
                        Stat(Icons.Rounded.Air, "風", d.windKmh?.let { "${windText(d.windDir)} ${windLevel(it)} 級" } ?: "--", d.windKmh?.let { "$it 公里/時" }),
                        Stat(Icons.Rounded.WbSunny, "紫外線", d.uv?.let { "${uvText(it)} ${Math.round(it)}" } ?: "--", null),
                        Stat(Icons.Rounded.Eco, "空氣品質", d.aqi?.let { "${aqiText(it)} $it" } ?: "--", d.pm25?.let { "PM2.5 $it" }),
                        Stat(Icons.Rounded.WbTwilight, "日出", d.sunrise ?: "--", null),
                        Stat(Icons.Rounded.NightsStay, "日落", d.sunset ?: "--", null),
                    )
                    stats.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { st -> StatBox(st, Modifier.weight(1f)) }
                        }
                    }
                }
            }
            // 未來 7 天
            Column(Modifier.weight(1f).fillMaxHeight().card(RoundedCornerShape(24.dp)).padding(horizontal = 22.dp, vertical = 16.dp)) {
                Text("未來 7 天", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = C.Gold, modifier = Modifier.padding(bottom = 6.dp))
                val days = d?.days.orEmpty()
                if (days.isEmpty()) Text("暫時沒有資料", fontSize = 20.sp, color = C.Sub)
                val lo = days.minOfOrNull { it.min } ?: 0
                val hi = days.maxOfOrNull { it.max } ?: 1
                days.forEach { day ->
                    Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(day.label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(62.dp))
                        val (ic, tint) = weatherIcon(weatherKind(day.code))
                        Icon(ic, null, tint = tint, modifier = Modifier.size(32.dp))
                        Text("${day.rain ?: 0}%", fontSize = 17.sp, color = RainBlue, textAlign = TextAlign.End, modifier = Modifier.width(54.dp))
                        Text("${day.min}°", fontSize = 21.sp, color = C.Sub, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        // 溫度範圍條
                        Box(Modifier.padding(horizontal = 10.dp).width(96.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x22FFFFFF))) {
                            val span = (hi - lo).coerceAtLeast(1).toFloat()
                            val a = (day.min - lo) / span
                            val b = (day.max - lo) / span
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth(b.coerceIn(0.05f, 1f)).padding(start = (96 * a).dp)
                                    .clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(RainBlue, C.Gold))),
                            )
                        }
                        Text("${day.max}°", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(46.dp))
                    }
                }
            }
        }
    }
}

private data class Stat(val icon: ImageVector, val name: String, val value: String, val sub: String?)

@Composable
private fun StatBox(s: Stat, modifier: Modifier) {
    Row(modifier.height(84.dp).card(RoundedCornerShape(18.dp)).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(s.icon, null, tint = C.Gold, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(s.name, fontSize = 17.sp, color = C.Sub)
            Text(s.value, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (s.sub != null) Text(s.sub, fontSize = 15.sp, color = C.Sub, maxLines = 1)
        }
    }
}
