package app.anitv.core

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlin.time.TimeSource

/**
 * End-to-end check of everything the app depends on, run on a real device or machine:
 * AniList, AniSkip, cover images, the selected source and the stream it returns.
 * Each line is reported through [log]; the last line is `SELFTEST DONE pass=N fail=N`.
 */
object SelfTest {
    suspend fun run(core: AniTvCore, log: (String) -> Unit): Boolean {
        var passed = 0
        var failed = 0
        suspend fun step(name: String, block: suspend () -> String) {
            val start = TimeSource.Monotonic.markNow()
            try {
                val detail = block()
                passed++
                log("PASS $name (${start.elapsedNow().inWholeMilliseconds} ms): $detail")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                failed++
                log("FAIL $name (${start.elapsedNow().inWholeMilliseconds} ms): ${e::class.simpleName}: ${e.message}")
            }
        }

        log("SELFTEST START source=${core.sources.selected.value?.name}")
        var feed: app.anitv.core.model.HomeFeed? = null
        step("anilist home") {
            val home = core.aniList.home().also { feed = it }
            check(home.trending.isNotEmpty()) { "no trending anime" }
            "trending=${home.trending.size} seasonal=${home.seasonal.size} popular=${home.popular.size} top=${home.topRated.size}"
        }
        step("anilist search") {
            val results = core.aniList.search("Frieren")
            check(results.isNotEmpty()) { "no results" }
            "results=${results.size} first=\"${results.first().title}\""
        }
        val anime = feed?.trending?.firstOrNull()
        step("anilist details") {
            val details = core.aniList.details(anime?.id ?: FRIEREN_ID)
            "\"${details.anime.title}\" recommendations=${details.recommendations.size}"
        }
        step("cover image") {
            val url = anime?.coverUrl ?: error("no cover url")
            val response = core.http.get(url)
            check(response.status.isSuccess()) { "HTTP ${response.status.value} for $url" }
            "HTTP ${response.status.value} ${response.headers["Content-Type"]}"
        }
        step("aniskip") {
            val skip = core.aniSkip.skipTimes(FRIEREN_MAL_ID, 1) ?: error("no skip times (AniSkip unreachable?)")
            "opening=${skip.opening} ending=${skip.ending}"
        }
        step("source episodes + stream") {
            val target = anime ?: error("no anime to resolve")
            val episodes = core.episodes(target)
            check(episodes.isNotEmpty()) { "no episodes" }
            val stream = core.streams(episodes.first()).firstOrNull() ?: error("no streams")
            val response = core.http.get(stream.url) {
                stream.headers.forEach { (k, v) -> header(k, v) }
            }
            check(response.status.isSuccess()) { "HTTP ${response.status.value} for ${stream.url}" }
            val head = response.bodyAsText().take(7)
            "episodes=${episodes.size} stream=${stream.url} HTTP ${response.status.value} starts=\"$head\""
        }
        log("SELFTEST DONE pass=$passed fail=$failed")
        return failed == 0
    }

    private const val FRIEREN_ID = 154587
    private const val FRIEREN_MAL_ID = 52991
}
