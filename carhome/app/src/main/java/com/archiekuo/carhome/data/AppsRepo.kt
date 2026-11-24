package com.archiekuo.carhome.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AppInfo(val pkg: String, val label: String, val component: ComponentName, val system: Boolean = false)

/** 車機上已安裝的 App */
class AppsRepo(private val ctx: Context) {
    private val pm: PackageManager = ctx.packageManager
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    private val icons = HashMap<String, ImageBitmap>()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map {
                val flags = it.activityInfo.applicationInfo.flags
                val sys = flags and (android.content.pm.ApplicationInfo.FLAG_SYSTEM or android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString().trim(), ComponentName(it.activityInfo.packageName, it.activityInfo.name), sys)
            }
            .distinctBy { it.pkg }
            .sortedBy { it.label }
        _apps.value = list
    }

    fun find(pkg: String): AppInfo? = if (pkg.isBlank()) null else _apps.value.firstOrNull { it.pkg == pkg }

    fun findByPackages(vararg pkgs: String): AppInfo? = pkgs.firstNotNullOfOrNull { p -> _apps.value.firstOrNull { it.pkg == p } }

    fun findByLabel(vararg words: String): AppInfo? =
        words.firstNotNullOfOrNull { w -> _apps.value.firstOrNull { it.label.contains(w, ignoreCase = true) } }

    fun icon(pkg: String): ImageBitmap? {
        icons[pkg]?.let { return it }
        return runCatching { pm.getApplicationIcon(pkg).toBitmap(144, 144).asImageBitmap() }.getOrNull()?.also { icons[pkg] = it }
    }

    fun isInstalled(pkg: String): Boolean = runCatching { pm.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    fun launch(pkg: String): Boolean {
        val app = find(pkg)
        val intent = if (app != null) {
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
        } else pm.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return runCatching { ctx.startActivity(intent); true }.getOrDefault(false)
    }

    /** 原廠桌面（其他的桌面程式） */
    fun otherHomes(): List<ResolveInfo> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .filter { it.activityInfo.packageName != ctx.packageName && it.activityInfo.packageName != "com.android.settings" }

    fun launchOtherHome(): Boolean {
        val h = otherHomes().firstOrNull() ?: return false
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .setComponent(ComponentName(h.activityInfo.packageName, h.activityInfo.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { ctx.startActivity(i); true }.getOrDefault(false)
    }
}
