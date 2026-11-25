package com.archiekuo.radio.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.archiekuo.radio.ui.components.Backdrop
import com.archiekuo.radio.ui.components.BottomBar
import com.archiekuo.radio.ui.components.MiniPlayer
import com.archiekuo.radio.ui.screens.CategoryScreen
import com.archiekuo.radio.ui.screens.FavoritesScreen
import com.archiekuo.radio.ui.screens.HistoryScreen
import com.archiekuo.radio.ui.screens.HomeScreen
import com.archiekuo.radio.ui.screens.HotScreen
import com.archiekuo.radio.ui.screens.MineScreen
import com.archiekuo.radio.ui.screens.PlayerScreen
import com.archiekuo.radio.ui.screens.SearchScreen
import com.archiekuo.radio.ui.screens.SettingsScreen
import com.archiekuo.radio.viewmodel.MainViewModel

object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val FAVORITES = "favorites"
    const val MINE = "mine"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val PLAYER = "player"
    const val HOT = "hot"
}

@Composable
fun AppRoot(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.HOME
    val ps by vm.playerState.collectAsStateWithLifecycle()

    // Android 13 以上：要求通知權限，通知列才看得到播放控制
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val tab = when {
        route.startsWith(Routes.CATEGORIES) -> 1
        route == Routes.FAVORITES -> 2
        route == Routes.MINE || route == Routes.HISTORY || route == Routes.SETTINGS -> 3
        else -> 0
    }
    val isPlayer = route == Routes.PLAYER

    fun goTab(r: String) {
        // 首頁：一律直接回到首頁最上面（之前會把分類畫面還原回來，看起來按不回去）
        if (r == Routes.HOME) {
            if (!nav.popBackStack(Routes.HOME, inclusive = false)) nav.navigate(Routes.HOME) { launchSingleTop = true }
            return
        }
        nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun goCategory(key: String) = nav.navigate("${Routes.CATEGORIES}?key=$key") {
        popUpTo(nav.graph.findStartDestination().id)
        launchSingleTop = true
    }
    fun open(r: String) = nav.navigate(r) { launchSingleTop = true }
    val openPlayer = { open(Routes.PLAYER) }
    val back: () -> Unit = { nav.popBackStack() }

    Backdrop {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .statusBarsPadding(),
            ) {
                NavHost(
                    navController = nav,
                    startDestination = Routes.HOME,
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(220)) },
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            vm,
                            onSearch = { open(Routes.SEARCH) },
                            onSettings = { open(Routes.SETTINGS) },
                            onCategory = ::goCategory,
                            onAllCategories = { goTab(Routes.CATEGORIES) },
                            onOpenPlayer = openPlayer,
                            onHot = { cc -> open("${Routes.HOT}?cc=$cc") },
                        )
                    }
                    composable(
                        "${Routes.HOT}?cc={cc}",
                        arguments = listOf(navArgument("cc") { type = NavType.StringType; defaultValue = "TW" }),
                    ) {
                        HotScreen(vm, it.arguments?.getString("cc") ?: "TW", back, openPlayer)
                    }
                    composable(
                        "${Routes.CATEGORIES}?key={key}",
                        arguments = listOf(navArgument("key") { type = NavType.StringType; defaultValue = "classical" }),
                    ) {
                        CategoryScreen(vm, it.arguments?.getString("key") ?: "classical", { open(Routes.SEARCH) }, openPlayer)
                    }
                    composable(Routes.FAVORITES) { FavoritesScreen(vm, openPlayer) }
                    composable(Routes.MINE) {
                        MineScreen(vm, { open(Routes.HISTORY) }, { open(Routes.SETTINGS) }, { goTab(Routes.FAVORITES) })
                    }
                    composable(Routes.HISTORY) { HistoryScreen(vm, back, openPlayer) }
                    composable(Routes.SETTINGS) { SettingsScreen(vm, back) }
                    composable(Routes.SEARCH) { SearchScreen(vm, back, openPlayer) }
                    composable(
                        Routes.PLAYER,
                        enterTransition = { slideInVertically(tween(300)) { it / 3 } + fadeIn(tween(300)) },
                        exitTransition = { fadeOut(tween(200)) },
                        popExitTransition = { slideOutVertically(tween(260)) { it / 3 } + fadeOut(tween(260)) },
                    ) { PlayerScreen(vm, back) }
                }
            }
            if (!isPlayer) {
                // 選了電台（或正在播）才出現下方播放器
                val opened by vm.opened.collectAsStateWithLifecycle()
                if (ps.station != null && ps.station!!.name.isNotBlank() && (opened || ps.isPlaying || ps.buffering)) {
                    MiniPlayer(ps, onOpen = openPlayer, onToggle = vm::toggle, onPrev = vm::previous, onNext = vm::next)
                }
                BottomBar(tab) { i ->
                    goTab(listOf(Routes.HOME, Routes.CATEGORIES, Routes.FAVORITES, Routes.MINE)[i])
                }
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
