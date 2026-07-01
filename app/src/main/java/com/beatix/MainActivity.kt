package com.beatix

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private lateinit var midi: MidiClient
    private lateinit var discovery: NsdDiscovery

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen awake while DJing.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Draw edge-to-edge into the camera cutout area to reclaim that space.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        // Immersive fullscreen: reclaim the full 2400px landscape width on the Redmi.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val prefs = getSharedPreferences("beatix", MODE_PRIVATE)
        discovery = NsdDiscovery(this)
        val saved = prefs.getString("host", "127.0.0.1") ?: "127.0.0.1"
        val lastIp = prefs.getString("lastIp", "127.0.0.1") ?: "127.0.0.1"
        // Auto mode: connect to the last-known DHCP IP INSTANTLY, and let mDNS
        // run in the background only to correct it if the router changed the IP.
        val onFound: (String) -> Unit = { ip ->
            prefs.edit().putString("lastIp", ip).apply()
            if (ip != midi.host) midi.setHost(ip)
        }
        midi = MidiClient(if (saved == "auto") lastIp else saved)
        midi.start()
        if (saved == "auto") discovery.start(onFound)
        setContent {
            BeatixTheme {
                ConsoleScreen(midi) { newHost ->
                    if (newHost == "auto") {
                        prefs.edit().putString("host", "auto").apply()
                        discovery.start(onFound)
                    } else {
                        prefs.edit().putString("host", newHost).apply()
                        discovery.stop()
                        midi.setHost(newHost)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        discovery.stop()
        midi.stop()
    }
}
