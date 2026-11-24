package com.archiekuo.radio

import android.app.Application
import com.archiekuo.radio.data.LocalStore
import com.archiekuo.radio.data.RadioBrowserApi
import com.archiekuo.radio.player.PlayerConnection
import com.archiekuo.radio.repository.CatalogRepository
import com.archiekuo.radio.repository.StationRepository

class RadioApp : Application() {
    lateinit var store: LocalStore
    lateinit var repo: StationRepository
    lateinit var player: PlayerConnection

    override fun onCreate() {
        super.onCreate()
        val api = RadioBrowserApi()
        CatalogRepository(this, api).apply { loadLocal(); refreshRemote() }
        store = LocalStore(this)
        repo = StationRepository(api)
        player = PlayerConnection(this, store)
    }
}
