package dev.quxomiya.powerline.search

data class SearchRequest(
    // Width-folded (NFKC) with whitespace collapsed; case kept for display and sharing
    val text: String,
    // `text` lowercased; what matching compares against
    val normalizedQuery: String,
    val tokens: List<String>,
    val mode: SearchMode,
    // In ALIAS mode, exactly one of these is set
    val engine: SearchEngine? = null,
    val shareTarget: ShareTarget? = null,
    // In ALIAS mode, the part after the alias
    val aliasQuery: String = ""
)
