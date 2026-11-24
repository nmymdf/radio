package com.archiekuo.carhome.media

import android.media.audiofx.Visualizer
import android.os.SystemClock
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 跳動光條的聲音分析：
 * 用 Android 的 Visualizer 讀取正在播放的聲音（需要「錄音」權限，只分析不錄音）。
 * 讀不到聲音時，改成柔和的自動起伏。
 */
class VisualizerEngine {
    val n = 64
    /** 畫面用的每一段高度 0～1 */
    val bands = FloatArray(n)
    private val target = FloatArray(n)
    @Volatile var level = 0f; private set
    /** 流動速度倍數（依節奏） */
    @Volatile var tempo = 1f; private set
    @Volatile private var lastAudio = 0L
    private var viz: Visualizer? = null
    private var avgBass = 0.1f
    private var lastBeat = 0L
    private val beatGaps = ArrayDeque<Long>()

    val hasRealAudio: Boolean get() = SystemClock.elapsedRealtime() - lastAudio < 2000

    fun start(): Boolean {
        if (viz != null) return true
        return runCatching {
            val v = Visualizer(0)
            v.enabled = false
            v.captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft != null) process(fft)
                }
            }, (Visualizer.getMaxCaptureRate() * 3 / 4), false, true)
            v.enabled = true
            viz = v
            true
        }.getOrDefault(false)
    }

    fun stop() {
        runCatching { viz?.enabled = false; viz?.release() }
        viz = null
    }

    private fun process(fft: ByteArray) {
        val bins = fft.size / 2
        if (bins < 8) return
        var sum = 0f
        val mags = FloatArray(bins)
        for (k in 1 until bins) {
            val re = fft[2 * k].toFloat()
            val im = fft[2 * k + 1].toFloat()
            mags[k] = hypot(re, im)
            sum += mags[k]
        }
        if (sum < 2f) return
        lastAudio = SystemClock.elapsedRealtime()
        val maxBin = (bins - 1).toFloat()
        for (b in 0 until n) {
            val lo = (maxBin.pow(b / n.toFloat())).toInt().coerceIn(1, bins - 1)
            val hi = (maxBin.pow((b + 1) / n.toFloat())).toInt().coerceIn(lo, bins - 1)
            var m = 0f
            for (k in lo..hi) if (mags[k] > m) m = mags[k]
            // 高頻能量本來就小，稍微補強
            val boost = 1f + b / n.toFloat() * 1.5f
            target[b] = (sqrt(m * boost / 110f)).coerceIn(0f, 1f)
        }
        // 節奏：低音突然變大就算一拍
        val bass = (target[0] + target[1] + target[2] + target[3] + target[4]) / 5f
        avgBass = avgBass * 0.94f + bass * 0.06f
        val now = SystemClock.elapsedRealtime()
        if (bass > avgBass * 1.3f && bass > 0.22f && now - lastBeat > 260) {
            if (lastBeat > 0) {
                val gap = now - lastBeat
                if (gap in 260..1500) { beatGaps.addLast(gap); if (beatGaps.size > 8) beatGaps.removeFirst() }
            }
            lastBeat = now
        }
        if (beatGaps.size >= 3) {
            val bpm = 60000f / beatGaps.sorted()[beatGaps.size / 2]
            tempo = (bpm / 100f).coerceIn(0.5f, 2f)
        }
        if (now - lastBeat > 4000) tempo += (0.6f - tempo) * 0.05f
    }

    /** 每一格畫面呼叫一次；active 代表有東西在播放 */
    fun frame(timeSec: Float, active: Boolean) {
        val real = hasRealAudio
        if (!real) {
            tempo += ((if (active) 0.9f else 0.4f) - tempo) * 0.02f
            for (b in 0 until n) {
                val x = b / n.toFloat()
                val wave = 0.5f + 0.5f * sin(timeSec * 2.2f + x * 9f) * sin(timeSec * 0.9f + x * 23f)
                target[b] = if (active) 0.08f + 0.22f * wave * (1.1f - x * 0.6f) else 0.035f + 0.02f * wave
            }
        }
        var s = 0f
        for (b in 0 until n) {
            val t = target[b]
            val c = bands[b]
            bands[b] = c + (t - c) * (if (t > c) 0.55f else 0.12f)
            s += bands[b]
        }
        level = s / n
    }
}
