package app.anitv.core.model

import kotlinx.serialization.Serializable

/** An anime as described by AniList (metadata only, no streams). */
@Serializable
data class Anime(
    val id: Int,
    val malId: Int? = null,
    val title: String,
    val titleRomaji: String? = null,
    val titleEnglish: String? = null,
    val titleNative: String? = null,
    val synonyms: List<String> = emptyList(),
    val coverUrl: String? = null,
    val bannerUrl: String? = null,
    /** Dominant cover color as `#rrggbb`, handy for placeholders and backgrounds. */
    val color: String? = null,
    val description: String? = null,
    val episodeCount: Int? = null,
    val episodeMinutes: Int? = null,
    val genres: List<String> = emptyList(),
    /** Average score from 0 to 100. */
    val score: Int? = null,
    val year: Int? = null,
    val season: String? = null,
    val format: String? = null,
    val status: String? = null,
    val nextEpisode: Int? = null,
    /** Unix seconds. */
    val nextEpisodeAiringAt: Long? = null,
) {
    /** Every title we know, best first, for matching against sources. */
    val searchTitles: List<String>
        get() = (listOfNotNull(titleEnglish, titleRomaji, title) + synonyms)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
}

/** Anime plus the extra sections shown on its details page. */
data class AnimeDetails(
    val anime: Anime,
    val recommendations: List<Anime> = emptyList(),
)

/** Rows shown on the home screen. */
data class HomeFeed(
    val trending: List<Anime>,
    val seasonal: List<Anime>,
    val popular: List<Anime>,
    val topRated: List<Anime>,
)
