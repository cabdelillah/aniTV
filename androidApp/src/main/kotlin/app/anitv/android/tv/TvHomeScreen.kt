package app.anitv.android.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.anitv.core.model.Anime
import app.anitv.ui.components.LoadingView
import app.anitv.ui.components.MetaLine
import app.anitv.ui.presentation.HomeModel
import app.anitv.ui.presentation.Load

@Composable
fun TvHomeScreen(model: HomeModel, onOpen: (Anime) -> Unit, onSearch: () -> Unit) {
    val feed by model.feed.collectAsState()
    val continueWatching by model.continueWatching.collectAsState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = feed) {
            Load.Loading -> LoadingView()
            is Load.Failed -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.message)
                Spacer(Modifier.height(16.dp))
                Button(onClick = model::refresh) { Text("Retry") }
            }
            is Load.Ready -> {
                val rows = state.value
                var focused by remember { mutableStateOf(rows.trending.firstOrNull()) }

                Backdrop(focused)
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.padding(start = TvGutter, end = TvGutter, top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("aniTV", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(24.dp))
                        Button(onClick = onSearch) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Text("Search", Modifier.padding(start = 8.dp))
                        }
                    }
                    FocusedInfo(focused, Modifier.padding(start = TvGutter, top = 24.dp).height(170.dp))
                    LazyColumn(contentPadding = PaddingValues(bottom = 48.dp)) {
                        if (continueWatching.isNotEmpty()) {
                            item(key = "continue") {
                                val byId = continueWatching.associateBy { it.anime.id }
                                TvRow(
                                    "Continue watching",
                                    continueWatching.map { it.anime },
                                    onOpen,
                                    onFocused = { focused = it },
                                    focusFirstItem = true,
                                    progress = { byId[it.id]?.fraction },
                                )
                            }
                        }
                        item(key = "trending") {
                            TvRow(
                                "Trending now",
                                rows.trending,
                                onOpen,
                                onFocused = { focused = it },
                                focusFirstItem = continueWatching.isEmpty(),
                            )
                        }
                        item(key = "seasonal") { TvRow("Popular this season", rows.seasonal, onOpen, { focused = it }) }
                        item(key = "popular") { TvRow("All-time popular", rows.popular, onOpen, { focused = it }) }
                        item(key = "top") { TvRow("Top rated", rows.topRated, onOpen, { focused = it }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusedInfo(anime: Anime?, modifier: Modifier) {
    Column(modifier.widthIn(max = 640.dp)) {
        if (anime == null) return@Column
        Text(anime.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        MetaLine(anime, Modifier.padding(vertical = 8.dp))
        anime.description?.let {
            Text(it, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
