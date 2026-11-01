package app.anitv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.anitv.core.model.Anime
import app.anitv.core.progress.WatchProgress
import app.anitv.ui.components.AnimeRow
import app.anitv.ui.components.Artwork
import app.anitv.ui.components.ErrorView
import app.anitv.ui.components.LoadingView
import app.anitv.ui.components.MetaLine
import app.anitv.ui.components.PosterCard
import app.anitv.ui.components.SectionTitle
import app.anitv.ui.presentation.HomeModel
import app.anitv.ui.presentation.Load

@Composable
fun HomeScreen(model: HomeModel, onOpen: (Anime) -> Unit, onSearch: () -> Unit) {
    val feed by model.feed.collectAsState()
    val continueWatching by model.continueWatching.collectAsState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = feed) {
            Load.Loading -> LoadingView()
            is Load.Failed -> ErrorView(state.message, onRetry = model::refresh, modifier = Modifier.align(Alignment.Center))
            is Load.Ready -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                state.value.trending.firstOrNull()?.let { featured ->
                    item(key = "hero") { Hero(featured, onOpen) }
                }
                if (continueWatching.isNotEmpty()) {
                    item(key = "continue") { ContinueWatchingRow(continueWatching, onOpen) }
                }
                item(key = "trending") { AnimeRow("Trending now", state.value.trending.drop(1), onOpen) }
                item(key = "seasonal") { AnimeRow("Popular this season", state.value.seasonal, onOpen) }
                item(key = "popular") { AnimeRow("All-time popular", state.value.popular, onOpen) }
                item(key = "top") { AnimeRow("Top rated", state.value.topRated, onOpen) }
            }
        }
        Row(
            Modifier.fillMaxWidth().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("aniTV", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch) { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White) }
        }
    }
}

@Composable
private fun Hero(anime: Anime, onOpen: (Anime) -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = 360.dp).height(440.dp)) {
        Artwork(anime.bannerUrl ?: anime.coverUrl, anime.color, Modifier.fillMaxSize(), anime.title)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    0.35f to Color.Transparent,
                    1f to MaterialTheme.colorScheme.background,
                ),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp).widthIn(max = 640.dp)) {
            Text(
                anime.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            MetaLine(anime, Modifier.padding(vertical = 6.dp))
            anime.description?.let {
                Text(it, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.size(12.dp))
            Button(onClick = { onOpen(anime) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("Watch now", Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun ContinueWatchingRow(entries: List<WatchProgress>, onOpen: (Anime) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        SectionTitle("Continue watching")
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(entries, key = { it.anime.id }) { entry ->
                PosterCard(
                    entry.anime,
                    onClick = { onOpen(entry.anime) },
                    progress = entry.fraction,
                    subtitle = "Episode ${entry.episodeNumber}",
                )
            }
        }
    }
}
