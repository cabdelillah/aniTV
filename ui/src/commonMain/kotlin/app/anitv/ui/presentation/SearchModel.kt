package app.anitv.ui.presentation

import app.anitv.core.AniTvCore
import app.anitv.core.model.Anime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchModel(private val core: AniTvCore, scope: CoroutineScope) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<Load<List<Anime>>>(Load.Ready(emptyList()))
    val results: StateFlow<Load<List<Anime>>> = _results.asStateFlow()

    init {
        scope.launch {
            _query.debounce(350).distinctUntilChanged().collectLatest { text ->
                if (text.isBlank()) {
                    _results.value = Load.Ready(emptyList())
                    return@collectLatest
                }
                _results.value = Load.Loading
                _results.value = try {
                    Load.Ready(core.aniList.search(text))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Load.Failed(e.userMessage())
                }
            }
        }
    }

    fun onQueryChange(text: String) {
        _query.value = text
    }
}
