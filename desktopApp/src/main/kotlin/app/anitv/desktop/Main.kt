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
import app.anitv.core.SelfTest
import app.anitv.core.model.PlaybackRequest
import app.anitv.core.progress.InMemoryTextStorage
import app.anitv.ui.player.MpvPlayer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.system.exitProcess
import app.anitv.ui.AniTvApp
import app.anitv.ui.Navigator
import app.anitv.ui.Screen
import java.io.File

fun main() {
    if (System.getenv("ANITV_SELFTEST") == "1") exitProcess(runSelfTest())
    launchApp()
}

private fun launchApp() = application {
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

/**
 * Headless check of the packaged app: network stack, AniList, AniSkip, the source, and real
 * playback through libmpv (frames must actually be rendered). Prints results, returns exit code.
 */
private fun runSelfTest(): Int = runBlocking {
    val core = AniTvCore(progressStorage = InMemoryTextStorage())
    val log = { line: String -> println("ANITV_SELFTEST $line") }
    var ok = SelfTest.run(core, log)

    val anime = runCatching { core.aniList.home().trending.first() }.getOrNull()
    val episode = anime?.let { runCatching { core.episodes(it).first() }.getOrNull() }
    val stream = episode?.let { runCatching { core.streams(it).first() }.getOrNull() }
    if (anime == null || episode == null || stream == null) {
        log("FAIL mpv playback: nothing to play")
        ok = false
    } else {
        val result = runCatching {
            val player = MpvPlayer(PlaybackRequest(anime, episode, stream)) {}
            try {
                player.setViewportSize(1280, 720)
                val frames = withTimeoutOrNull(45_000) {
                    player.frame.filterNotNull().first { it.generation >= 50 }
                }
                checkNotNull(frames) { "fewer than 50 frames in 45 s; error=${player.error.value}" }
                "rendered ${frames.generation} frames ${frames.bitmap.width}x${frames.bitmap.height}, position=${player.positionMs} ms"
            } finally {
                player.release()
            }
        }
        result.onSuccess { log("PASS mpv playback: $it") }.onFailure {
            log("FAIL mpv playback: ${it::class.simpleName}: ${it.message}")
            ok = false
        }
    }
    log("SELFTEST RESULT ${if (ok) "OK" else "FAILED"}")
    core.close()
    if (ok) 0 else 1
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
