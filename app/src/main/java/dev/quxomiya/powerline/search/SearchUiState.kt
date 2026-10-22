package dev.quxomiya.powerline.search

data class SectionResults(
    val section: ResultSection,
    val items: List<SearchItem>
)

data class SearchUiState(
    val sections: List<SectionResults> = emptyList(),
    val selectedIndex: Int = 0,
    val isLoading: Boolean = false
) {
    // Sections flattened in display order; selectedIndex points into this list
    val items: List<SearchItem> = sections.flatMap { it.items }

    // Index of each section's first item in `items`
    private val sectionStarts: List<Int> = sections.runningFold(0) { start, section -> start + section.items.size }.dropLast(1)

    // Tab: first item of the next section, wrapping to the first
    fun nextSectionStart(): Int =
        sectionStarts.firstOrNull { it > selectedIndex } ?: sectionStarts.firstOrNull() ?: 0

    // Shift+Tab: first item of the previous section, wrapping to the last
    fun previousSectionStart(): Int {
        val current = sectionStarts.lastOrNull { it <= selectedIndex } ?: return 0
        return sectionStarts.lastOrNull { it < current } ?: sectionStarts.last()
    }
}
