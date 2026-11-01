package app.anitv.core

import app.anitv.core.anilist.AniListClient
import app.anitv.core.aniskip.AniSkipClient
import app.anitv.core.model.Anime
import app.anitv.core.model.Episode
import app.anitv.core.model.SkipTimes
import app.anitv.core.model.Stream
import app.anitv.core.progress.TextStorage
import app.anitv.core.progress.WatchProgressStore
import app.anitv.core.source.AnimeSource
import app.anitv.core.source.DemoSource
import app.anitv.core.source.SourceRegistry
import app.anitv.core.source.TitleMatcher
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class SourceNotFoundException(title: String, source: String) :
    Exception("\"$title\" was not found on $source")

/** The app's single entry point into data: metadata, sources, skip times and progress. */
class AniTvCore(
    progressStorage: TextStorage,
    sources: List<AnimeSource> = listOf(DemoSource()),
    val http: HttpClient = defaultHttpClient(),
) {
    val aniList = AniListClient(http)
    val aniSkip = AniSkipClient(http)
    val sources = SourceRegistry(sources)
    val progress = WatchProgressStore(progressStorage)

    private val cacheLock = Mutex()
    private val episodeCache = HashMap<Pair<String, Int>, List<Episode>>()

    /** Episodes of [anime] on the selected source, matched by title. */
    suspend fun episodes(anime: Anime): List<Episode> {
        val source = this.sources.selected.value ?: throw IllegalStateException("No source installed")
        val key = source.id to anime.id
        cacheLock.withLock { episodeCache[key] }?.let { return it }

        val match = anime.searchTitles.take(3).firstNotNullOfOrNull { title ->
            TitleMatcher.bestMatch(anime.searchTitles, source.search(title))
        } ?: throw SourceNotFoundException(anime.title, source.name)

        val episodes = source.episodes(match).sortedBy { it.number }
        cacheLock.withLock { episodeCache[key] = episodes }
        return episodes
    }

    suspend fun streams(episode: Episode): List<Stream> {
        val source = this.sources.selected.value ?: throw IllegalStateException("No source installed")
        return source.streams(episode)
    }

    suspend fun skipTimes(anime: Anime, episode: Episode): SkipTimes? =
        anime.malId?.let { aniSkip.skipTimes(it, episode.number) }

    fun close() = http.close()

    companion object {
        const val USER_AGENT = "aniTV/0.1 (+https://github.com/cabdelillah/aniTV)"

        fun defaultHttpClient() = HttpClient {
            expectSuccess = false
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 20_000
            }
            defaultRequest { headers.append(HttpHeaders.UserAgent, USER_AGENT) }
        }
    }
}
