package com.archiekuo.carhome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 車機開機後，把桌面叫起來（桌面會自動執行發動後的動作） */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as CarApp
        app.onFirstLaunch()
    }
}
