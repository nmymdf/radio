package com.archiekuo.carhome.split

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.archiekuo.carhome.CarApp
import com.archiekuo.carhome.ui.CarTheme
import com.archiekuo.carhome.ui.CompactDesk

/** 分割畫面時，放在導航旁邊的精簡桌面 */
class SplitDeskActivity : ComponentActivity() {
    private val app get() = application as CarApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            CarTheme {
                // 高度固定照 720 設計，寬度跟著分割比例（1/3、1/2、2/3）
                val cfg = LocalConfiguration.current
                val scale = (cfg.screenHeightDp / 720f).coerceAtLeast(0.3f)
                val designW = (cfg.screenWidthDp / scale).coerceAtLeast(300f)
                val d = LocalDensity.current
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalDensity provides Density(d.density * scale, 1f)) {
                        Box(Modifier.requiredSize(designW.dp, 720.dp)) {
                            CompactDesk(app, designW, onClose = { finish() })
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 沒有在分割畫面裡（例如車機不支援）就不要佔滿整個螢幕，回到完整桌面
        if (android.os.Build.VERSION.SDK_INT >= 24 && !isInMultiWindowMode) {
            window.decorView.postDelayed({
                if (!isFinishing && !isInMultiWindowMode) { finish(); app.bringHome() }
            }, 1200)
        }
        if (app.prefs.hideStatus.value) {
            val c = WindowInsetsControllerCompat(window, window.decorView)
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    /** 離開分割畫面（變成全畫面）時，交給完整桌面 */
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        if (!isInMultiWindowMode) finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        @Suppress("DEPRECATION") super.onMultiWindowModeChanged(isInMultiWindowMode)
        if (!isInMultiWindowMode) finish()
    }
}
