package app.anitv.core.source

import app.anitv.core.model.Episode
import app.anitv.core.model.Stream

/**
 * Plays public test streams for any title, so the whole app can be exercised end to end
 * before a real source is plugged in.
 */
class DemoSource : AnimeSource {
    override val id = "demo"
    override val name = "Demo (test streams)"

    override suspend fun search(query: String) =
        listOf(SourceAnime(id = query, title = query))

    override suspend fun episodes(anime: SourceAnime, dub: Boolean) =
        STREAMS.indices.map { i -> Episode(id = i.toString(), number = i + 1, title = TITLES[i]) }

    override suspend fun streams(episode: Episode, dub: Boolean) =
        listOf(Stream(url = STREAMS[episode.id.toInt()], quality = "auto", server = "Demo"))

    private companion object {
        val TITLES = listOf("Big Buck Bunny", "Sintel", "Tears of Steel")
        val STREAMS = listOf(
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
        )
    }
}
