package com.archiekuo.radio

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.archiekuo.radio.navigation.AppRoot
import com.archiekuo.radio.ui.car.CarRoot
import com.archiekuo.radio.ui.theme.RadioTheme

/** 螢幕本來就是橫的（車機、電視）→ 用車機版 */
fun isNaturalLandscape(a: Activity): Boolean {
    @Suppress("DEPRECATION")
    val rot = (a.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
    val land = a.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    return if (rot == Surface.ROTATION_0 || rot == Surface.ROTATION_180) land else !land
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as RadioApp
        val car = when (app.store.screenMode.value) {
            "car" -> true
            "phone" -> false
            else -> isNaturalLandscape(this)
        }
        requestedOrientation = if (car) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (car) {
            // 車機版：藏起系統狀態列，畫面更乾淨（從上方往下滑可以叫出來）
            val c = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
            c.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
        app.player.connect()
        if (savedInstanceState == null && app.store.autoPlay.value) app.player.autoResume()
        setContent {
            RadioTheme {
                if (!car) AppRoot()
                else {
                    // 車機版照 1280×720 設計，依螢幕等比縮放
                    val cfg = LocalConfiguration.current
                    val w = maxOf(cfg.screenWidthDp, cfg.screenHeightDp)
                    val h = minOf(cfg.screenWidthDp, cfg.screenHeightDp)
                    val scale = minOf(w / 1280f, h / 720f)
                    val d = LocalDensity.current
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CompositionLocalProvider(LocalDensity provides Density(d.density * scale, 1f)) {
                            Box(Modifier.requiredSize(1280.dp, 720.dp)) { CarRoot() }
                        }
                    }
                }
            }
        }
    }
}
