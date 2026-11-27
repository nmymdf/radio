package com.archiekuo.carhome.shield

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.archiekuo.carhome.CarApp

/**
 * 看通知列：神盾在背景執行時會有常駐通知，有＝開著、沒有＝關著。
 * 需要使用者在車機設定裡允許「讀取通知」一次。
 * 作者：ArchieKUO
 */
class ShieldWatcher : NotificationListenerService() {
    private val app get() = application as CarApp

    override fun onListenerConnected() {
        instance = this
        refresh()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        app.shieldOn.value = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) { if (sbn.packageName == app.shieldPackage()) refresh() }
    override fun onNotificationRemoved(sbn: StatusBarNotification) { if (sbn.packageName == app.shieldPackage()) refresh() }

    private fun shieldNotes(): List<StatusBarNotification> {
        val pkg = app.shieldPackage() ?: return emptyList()
        return runCatching { activeNotifications?.filter { it.packageName == pkg } }.getOrNull() ?: emptyList()
    }

    fun refresh() { app.shieldOn.value = shieldNotes().isNotEmpty() }

    /** 按神盾通知上的「結束／關閉」按鈕；找不到回傳 false */
    fun pressExit(): Boolean {
        val words = listOf("結束", "结束", "關閉", "关闭", "停止", "離開", "离开", "退出", "exit", "stop", "close", "quit")
        for (sbn in shieldNotes()) {
            val acts: Array<Notification.Action> = sbn.notification.actions ?: continue
            val a = acts.firstOrNull { act -> val t = act.title?.toString()?.lowercase() ?: ""; words.any { t.contains(it) } } ?: continue
            if (runCatching { a.actionIntent?.send() }.isSuccess) return true
        }
        return false
    }

    /** 通知上所有按鈕的文字（給設定頁看，方便知道神盾有哪些按鈕） */
    fun actionTitles(): String = shieldNotes().flatMap { s -> s.notification.actions?.map { it.title.toString() } ?: emptyList() }.joinToString("、")

    companion object { @Volatile var instance: ShieldWatcher? = null }
}
