package app.anitv.ui.player

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import app.anitv.core.AniTvCore
import app.anitv.core.model.PlaybackRequest
import app.anitv.core.model.Subtitle

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(
    request: PlaybackRequest,
    modifier: Modifier,
    onController: (PlayerController?) -> Unit,
    onEnded: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnEnded by rememberUpdatedState(onEnded)

    val player = remember(request) {
        val headers = request.stream.headers
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(headers.entries.firstOrNull { it.key.equals("User-Agent", true) }?.value ?: AniTvCore.USER_AGENT)
            .setDefaultRequestProperties(headers.filterKeys { !it.equals("User-Agent", true) })
            .setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
            .apply {
                setMediaItem(request.toMediaItem(), request.startPositionMs)
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        val controller = object : PlayerController {
            override val positionMs get() = player.currentPosition
            override val durationMs get() = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            override val isPlaying get() = player.isPlaying
            override fun seekTo(positionMs: Long) = player.seekTo(positionMs)
            override fun togglePlay() {
                if (player.isPlaying) player.pause() else player.play()
            }
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) currentOnEnded()
            }
        }
        player.addListener(listener)

        // Pause when the app goes to the background.
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycle?.addObserver(observer)
        onController(controller)

        onDispose {
            onController(null)
            lifecycle?.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                keepScreenOn = true
                setShowSubtitleButton(true)
                setShowNextButton(false)
                setShowPreviousButton(false)
                this.player = player
                requestFocus()
            }
        },
        update = { it.player = player },
        modifier = modifier,
    )
}

private fun PlaybackRequest.toMediaItem(): MediaItem {
    val url = stream.url
    val builder = MediaItem.Builder().setUri(url)
    if (url.substringBefore('?').endsWith(".m3u8")) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
    if (url.substringBefore('?').endsWith(".mpd")) builder.setMimeType(MimeTypes.APPLICATION_MPD)
    builder.setSubtitleConfigurations(stream.subtitles.mapIndexed { i, sub -> sub.toConfiguration(default = i == 0) })
    return builder.build()
}

private fun Subtitle.toConfiguration(default: Boolean): MediaItem.SubtitleConfiguration {
    val mime = when (format?.lowercase() ?: url.substringBefore('?').substringAfterLast('.').lowercase()) {
        "srt" -> MimeTypes.APPLICATION_SUBRIP
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        else -> MimeTypes.TEXT_VTT
    }
    return MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
        .setMimeType(mime)
        .setLanguage(language)
        .setLabel(language)
        .setSelectionFlags(if (default) C.SELECTION_FLAG_DEFAULT else 0)
        .build()
}
