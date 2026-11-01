package app.anitv.core

import app.anitv.core.source.SourceAnime
import app.anitv.core.source.TitleMatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TitleMatcherTest {
    private val candidates = listOf(
        SourceAnime("1", "Frieren: Beyond Journey's End"),
        SourceAnime("2", "Frieren: Beyond Journey's End (Dub)"),
        SourceAnime("3", "Fullmetal Alchemist: Brotherhood"),
    )

    @Test
    fun picksExactTitleOverLooserMatches() {
        val match = TitleMatcher.bestMatch(listOf("Frieren: Beyond Journey’s End", "Sousou no Frieren"), candidates)
        assertEquals("1", match?.id)
    }

    @Test
    fun matchesAcrossAlternativeTitles() {
        val match = TitleMatcher.bestMatch(listOf("Hagane no Renkinjutsushi", "Fullmetal Alchemist Brotherhood"), candidates)
        assertEquals("3", match?.id)
    }

    @Test
    fun returnsNullWhenNothingIsClose() {
        assertNull(TitleMatcher.bestMatch(listOf("One Piece"), candidates))
    }
}
