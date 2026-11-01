package app.anitv.core.anilist

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class GraphQlRequest(val query: String, val variables: Map<String, JsonElement> = emptyMap())

@Serializable
internal data class GraphQlResponse<T>(val data: T? = null, val errors: List<GraphQlError>? = null)

@Serializable
internal data class GraphQlError(val message: String, val status: Int? = null)

@Serializable
internal data class PageDto(val media: List<MediaDto> = emptyList())

@Serializable
internal data class HomeData(
    val trending: PageDto = PageDto(),
    val seasonal: PageDto = PageDto(),
    val popular: PageDto = PageDto(),
    val topRated: PageDto = PageDto(),
)

@Serializable
internal data class SearchData(val Page: PageDto = PageDto())

@Serializable
internal data class DetailsData(val Media: DetailsMediaDto? = null)

@Serializable
internal data class TitleDto(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
    val userPreferred: String? = null,
)

@Serializable
internal data class CoverDto(val extraLarge: String? = null, val large: String? = null, val color: String? = null)

@Serializable
internal data class AiringDto(val episode: Int, val airingAt: Long)

@Serializable
internal data class MediaDto(
    val id: Int,
    val idMal: Int? = null,
    val title: TitleDto = TitleDto(),
    val synonyms: List<String> = emptyList(),
    val coverImage: CoverDto? = null,
    val bannerImage: String? = null,
    val description: String? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val genres: List<String> = emptyList(),
    val averageScore: Int? = null,
    val seasonYear: Int? = null,
    val season: String? = null,
    val format: String? = null,
    val status: String? = null,
    val nextAiringEpisode: AiringDto? = null,
)

@Serializable
internal data class DetailsMediaDto(
    val id: Int,
    val idMal: Int? = null,
    val title: TitleDto = TitleDto(),
    val synonyms: List<String> = emptyList(),
    val coverImage: CoverDto? = null,
    val bannerImage: String? = null,
    val description: String? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val genres: List<String> = emptyList(),
    val averageScore: Int? = null,
    val seasonYear: Int? = null,
    val season: String? = null,
    val format: String? = null,
    val status: String? = null,
    val nextAiringEpisode: AiringDto? = null,
    val recommendations: RecommendationsDto? = null,
) {
    fun toMedia() = MediaDto(
        id, idMal, title, synonyms, coverImage, bannerImage, description, episodes, duration,
        genres, averageScore, seasonYear, season, format, status, nextAiringEpisode,
    )
}

@Serializable
internal data class RecommendationsDto(val nodes: List<RecommendationNode> = emptyList())

@Serializable
internal data class RecommendationNode(val mediaRecommendation: MediaDto? = null)
