package dev.quxomiya.powerline.search

data class SearchRequest(
    // Width-folded (NFKC) with whitespace collapsed; case kept for display and sharing
    val text: String,
    // `text` lowercased; what matching compares against
    val normalizedQuery: String,
    val tokens: List<String>,
    // Set when the query starts with an alias: a search engine's ("yt laneige", only Web runs)
    // or a share target's ("cl hello", only Share runs). Never both.
    val engine: SearchEngine? = null,
    val shareTarget: ShareTarget? = null,
    // With an alias, the text after it
    val aliasQuery: String = ""
) {
    val isAlias: Boolean get() = engine != null || shareTarget != null
}
