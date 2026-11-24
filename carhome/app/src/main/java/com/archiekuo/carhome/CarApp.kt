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
    val viz = VisualizerEngine()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
