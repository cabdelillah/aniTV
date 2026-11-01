package app.anitv.ui.presentation

import app.anitv.core.AniTvCore
import app.anitv.core.model.Anime
import app.anitv.core.model.AnimeDetails
import app.anitv.core.model.Episode
import app.anitv.core.model.PlaybackRequest
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

class DetailsModel(
    private val core: AniTvCore,
    private val initial: Anime,
    private val scope: CoroutineScope,
) {
    private val _details = MutableStateFlow(AnimeDetails(initial))
    val details: StateFlow<AnimeDetails> = _details.asStateFlow()

    private val _episodes = MutableStateFlow<Load<List<Episode>>>(Load.Loading)
    val episodes: StateFlow<Load<List<Episode>>> = _episodes.asStateFlow()

    val progress: StateFlow<WatchProgress?> = core.progress.entries
        .map { entries -> entries.firstOrNull { it.anime.id == initial.id } }
        .stateIn(scope, SharingStarted.Eagerly, core.progress.get(initial.id))

    /** Episode whose streams are being resolved, to show a spinner on it. */
    private val _resolving = MutableStateFlow<Int?>(null)
    val resolving: StateFlow<Int?> = _resolving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        scope.launch {
            runCatching { core.aniList.details(initial.id) }.onSuccess { _details.value = it }
        }
        loadEpisodes()
    }

    fun loadEpisodes() {
        scope.launch {
            _episodes.value = Load.Loading
            _episodes.value = try {
                Load.Ready(core.episodes(initial))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Failed(e.userMessage())
            }
        }
    }

    /** The episode the main button should play: the in-progress one, the next one, or the first. */
    fun resumeTarget(episodes: List<Episode>, progress: WatchProgress?): Episode? {
        if (progress == null) return episodes.firstOrNull()
        val current = episodes.firstOrNull { it.number == progress.episodeNumber }
        if (current != null && !progress.isFinished) return current
        return episodes.firstOrNull { it.number > progress.episodeNumber } ?: current ?: episodes.firstOrNull()
    }

    fun play(episode: Episode, onReady: (PlaybackRequest) -> Unit) {
        if (_resolving.value != null) return
        scope.launch {
            _resolving.value = episode.number
            _error.value = null
            try {
                val stream = core.streams(episode).firstOrNull()
                if (stream == null) {
                    _error.value = "No streams found for episode ${episode.number}"
                } else {
                    val saved = progress.value
                    val start = if (saved != null && saved.episodeNumber == episode.number && !saved.isFinished) {
                        saved.positionMs
                    } else {
                        0L
                    }
                    onReady(PlaybackRequest(details.value.anime, episode, stream, start))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.userMessage()
            } finally {
                _resolving.value = null
            }
        }
    }

    fun dismissError() {
        _error.value = null
    }
}
