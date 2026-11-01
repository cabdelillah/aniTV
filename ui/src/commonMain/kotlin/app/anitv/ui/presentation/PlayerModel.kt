package app.anitv.ui.presentation

import app.anitv.core.AniTvCore
import app.anitv.core.model.PlaybackRequest
import app.anitv.core.model.SkipTimes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the skip button should offer at the current position. */
data class SkipAction(val label: String, val targetMs: Long)

class PlayerModel(
    private val core: AniTvCore,
    val request: PlaybackRequest,
    private val scope: CoroutineScope,
) {
    private val _skipTimes = MutableStateFlow<SkipTimes?>(null)
    val skipTimes: StateFlow<SkipTimes?> = _skipTimes.asStateFlow()

    private var lastSavedMs = -1L

    init {
        scope.launch { _skipTimes.value = core.skipTimes(request.anime, request.episode) }
    }

    fun skipActionAt(positionMs: Long): SkipAction? {
        val skip = _skipTimes.value ?: return null
        skip.opening?.let { if (positionMs in it) return SkipAction("Skip intro", it.last) }
        skip.ending?.let { if (positionMs in it) return SkipAction("Skip outro", it.last) }
        return null
    }

    /** Called periodically by the player; persists at most every 5 seconds unless forced. */
    fun onProgress(positionMs: Long, durationMs: Long, force: Boolean = false) {
        if (durationMs <= 0 || positionMs < 0) return
        if (!force && lastSavedMs >= 0 && kotlin.math.abs(positionMs - lastSavedMs) < 5_000) return
        lastSavedMs = positionMs
        core.progress.save(request.anime, request.episode.number, positionMs, durationMs)
    }

    /** Resolves the next episode's stream, or returns null when this was the last one. */
    suspend fun nextEpisode(): PlaybackRequest? = runCatching {
        val episodes = core.episodes(request.anime)
        val next = episodes.firstOrNull { it.number > request.episode.number } ?: return null
        val stream = core.streams(next).firstOrNull() ?: return null
        PlaybackRequest(request.anime, next, stream)
    }.getOrNull()
}
