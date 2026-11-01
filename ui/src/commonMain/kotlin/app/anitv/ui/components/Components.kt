package app.anitv.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.anitv.core.model.Anime
import app.anitv.ui.theme.AniTvColors
import app.anitv.ui.theme.parseHexColor
import coil3.compose.AsyncImage

/** Image with the anime's dominant color as placeholder while it loads. */
@Composable
fun Artwork(url: String?, placeholder: String?, modifier: Modifier = Modifier, description: String? = null) {
    Box(modifier.background(parseHexColor(placeholder)?.copy(alpha = 0.35f) ?: AniTvColors.SurfaceHigh)) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun PosterCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 132.dp,
    progress: Float? = null,
    subtitle: String? = null,
) {
    Column(modifier.width(width).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(10.dp))) {
            Artwork(anime.coverUrl, anime.color, Modifier.fillMaxSize(), anime.title)
            anime.score?.let { ScoreBadge(it, Modifier.align(Alignment.TopEnd).padding(6.dp)) }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                    trackColor = Color.Black.copy(alpha = 0.5f),
                )
            }
        }
        Text(
            anime.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
fun ScoreBadge(score: Int, modifier: Modifier = Modifier) {
    Text(
        "★ ${score / 10.0}",
        style = MaterialTheme.typography.labelSmall,
        color = AniTvColors.Score,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun AnimeRow(title: String, items: List<Anime>, onOpen: (Anime) -> Unit) {
    if (items.isEmpty()) return
    Column(Modifier.padding(vertical = 8.dp)) {
        SectionTitle(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { anime -> PosterCard(anime, onClick = { onOpen(anime) }) }
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorView(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRetry != null) {
            Spacer(Modifier.size(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun MetaLine(anime: Anime, modifier: Modifier = Modifier) {
    val parts = listOfNotNull(
        anime.format?.replace('_', ' '),
        anime.year?.toString(),
        anime.episodeCount?.let { "$it eps" },
        anime.status?.lowercase()?.replace('_', ' ')?.replaceFirstChar { it.uppercase() },
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        anime.score?.let {
            Text("★ ${it / 10.0}", color = AniTvColors.Score, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            if (parts.isNotEmpty()) Text("  ·  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(parts.joinToString("  ·  "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
