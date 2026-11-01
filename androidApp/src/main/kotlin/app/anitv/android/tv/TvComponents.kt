package app.anitv.android.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.anitv.core.model.Anime
import app.anitv.ui.components.Artwork
import app.anitv.ui.components.ScoreBadge

internal val TvGutter = 48.dp

@Composable
internal fun focusBorder() = CardDefaults.border(
    focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(10.dp)),
)

@Composable
fun TvPosterCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 140.dp,
    progress: Float? = null,
    requestFocus: Boolean = false,
    onFocused: () -> Unit = {},
) {
    val focus = remember { FocusRequester() }
    if (requestFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(modifier.width(width)) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .focusRequester(focus)
                .onFocusChanged { if (it.isFocused) onFocused() },
            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
            scale = CardDefaults.scale(focusedScale = 1.08f),
            border = focusBorder(),
        ) {
            Box(Modifier.fillMaxSize()) {
                Artwork(anime.coverUrl, anime.color, Modifier.fillMaxSize(), anime.title)
                anime.score?.let { ScoreBadge(it, Modifier.align(Alignment.TopEnd).padding(6.dp)) }
                if (progress != null) ProgressBar(progress, Modifier.align(Alignment.BottomCenter))
            }
        }
        Text(
            anime.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).background(Color.Black.copy(alpha = 0.5f))) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
fun TvRow(
    title: String,
    items: List<Anime>,
    onOpen: (Anime) -> Unit,
    onFocused: (Anime) -> Unit,
    focusFirstItem: Boolean = false,
    progress: (Anime) -> Float? = { null },
) {
    if (items.isEmpty()) return
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = TvGutter, bottom = 12.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = TvGutter, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { index, anime ->
                TvPosterCard(
                    anime,
                    onClick = { onOpen(anime) },
                    progress = progress(anime),
                    onFocused = { onFocused(anime) },
                    requestFocus = focusFirstItem && index == 0,
                )
            }
        }
    }
}

/** Banner art on the right, fading into the background so text on the left stays readable. */
@Composable
fun BoxScope.Backdrop(anime: Anime?) {
    if (anime == null) return
    val background = MaterialTheme.colorScheme.background
    Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.72f).fillMaxHeight(0.7f)) {
        Artwork(anime.bannerUrl ?: anime.coverUrl, anime.color, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to background, 0.45f to background.copy(alpha = 0.4f), 1f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to background)))
    }
}
