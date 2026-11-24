package com.archiekuo.carhome.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archiekuo.carhome.location.Weather
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** 每秒更新的現在時間 */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000L - now % 1000L) } }
    return now
}

@Composable
fun TopBar(now: Long, online: Boolean, road: String?, roadStale: Boolean, speed: Float?) {
    val date = remember(now / 60000) { SimpleDateFormat("M 月 d 日 EEEE", Locale.TAIWAN).format(Date(now)) }
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(date, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (online) Icons.Rounded.Wifi else Icons.Rounded.WifiOff, null, tint = if (online) C.GreenText else C.Red, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (online) "網路已連線" else "沒有網路", fontSize = 19.sp, color = if (online) C.GreenText else C.Red, maxLines = 1)
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Place, null, tint = C.Gold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                (road ?: "正在找目前的路名…") + if (roadStale) "（未更新）" else "",
                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(14.dp))
            Box(Modifier.clip(RoundedCornerShape(18.dp)).background(GoldBrush).padding(horizontal = 14.dp, vertical = 3.dp)) {
                Text(if (speed != null) "${speed.toInt()} km/h" else "-- km/h", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.Ink, maxLines = 1)
            }
        }
        AuthorLine()
    }
}

private val dirNames = listOf("北", "東北", "東", "東南", "南", "西南", "西", "西北")
fun headingText(h: Float?): String {
    if (h == null) return "等待 GPS"
    val d = ((h % 360) + 360) % 360
    return "${dirNames[((d + 22.5f) / 45f).toInt() % 8]} ${d.toInt()}°"
}

/** 時鐘＋指南針共用一個圓 */
@Composable
fun ClockCompass(now: Long, heading: Float?) {
    val time = remember(now / 1000) { SimpleDateFormat("HH:mm", Locale.TAIWAN).format(Date(now)) }
    Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color(0xBF0A1030), r - 2.dp.toPx(), c)
            drawCircle(C.Gold.copy(alpha = 0.7f), r - 2.dp.toPx(), c, style = Stroke(2.dp.toPx()))
            drawCircle(C.Gold.copy(alpha = 0.25f), r * 0.64f, c, style = Stroke(1.dp.toPx()))
            for (d in 0 until 360 step 10) {
                val a = Math.toRadians(d.toDouble())
                val r1 = r * 0.947f
                val r2 = when { d % 90 == 0 -> r * 0.867f; d % 30 == 0 -> r * 0.893f; else -> r * 0.913f }
                drawLine(
                    C.Gold.copy(alpha = if (d % 90 == 0) 1f else 0.55f),
                    Offset(c.x + r1 * sin(a).toFloat(), c.y - r1 * cos(a).toFloat()),
                    Offset(c.x + r2 * sin(a).toFloat(), c.y - r2 * cos(a).toFloat()),
                    strokeWidth = (if (d % 90 == 0) 3 else 1.5f).dp.toPx(),
                )
            }
            if (heading != null) {
                rotate(heading, c) {
                    val p = Path().apply {
                        moveTo(c.x, c.y - r * 0.987f)
                        lineTo(c.x - 7.dp.toPx(), c.y - r * 0.827f)
                        lineTo(c.x + 7.dp.toPx(), c.y - r * 0.827f)
                        close()
                    }
                    drawPath(p, C.Gold)
                }
            }
        }
        // 東南西北
        listOf(0 to "北", 90 to "東", 180 to "南", 270 to "西").forEach { (d, t) ->
            val a = Math.toRadians(d.toDouble())
            Text(
                t, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (d == 0) Color(0xFFFF8A5C) else C.Gold,
                modifier = Modifier.offset(x = (112 * sin(a)).dp, y = (-112 * cos(a)).dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 72.sp)
            Text(headingText(heading), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = C.Gold, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun WeatherCard(w: Weather?, place: String?, online: Boolean) {
    Row(
        Modifier.width(360.dp).height(154.dp).card().padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (w == null) {
            Icon(Icons.Rounded.Cloud, null, tint = C.Sub, modifier = Modifier.size(64.dp))
            Text(if (online) "天氣載入中…" else "沒有網路，暫時看不到天氣", fontSize = 21.sp, color = C.Sub)
            return@Row
        }
        val (icon, tint) = when (w.kind) {
            "sun" -> Icons.Rounded.WbSunny to C.Gold
            "partly" -> Icons.Rounded.WbCloudy to C.GoldLight
            "rain" -> Icons.Rounded.Umbrella to Color(0xFF8FB4FF)
            "storm" -> Icons.Rounded.Thunderstorm to Color(0xFFFFD36A)
            "snow" -> Icons.Rounded.AcUnit to Color(0xFFDFE6FF)
            else -> Icons.Rounded.Cloud to Color(0xFFDFE6FF)
        }
        Icon(icon, w.text, tint = tint, modifier = Modifier.size(72.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${w.temp}°", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 52.sp)
                Spacer(Modifier.width(10.dp))
                Text(w.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(listOfNotNull(place, w.rain?.let { "降雨 $it%" }).joinToString(" ・ "), fontSize = 19.sp, color = C.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            if (w.max != null && w.min != null) Text("最高 ${w.max}° ・ 最低 ${w.min}°", fontSize = 17.sp, color = C.Sub, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
