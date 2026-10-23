package dev.quxomiya.powerline.search

import android.content.Context
import android.util.Log
import dev.quxomiya.powerline.data.PowerlineDefaults
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

// Parses the query, runs every provider in parallel with a timeout, then ranks,
// deduplicates and trims the results. Empty queries are the standby state, not a search.
class SearchPipeline(
    private val context: Context,
    private val registry: ProviderRegistry,
    private val parser: QueryParser,
    private val signals: () -> RankingSignals
) {
    // Provider work runs outside the caller's scope so a provider stuck in blocking code
    // cannot hold the search past its timeout; it is cancelled and its result dropped.
    private val workScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun search(rawQuery: String): List<SectionResults> = coroutineScope {
        val request = parser.parse(rawQuery)
        if (request.text.isEmpty()) return@coroutineScope emptyList()

        val deadline = System.nanoTime() + PowerlineDefaults.SEARCH_TIMEOUT_MS * 1_000_000
        val providers = registry.providersFor(request)
        val signals = signals()

        val items = providers
            .map { provider -> async { runProvider(provider, request, deadline) } }
            .awaitAll()
            .flatten()

        val priorities = providers.associate { it.id to it.priority }
        val ranked = items
            .map { it to SearchRanker.score(it, signals) }
            .sortedWith(
                compareBy<Pair<SearchItem, Int>> { it.first.section.ordinal }
                    .thenByDescending { it.second }
                    .thenByDescending { priorities[it.first.providerId] ?: 0 }
                    .thenBy { it.first.title.lowercase() }
            )
            .map { it.first }

        var remaining = PowerlineDefaults.MAX_TOTAL_RESULTS
        SearchDeduplicator.deduplicate(ranked)
            .groupBy { it.section }
            .map { (section, sectionItems) ->
                val kept = sectionItems.take(minOf(PowerlineDefaults.MAX_RESULTS_PER_SECTION, remaining))
                remaining -= kept.size
                SectionResults(section, kept)
            }
            .filter { it.items.isNotEmpty() }
    }

    private suspend fun runProvider(
        provider: SearchProvider,
        request: SearchRequest,
        deadline: Long
    ): List<SearchItem> {
        val work = workScope.async { provider.search(request, context) }
        val untilDeadline = (deadline - System.nanoTime()) / 1_000_000
        val timeout = minOf(PowerlineDefaults.PROVIDER_TIMEOUT_MS, untilDeadline).coerceAtLeast(0)
        return try {
            withTimeoutOrNull(timeout) { work.await() } ?: run {
                Log.w(TAG, "${provider.id} timed out after $timeout ms")
                emptyList()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "${provider.id} failed", e)
            emptyList()
        } finally {
            work.cancel()
        }
    }

    fun close() {
        workScope.cancel()
    }

    private companion object {
        const val TAG = "SearchPipeline"
    }
}
