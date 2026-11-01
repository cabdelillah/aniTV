package app.anitv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import app.anitv.core.model.PlaybackRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun VideoPlayer(
    request: PlaybackRequest,
    modifier: Modifier,
    onController: (PlayerController?) -> Unit,
    onEnded: () -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentOnEnded by rememberUpdatedState(onEnded)
    val player = remember(request) {
        runCatching { MpvPlayer(request) { scope.launch(Dispatchers.Main) { currentOnEnded() } } }
    }
    val mpv = player.getOrNull()
    DisposableEffect(mpv) {
        onController(mpv)
        onDispose {
            onController(null)
            mpv?.release()
        }
    }

    if (mpv == null) {
        val message = player.exceptionOrNull()?.message ?: "Playback failed"
        LaunchedEffect(message) { onError(message) }
        Box(modifier.background(Color.Black))
        return
    }

    val frame by mpv.frame.collectAsState()
    val error by mpv.error.collectAsState()
    LaunchedEffect(error) { error?.let(onError) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(true) }
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    var activity by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(mpv) {
        focus.requestFocus()
        while (true) {
            position = mpv.positionMs
            duration = mpv.durationMs
            playing = mpv.isPlaying
            delay(250)
        }
    }
    LaunchedEffect(activity) {
        controlsVisible = true
        delay(3_000)
        controlsVisible = false
    }

    Box(
        modifier
            .background(Color.Black)
            .onSizeChanged { mpv.setViewportSize(it.width, it.height) }
            .onPointerEvent(PointerEventType.Move) { activity++ }
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                activity++
                when (event.key) {
                    Key.Spacebar, Key.K -> mpv.togglePlay()
                    Key.DirectionRight, Key.L -> mpv.seekBy(10_000)
                    Key.DirectionLeft, Key.J -> mpv.seekBy(-10_000)
                    else -> return@onKeyEvent false
                }
                true
            },
    ) {
        frame?.let { current ->
            val image = remember(current.generation) { current.bitmap.asComposeImageBitmap() }
            Canvas(Modifier.fillMaxSize()) {
                val scale = minOf(size.width / image.width, size.height / image.height)
                val w = image.width * scale
                val h = image.height * scale
                drawImage(
                    image,
                    dstOffset = IntOffset(((size.width - w) / 2).toInt(), ((size.height - h) / 2).toInt()),
                    dstSize = IntSize(w.toInt(), h.toInt()),
                    filterQuality = FilterQuality.Medium,
                )
            }
        } ?: if (error == null) CircularProgressIndicator(Modifier.align(Alignment.Center)) else Unit

        AnimatedVisibility(
            controlsVisible || !playing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { mpv.togglePlay(); activity++ }) {
                    if (playing) PauseIcon() else Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                }
                TextButton(onClick = { mpv.seekBy(-10_000); activity++ }) { Text("−10s", color = Color.White) }
                TextButton(onClick = { mpv.seekBy(10_000); activity++ }) { Text("+10s", color = Color.White) }
                Text(formatTime(scrubbing?.toLong() ?: position), color = Color.White, style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = scrubbing ?: position.toFloat(),
                    onValueChange = { scrubbing = it; activity++ },
                    onValueChangeFinished = {
                        scrubbing?.let { mpv.seekTo(it.toLong()) }
                        scrubbing = null
                    },
                    valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                Text(formatTime(duration), color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PauseIcon() {
    Canvas(Modifier.size(24.dp)) {
        val barWidth = size.width * 0.22f
        val top = size.height * 0.18f
        val barHeight = size.height * 0.64f
        drawRect(Color.White, Offset(size.width * 0.22f, top), Size(barWidth, barHeight))
        drawRect(Color.White, Offset(size.width * 0.56f, top), Size(barWidth, barHeight))
    }
}
