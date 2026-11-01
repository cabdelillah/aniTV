package app.anitv.ui.presentation

import app.anitv.core.AniTvCore
import app.anitv.core.model.HomeFeed
import app.anitv.core.progress.WatchProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeModel(private val core: AniTvCore, private val scope: CoroutineScope) {
    private val _feed = MutableStateFlow<Load<HomeFeed>>(Load.Loading)
    val feed: StateFlow<Load<HomeFeed>> = _feed.asStateFlow()

    /** In-progress shows, most recent first; finished final episodes drop off. */
    val continueWatching: StateFlow<List<WatchProgress>> = core.progress.entries
        .map { entries ->
            entries.filterNot { it.isFinished && it.episodeNumber == it.anime.episodeCount }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            if (_feed.value !is Load.Ready) _feed.value = Load.Loading
            _feed.value = try {
                Load.Ready(core.aniList.home())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Failed(e.userMessage())
            }
        }
    }
}
