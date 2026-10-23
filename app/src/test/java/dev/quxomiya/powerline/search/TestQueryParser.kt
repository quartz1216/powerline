package dev.quxomiya.powerline.search

// A parser over fixed lists, for tests
fun QueryParser(engines: List<SearchEngine>, shareTargets: List<ShareTarget> = emptyList()) =
    QueryParser({ engines }, { shareTargets })
