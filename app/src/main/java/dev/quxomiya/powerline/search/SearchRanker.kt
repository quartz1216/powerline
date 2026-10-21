package dev.quxomiya.powerline.search

import java.text.Normalizer

// Usage data read once per search
data class RankingSignals(
    val favoriteApps: Set<String> = emptySet(),
    // Package name -> launch weight (0–100, see UserDataRepository.recordAppLaunch)
    val appWeights: Map<String, Int> = emptyMap()
)

object SearchRanker {

    const val EXACT = 1000
    const val PREFIX = 800
    const val WORD = 600
    const val SUBSTRING = 400
    const val FUZZY = 200

    // A favorite outranks one match tier, frecency at most one
    private const val FAVORITE_BONUS = 300
    private const val WEIGHT_MULTIPLIER = 2

    private val wordSeparator = Regex("[^\\p{L}\\p{N}]+")

    fun score(item: SearchItem, request: SearchRequest, signals: RankingSignals): Int {
        var score = item.score
        if (item.rankByMatch) {
            score += (listOf(item.title) + item.keywords).maxOf { match(request, normalize(it)) }
        }
        val action = item.action
        if (action is SearchAction.LaunchApp) {
            if (action.packageName in signals.favoriteApps) score += FAVORITE_BONUS
            score += (signals.appWeights[action.packageName] ?: 0) * WEIGHT_MULTIPLIER
        }
        return score
    }

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase().trim()

    // Best tier for which the normalized query matches the normalized target
    fun match(request: SearchRequest, target: String): Int {
        val query = request.normalizedQuery
        if (query.isEmpty() || target.isEmpty()) return 0
        if (target == query) return EXACT
        if (target.startsWith(query)) return PREFIX

        val words = target.split(wordSeparator).filter { it.isNotEmpty() }
        val initials = words.joinToString("") { it.take(1) }
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
