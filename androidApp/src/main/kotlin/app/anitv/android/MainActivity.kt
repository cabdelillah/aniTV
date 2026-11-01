package app.anitv.android

import android.app.UiModeManager
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import app.anitv.core.SelfTest
import kotlinx.coroutines.launch
import app.anitv.android.tv.TvApp
import app.anitv.ui.AniTvApp
import app.anitv.ui.Navigator
import app.anitv.ui.Screen

class MainActivity : ComponentActivity() {
    private val navigator = Navigator()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val core = (application as AniTvApplication).core
        val isTv = isTelevision()

        // `adb shell am start -n app.anitv/app.anitv.android.MainActivity --ez selftest true`
        if (intent.getBooleanExtra("selftest", false)) {
            lifecycleScope.launch {
                Log.i(SELFTEST_TAG, "device=${Build.MANUFACTURER} ${Build.MODEL} sdk=${Build.VERSION.SDK_INT} tv=$isTv")
                SelfTest.run(core) { Log.i(SELFTEST_TAG, it) }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navigator.pop()) finish()
            }
        })

        setContent {
            val playing = navigator.current is Screen.Player
            val insets = remember { WindowCompat.getInsetsController(window, window.decorView) }
            LaunchedEffect(playing) {
                // Phones play in landscape; TVs already are.
                if (!isTv) {
                    requestedOrientation = if (playing) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
                if (playing) {
                    insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insets.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    insets.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            if (isTv) TvApp(core, navigator) else AniTvApp(core, navigator)
        }
    }

    private companion object {
        const val SELFTEST_TAG = "ANITV_SELFTEST"
    }

    private fun isTelevision(): Boolean {
        val uiMode = getSystemService(UiModeManager::class.java)?.currentModeType
        return uiMode == Configuration.UI_MODE_TYPE_TELEVISION ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
}
