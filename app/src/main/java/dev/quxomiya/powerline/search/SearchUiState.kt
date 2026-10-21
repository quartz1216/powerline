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
}
