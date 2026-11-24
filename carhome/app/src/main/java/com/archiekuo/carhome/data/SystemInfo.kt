package com.archiekuo.carhome.data

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

data class MemInfo(val usedMb: Long, val totalMb: Long) {
    val percent: Int get() = if (totalMb <= 0) 0 else (usedMb * 100 / totalMb).toInt()
    private fun gb(mb: Long) = String.format(java.util.Locale.US, "%.1f", mb / 1024f)
    val text: String get() = "已用 ${gb(usedMb)} GB ／ 共 ${gb(totalMb)} GB（$percent%）"
}

data class RecentApp(val pkg: String, val lastUsed: Long)

/** 記憶體、最近用過的 App、打開系統設定頁 */
object SystemInfo {
    fun memory(ctx: Context): MemInfo {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = mi.totalMem / (1024 * 1024)
        val avail = mi.availMem / (1024 * 1024)
        return MemInfo((total - avail).coerceAtLeast(0), total)
    }

    /** 有沒有「使用情況存取權」 */
    fun hasUsageAccess(ctx: Context): Boolean = runCatching {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= 29) ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        else ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /** 最近 6 小時用過的 App（最新的在前面） */
    fun recentApps(ctx: Context, onlyThese: Set<String>): List<RecentApp> = runCatching {
        val um = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val since = now - 6 * 3600_000L
        um.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, since, now)
            .filter { it.lastTimeUsed >= since && it.packageName in onlyThese && it.packageName != ctx.packageName }
            .groupBy { it.packageName }
            .map { (p, list) -> RecentApp(p, list.maxOf { it.lastTimeUsed }) }
            .sortedByDescending { it.lastUsed }
    }.getOrDefault(emptyList())

    private fun open(ctx: Context, i: Intent): Boolean =
        runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)

    fun openUsageAccess(ctx: Context): Boolean =
        open(ctx, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) || open(ctx, Intent(Settings.ACTION_SETTINGS))

    fun openAppInfo(ctx: Context, pkg: String): Boolean =
        open(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))

    /** 移除 App（系統會跳出確認） */
    fun uninstall(ctx: Context, pkg: String): Boolean =
        open(ctx, Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))

    fun ago(ms: Long): String {
        val min = ((System.currentTimeMillis() - ms) / 60_000).coerceAtLeast(0)
        return when {
            min < 1 -> "剛剛用過"
            min < 60 -> "$min 分鐘前用過"
            else -> "${min / 60} 小時前用過"
        }
    }
}
