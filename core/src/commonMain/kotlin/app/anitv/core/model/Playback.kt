package app.anitv.core.model

import kotlinx.serialization.Serializable

/** An episode as listed by a source. */
@Serializable
data class Episode(
    /** Source-specific identifier passed back to [app.anitv.core.source.AnimeSource.streams]. */
    val id: String,
    val number: Int,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val isFiller: Boolean = false,
    val hasDub: Boolean = false,
)

@Serializable
data class Subtitle(
    val url: String,
    val language: String,
    /** `vtt`, `srt` or `ass`. Null lets the player sniff it. */
    val format: String? = null,
)

/** A playable video: a direct file or an HLS/DASH playlist. */
@Serializable
data class Stream(
    val url: String,
    /** For example `1080p`, or `auto` for adaptive playlists. */
    val quality: String,
    val server: String,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<Subtitle> = emptyList(),
    val isDub: Boolean = false,
)

/** OP/ED intervals in milliseconds. */
data class SkipTimes(
    val opening: LongRange? = null,
    val ending: LongRange? = null,
)

/** Everything a player needs to start an episode. */
data class PlaybackRequest(
    val anime: Anime,
    val episode: Episode,
    val stream: Stream,
    val startPositionMs: Long = 0,
)
