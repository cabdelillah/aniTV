package app.anitv.core.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** An anime broadcast season, as AniList names them. */
internal data class Season(val name: String, val year: Int) {
    companion object {
        @OptIn(ExperimentalTime::class)
        fun current(epochMillis: Long = Clock.System.now().toEpochMilliseconds()): Season {
            val (year, month) = yearMonth(epochMillis)
            val name = when (month) {
                12, 1, 2 -> "WINTER"
                in 3..5 -> "SPRING"
                in 6..8 -> "SUMMER"
                else -> "FALL"
            }
            // December belongs to the next year's winter season.
            return Season(name, if (month == 12) year + 1 else year)
        }

        /** UTC year and month (1-12) from epoch millis, using Howard Hinnant's civil-from-days. */
        fun yearMonth(epochMillis: Long): Pair<Int, Int> {
            val days = epochMillis.floorDiv(86_400_000L) + 719_468
            val era = days.floorDiv(146_097L)
            val dayOfEra = days - era * 146_097
            val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val mp = (5 * dayOfYear + 2) / 153
            val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
            val year = (yearOfEra + era * 400 + if (month <= 2) 1 else 0).toInt()
            return year to month
        }
    }
}
