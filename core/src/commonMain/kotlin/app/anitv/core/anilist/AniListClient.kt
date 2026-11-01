package app.anitv.core.anilist

import app.anitv.core.model.Anime
import app.anitv.core.model.AnimeDetails
import app.anitv.core.model.HomeFeed
import app.anitv.core.util.Html
import app.anitv.core.util.Season
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

class AniListException(message: String) : Exception(message)

/** Read-only AniList GraphQL client. */
class AniListClient(
    private val http: HttpClient,
    private val endpoint: String = "https://graphql.anilist.co",
) {
    suspend fun home(perPage: Int = 20): HomeFeed {
        val season = Season.current()
        val data: HomeData = query(
            AniListQueries.HOME,
            mapOf(
                "season" to JsonPrimitive(season.name),
                "year" to JsonPrimitive(season.year),
                "perPage" to JsonPrimitive(perPage),
            ),
        )
        return HomeFeed(
            trending = data.trending.media.map { it.toAnime() },
            seasonal = data.seasonal.media.map { it.toAnime() },
            popular = data.popular.media.map { it.toAnime() },
            topRated = data.topRated.media.map { it.toAnime() },
        )
    }

    suspend fun search(text: String, perPage: Int = 30): List<Anime> {
        if (text.isBlank()) return emptyList()
        val data: SearchData = query(
            AniListQueries.SEARCH,
            mapOf("search" to JsonPrimitive(text.trim()), "perPage" to JsonPrimitive(perPage)),
        )
        return data.Page.media.map { it.toAnime() }
    }

    suspend fun details(id: Int): AnimeDetails {
        val data: DetailsData = query(AniListQueries.DETAILS, mapOf("id" to JsonPrimitive(id)))
        val media = data.Media ?: throw AniListException("Anime $id not found")
        return AnimeDetails(
            anime = media.toMedia().toAnime(),
            recommendations = media.recommendations?.nodes.orEmpty()
                .mapNotNull { it.mediaRecommendation?.toAnime() },
        )
    }

    private suspend inline fun <reified T> query(query: String, variables: Map<String, JsonElement>): T {
        val response = http.post(endpoint) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(GraphQlRequest(query.trimIndent(), variables))
        }
        if (!response.status.isSuccess() && response.status.value != 404) {
            throw AniListException("AniList returned HTTP ${response.status.value}")
        }
        val body: GraphQlResponse<T> = response.body()
        body.errors?.firstOrNull()?.let { if (body.data == null) throw AniListException(it.message) }
        return body.data ?: throw AniListException("AniList returned no data")
    }
}

internal fun MediaDto.toAnime() = Anime(
    id = id,
    malId = idMal,
    title = title.english ?: title.userPreferred ?: title.romaji ?: title.native ?: "Untitled",
    titleRomaji = title.romaji,
    titleEnglish = title.english,
    titleNative = title.native,
    synonyms = synonyms,
    coverUrl = coverImage?.extraLarge ?: coverImage?.large,
    bannerUrl = bannerImage,
    color = coverImage?.color,
    description = description?.let(Html::toPlainText),
    episodeCount = episodes,
    episodeMinutes = duration,
    genres = genres,
    score = averageScore,
    year = seasonYear,
    season = season,
    format = format,
    status = status,
    nextEpisode = nextAiringEpisode?.episode,
    nextEpisodeAiringAt = nextAiringEpisode?.airingAt,
)
