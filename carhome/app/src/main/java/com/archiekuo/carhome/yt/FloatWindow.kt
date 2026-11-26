package com.archiekuo.carhome.yt

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.archiekuo.carhome.CarApp

/**
 * 浮在所有 App 上面的 YouTube 小視窗（螢幕的 1/4：寬一半、高一半）。
 * 上面一條金色把手可以拖到任何位置，位置會記住。
 * 作者：ArchieKUO
 */
class FloatWindow(private val app: CarApp, private val ctrl: YtController) {
    private val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: LinearLayout? = null
    private var box: FrameLayout? = null
    private var lp: WindowManager.LayoutParams? = null

    private val gold = Color.parseColor("#F2C46D")
    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, app.resources.displayMetrics).toInt()

    private fun screen(): Point {
        val p = Point()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealSize(p)
        return p
    }

    /** 顯示小視窗，回傳放影片的位置 */
    fun show(): FrameLayout? {
        box?.let { return it }
        return runCatching { create() }.getOrElse { android.util.Log.w(YtController.TAG, "float failed", it); null }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun create(): FrameLayout {
        val s = screen()
        val w = s.x / 2
        val h = s.y / 2
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val margin = dp(16f)
        val px = app.prefs.floatX.value
        val py = app.prefs.floatY.value
        val params = WindowManager.LayoutParams(
            w, h, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // 預設右下角
            x = if (px >= 0) px.coerceIn(0, s.x - w) else s.x - w - margin
            y = if (py >= 0) py.coerceIn(0, s.y - h) else s.y - h - margin
        }

        val r = LinearLayout(app).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F0080D2A")); cornerRadius = dp(16f).toFloat(); setStroke(dp(2f), gold)
            }
            setPadding(dp(3f), dp(3f), dp(3f), dp(3f))
            clipToOutline = true
        }
        // 金色把手：拖曳＋三個大按鈕
        val bar = LinearLayout(app).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), 0, dp(4f), 0)
        }
        val grip = TextView(app).apply {
            text = "⠿  YouTube"
            setTextColor(gold); textSize = 19f; typeface = Typeface.DEFAULT_BOLD
            contentDescription = "拖曳移動"
        }
        bar.addView(grip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { gravity = Gravity.CENTER_VERTICAL })
        grip.gravity = Gravity.CENTER_VERTICAL
        fun btn(label: String, onClick: () -> Unit) = TextView(app).apply {
            text = label
            setTextColor(Color.WHITE); textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Color.parseColor("#33F2C46D")); cornerRadius = dp(12f).toFloat(); setStroke(dp(1f), gold) }
            setPadding(dp(12f), 0, dp(12f), 0)
            setOnClickListener { onClick() }
        }
        val lpBtn = { LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40f)).apply { marginEnd = dp(6f) } }
        bar.addView(btn("回桌面") { ctrl.setMode(YtController.Mode.CARD); app.bringHome() }, lpBtn())
        bar.addView(btn("放大") { ctrl.setMode(YtController.Mode.FULL); app.bringHome() }, lpBtn())
        bar.addView(btn("✕ 關閉") { ctrl.close() }, lpBtn())
        r.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50f)))

        val b = FrameLayout(app).apply { setBackgroundColor(Color.BLACK) }
        r.addView(b, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // 拖曳把手移動視窗
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        val drag = View.OnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = params.x; startY = params.y; true }
                MotionEvent.ACTION_MOVE -> {
                    val sz = screen()
                    params.x = (startX + (e.rawX - downX).toInt()).coerceIn(0, (sz.x - params.width).coerceAtLeast(0))
                    params.y = (startY + (e.rawY - downY).toInt()).coerceIn(0, (sz.y - params.height).coerceAtLeast(0))
                    runCatching { wm.updateViewLayout(r, params) }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    app.prefs.floatX.set(params.x); app.prefs.floatY.set(params.y); true
                }
                else -> false
            }
        }
        grip.setOnTouchListener(drag)

        wm.addView(r, params)
        root = r; box = b; lp = params
        return b
    }

    fun hide() {
        val r = root ?: return
        // 影片先拿出來，別跟著視窗一起被丟掉
        box?.removeAllViews()
        runCatching { wm.removeView(r) }
        root = null; box = null; lp = null
    }
}
