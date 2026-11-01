package app.anitv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.anitv.core.model.Anime
import app.anitv.core.model.Episode
import app.anitv.core.model.PlaybackRequest
import app.anitv.core.progress.WatchProgress
import app.anitv.ui.components.AnimeRow
import app.anitv.ui.components.Artwork
import app.anitv.ui.components.ErrorView
import app.anitv.ui.components.MetaLine
import app.anitv.ui.player.formatTime
import app.anitv.ui.presentation.DetailsModel
import app.anitv.ui.presentation.Load

@Composable
fun DetailsScreen(
    model: DetailsModel,
    onPlay: (PlaybackRequest) -> Unit,
    onOpen: (Anime) -> Unit,
    onBack: () -> Unit,
) {
    val details by model.details.collectAsState()
    val episodes by model.episodes.collectAsState()
    val progress by model.progress.collectAsState()
    val resolving by model.resolving.collectAsState()
    val error by model.error.collectAsState()
    val anime = details.anime

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "header") { Header(anime) }
            item(key = "actions") {
                Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 900.dp)) {
                    val list = (episodes as? Load.Ready)?.value.orEmpty()
                    val target = model.resumeTarget(list, progress)
                    Button(
                        onClick = { target?.let { model.play(it, onPlay) } },
                        enabled = target != null && resolving == null,
                        modifier = Modifier.padding(vertical = 12.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text(playLabel(target, progress), Modifier.padding(start = 6.dp))
                    }
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.clickable { model.dismissError() })
                    }
                    if (anime.genres.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                            anime.genres.take(4).forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                        }
                    }
                    anime.description?.let { Synopsis(it) }
                    Text(
                        "Episodes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
            }
            when (val state = episodes) {
                Load.Loading -> item(key = "episodes-loading") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                is Load.Failed -> item(key = "episodes-error") { ErrorView(state.message, onRetry = model::loadEpisodes) }
                is Load.Ready -> items(state.value, key = { "ep-${it.id}" }) { episode ->
                    EpisodeRow(
                        episode = episode,
                        progress = progress?.takeIf { it.episodeNumber == episode.number },
                        watched = progress?.let { episode.number < it.episodeNumber } == true,
                        loading = resolving == episode.number,
                        onClick = { model.play(episode, onPlay) },
                    )
                }
            }
            if (details.recommendations.isNotEmpty()) {
                item(key = "recs") { AnimeRow("More like this", details.recommendations, onOpen) }
            }
        }
        FilledIconButton(
            onClick = onBack,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            modifier = Modifier.safeDrawingPadding().padding(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

private fun playLabel(target: Episode?, progress: WatchProgress?): String = when {
    target == null -> "Play"
    progress != null && progress.episodeNumber == target.number && !progress.isFinished ->
        "Resume episode ${target.number} · ${formatTime(progress.positionMs)}"
    progress != null -> "Play episode ${target.number}"
    else -> "Play episode ${target.number}"
}

@Composable
private fun Header(anime: Anime) {
    Box(Modifier.fillMaxWidth().height(320.dp)) {
        Artwork(anime.bannerUrl ?: anime.coverUrl, anime.color, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, 1f to MaterialTheme.colorScheme.background),
            ),
        )
        Row(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.Bottom) {
            Artwork(
                anime.coverUrl,
                anime.color,
                Modifier.width(110.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(10.dp)),
                anime.title,
            )
            Column(Modifier.padding(start = 16.dp)) {
                Text(anime.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                anime.titleNative?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                MetaLine(anime, Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun Synopsis(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Show less" else "Show more") }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, progress: WatchProgress?, watched: Boolean, loading: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp).widthIn(max = 900.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp, 40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    episode.number.toString(),
                    fontWeight = FontWeight.Bold,
                    color = if (watched) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                episode.title ?: "Episode ${episode.number}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (watched) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (episode.isFiller) Text("Filler", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            if (progress != null) {
                Spacer(Modifier.size(6.dp))
                LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth().height(3.dp))
            }
        }
    }
}
