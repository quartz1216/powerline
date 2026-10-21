package dev.quxomiya.powerline.search

import java.text.Normalizer

class QueryParser(private val engines: List<SearchEngine>) {

    private val whitespace = Regex("\\s+")

    fun parse(rawQuery: String): SearchRequest {
        // Full-width letters, digits and spaces become their ASCII forms
        val folded = Normalizer.normalize(rawQuery, Normalizer.Form.NFKC)
        // Keep a trailing space for now: "yt " (from an engine chip) is an alias with no terms yet
        val leading = folded.trimStart().replace(whitespace, " ")
        val text = leading.trimEnd()
        val normalized = text.lowercase()

        val engine = engines.firstOrNull { engine ->
            val alias = engine.alias
            !alias.isNullOrBlank() && leading.lowercase().startsWith("${alias.lowercase()} ")
        }

        return SearchRequest(
            rawQuery = rawQuery,
            text = text,
            normalizedQuery = normalized,
            tokens = if (normalized.isEmpty()) emptyList() else normalized.split(' '),
            mode = if (engine != null) SearchMode.ALIAS else SearchMode.TEXT,
            engine = engine,
            aliasQuery = engine?.let { leading.substring(it.alias!!.length + 1).trim() } ?: ""
        )
    }
}
