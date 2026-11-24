package com.archiekuo.carhome.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.archiekuo.carhome.media.VisualizerEngine

/** 下方彩虹光條：細光條會發光、往上散出光束、下面有倒影，顏色一直流動 */
@Composable
fun VisualizerLayer(engine: VisualizerEngine, active: Boolean, followTempo: Boolean, modifier: Modifier) {
    var frameTime by remember { mutableLongStateOf(0L) }
    var hue by remember { mutableFloatStateOf(200f) }
    var lastNs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { ns ->
                val dt = if (lastNs == 0L) 0f else ((ns - lastNs) / 1e9f).coerceAtMost(0.1f)
                lastNs = ns
                engine.frame(ns / 1e9f, active)
                val speed = if (followTempo) 18f * engine.tempo else 16f
                hue = (hue + dt * speed) % 360f
                frameTime = ns
            }
        }
    }
    Canvas(modifier) {
        if (frameTime < 0L) return@Canvas
        drawBars(engine, hue)
    }
}

private fun DrawScope.drawBars(engine: VisualizerEngine, hueBase: Float) {
    val count = 170
    val w = size.width
    val base = size.height - 16.dp.toPx()
    val step = (w - 24.dp.toPx()) / count
    val barW = 3.dp.toPx()
    val maxH = 46.dp.toPx()
    val n = engine.n
    for (i in 0 until count) {
        val x = 12.dp.toPx() + i * step
        val pos = i / count.toFloat()
        // 低音在左、高音在右
        val f = pos * (n - 1)
        val i0 = f.toInt().coerceIn(0, n - 1)
        val i1 = (i0 + 1).coerceAtMost(n - 1)
        val v = engine.bands[i0] + (engine.bands[i1] - engine.bands[i0]) * (f - i0)
        val h = 3.dp.toPx() + v * maxH
        val hue = (hueBase + pos * 576f) % 360f
        val col = Color.hsv(hue, 0.85f, 1f)
        // 光束
        if (i % 4 == 0) {
            val bh = 60.dp.toPx() + v * 190.dp.toPx()
            val bw = 10.dp.toPx()
            drawRect(
                Brush.verticalGradient(listOf(col.copy(alpha = 0f), col.copy(alpha = 0.06f + 0.24f * v)), startY = base - bh, endY = base),
                topLeft = Offset(x - bw / 2 + barW / 2, base - bh), size = Size(bw, bh),
            )
        }
        // 發光（兩層半透明）＋ 光條本體
        drawRect(col.copy(alpha = 0.12f), Offset(x - barW * 1.5f, base - h - 2.dp.toPx()), Size(barW * 4f, h + 4.dp.toPx()))
        drawRect(col.copy(alpha = 0.3f), Offset(x - barW * 0.5f, base - h - 1.dp.toPx()), Size(barW * 2f, h + 2.dp.toPx()))
        drawRect(col, Offset(x, base - h), Size(barW, h))
        // 倒影
        val rh = h * 0.45f
        drawRect(Brush.verticalGradient(listOf(col.copy(alpha = 0.32f), col.copy(alpha = 0f)), startY = base + 2.dp.toPx(), endY = base + 2.dp.toPx() + rh), Offset(x, base + 2.dp.toPx()), Size(barW, rh))
    }
}
