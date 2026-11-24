package com.archiekuo.carhome

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import com.archiekuo.carhome.data.AppsRepo
import com.archiekuo.carhome.data.Prefs
import com.archiekuo.carhome.location.LocationRepo
import com.archiekuo.carhome.location.WeatherRepo
import com.archiekuo.carhome.media.LocalMusicRemote
import com.archiekuo.carhome.media.RadioRemote
import com.archiekuo.carhome.media.VisualizerEngine
import com.archiekuo.carhome.net.NetworkMonitor
import com.archiekuo.carhome.nav.NavInfo
import com.archiekuo.carhome.split.SplitDeskActivity
import com.archiekuo.carhome.split.SplitService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class CarApp : Application() {
    lateinit var prefs: Prefs
    lateinit var apps: AppsRepo
    lateinit var net: NetworkMonitor
    lateinit var location: LocationRepo
    lateinit var weather: WeatherRepo
    lateinit var radio: RadioRemote
    lateinit var local: LocalMusicRemote
    lateinit var nav: NavInfo
    val viz = VisualizerEngine()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** 畫面上方的大字提示（3 秒後消失） */
    val message = MutableStateFlow<String?>(null)
    private var messageJob: Job? = null
    fun say(text: String) {
        message.value = text
        messageJob?.cancel()
        messageJob = scope.launch { delay(3500); message.value = null }
    }

    /** 副駕駛觀看：這一趟有效，下次發動恢復 */
    val passengerMode = MutableStateFlow(false)
    /** 沒網路時自動切到 FM 的狀態 */
    private var autoSwitchedToFm = false
    private var offlineJob: Job? = null
    private var screenOffAt = 0L
    private var ignitionDone = false
    /** 剛發動時手機熱點還在連線，這段時間不要急著切 FM */
    private var ignitionAt = SystemClock.elapsedRealtime()

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        apps = AppsRepo(this)
        net = NetworkMonitor(this)
        location = LocationRepo(this) { net.online.value }
        weather = WeatherRepo()
        radio = RadioRemote(this)
        local = LocalMusicRemote(this, prefs)
        nav = NavInfo(this, scope)
        scope.launch { apps.refresh() }
        watchNetwork()
        // 車機休眠／喚醒（很多車機熄火是休眠，不是關機）
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                when (i.action) {
                    Intent.ACTION_SCREEN_OFF -> screenOffAt = SystemClock.elapsedRealtime()
                    Intent.ACTION_SCREEN_ON -> if (screenOffAt > 0 && SystemClock.elapsedRealtime() - screenOffAt > 60_000) onIgnition()
                }
            }
        }, IntentFilter().apply { addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF) })
    }

    /** 開機（程式第一次啟動）時呼叫一次 */
    fun onFirstLaunch() {
        if (ignitionDone) return
        ignitionDone = true
        onIgnition()
    }

    /** 發動車子：自動播收音機、自動開神盾 */
    fun onIgnition() {
        passengerMode.value = false
        ignitionAt = SystemClock.elapsedRealtime()
        scope.launch {
            apps.refresh()
            if (prefs.autoShield.value) {
                val shield = shieldPackage()
                if (shield != null) {
                    delay(1500)
                    apps.launch(shield)
                    // 神盾打開後回到桌面，讓神盾變成懸浮視窗
                    delay(5000)
                    bringHome()
                }
            }
            if (prefs.autoRadio.value && prefs.mode.value == "radio") {
                // 等手機熱點連上（最多 60 秒）
                val ok = withTimeoutOrNull(60_000) { net.online.first { it } }
                if (ok == true) radio.resumeLast()
            } else if (prefs.autoRadio.value && prefs.mode.value == "local") {
                local.play()
            }
        }
    }

    fun shieldPackage(): String? =
        prefs.shieldPkg.value.takeIf { it.isNotBlank() && apps.isInstalled(it) } ?: apps.findByLabel("神盾")?.pkg

    fun fmPackage(): String? =
        prefs.fmPkg.value.takeIf { it.isNotBlank() && apps.isInstalled(it) }
            ?: apps.findByLabel("收音機", "收音机", "Radio", "FM")?.pkg?.takeIf { it != RadioRemote.PKG }

    // ---- 高德導航 ----
    private val amapAuto = listOf("com.autonavi.amapauto", "com.autonavi.amapautolite", "com.autonavi.amapauto.gongban")
    fun navPackage(): String? =
        prefs.tile(2).value.takeIf { it.isNotBlank() && apps.isInstalled(it) }
            ?: apps.findByPackages(*(amapAuto + listOf("com.autonavi.minimap", "com.google.android.apps.maps")).toTypedArray())?.pkg
            ?: apps.findByLabel("高德", "导航", "導航", "地圖", "地图")?.pkg

    private fun isAmapAuto(pkg: String) = pkg in amapAuto || (pkg.startsWith("com.autonavi") && pkg.contains("auto"))

    /** 高德車機版的公開接收方式 */
    private fun amapBroadcast(pkg: String, type: Int, fill: Intent.() -> Unit) {
        val i = Intent("AUTONAVI_STANDARD_BROADCAST_RECV").setPackage(pkg)
            .putExtra("KEY_TYPE", type).putExtra("SOURCE_APP", "CarHome")
        i.fill()
        sendBroadcast(i)
    }

    /** 把關鍵字交給高德搜尋 */
    fun navSearch(keyword: String) {
        val pkg = navPackage() ?: run { say("找不到導航 App，請長按「導航」方塊選擇"); return }
        when {
            isAmapAuto(pkg) -> {
                apps.launch(pkg)
                scope.launch {
                    delay(1800)
                    amapBroadcast(pkg, 10036) { putExtra("KEYWORDS", keyword) }
                }
                say("已交給高德搜尋：$keyword")
            }
            pkg == "com.autonavi.minimap" -> {
                val uri = android.net.Uri.parse("androidamap://poi?sourceApplication=CarHome&keywords=" + android.net.Uri.encode(keyword) + "&dev=0")
                val ok = runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
                say(if (ok) "已交給高德搜尋：$keyword" else "高德沒有回應，請直接在高德裡搜尋")
            }
            pkg == "com.google.android.apps.maps" -> {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(keyword))).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                say("已交給地圖搜尋：$keyword")
            }
            else -> { apps.launch(pkg); say("這個導航 App 不接受語音，已幫你打開") }
        }
    }

    /** 回家（0）或去公司（1） */
    fun navHome(company: Boolean) {
        val pkg = navPackage() ?: run { say("找不到導航 App，請長按「導航」方塊選擇"); return }
        val name = if (company) "公司" else "家"
        if (isAmapAuto(pkg)) {
            apps.launch(pkg)
            scope.launch {
                delay(1800)
                amapBroadcast(pkg, 10040) { putExtra("DEST", if (company) 1 else 0); putExtra("EXTRA_DEST", if (company) 1 else 0); putExtra("IS_START_NAVI", 0) }
            }
            say("請高德導航到$name")
        } else {
            apps.launch(pkg)
            say("已打開導航，請在裡面按「回$name」")
        }
    }

    // ---- 分割畫面導航 ----
    /** 使用者正在用「導航＋桌面」分割畫面（按首頁鍵會回到分割） */
    val splitSession = MutableStateFlow(false)
    /** 要求桌面打開全畫面 YouTube（從分割畫面的 YouTube 方塊） */
    val ytRequest = MutableStateFlow(0)
    private var splitJob: Job? = null
    /** 最後一次看到導航畫面的時間（太久沒看到就不自動回分割） */
    @Volatile var navSeenAt = 0L

    fun openAccessibilitySettings() {
        runCatching { startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** 檢查能不能用分割畫面；不能的話說明原因 */
    private fun splitReady(): SplitService? {
        if (android.os.Build.VERSION.SDK_INT < 24) { say("這台車機的 Android 版本太舊，不支援分割畫面"); return null }
        val svc = SplitService.instance
        if (svc == null) {
            say("請在「協助工具」裡打開「車機桌面」")
            openAccessibilitySettings()
            return null
        }
        return svc
    }

    fun testOpenSplitDesk() = openSplitDesk()

    private fun openSplitDesk() {
        android.util.Log.i("CarHome", "openSplitDesk split=" + SplitService.instance?.isSplit())
        // 分割畫面時，新開的 App 會自動放進空的那一半
        val i = Intent(this, SplitDeskActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(i) }
    }

    /** 從桌面開始：打開導航 → 分割 → 另一邊放桌面 */
    fun startSplit() {
        val svc = splitReady() ?: return
        val pkg = navPackage() ?: run { say("找不到導航 App，請長按「導航」方塊選擇"); return }
        splitJob?.cancel()
        splitJob = scope.launch {
            if (svc.isSplit()) { splitSession.value = true; openSplitDesk(); return@launch }
            apps.launch(pkg)
            // 等導航畫面出來（最多 8 秒）
            var waited = 0
            while (waited < 8000 && !svc.isVisible(pkg)) { delay(250); waited += 250 }
            delay(600)
            enterSplitNow(svc)
        }
    }

    /** 導航正在全畫面時（浮動按鈕）：分割，另一邊放桌面 */
    fun enterSplitFromNav() {
        val svc = splitReady() ?: return
        splitJob?.cancel()
        splitJob = scope.launch { enterSplitNow(svc) }
    }

    private suspend fun enterSplitNow(svc: SplitService) {
        android.util.Log.i("CarHome", "enterSplit toggle=" + svc.toggleSplit())
        var waited = 0
        while (waited < 2500 && !svc.isSplit()) { delay(200); waited += 200 }
        if (svc.isSplit()) {
            splitSession.value = true
            delay(400)
            openSplitDesk()
        } else {
            splitSession.value = false
            say("這個導航 App 不支援分割畫面")
        }
    }

    /** 分割中 → 導航變全畫面（之後按首頁鍵會回到分割） */
    fun navFull() {
        val svc = SplitService.instance ?: return
        if (svc.isSplit()) svc.toggleSplit()
    }

    /** 結束分割，回到完整桌面 */
    fun endSplit() {
        splitSession.value = false
        splitJob?.cancel()
        val svc = SplitService.instance
        scope.launch {
            if (svc != null && svc.isSplit()) {
                svc.toggleSplit()
                var waited = 0
                while (waited < 2000 && svc.isSplit()) { delay(150); waited += 150 }
                delay(300)
            }
            bringHome()
        }
    }

    /** 按首頁鍵：如果正在用分割畫面導航，就回到分割 */
    fun restoreSplit() {
        if (!splitSession.value) return
        if (SystemClock.elapsedRealtime() - navSeenAt > 30 * 60_000L) { splitSession.value = false; return }
        val svc = SplitService.instance ?: return
        val pkg = navPackage() ?: return
        splitJob?.cancel()
        splitJob = scope.launch {
            delay(300)
            if (svc.isSplit()) { openSplitDesk(); return@launch }
            apps.launch(pkg)
            var waited = 0
            while (waited < 5000 && !svc.isVisible(pkg)) { delay(250); waited += 250 }
            delay(500)
            enterSplitNow(svc)
        }
    }

    /** 從分割畫面打開桌面的全畫面 YouTube */
    fun openYouTubeOnDesk() {
        ytRequest.value++
        bringHome()
    }

    fun bringHome() {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        runCatching { startActivity(i) }
    }

    /** 網路收音機模式下，沒網路超過 10 秒自動切 FM；網路恢復再切回來 */
    private fun watchNetwork() {
        scope.launch {
            net.online.collect { online ->
                offlineJob?.cancel()
                if (!online) {
                    offlineJob = scope.launch {
                        delay(10_000)
                        val wait = 45_000 - (SystemClock.elapsedRealtime() - ignitionAt)
                        if (wait > 0) delay(wait)
                        if (!net.online.value && prefs.autoFm.value && prefs.mode.value == "radio") {
                            val fm = fmPackage() ?: return@launch
                            radio.pause()
                            autoSwitchedToFm = true
                            prefs.mode.set("fm")
                            apps.launch(fm)
                        }
                    }
                } else if (autoSwitchedToFm) {
                    autoSwitchedToFm = false
                    prefs.mode.set("radio")
                    bringHome()
                    delay(1500)
                    radio.resumeLast()
                }
            }
        }
    }

    /** 從內部音樂清單點一首歌 */
    fun playLocalSong(id: String) {
        autoSwitchedToFm = false
        prefs.mode.set("local")
        radio.pause()
        local.playSong(id)
    }

    /** 切換播放模式 */
    fun switchMode(m: String) {
        autoSwitchedToFm = false
        prefs.mode.set(m)
        when (m) {
            "radio" -> { local.pause(); radio.resumeLast() }
            "local" -> { radio.pause(); local.play() }
            "fm" -> { radio.pause(); local.pause() }
        }
    }
}
