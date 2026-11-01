package app.anitv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import app.anitv.core.model.Anime
import app.anitv.ui.components.ErrorView
import app.anitv.ui.components.LoadingView
import app.anitv.ui.components.PosterCard
import app.anitv.ui.presentation.Load
import app.anitv.ui.presentation.SearchModel

@Composable
fun SearchScreen(model: SearchModel, onOpen: (Anime) -> Unit, onBack: () -> Unit) {
    val query by model.query.collectAsState()
    val results by model.results.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            OutlinedTextField(
                value = query,
                onValueChange = model::onQueryChange,
                placeholder = { Text("Search anime") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f).padding(end = 8.dp).focusRequester(focus),
            )
        }
        when (val state = results) {
            Load.Loading -> LoadingView()
            is Load.Failed -> ErrorView(state.message, onRetry = null)
            is Load.Ready -> if (state.value.isEmpty() && query.isNotBlank()) {
                ErrorView("No results for \"$query\"", onRetry = null)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(132.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.value, key = { it.id }) { anime ->
                        PosterCard(anime, onClick = { onOpen(anime) }, width = 132.dp, subtitle = anime.year?.toString())
                    }
                }
            }
        }
    }
}
