package app.anitv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.anitv.core.model.PlaybackRequest

/** Platform-neutral handle on the running player. */
interface PlayerController {
    val positionMs: Long
    val durationMs: Long
    val isPlaying: Boolean
    fun seekTo(positionMs: Long)
    fun togglePlay()
}

/**
 * Plays [request] with the platform's best engine: Media3/ExoPlayer on Android and Android TV,
 * libmpv on macOS. Hands back a [PlayerController] once playback is set up (and null when it is
 * torn down), calls [onEnded] when the video finishes and [onError] when it cannot be played.
 */
@Composable
expect fun VideoPlayer(
    request: PlaybackRequest,
    modifier: Modifier = Modifier,
    onController: (PlayerController?) -> Unit,
    onEnded: () -> Unit,
    onError: (String) -> Unit,
)

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    fun two(n: Long) = n.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(s)}" else "$m:${two(s)}"
}
