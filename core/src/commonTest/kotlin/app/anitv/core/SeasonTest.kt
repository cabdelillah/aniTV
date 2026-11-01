package app.anitv.core

import app.anitv.core.util.Season
import kotlin.test.Test
import kotlin.test.assertEquals

class SeasonTest {
    @Test
    fun convertsEpochToYearMonth() {
        assertEquals(1970 to 1, Season.yearMonth(0))
        assertEquals(2026 to 10, Season.yearMonth(1_793_404_800_000)) // 2026-10-31
        assertEquals(2024 to 2, Season.yearMonth(1_709_164_800_000)) // 2024-02-29
    }

    @Test
    fun mapsMonthsToAniListSeasons() {
        assertEquals(Season("FALL", 2026), Season.current(1_793_404_800_000))
        assertEquals(Season("WINTER", 2026), Season.current(1_764_547_200_000)) // 2025-12-01
        assertEquals(Season("SPRING", 2025), Season.current(1_743_465_600_000)) // 2025-04-01
    }
}
