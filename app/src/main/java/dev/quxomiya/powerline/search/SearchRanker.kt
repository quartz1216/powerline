package dev.quxomiya.powerline.search

import java.text.Normalizer

// Usage data read once per search
data class RankingSignals(
    val favoriteApps: Set<String> = emptySet(),
    // Package name -> time-decayed launch count (see UserDataRepository.getAppFrecency)
    val appFrecency: Map<String, Double> = emptyMap()
)

// A search target normalized and split into words once (at load time for apps), so matching does
// no splitting per keystroke
data class MatchKey(val text: String) {
    val words: List<String> = text.split(WORD_SEPARATOR).filter { it.isNotEmpty() }
    val initials: String = words.joinToString("") { it.take(1) }

    companion object {
        // null when nothing is left to match after normalizing
        fun of(raw: String): MatchKey? = SearchRanker.normalize(raw).takeIf { it.isNotEmpty() }?.let(::MatchKey)
    }
}

private val WORD_SEPARATOR = Regex("[^\\p{L}\\p{N}]+")

object SearchRanker {

    const val EXACT = 1000
    const val PREFIX = 800
    const val WORD = 600
    const val SUBSTRING = 400
    const val FUZZY = 200

    // A favorite outranks one match tier, frecency at most one
    const val FAVORITE_BONUS = 300
    const val FRECENCY_PER_LAUNCH = 40
    const val MAX_FRECENCY_BONUS = 200

    // Provider score (match tier for apps) plus favorite and frecency bonuses for apps
    fun score(item: SearchItem, signals: RankingSignals): Int {
        var score = item.score
        val action = item.action
        if (action is SearchAction.LaunchApp) {
            if (action.packageName in signals.favoriteApps) score += FAVORITE_BONUS
            val frecency = signals.appFrecency[action.packageName] ?: 0.0
            score += (frecency * FRECENCY_PER_LAUNCH).toInt().coerceAtMost(MAX_FRECENCY_BONUS)
        }
        return score
    }

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase().trim()

    // Best tier for which the normalized query matches the key
    fun match(request: SearchRequest, key: MatchKey): Int {
        val query = request.normalizedQuery
        val target = key.text
        if (query.isEmpty() || target.isEmpty()) return 0
        if (target == query) return EXACT
        if (target.startsWith(query)) return PREFIX

        val words = key.words
        val initials = key.initials
        val wordMatch = words.any { it.startsWith(query) } ||
            (query.length >= 2 && initials.startsWith(query)) ||
            // "maps goo": every token starts a word
            (request.tokens.size > 1 && request.tokens.all { token -> words.any { it.startsWith(token) } })
        if (wordMatch) return WORD

        if (target.contains(query)) return SUBSTRING
        if (isSubsequence(query, target)) return FUZZY
        return 0
    }

    private fun isSubsequence(query: String, target: String): Boolean {
        var q = 0
        for (c in target) {
            if (q < query.length && query[q] == c) q++
        }
        return q == query.length
    }
}
