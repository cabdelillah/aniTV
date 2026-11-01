package app.anitv.core.source

/** Picks the source result that best matches an AniList title. */
object TitleMatcher {
    /** Results scoring below this are treated as "not found". */
    const val MIN_SCORE = 0.6

    fun bestMatch(titles: List<String>, candidates: List<SourceAnime>): SourceAnime? {
        if (titles.isEmpty()) return null
        return candidates
            .map { candidate -> candidate to titles.maxOf { similarity(it, candidate.title) } }
            .filter { it.second >= MIN_SCORE }
            .maxByOrNull { it.second }
            ?.first
    }

    /** Dice coefficient over character bigrams of normalized titles, from 0 to 1. */
    fun similarity(a: String, b: String): Double {
        val x = normalize(a)
        val y = normalize(b)
        if (x == y) return 1.0
        if (x.length < 2 || y.length < 2) return 0.0
        val bigrams = HashMap<String, Int>()
        for (i in 0 until x.length - 1) {
            val bg = x.substring(i, i + 2)
            bigrams[bg] = (bigrams[bg] ?: 0) + 1
        }
        var overlap = 0
        for (i in 0 until y.length - 1) {
            val bg = y.substring(i, i + 2)
            val count = bigrams[bg] ?: 0
            if (count > 0) {
                bigrams[bg] = count - 1
                overlap++
            }
        }
        return 2.0 * overlap / (x.length - 1 + y.length - 1)
    }

    fun normalize(title: String): String = title
        .lowercase()
        .replace(Regex("""\((tv|dub|sub)\)"""), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
}
