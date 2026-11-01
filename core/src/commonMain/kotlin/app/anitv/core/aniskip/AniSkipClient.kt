package app.anitv.core.aniskip

import app.anitv.core.model.SkipTimes
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/** Opening/ending timestamps from https://aniskip.com, keyed by MyAnimeList id. */
class AniSkipClient(
    private val http: HttpClient,
    private val baseUrl: String = "https://api.aniskip.com/v2",
) {
    /** Returns null when AniSkip has no data or is unreachable; skipping is best effort. */
    suspend fun skipTimes(malId: Int, episode: Int, episodeLengthSeconds: Double = 0.0): SkipTimes? =
        runCatching {
            val response = http.get("$baseUrl/skip-times/$malId/$episode") {
                parameter("types", "op")
                parameter("types", "ed")
                parameter("episodeLength", episodeLengthSeconds)
            }
            if (!response.status.isSuccess()) return null
            val body: SkipResponse = response.body()
            if (!body.found) return null
            fun range(type: String) = body.results.firstOrNull { it.skipType == type }?.interval?.let {
                (it.startTime * 1000).toLong()..(it.endTime * 1000).toLong()
            }
            SkipTimes(opening = range("op"), ending = range("ed"))
        }.getOrNull()

    @Serializable
    private data class SkipResponse(val found: Boolean = false, val results: List<SkipResult> = emptyList())

    @Serializable
    private data class SkipResult(val skipType: String, val interval: Interval)

    @Serializable
    private data class Interval(val startTime: Double, val endTime: Double)
}
