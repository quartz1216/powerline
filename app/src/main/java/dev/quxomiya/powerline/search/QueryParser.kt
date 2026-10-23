package dev.quxomiya.powerline.search

import java.text.Normalizer

// Engines and share targets are read on every parse, so edits in settings apply at once
class QueryParser(
    private val engines: () -> List<SearchEngine>,
    private val shareTargets: () -> List<ShareTarget>
) {

    private val whitespace = Regex("\\s+")

    fun parse(rawQuery: String): SearchRequest {
        // Full-width letters, digits and spaces become their ASCII forms
        val folded = Normalizer.normalize(rawQuery, Normalizer.Form.NFKC)
        // Keep a trailing space for now: "yt " (from an engine chip) is an alias with no terms yet
        val leading = folded.trimStart().replace(whitespace, " ")
        val text = leading.trimEnd()
        val normalized = text.lowercase()
        val lowerLeading = leading.lowercase()

        fun matches(alias: String?) = !alias.isNullOrBlank() && lowerLeading.startsWith("${alias.lowercase()} ")

        // The longest alias wins, so "ytm lofi" is never read as "yt" + "m lofi"
        val engine = engines().filter { matches(it.alias) }.maxByOrNull { it.alias!!.length }
        val shareTarget = shareTargets().filter { matches(it.alias) }.maxByOrNull { it.alias.length }
            ?.takeIf { engine == null || it.alias.length > engine.alias!!.length }
        val alias = shareTarget?.alias ?: engine?.alias

        return SearchRequest(
            text = text,
            normalizedQuery = normalized,
            tokens = if (normalized.isEmpty()) emptyList() else normalized.split(' '),
            engine = engine.takeIf { shareTarget == null },
            shareTarget = shareTarget,
            aliasQuery = alias?.let { leading.substring(it.length + 1).trim() } ?: ""
        )
    }

    // An alias query split for the search field: the parsed request, and the raw text after the
    // alias and its space (as typed, not normalized). Null when the query has no alias.
    fun splitAlias(rawQuery: String): Pair<SearchRequest, String>? {
        val request = parse(rawQuery)
        if (!request.isAlias) return null
        // An alias has no spaces, so it ends at the first one (full-width included)
        val trimmed = rawQuery.trimStart()
        val gap = trimmed.indexOfFirst { it.isWhitespace() }
        return request to trimmed.substring(gap + 1).trimStart()
    }
}
