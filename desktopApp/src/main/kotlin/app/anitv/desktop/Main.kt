package app.anitv.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.anitv.core.AniTvCore
import app.anitv.core.FileTextStorage
import app.anitv.ui.AniTvApp
import app.anitv.ui.Navigator
import app.anitv.ui.Screen
import java.io.File

fun main() = application {
    val core = remember { AniTvCore(progressStorage = FileTextStorage(File(dataDir(), "progress.json"))) }
    val navigator = remember { Navigator() }
    val state = rememberWindowState(width = 1280.dp, height = 800.dp)

    Window(
        onCloseRequest = {
            core.close()
            exitApplication()
        },
        state = state,
        title = "aniTV",
        onPreviewKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) return@Window false
            when (event.key) {
                Key.F -> if (navigator.current is Screen.Player) {
                    state.placement = if (state.placement == WindowPlacement.Fullscreen) WindowPlacement.Floating else WindowPlacement.Fullscreen
                    true
                } else {
                    false
                }
                Key.Escape -> when {
                    state.placement == WindowPlacement.Fullscreen -> {
                        state.placement = WindowPlacement.Floating
                        true
                    }
                    else -> navigator.pop()
                }
                else -> false
            }
        },
    ) {
        AniTvApp(core, navigator)
    }
}

/** ~/Library/Application Support/aniTV on macOS, ~/.local/share/aniTV elsewhere. */
private fun dataDir(): File {
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    return when {
        "mac" in os -> File(home, "Library/Application Support/aniTV")
        "win" in os -> File(System.getenv("APPDATA") ?: home, "aniTV")
        else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "aniTV")
    }
}
