package com.archiekuo.carhome.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archiekuo.carhome.CarApp
import com.archiekuo.carhome.data.SystemInfo
import com.archiekuo.carhome.media.RadioRemote
import kotlin.random.Random

private sealed interface Overlay {
    data object None : Overlay
    data object Drawer : Overlay
    data object Settings : Overlay
    data class PickTile(val slot: Int) : Overlay
    /** back：按返回要回到哪裡（從設定進來回設定，長按圓圈進來回桌面） */
    data class PickShield(val back: Overlay = Settings) : Overlay
    data object PickFm : Overlay
    data object PickPhoto : Overlay
    data object NavMenu : Overlay
    data object Weather : Overlay
    data object Music : Overlay
    data object Background : Overlay
}

/** 版面位置（照 1280×720 設計） */
private object Layout {
    const val PAD_X = 20
    const val TOP = 16 + 44 + 14 // 上方留白＋上方列＋間距
    const val MID_H = 470
    const val SIDE_W = 400
    const val NAV_H = 124
}

@Composable
fun HomeScreen(
    app: CarApp, homeTick: Int, onAskPermissions: () -> Unit,
    onVoice: (String, (String) -> Unit) -> Unit, onPickPhoto: ((String) -> Unit) -> Unit,
    overlayOk: () -> Boolean,
) {
    val ctx = LocalContext.current
    val p = app.prefs
    val now = rememberNow()
    val online by app.net.online.collectAsStateWithLifecycle()
    val fix by app.location.fix.collectAsStateWithLifecycle()
    val heading by app.location.heading.collectAsStateWithLifecycle()
    val road by app.location.road.collectAsStateWithLifecycle()
    val roadStale by app.location.roadStale.collectAsStateWithLifecycle()
    val district by app.location.district.collectAsStateWithLifecycle()
    val weather by app.weather.weather.collectAsStateWithLifecycle()
    val radio by app.radio.state.collectAsStateWithLifecycle()
    val local by app.local.state.collectAsStateWithLifecycle()
    val library by app.local.library.collectAsStateWithLifecycle()
    val recent by app.local.recent.collectAsStateWithLifecycle()
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    val guide by app.nav.guide.collectAsStateWithLifecycle()
    val mode by p.mode.flow.collectAsStateWithLifecycle()
    val autoFm by p.autoFm.flow.collectAsStateWithLifecycle()
    val ytRestrict by p.ytRestrict.flow.collectAsStateWithLifecycle()
    val vizOn by p.viz.flow.collectAsStateWithLifecycle()
    val vizTempo by p.vizTempo.flow.collectAsStateWithLifecycle()
    val autoRadio by p.autoRadio.flow.collectAsStateWithLifecycle()
    val autoShield by p.autoShield.flow.collectAsStateWithLifecycle()
    val hideStatus by p.hideStatus.flow.collectAsStateWithLifecycle()
    val black by p.black.flow.collectAsStateWithLifecycle()
    val circleMode by p.circleMode.flow.collectAsStateWithLifecycle()
    val shieldLeft by p.shieldLeft.flow.collectAsStateWithLifecycle()
    val shieldPkg by p.shieldPkg.flow.collectAsStateWithLifecycle()
    val fmPkg by p.fmPkg.flow.collectAsStateWithLifecycle()
    val tilePkgs = (0 until 7).map { i -> p.tile(i).flow.collectAsStateWithLifecycle().value }

    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    val ytPhoto by p.ytPhoto.flow.collectAsStateWithLifecycle()
    val message by app.message.collectAsStateWithLifecycle()
    val ytWinMethod by p.ytWinMethod.flow.collectAsStateWithLifecycle()
    var resetTiles by remember { mutableStateOf(false) }

    // 按車機的「首頁」鍵：關掉所有面板、YouTube 縮回小畫面（繼續播）
    LaunchedEffect(homeTick) { if (homeTick > 0) overlay = Overlay.None }
    // 桌面本身不能被「返回」關掉
    BackHandler(enabled = true) {
        when {
            overlay == Overlay.Background -> overlay = Overlay.Settings
            overlay is Overlay.PickShield -> overlay = (overlay as Overlay.PickShield).back
            overlay != Overlay.None -> overlay = Overlay.None
        }
    }
    // 天氣：有 GPS 用 GPS，沒有就先用新店
    LaunchedEffect(fix?.lat?.times(100)?.toInt(), fix?.lon?.times(100)?.toInt(), online) {
        if (!online) return@LaunchedEffect
        val f = fix
        if (f != null) app.weather.refresh(f.lat, f.lon) else app.weather.refresh(24.9676, 121.5419)
    }

    fun shieldApp() = app.shieldPackage()?.let { app.apps.find(it) }
    fun fmApp() = app.fmPackage()?.let { app.apps.find(it) }
    fun launchShield() { val s = app.shieldPackage(); if (s != null) app.apps.launch(s) else { app.say("找不到神盾 App，請選擇"); overlay = Overlay.PickShield(Overlay.None) } }
    fun openNav() { val n = app.navPackage(); if (n != null) app.apps.launch(n) else overlay = Overlay.PickTile(2) }
    val navLabel = app.navPackage()?.let { app.apps.find(it)?.label } ?: "導航"

    // 方塊預設要開哪個 App
    fun defaultPkg(role: Role): String? = when (role) {
        Role.SHIELD -> app.shieldPackage()
        Role.YOUTUBE -> app.youtubePackage()
        Role.PHONE -> app.apps.findByLabel("藍牙電話", "蓝牙电话", "藍牙", "蓝牙", "Bluetooth", "電話", "电话", "BT")?.pkg
        else -> null
    }
    val roles = Role.entries
    val tiles = roles.mapIndexed { i, r ->
        val custom = tilePkgs[i].takeIf { it.isNotBlank() }?.let { app.apps.find(it) }
        when {
            custom != null && r == Role.NAV -> TileUi(r, custom.label, null)
            custom != null -> TileUi(r, custom.label, app.apps.icon(custom.pkg))
            else -> TileUi(r, r.label, null)
        }
    }
    fun tap(i: Int) {
        val r = roles[i]
        val custom = tilePkgs[i]
        // 導航方塊就算換了 App，也是先出現四個大按鈕
        if (r != Role.NAV && custom.isNotBlank() && app.apps.isInstalled(custom)) { app.apps.launch(custom); return }
        when (r) {
            Role.SETTINGS -> overlay = Overlay.Settings
            Role.APPS -> overlay = Overlay.Drawer
            Role.OEM -> if (!app.apps.launchOtherHome()) runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            Role.NAV -> overlay = Overlay.NavMenu
            else -> {
                val d = defaultPkg(r)
                if (d != null) app.apps.launch(d) else overlay = Overlay.PickTile(i)
            }
        }
    }

    val active = radio.playing || local.playing
    Box(
        Modifier.fillMaxSize().drawBehind {
            if (black) {
                drawRect(Color.Black)
                return@drawBehind
            }
            drawRect(Brush.verticalGradient(listOf(C.Navy2, C.Navy1, C.Navy0)))
            drawRect(Brush.radialGradient(listOf(C.Glow, Color.Transparent), center = Offset(size.width / 2f, 0f), radius = size.width * 0.7f))
            val r = Random(7)
            repeat(80) { k ->
                drawCircle(Color.White.copy(alpha = if (k % 3 == 0) 0.7f else 0.35f), (0.6f + r.nextFloat() * 1.3f) * density, Offset(r.nextFloat() * size.width, r.nextFloat() * size.height * 0.5f))
            }
        },
    ) {
        if (vizOn) VisualizerLayer(app.viz, active, vizTempo, Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter))

        Column(Modifier.fillMaxSize().padding(start = Layout.PAD_X.dp, end = Layout.PAD_X.dp, top = 16.dp, bottom = 46.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TopBar(now, online, road, roadStale, fix?.speedKmh, navLabel = if (guide != null) navLabel else null, onNav = ::openNav)
            Row(Modifier.fillMaxWidth().height(Layout.MID_H.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                // 時鐘指南針＋天氣（導航中換成導航小卡）
                val clockSide: @Composable () -> Unit = {
                    Column(Modifier.width(360.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ClockCompass(now, heading, circleMode, 300, onTap = { p.circleMode.set((circleMode + 1) % 3) }, onLong = ::launchShield)
                        WeatherCard(weather, district ?: (if (fix == null) "新店" else null), online, onClick = { overlay = Overlay.Weather })
                    }
                }
                // 導航區（導航中＝導航小卡；沒在導航＝三個大按鈕）＋ YouTube
                val navSide: @Composable () -> Unit = {
                    Column(Modifier.width(Layout.SIDE_W.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        val g = guide
                        if (g != null) NavCard(g, Modifier.fillMaxWidth().height(Layout.NAV_H.dp), onClick = ::openNav)
                        else NavQuick(
                            Modifier.fillMaxWidth().height(Layout.NAV_H.dp),
                            onVoice = { onVoice("請說目的地") { q -> app.navSearch(q) } },
                            onHome = { app.navHome(false) }, onWork = { app.navHome(true) },
                        )
                        YouTubeCard(
                            ytPhoto, Modifier.fillMaxWidth().weight(1f),
                            onOpen = app::openYouTube,
                            onVoice = { onVoice("說出想看的影片") { q -> app.searchYouTube(q) } },
                            onChangePhoto = { overlay = Overlay.PickPhoto },
                            onSmall = app::openYouTubeWindow,
                        )
                    }
                }
                val player: @Composable () -> Unit = {
                    PlayerCard(
                        mode = mode, radio = radio, local = local, fmLabel = fmApp()?.label, autoFm = autoFm,
                        onMode = { m ->
                            app.switchMode(m)
                            if (m == "fm") { val a = fmApp(); if (a != null) app.apps.launch(a.pkg) else overlay = Overlay.PickFm }
                        },
                        onRadioToggle = app.radio::toggle, onRadioPrev = app.radio::previous, onRadioNext = app.radio::next,
                        onOpenRadioApp = { if (!app.apps.launch(RadioRemote.PKG)) app.say("請先安裝「網路收音機」App") },
                        onLocalToggle = app.local::toggle, onLocalPrev = app.local::previous, onLocalNext = app.local::next,
                        onShuffle = app.local::toggleShuffle, onRescan = { app.local.rescan() },
                        onOpenFm = { fmApp()?.let { app.apps.launch(it.pkg) } ?: run { overlay = Overlay.PickFm } }, onPickFm = { overlay = Overlay.PickFm },
                        onLocalPage = { if (!local.scanned && !local.scanning) app.local.rescan(); overlay = Overlay.Music },
                    )
                }
                if (shieldLeft) { navSide(); player(); clockSide() } else { clockSide(); player(); navSide() }
            }
            Dock(tiles, ::tap) { i -> overlay = Overlay.PickTile(i) }
        }

        when (val o = overlay) {
            Overlay.Drawer -> AppsPage(
                app.apps, apps, tiles.map { it.label },
                onLaunch = { a -> overlay = Overlay.None; app.apps.launch(a.pkg) },
                onAssign = { a, i -> p.tile(i).set(a.pkg); app.say("已放到下方第 ${i + 1} 個方塊") },
                onInfo = { a -> SystemInfo.openAppInfo(ctx, a.pkg) },
                onUninstall = { a -> app.uninstallOrInfo(a.pkg, a.system) },
                onVoice = { cb -> onVoice("說出 App 名稱", cb) },
            ) { overlay = Overlay.None }
            is Overlay.PickTile -> AppPicker("選擇「${roles[o.slot].label}」要打開的 App", app.apps, apps, allowReset = true, onPick = { a ->
                p.tile(o.slot).set(a?.pkg ?: "")
                overlay = Overlay.None
                if (a != null && roles[o.slot] == Role.NAV) app.apps.launch(a.pkg)
            }) { overlay = Overlay.None }
            is Overlay.PickShield -> AppPicker("選擇神盾 App", app.apps, apps, allowReset = true, onPick = { a -> p.shieldPkg.set(a?.pkg ?: ""); overlay = o.back }) { overlay = o.back }
            Overlay.PickFm -> AppPicker("選擇 FM 收音機 App", app.apps, apps, allowReset = true, onPick = { a -> p.fmPkg.set(a?.pkg ?: ""); overlay = if (mode == "fm") Overlay.None else Overlay.Settings }) { overlay = Overlay.None }
            Overlay.Settings -> {
                val mem = rememberMemory()
                SettingsPanel(
                    SettingsModel(
                        autoRadio, autoShield, shieldApp()?.label ?: (if (shieldPkg.isBlank()) "自動尋找（找不到請點這裡選）" else "未安裝"),
                        autoFm, fmApp()?.label ?: (if (fmPkg.isBlank()) "自動尋找（找不到請點這裡選）" else "未安裝"),
                        ytRestrict, vizOn, vizTempo, hideStatus, overlayOk(), app.viz.hasRealAudio, black,
                        shieldLeft = shieldLeft, memText = mem?.text ?: "",
                        ytWin = (if (ytWinMethod in 1..3) "固定用方法 $ytWinMethod（${app.winMethodNames[ytWinMethod - 1]}）" else "每按一次換下一種（找方法中）") + " ・ " + app.windowInfo(),
                    ),
                    onYtWin = {
                        val next = (ytWinMethod + 1) % 4
                        p.ytWinMethod.set(next)
                        app.say(if (next == 0) "YouTube 小窗：每按一次換下一種" else "YouTube 小窗：固定用方法 $next")
                    },
                    onAutoRadio = p.autoRadio::set, onAutoShield = p.autoShield::set, onPickShield = { overlay = Overlay.PickShield(Overlay.Settings) },
                    onAutoFm = p.autoFm::set, onPickFm = { overlay = Overlay.PickFm }, onYt = p.ytRestrict::set,
                    onViz = { v -> p.viz.set(v); if (v) app.viz.start() else app.viz.stop() }, onVizTempo = p.vizTempo::set,
                    onHideStatus = p.hideStatus::set, onBlack = p.black::set,
                    onOverlay = { runCatching { ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                    onPermissions = onAskPermissions,
                    onDefaultHome = { runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { runCatching { ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } },
                    onResetTiles = { resetTiles = true },
                    onShieldLeft = p.shieldLeft::set, onMemory = { overlay = Overlay.Background },
                ) { overlay = Overlay.None }
            }
            Overlay.PickPhoto -> PhotoPicker(ytPhoto, onPick = { k -> p.ytPhoto.set(k); overlay = Overlay.None },
                onUsb = { onPickPhoto { path -> p.ytPhoto.set(path); overlay = Overlay.None } }) { overlay = Overlay.None }
            Overlay.NavMenu -> {
                val navPkg = app.navPackage()
                NavMenu(
                    navPkg?.let { app.apps.find(it)?.label },
                    onVoice = { onVoice("請說目的地") { q -> overlay = Overlay.None; app.navSearch(q) } },
                    onHome = { overlay = Overlay.None; app.navHome(false) },
                    onWork = { overlay = Overlay.None; app.navHome(true) },
                    onOpen = { if (navPkg != null) { overlay = Overlay.None; app.apps.launch(navPkg) } else overlay = Overlay.PickTile(2) },
                ) { overlay = Overlay.None }
            }
            Overlay.Weather -> {
                LaunchedEffect(Unit) {
                    // 沒有詳細資料（舊資料）就馬上重新抓
                    if (online && weather?.detail == null) { val f = fix; if (f != null) app.weather.refresh(f.lat, f.lon, true) else app.weather.refresh(24.9676, 121.5419, true) }
                }
                WeatherPage(weather, district ?: (if (fix == null) "新店" else null), online) { overlay = Overlay.None }
            }
            Overlay.Music -> MusicPage(
                local, library, recent,
                onPlay = { id -> app.playLocalSong(id) }, onShuffle = app.local::toggleShuffle, onRescan = { app.local.rescan() },
            ) { overlay = Overlay.None }
            Overlay.Background -> BackgroundPage(app.apps, setOfNotNull(app.shieldPackage(), app.navPackage(), app.fmPackage(), RadioRemote.PKG)) { overlay = Overlay.Settings }
            Overlay.None -> {}
        }
        MessageBanner(message, Modifier.align(Alignment.TopCenter).padding(top = 70.dp))

        if (resetTiles) ConfirmBox("恢復預設", "確定把下方七個方塊全部恢復成預設嗎？", "恢復",
            onOk = { (0 until 7).forEach { p.tile(it).set("") }; resetTiles = false }, onCancel = { resetTiles = false })
    }
}
