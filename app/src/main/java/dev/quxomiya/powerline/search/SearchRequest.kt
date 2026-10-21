package dev.quxomiya.powerline.search

import dev.quxomiya.powerline.domain.models.CustomSearchEngine

data class SearchRequest(
    // As typed, for the search field
    val rawQuery: String,
    // Width-folded (NFKC) with whitespace collapsed; case kept for display and sharing
    val text: String,
    // `text` lowercased; what matching compares against
    val normalizedQuery: String,
    val tokens: List<String>,
    val mode: SearchMode,
    // Set in ALIAS mode
    val engine: CustomSearchEngine? = null,
    // In ALIAS mode, the part after the alias
    val aliasQuery: String = ""
)
