package com.archiekuo.carhome.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 網路有沒有連上（例如手機熱點）；支援 Android 5 以上 */
class NetworkMonitor(ctx: Context) {
    private val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val _online = MutableStateFlow(check())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    @Suppress("DEPRECATION")
    private fun check(): Boolean {
        val c = cm ?: return false
        return if (Build.VERSION.SDK_INT >= 23) {
            val caps = c.getNetworkCapabilities(c.activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else c.activeNetworkInfo?.isConnected == true
    }

    init {
        runCatching {
            val req = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            cm?.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { _online.value = true }
                override fun onLost(network: Network) { _online.value = check() }
            })
        }
    }
}
