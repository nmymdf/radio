package com.archiekuo.carhome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.archiekuo.carhome.ui.CarTheme
import com.archiekuo.carhome.ui.Look
import com.archiekuo.carhome.ui.HomeScreen
import com.archiekuo.carhome.ui.createYouTubeWebView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app get() = application as CarApp
    private var homeTick by mutableIntStateOf(0)
    private var web: WebView? = null

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onPermissionsResult() }

    private fun neededPermissions(): Array<String> {
        val list = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) { list += Manifest.permission.READ_MEDIA_AUDIO; list += Manifest.permission.POST_NOTIFICATIONS }
        else list += Manifest.permission.READ_EXTERNAL_STORAGE
        return list.toTypedArray()
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    fun askPermissions() { permLauncher.launch(neededPermissions()) }

    private var firstDone = false

    private fun onPermissionsResult() {
        // 第一次（權限問完後）才執行發動後的動作，避免神盾蓋住權限詢問
        if (!firstDone) { firstDone = true; app.onFirstLaunch() }
        if (granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION)) app.location.start()
        if (granted(Manifest.permission.RECORD_AUDIO) && app.prefs.viz.value) app.viz.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val w = createYouTubeWebView(this, app.prefs.ytUrl.value) { app.prefs.ytUrl.set(it) }
        web = w
        lifecycleScope.launch { app.prefs.hideStatus.flow.collectLatest { applyBars(it) } }

        if (neededPermissions().any { !granted(it) }) askPermissions() else onPermissionsResult()

        setContent {
            CarTheme {
                // 依螢幕大小等比縮放：照 1280×720 設計，任何尺寸的車機都能完整顯示
                val cfg = LocalConfiguration.current
                val scale = minOf(cfg.screenWidthDp / 1280f, cfg.screenHeightDp / 720f)
                val d = LocalDensity.current
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalDensity provides Density(d.density * scale, 1f)) {
                        Box(Modifier.requiredSize(1280.dp, 720.dp)) {
                            HomeScreen(app, w, homeTick, ::askPermissions) { Settings.canDrawOverlays(this@MainActivity) }
                        }
                    }
                }
            }
        }
    }

    private fun applyBars(hide: Boolean) {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        if (hide) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.statusBars())
        } else c.show(WindowInsetsCompat.Type.statusBars())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeTick++
    }

    override fun onResume() {
        super.onResume()
        app.radio.connect()
        lifecycleScope.launch { app.apps.refresh() }
        if (firstDone) onPermissionsResult()
        web?.onResume()
        applyBars(app.prefs.hideStatus.value)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyBars(app.prefs.hideStatus.value)
    }

    override fun onDestroy() {
        web?.destroy()
        super.onDestroy()
    }
}
