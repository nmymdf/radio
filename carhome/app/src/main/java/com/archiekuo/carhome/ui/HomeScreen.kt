package com.archiekuo.carhome.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archiekuo.carhome.CarApp
import com.archiekuo.carhome.media.RadioRemote
import kotlin.random.Random

private sealed interface Overlay {
    data object None : Overlay
    data object Drawer : Overlay
    data object Settings : Overlay
    data class PickTile(val slot: Int) : Overlay
    data object PickShield : Overlay
    data object PickFm : Overlay
}

@Composable
fun HomeScreen(app: CarApp, web: WebView, homeTick: Int, onAskPermissions: () -> Unit, overlayOk: () -> Boolean) {
    val ctx = LocalContext.current
    val p = app.prefs
    val now = rememberNow()
    val online by app.net.online.collectAsStateWithLifecycle()
    val fix by app.location.fix.collectAsStateWithLifecycle()
    val heading by app.location.heading.collectAsStateWithLifecycle()
    val road by app.location.road.collectAsStateWithLifecycle()
    val roadStale by app.location.roadStale.collectAsStateWithLifecycle()
    val district by app.location.district.collectAsStateWithLifecycle()
    val driving by app.location.driving.collectAsStateWithLifecycle()
    val weather by app.weather.weather.collectAsStateWithLifecycle()
    val radio by app.radio.state.collectAsStateWithLifecycle()
    val local by app.local.state.collectAsStateWithLifecycle()
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    val passenger by app.passengerMode.collectAsStateWithLifecycle()
    val mode by p.mode.flow.collectAsStateWithLifecycle()
    val autoFm by p.autoFm.flow.collectAsStateWithLifecycle()
    val ytRestrict by p.ytRestrict.flow.collectAsStateWithLifecycle()
    val vizOn by p.viz.flow.collectAsStateWithLifecycle()
    val vizTempo by p.vizTempo.flow.collectAsStateWithLifecycle()
    val autoRadio by p.autoRadio.flow.collectAsStateWithLifecycle()
    val autoShield by p.autoShield.flow.collectAsStateWithLifecycle()
    val hideStatus by p.hideStatus.flow.collectAsStateWithLifecycle()
    val black by p.black.flow.collectAsStateWithLifecycle()
    Look.black = black
    val shieldPkg by p.shieldPkg.flow.collectAsStateWithLifecycle()
    val fmPkg by p.fmPkg.flow.collectAsStateWithLifecycle()
    val tilePkgs = (0 until 7).map { i -> p.tile(i).flow.collectAsStateWithLifecycle().value }

    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var ytFull by remember { mutableStateOf(false) }
    var askPassenger by remember { mutableStateOf(false) }
    var resetTiles by remember { mutableStateOf(false) }

    // 按車機的「首頁」鍵：關掉所有面板
    LaunchedEffect(homeTick) { if (homeTick > 0) { overlay = Overlay.None; ytFull = false } }
    // 桌面本身不能被「返回」關掉
    BackHandler(enabled = true) {
        when {
            overlay != Overlay.None -> overlay = Overlay.None
            ytFull -> ytFull = false
            web.canGoBack() -> web.goBack()
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

    // 方塊預設要開哪個 App
    fun defaultPkg(role: Role): String? = when (role) {
        Role.SHIELD -> app.shieldPackage()
        Role.YOUTUBE -> app.apps.findByPackages("com.google.android.youtube", "com.google.android.apps.youtube.music")?.pkg ?: app.apps.findByLabel("YouTube")?.pkg
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
        if (custom.isNotBlank() && app.apps.isInstalled(custom)) { app.apps.launch(custom); return }
        when (r) {
            Role.SETTINGS -> overlay = Overlay.Settings
            Role.APPS -> overlay = Overlay.Drawer
            Role.OEM -> if (!app.apps.launchOtherHome()) runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            Role.NAV -> overlay = Overlay.PickTile(i) // 第一次讓你選導航（例如高德）
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

        Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 46.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TopBar(now, online, road, roadStale, fix?.speedKmh)
            Box(Modifier.fillMaxWidth().height(470.dp)) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.width(360.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ClockCompass(now, heading)
                        WeatherCard(weather, district ?: (if (fix == null) "新店" else null), online)
                    }
                    PlayerCard(
                        mode = mode, radio = radio, local = local, fmLabel = fmApp()?.label, autoFm = autoFm,
                        onMode = { m ->
                            app.switchMode(m)
                            if (m == "fm") { val a = fmApp(); if (a != null) app.apps.launch(a.pkg) else overlay = Overlay.PickFm }
                        },
                        onRadioToggle = app.radio::toggle, onRadioPrev = app.radio::previous, onRadioNext = app.radio::next,
                        onOpenRadioApp = { app.apps.launch(RadioRemote.PKG) },
                        onLocalToggle = app.local::toggle, onLocalPrev = app.local::previous, onLocalNext = app.local::next,
                        onShuffle = app.local::toggleShuffle, onRescan = { app.local.rescan() },
                        onOpenFm = { fmApp()?.let { app.apps.launch(it.pkg) } }, onPickFm = { overlay = Overlay.PickFm },
                    )
                    Column(Modifier.width(400.dp)) {
                        // 神盾懸浮視窗保留位置
                        Row(
                            Modifier.fillMaxWidth().height(110.dp).drawBehind {
                                drawRoundRect(C.Gold.copy(alpha = 0.55f), cornerRadius = CornerRadius(20.dp.toPx()),
                                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()))))
                            },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Rounded.Shield, null, tint = C.Gold, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("神盾懸浮視窗", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = C.Gold)
                                Text(if (autoShield) "發動後自動開啟，保留這個位置" else "保留這個位置", fontSize = 16.sp, color = C.Sub)
                            }
                        }
                    }
                }
                // YouTube（放大時蓋滿整排，同一個網頁不會重新載入）
                YouTubePanel(
                    web = web,
                    modifier = if (ytFull) Modifier.fillMaxSize() else Modifier.align(Alignment.BottomEnd).width(400.dp).height(346.dp),
                    driving = driving, restricted = ytRestrict, passenger = passenger, full = ytFull,
                    onPassenger = { askPassenger = true }, onFull = { ytFull = !ytFull },
                    onOpenApp = { defaultPkg(Role.YOUTUBE)?.let { app.apps.launch(it) } },
                )
            }
            Dock(tiles, ::tap) { i -> overlay = Overlay.PickTile(i) }
        }

        when (val o = overlay) {
            Overlay.Drawer -> AppDrawer(app.apps, apps, { a -> overlay = Overlay.None; app.apps.launch(a.pkg) }) { overlay = Overlay.None }
            is Overlay.PickTile -> AppPicker("選擇「${roles[o.slot].label}」要打開的 App", app.apps, apps, allowReset = true, onPick = { a ->
                p.tile(o.slot).set(a?.pkg ?: "")
                overlay = Overlay.None
                if (a != null && roles[o.slot] == Role.NAV) app.apps.launch(a.pkg)
            }) { overlay = Overlay.None }
            Overlay.PickShield -> AppPicker("選擇神盾 App", app.apps, apps, allowReset = true, onPick = { a -> p.shieldPkg.set(a?.pkg ?: ""); overlay = Overlay.Settings }) { overlay = Overlay.Settings }
            Overlay.PickFm -> AppPicker("選擇 FM 收音機 App", app.apps, apps, allowReset = true, onPick = { a -> p.fmPkg.set(a?.pkg ?: ""); overlay = if (mode == "fm") Overlay.None else Overlay.Settings }) { overlay = Overlay.None }
            Overlay.Settings -> SettingsPanel(
                SettingsModel(
                    autoRadio, autoShield, shieldApp()?.label ?: (if (shieldPkg.isBlank()) "自動尋找（找不到請點這裡選）" else "未安裝"),
                    autoFm, fmApp()?.label ?: (if (fmPkg.isBlank()) "自動尋找（找不到請點這裡選）" else "未安裝"),
                    ytRestrict, vizOn, vizTempo, hideStatus, overlayOk(), app.viz.hasRealAudio, black,
                ),
                onAutoRadio = p.autoRadio::set, onAutoShield = p.autoShield::set, onPickShield = { overlay = Overlay.PickShield },
                onAutoFm = p.autoFm::set, onPickFm = { overlay = Overlay.PickFm }, onYt = p.ytRestrict::set,
                onViz = { v -> p.viz.set(v); if (v) app.viz.start() else app.viz.stop() }, onVizTempo = p.vizTempo::set,
                onHideStatus = p.hideStatus::set, onBlack = p.black::set,
                onOverlay = { runCatching { ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                onPermissions = onAskPermissions,
                onDefaultHome = { runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { runCatching { ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } },
                onResetTiles = { resetTiles = true },
            ) { overlay = Overlay.None }
            Overlay.None -> {}
        }

        if (askPassenger) ConfirmBox("副駕駛觀看", "確定由副駕駛觀看影片嗎？\n駕駛請專心開車。這一趟有效，下次發動車子會恢復。", "由副駕駛觀看",
            onOk = { app.passengerMode.value = true; askPassenger = false }, onCancel = { askPassenger = false })
        if (resetTiles) ConfirmBox("恢復預設", "確定把下方七個方塊全部恢復成預設嗎？", "恢復",
            onOk = { (0 until 7).forEach { p.tile(it).set("") }; resetTiles = false }, onCancel = { resetTiles = false })
    }
}
