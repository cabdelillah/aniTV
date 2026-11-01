package app.anitv.android.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.anitv.core.model.Anime
import app.anitv.core.model.Episode
import app.anitv.core.model.PlaybackRequest
import app.anitv.ui.components.MetaLine
import app.anitv.ui.player.formatTime
import app.anitv.ui.presentation.DetailsModel
import app.anitv.ui.presentation.Load

@Composable
fun TvDetailsScreen(model: DetailsModel, onPlay: (PlaybackRequest) -> Unit, onOpen: (Anime) -> Unit) {
    val details by model.details.collectAsState()
    val episodes by model.episodes.collectAsState()
    val progress by model.progress.collectAsState()
    val resolving by model.resolving.collectAsState()
    val error by model.error.collectAsState()
    val anime = details.anime
    val playFocus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(anime) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Backdrop(focused)
        LazyColumn(contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp)) {
            item(key = "info") {
                Column(Modifier.padding(horizontal = TvGutter).widthIn(max = 720.dp)) {
                    Text(anime.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    MetaLine(anime, Modifier.padding(vertical = 8.dp))
                    if (anime.genres.isNotEmpty()) {
                        Text(anime.genres.joinToString("  ·  "), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    anime.description?.let {
                        Text(
                            it,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    val list = (episodes as? Load.Ready)?.value.orEmpty()
                    val target = model.resumeTarget(list, progress)
                    LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }
                    Row(Modifier.padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { target?.let { model.play(it, onPlay) } },
                            enabled = target != null,
                            modifier = Modifier.focusRequester(playFocus),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = LocalContentColor.current)
                            val label = when {
                                target == null -> "Play"
                                progress?.episodeNumber == target.number && progress?.isFinished == false ->
                                    "Resume episode ${target.number} · ${formatTime(progress!!.positionMs)}"
                                else -> "Play episode ${target.number}"
                            }
                            Text(label, Modifier.padding(start = 8.dp))
                        }
                        if (resolving != null) {
                            CircularProgressIndicator(Modifier.padding(start = 16.dp).size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
                }
            }
            item(key = "episodes") {
                Column(Modifier.padding(top = 32.dp)) {
                    Text("Episodes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = TvGutter, bottom = 12.dp))
                    when (val state = episodes) {
                        Load.Loading -> CircularProgressIndicator(Modifier.padding(start = TvGutter))
                        is Load.Failed -> Row(Modifier.padding(horizontal = TvGutter), verticalAlignment = Alignment.CenterVertically) {
                            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = model::loadEpisodes, modifier = Modifier.padding(start = 16.dp)) { Text("Retry") }
                        }
                        is Load.Ready -> LazyRow(
                            contentPadding = PaddingValues(horizontal = TvGutter, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(state.value, key = { it.id }) { episode ->
                                EpisodeCard(
                                    episode,
                                    progress = progress?.takeIf { it.episodeNumber == episode.number }?.fraction,
                                    onClick = { model.play(episode, onPlay) },
                                )
                            }
                        }
                    }
                }
            }
            if (details.recommendations.isNotEmpty()) {
                item(key = "recs") {
                    Box(Modifier.padding(top = 20.dp)) {
                        TvRow("More like this", details.recommendations, onOpen, onFocused = { focused = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(episode: Episode, progress: Float?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(220.dp).height(124.dp),
        shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
        scale = CardDefaults.scale(focusedScale = 1.06f),
        border = focusBorder(),
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(14.dp)) {
                Text("EPISODE ${episode.number}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    episode.title ?: "Episode ${episode.number}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (episode.isFiller) Text("Filler", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (progress != null) ProgressBar(progress, Modifier.align(Alignment.BottomCenter).fillMaxWidth())
        }
    }
}
