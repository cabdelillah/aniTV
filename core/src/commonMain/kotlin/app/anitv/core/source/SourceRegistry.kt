package app.anitv.core.source

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the installed sources and which one is active. */
class SourceRegistry(sources: List<AnimeSource>) {
    private val _sources = MutableStateFlow(sources)
    val sources: StateFlow<List<AnimeSource>> = _sources.asStateFlow()

    private val _selected = MutableStateFlow(sources.firstOrNull())
    val selected: StateFlow<AnimeSource?> = _selected.asStateFlow()

    fun register(source: AnimeSource) {
        _sources.value = _sources.value.filterNot { it.id == source.id } + source
        if (_selected.value == null) _selected.value = source
    }

    fun select(id: String) {
        _sources.value.firstOrNull { it.id == id }?.let { _selected.value = it }
    }
}
