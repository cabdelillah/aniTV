package app.anitv.core

import app.anitv.core.progress.InMemoryTextStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientsTest {
    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK) = HttpClient(MockEngine {
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private val media = """
        {"id": 154587, "idMal": 52991,
         "title": {"romaji": "Sousou no Frieren", "english": "Frieren: Beyond Journey's End", "native": "葬送のフリーレン"},
         "synonyms": ["Frieren"], "coverImage": {"extraLarge": "https://img/x.jpg", "color": "#e4a15d"},
         "description": "Line one<br><br>Line <i>two</i> &amp; more", "episodes": 28, "duration": 24,
         "genres": ["Adventure"], "averageScore": 90, "seasonYear": 2023, "season": "FALL",
         "format": "TV", "status": "FINISHED", "nextAiringEpisode": null}
    """

    @Test
    fun parsesHomeFeed() = runTest {
        val body = """{"data": {"trending": {"media": [$media]}, "seasonal": {"media": []},
            "popular": {"media": [$media, $media]}, "topRated": {"media": []}}}"""
        val core = AniTvCore(InMemoryTextStorage(), http = client(body))
        val feed = core.aniList.home()
        assertEquals(1, feed.trending.size)
        assertEquals(2, feed.popular.size)
        val anime = feed.trending.first()
        assertEquals("Frieren: Beyond Journey's End", anime.title)
        assertEquals(52991, anime.malId)
        assertEquals("Line one\n\nLine two & more", anime.description)
        assertEquals(listOf("Frieren: Beyond Journey's End", "Sousou no Frieren", "Frieren"), anime.searchTitles)
    }

    @Test
    fun parsesDetailsWithRecommendations() = runTest {
        val body = """{"data": {"Media": ${media.trimEnd().dropLast(1)},
            "recommendations": {"nodes": [{"mediaRecommendation": $media}, {"mediaRecommendation": null}]}}}}"""
        val details = AniTvCore(InMemoryTextStorage(), http = client(body)).aniList.details(154587)
        assertEquals(154587, details.anime.id)
        assertEquals(1, details.recommendations.size)
    }

    @Test
    fun parsesAniSkip() = runTest {
        val body = """{"found": true, "results": [
            {"interval": {"startTime": 90.5, "endTime": 180.2}, "skipType": "op", "skipId": "a", "episodeLength": 1420},
            {"interval": {"startTime": 1300, "endTime": 1390}, "skipType": "ed", "skipId": "b", "episodeLength": 1420}]}"""
        val skip = AniTvCore(InMemoryTextStorage(), http = client(body)).aniSkip.skipTimes(52991, 1)
        assertEquals(90_500L..180_200L, skip?.opening)
        assertEquals(1_300_000L..1_390_000L, skip?.ending)
    }

    @Test
    fun aniSkipMissingDataIsNull() = runTest {
        val core = AniTvCore(InMemoryTextStorage(), http = client("""{"found": false}""", HttpStatusCode.NotFound))
        assertNull(core.aniSkip.skipTimes(1, 1))
    }

    @Test
    fun resolvesEpisodesThroughDemoSourceAndRemembersProgress() = runTest {
        val body = """{"data": {"trending": {"media": [$media]}}}"""
        val storage = InMemoryTextStorage()
        val core = AniTvCore(storage, http = client(body))
        val anime = core.aniList.home().trending.first()
        val episodes = core.episodes(anime)
        assertEquals(3, episodes.size)
        assertTrue(core.streams(episodes.first()).first().url.endsWith(".m3u8"))

        core.progress.save(anime, episodeNumber = 2, positionMs = 60_000, durationMs = 600_000)
        val reloaded = AniTvCore(storage, http = client(body)).progress.get(anime.id)
        assertEquals(2, reloaded?.episodeNumber)
        assertEquals(0.1f, reloaded?.fraction)
    }
}
