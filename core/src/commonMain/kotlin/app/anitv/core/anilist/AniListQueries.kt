package app.anitv.core.anilist

internal object AniListQueries {
    private const val MEDIA_FIELDS = """
        id idMal
        title { romaji english native userPreferred }
        synonyms
        coverImage { extraLarge large color }
        bannerImage
        description(asHtml: false)
        episodes duration genres averageScore seasonYear season format status
        nextAiringEpisode { episode airingAt }
    """

    private const val FRAGMENT = "fragment media on Media { $MEDIA_FIELDS }"

    /** All home rows in a single request using aliases. */
    const val HOME = """
        query (${'$'}season: MediaSeason, ${'$'}year: Int, ${'$'}perPage: Int) {
          trending: Page(perPage: ${'$'}perPage) { media(sort: TRENDING_DESC, type: ANIME, isAdult: false) { ...media } }
          seasonal: Page(perPage: ${'$'}perPage) { media(sort: POPULARITY_DESC, type: ANIME, season: ${'$'}season, seasonYear: ${'$'}year, isAdult: false) { ...media } }
          popular: Page(perPage: ${'$'}perPage) { media(sort: POPULARITY_DESC, type: ANIME, isAdult: false) { ...media } }
          topRated: Page(perPage: ${'$'}perPage) { media(sort: SCORE_DESC, type: ANIME, isAdult: false) { ...media } }
        }
        $FRAGMENT
    """

    const val SEARCH = """
        query (${'$'}search: String, ${'$'}perPage: Int) {
          Page(perPage: ${'$'}perPage) { media(search: ${'$'}search, type: ANIME, isAdult: false, sort: SEARCH_MATCH) { ...media } }
        }
        $FRAGMENT
    """

    const val DETAILS = """
        query (${'$'}id: Int) {
          Media(id: ${'$'}id, type: ANIME) {
            ...media
            recommendations(perPage: 15, sort: RATING_DESC) { nodes { mediaRecommendation { ...media } } }
          }
        }
        $FRAGMENT
    """
}
