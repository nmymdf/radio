package com.archiekuo.radio

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.archiekuo.radio.navigation.AppRoot
import com.archiekuo.radio.ui.theme.RadioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val app = application as RadioApp
        app.player.connect()
        if (savedInstanceState == null && app.store.autoPlay.value) app.player.autoResume()
        setContent { RadioTheme { AppRoot() } }
    }
}
