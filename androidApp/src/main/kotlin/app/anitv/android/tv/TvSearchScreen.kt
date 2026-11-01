package app.anitv.android.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.anitv.core.model.Anime
import app.anitv.ui.components.LoadingView
import app.anitv.ui.presentation.Load
import app.anitv.ui.presentation.SearchModel

@Composable
fun TvSearchScreen(model: SearchModel, onOpen: (Anime) -> Unit) {
    val query by model.query.collectAsState()
    val results by model.results.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top = 32.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = model::onQueryChange,
            placeholder = { androidx.compose.material3.Text("Search anime") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.padding(horizontal = TvGutter).widthIn(min = 480.dp).focusRequester(focus),
        )
        when (val state = results) {
            Load.Loading -> LoadingView()
            is Load.Failed -> Text(state.message, modifier = Modifier.padding(TvGutter))
            is Load.Ready -> LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                contentPadding = PaddingValues(horizontal = TvGutter, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(state.value, key = { it.id }) { anime -> TvPosterCard(anime, onClick = { onOpen(anime) }) }
            }
        }
    }
}
