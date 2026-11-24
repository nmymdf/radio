package com.archiekuo.carhome.split

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.archiekuo.carhome.CarApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

/**
 * 協助工具服務：
 * 1. 用系統的「分割畫面」切換（和長按多工鍵一樣）
 * 2. 導航畫面出現時，顯示金色浮動按鈕：一鍵 全畫面 ↔ 分割
 */
class SplitService : AccessibilityService() {
    companion object {
        @Volatile var instance: SplitService? = null
            private set
        val running = MutableStateFlow(false)
    }

    private val app get() = application as CarApp
    private val handler = Handler(Looper.getMainLooper())
    private var button: FloatButton? = null
    private var buttonShown = false
    private val checkRunnable = Runnable { check() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        running.value = true
        scheduleCheck()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val t = event?.eventType ?: return
        if (t == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || t == AccessibilityEvent.TYPE_WINDOWS_CHANGED) scheduleCheck()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        hideButton()
        handler.removeCallbacksAndMessages(null)
        instance = null
        running.value = false
        super.onDestroy()
    }

    private fun scheduleCheck() {
        handler.removeCallbacks(checkRunnable)
        handler.postDelayed(checkRunnable, 350)
    }

    /** 現在是不是分割畫面 */
    fun isSplit(): Boolean {
        if (Build.VERSION.SDK_INT < 24) return false
        return runCatching { windows.any { it.type == AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER } }.getOrDefault(false)
    }

    /** 某個 App 的畫面有沒有顯示出來 */
    fun isVisible(pkg: String): Boolean = runCatching {
        windows.any { w -> w.type == AccessibilityWindowInfo.TYPE_APPLICATION && w.root?.packageName?.toString() == pkg }
    }.getOrDefault(false)

    /** 切換分割畫面（進入：把最上面的 App 放到一邊；離開：那個 App 變全畫面） */
    fun toggleSplit(): Boolean =
        Build.VERSION.SDK_INT >= 24 && runCatching { performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN) }.getOrDefault(false)

    private fun check() {
        if (Build.VERSION.SDK_INT < 24) return
        val nav = app.navPackage()
        val show = nav != null && isVisible(nav)
        if (show) app.navSeenAt = android.os.SystemClock.elapsedRealtime()
        if (show) showButton(isSplit()) else hideButton()
    }

    // ---------------- 金色浮動按鈕 ----------------

    private fun wm() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private fun screen(): DisplayMetrics {
        val m = DisplayMetrics()
        @Suppress("DEPRECATION") wm().defaultDisplay.getRealMetrics(m)
        return m
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showButton(split: Boolean) {
        if (Build.VERSION.SDK_INT < 22) return
        val b = button ?: FloatButton(this).also { button = it }
        b.split = split
        b.invalidate()
        if (buttonShown) return
        val size = (72 * resources.displayMetrics.density).toInt()
        val sc = screen()
        val margin = (18 * resources.displayMetrics.density).toInt()
        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = app.prefs.floatX.takeIf { it in 0..(sc.widthPixels - size) } ?: margin
            y = app.prefs.floatY.takeIf { it in 0..(sc.heightPixels - size) } ?: (sc.heightPixels - size - margin * 3)
        }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var dragging = false
        b.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; dragging = false; b.down = true; b.invalidate() }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (!dragging && (abs(dx) > 14 || abs(dy) > 14)) dragging = true
                    if (dragging) {
                        val s = screen()
                        lp.x = (startX + dx.toInt()).coerceIn(0, s.widthPixels - size)
                        lp.y = (startY + dy.toInt()).coerceIn(0, s.heightPixels - size)
                        runCatching { wm().updateViewLayout(v, lp) }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    b.down = false; b.invalidate()
                    if (dragging) { app.prefs.floatX = lp.x; app.prefs.floatY = lp.y }
                    else if (e.actionMasked == MotionEvent.ACTION_UP) onButton()
                }
            }
            true
        }
        b.contentDescription = "切換全畫面或分割畫面"
        if (runCatching { wm().addView(b, lp) }.isSuccess) buttonShown = true
    }

    private fun hideButton() {
        val b = button ?: return
        if (buttonShown) runCatching { wm().removeView(b) }
        buttonShown = false
    }

    /** 按浮動按鈕：分割中 → 導航全畫面；全畫面 → 分割 */
    private fun onButton() {
        if (isSplit()) {
            app.splitSession.value = true
            toggleSplit()
        } else {
            app.enterSplitFromNav()
        }
        scheduleCheck()
    }
}

/** 金色圓形按鈕；split＝true 顯示「全畫面」圖示，false 顯示「分割」圖示 */
class FloatButton(ctx: Context) : View(ctx) {
    var split = false
    var down = false
    private val d = ctx.resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66F2C86A }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A1405.toInt(); style = Paint.Style.STROKE; strokeWidth = 3.4f * ctx.resources.displayMetrics.density; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2; val cy = h / 2; val r = minOf(w, h) / 2
        c.drawCircle(cx, cy, r, glow)
        fill.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(0xFFFFE8AD.toInt(), 0xFFE8B65C.toInt(), 0xFFC78C2E.toInt()), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r - 4 * d, fill)
        if (down) c.drawCircle(cx, cy, r - 4 * d, Paint().apply { color = 0x33000000 })
        val s = 12 * d
        if (split) {
            // 全畫面圖示：四個角
            val k = 5 * d
            c.drawLine(cx - s, cy - s + k, cx - s, cy - s, ink); c.drawLine(cx - s, cy - s, cx - s + k, cy - s, ink)
            c.drawLine(cx + s - k, cy - s, cx + s, cy - s, ink); c.drawLine(cx + s, cy - s, cx + s, cy - s + k, ink)
            c.drawLine(cx - s, cy + s - k, cx - s, cy + s, ink); c.drawLine(cx - s, cy + s, cx - s + k, cy + s, ink)
            c.drawLine(cx + s - k, cy + s, cx + s, cy + s, ink); c.drawLine(cx + s, cy + s, cx + s, cy + s - k, ink)
        } else {
            // 分割圖示：方框中間一條線
            c.drawRoundRect(RectF(cx - s, cy - s * 0.8f, cx + s, cy + s * 0.8f), 3 * d, 3 * d, ink)
            c.drawLine(cx, cy - s * 0.8f, cx, cy + s * 0.8f, ink)
        }
    }
}
