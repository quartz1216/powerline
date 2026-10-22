package dev.quxomiya.powerline.search

enum class SearchMode {
    TEXT,

    // The query starts with an alias: a search engine's ("yt laneige", only Web runs)
    // or a share target's ("cl hello", only Share runs)
    ALIAS
}
