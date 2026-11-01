package app.anitv.core.source

import app.anitv.core.model.Episode
import app.anitv.core.model.Stream

/** A search hit inside a source. */
data class SourceAnime(
    val id: String,
    val title: String,
    val coverUrl: String? = null,
)

/**
 * A provider of episodes and streams. aniTV ships no third-party sources; implement this
 * interface and register it with [SourceRegistry].
 */
interface AnimeSource {
    /** Stable identifier, used to remember the selected source. */
    val id: String
    val name: String

    suspend fun search(query: String): List<SourceAnime>

    suspend fun episodes(anime: SourceAnime, dub: Boolean = false): List<Episode>

    /** Streams for an episode, best first. */
    suspend fun streams(episode: Episode, dub: Boolean = false): List<Stream>
}
