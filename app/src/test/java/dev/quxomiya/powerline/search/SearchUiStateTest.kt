package dev.quxomiya.powerline.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchUiStateTest {

    // apps: 0..2, calculator: 3, web: 4..5
    private val sections = listOf(
        section(ResultSection.APPS, 3),
        section(ResultSection.CALCULATOR, 1),
        section(ResultSection.WEB, 2)
    )

    private fun at(index: Int) = SearchUiState(sections = sections, selectedIndex = index)

    @Test
    fun tabGoesToNextSectionStart() {
        assertEquals(3, at(0).nextSectionStart())
        assertEquals(3, at(2).nextSectionStart())
        assertEquals(4, at(3).nextSectionStart())
    }

    @Test
    fun tabWrapsFromLastSection() {
        assertEquals(0, at(5).nextSectionStart())
    }

    @Test
    fun shiftTabGoesToPreviousSectionStart() {
        assertEquals(3, at(4).previousSectionStart())
        assertEquals(3, at(5).previousSectionStart())
        assertEquals(0, at(3).previousSectionStart())
    }

    @Test
    fun shiftTabWrapsFromFirstSection() {
        assertEquals(4, at(1).previousSectionStart())
    }

    @Test
    fun emptyStateStaysAtZero() {
        assertEquals(0, SearchUiState().nextSectionStart())
        assertEquals(0, SearchUiState().previousSectionStart())
    }

    private fun section(section: ResultSection, count: Int) = SectionResults(
        section,
        List(count) {
            SearchItem(
                id = "$section$it", providerId = "test", title = "$it", subtitle = null, icon = null,
                section = section, score = 0, action = SearchAction.CopyText("$section$it")
            )
        }
    )
}
