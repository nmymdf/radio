package com.archiekuo.carhome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import com.archiekuo.carhome.ui.CompactDesk
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
        val w = createYouTubeWebView(this) { app.prefs.ytUrl.set(it) }
        web = w
        lifecycleScope.launch { app.prefs.hideStatus.flow.collectLatest { applyBars(it) } }

        if (neededPermissions().any { !granted(it) }) askPermissions() else onPermissionsResult()
        handleTestIntent(intent)

        setContent {
            CarTheme {
                // 依螢幕大小等比縮放：照 1280×720 設計，任何尺寸的車機都能完整顯示
                val cfg = LocalConfiguration.current
                val d = LocalDensity.current
                val aspect = cfg.screenWidthDp.toFloat() / cfg.screenHeightDp.coerceAtLeast(1)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (aspect >= 1.5f) {
                        val scale = minOf(cfg.screenWidthDp / 1280f, cfg.screenHeightDp / 720f)
                        CompositionLocalProvider(LocalDensity provides Density(d.density * scale, 1f)) {
                            Box(Modifier.requiredSize(1280.dp, 720.dp)) {
                                HomeScreen(app, w, homeTick, ::askPermissions, ::startVoice, ::pickPhoto) { Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this@MainActivity) }
                            }
                        }
                    } else {
                        // 桌面本身被分割成窄的（有些車機會這樣）：改用精簡桌面
                        val scale = (cfg.screenHeightDp / 720f).coerceAtLeast(0.3f)
                        val designW = (cfg.screenWidthDp / scale).coerceAtLeast(300f)
                        CompositionLocalProvider(LocalDensity provides Density(d.density * scale, 1f)) {
                            Box(Modifier.requiredSize(designW.dp, 720.dp)) { CompactDesk(app, designW) {} }
                        }
                    }
                }
            }
        }
    }

    // ---- 語音辨識（YouTube 語音搜尋、說目的地） ----
    private var voiceCallback: ((String) -> Unit)? = null
    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val text = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        val cb = voiceCallback
        voiceCallback = null
        if (!text.isNullOrBlank()) cb?.invoke(text) else app.say("沒有聽到，請再說一次")
    }

    fun startVoice(prompt: String, onResult: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-TW")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        val ok = packageManager.queryIntentActivities(intent, 0).isNotEmpty()
        if (!ok) { app.say("這台車機沒有語音辨識服務，請改用打字"); return }
        voiceCallback = onResult
        runCatching { voiceLauncher.launch(intent) }.onFailure { voiceCallback = null; app.say("語音辨識打不開，請改用打字") }
    }

    // ---- 從隨身碟選照片（複製一份到桌面裡，之後拔掉隨身碟也還在） ----
    private var photoCallback: ((String) -> Unit)? = null
    private val photoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri = r.data?.data ?: return@registerForActivityResult
        lifecycleScope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val f = java.io.File(filesDir, "yt_photo_${System.currentTimeMillis()}.jpg")
                    contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { input.copyTo(it) } }
                    filesDir.listFiles()?.filter { it.name.startsWith("yt_photo_") && it != f }?.forEach { it.delete() }
                    f.absolutePath
                }.getOrNull()
            }
            if (path != null) photoCallback?.invoke("file:$path") else app.say("照片讀取失敗")
        }
    }

    fun pickPhoto(onPicked: (String) -> Unit) {
        photoCallback = onPicked
        val i = Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
        runCatching { photoLauncher.launch(Intent.createChooser(i, "選一張照片")) }.onFailure { app.say("這台車機沒有選照片的功能") }
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
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeTick++
            // 用分割畫面導航時，按首頁鍵回到「導航＋桌面」
            app.restoreSplit()
        }
        handleTestIntent(intent)
    }

    /** 雲端模擬車機測試用：指定導航 App（模擬器上沒有高德） */
    private fun handleTestIntent(i: Intent?) {
        val pkg = i?.getStringExtra("test_nav_pkg") ?: return
        if (pkg.matches(Regex("[A-Za-z0-9_.]+"))) app.prefs.tile(2).set(pkg)
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
