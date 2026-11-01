package app.anitv.core.progress

import app.anitv.core.model.Anime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class WatchProgress(
    val anime: Anime,
    val episodeNumber: Int,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long,
) {
    val fraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val isFinished: Boolean get() = durationMs > 0 && positionMs >= durationMs * FINISHED_FRACTION

    companion object {
        const val FINISHED_FRACTION = 0.9
    }
}

/** Where the store persists its JSON. */
interface TextStorage {
    fun read(): String?
    fun write(text: String)
}

/** Remembers the last watched episode and position for each anime. */
class WatchProgressStore(private val storage: TextStorage) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _entries = MutableStateFlow(load())

    /** Most recently watched first. */
    val entries: StateFlow<List<WatchProgress>> = _entries.asStateFlow()

    fun get(animeId: Int): WatchProgress? = _entries.value.firstOrNull { it.anime.id == animeId }

    @OptIn(ExperimentalTime::class)
    fun save(anime: Anime, episodeNumber: Int, positionMs: Long, durationMs: Long) {
        val entry = WatchProgress(anime, episodeNumber, positionMs, durationMs, Clock.System.now().toEpochMilliseconds())
        _entries.update { current -> listOf(entry) + current.filterNot { it.anime.id == anime.id } }
        persist()
    }

    fun remove(animeId: Int) {
        _entries.update { current -> current.filterNot { it.anime.id == animeId } }
        persist()
    }

    private fun load(): List<WatchProgress> = runCatching {
        storage.read()?.let { json.decodeFromString<List<WatchProgress>>(it) }
    }.getOrNull().orEmpty().sortedByDescending { it.updatedAtMs }

    private fun persist() {
        runCatching { storage.write(json.encodeToString(_entries.value)) }
    }
}

class InMemoryTextStorage(private var text: String? = null) : TextStorage {
    override fun read() = text
    override fun write(text: String) { this.text = text }
}
