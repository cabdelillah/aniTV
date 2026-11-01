package app.anitv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.anitv.core.AniTvCore
import app.anitv.core.model.PlaybackRequest
import app.anitv.ui.player.PlayerController
import app.anitv.ui.player.VideoPlayer
import app.anitv.ui.presentation.PlayerModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    core: AniTvCore,
    request: PlaybackRequest,
    onBack: () -> Unit,
    onNext: (PlaybackRequest) -> Unit,
    showBackButton: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val model = remember(request) { PlayerModel(core, request, scope) }
    var controller by remember { mutableStateOf<PlayerController?>(null) }
    var position by remember { mutableLongStateOf(request.startPositionMs) }
    var duration by remember { mutableLongStateOf(0L) }
    var lastActivity by remember { mutableLongStateOf(0L) }
    var chromeVisible by remember { mutableStateOf(true) }

    LaunchedEffect(controller) {
        val c = controller ?: return@LaunchedEffect
        while (true) {
            position = c.positionMs
            duration = c.durationMs
            model.onProgress(position, duration)
            delay(500)
        }
    }
    LaunchedEffect(lastActivity) {
        chromeVisible = true
        delay(4_000)
        chromeVisible = false
    }
    DisposableEffect(model) {
        onDispose { model.onProgress(position, duration, force = true) }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Move || event.type == PointerEventType.Press) {
                        lastActivity++
                    }
                }
            }
        },
    ) {
        VideoPlayer(
            request = request,
            modifier = Modifier.fillMaxSize(),
            onController = { controller = it },
            onEnded = {
                model.onProgress(duration, duration, force = true)
                scope.launch { model.nextEpisode()?.let(onNext) ?: onBack() }
            },
        )

        AnimatedVisibility(chromeVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopStart)) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    .safeDrawingPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showBackButton) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                Column(Modifier.padding(start = 8.dp)) {
                    Text(
                        request.anime.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull("Episode ${request.episode.number}", request.episode.title).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
        }

        val skip = model.skipActionAt(position)
        if (skip != null) {
            Row(
                Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(end = 24.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = { controller?.seekTo(skip.targetMs) }) { Text(skip.label) }
            }
        }
    }
}
