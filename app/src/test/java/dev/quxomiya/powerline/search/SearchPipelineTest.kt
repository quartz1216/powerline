package dev.quxomiya.powerline.search

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.test.core.app.ApplicationProvider
import dev.quxomiya.powerline.data.PowerlineDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SearchPipelineTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val pipelines = mutableListOf<SearchPipeline>()

    @After
    fun tearDown() {
        pipelines.forEach { it.close() }
    }

    @Test
    fun emptyQueryRunsNoProvider() = runBlocking {
        val provider = FakeProvider("apps", ResultSection.APPS)
        val results = pipeline(provider).search("   ")
        assertEquals(emptyList<SectionResults>(), results)
        assertEquals(0, provider.calls)
    }

    @Test
    fun sectionsFollowDisplayOrder() = runBlocking {
        val results = pipeline(
            FakeProvider("share", ResultSection.SHARE, items = { listOf(it.item("s")) }),
            FakeProvider("web", ResultSection.WEB, items = { listOf(it.item("w")) }),
            FakeProvider("apps", ResultSection.APPS, items = { listOf(it.item("a")) }),
            FakeProvider("calc", ResultSection.CALCULATOR, items = { listOf(it.item("c")) })
        ).search("q")
        assertEquals(
            listOf(ResultSection.APPS, ResultSection.CALCULATOR, ResultSection.WEB, ResultSection.SHARE),
            results.map { it.section }
        )
    }

    @Test
    fun itemsSortedByScoreWithinSection() = runBlocking {
        val provider = FakeProvider("apps", ResultSection.APPS, items = {
            listOf(it.item("low", score = 200), it.item("high", score = 1000), it.item("mid", score = 600))
        })
        val results = pipeline(provider).search("q")
        assertEquals(listOf("high", "mid", "low"), results.single().items.map { it.id })
    }

    @Test
    fun favoriteOutranksOneTier() = runBlocking {
        val provider = FakeProvider("apps", ResultSection.APPS, items = {
            listOf(
                it.item("prefix", score = SearchRanker.PREFIX, action = SearchAction.LaunchApp("com.prefix", "Main")),
                it.item("word", score = SearchRanker.WORD, action = SearchAction.LaunchApp("com.fav", "Main"))
            )
        })
        val results = pipeline(provider, signals = RankingSignals(favoriteApps = setOf("com.fav"))).search("q")
        assertEquals(listOf("word", "prefix"), results.single().items.map { it.id })
    }

    @Test
    fun providerPriorityBreaksTies() = runBlocking {
        val low = FakeProvider("low", ResultSection.WEB, priority = 1, items = { listOf(it.item("from-low")) })
        val high = FakeProvider("high", ResultSection.WEB, priority = 9, items = { listOf(it.item("from-high")) })
        val results = pipeline(low, high).search("q")
        assertEquals(listOf("from-high", "from-low"), results.single().items.map { it.id })
    }

    @Test
    fun perSectionLimit() = runBlocking {
        val provider = FakeProvider("apps", ResultSection.APPS, items = { r ->
            (1..12).map { r.item("app$it", action = SearchAction.LaunchApp("com.app$it", "Main")) }
        })
        val results = pipeline(provider).search("q")
        assertEquals(PowerlineDefaults.MAX_RESULTS_PER_SECTION, results.single().items.size)
    }

    // Deduplication runs before the per-section limit, so duplicates do not use up slots
    @Test
    fun duplicatesRemovedBeforeLimit() = runBlocking {
        val provider = FakeProvider("apps", ResultSection.APPS, items = { r ->
            (1..6).flatMap { n ->
                val action = SearchAction.LaunchApp("com.app$n", "Main")
                listOf(r.item("app$n", score = 100 - n, action = action), r.item("dup$n", score = 100 - n, action = action))
            }
        })
        val items = pipeline(provider).search("q").single().items
        assertEquals(PowerlineDefaults.MAX_RESULTS_PER_SECTION, items.size)
        assertEquals(PowerlineDefaults.MAX_RESULTS_PER_SECTION, items.map { it.action }.distinct().size)
    }

    @Test
    fun duplicateAcrossProvidersShownOnce() = runBlocking {
        val url = SearchAction.OpenUrl("https://example.com")
        val results = pipeline(
            FakeProvider("a", ResultSection.WEB, items = { listOf(it.item("a", action = url)) }),
            FakeProvider("b", ResultSection.WEB, items = { listOf(it.item("b", action = url)) })
        ).search("q")
        assertEquals(1, results.single().items.size)
    }

    @Test
    fun slowProviderDroppedOthersKept() = runBlocking {
        val slow = FakeProvider("slow", ResultSection.APPS, items = {
            delay(5_000)
            listOf(it.item("slow"))
        })
        val fast = FakeProvider("fast", ResultSection.WEB, items = { listOf(it.item("fast")) })
        lateinit var results: List<SectionResults>
        val elapsed = measureTimeMillis { results = pipeline(slow, fast).search("q") }
        assertEquals(listOf("fast"), results.flatMap { it.items }.map { it.id })
        assertTrue("took $elapsed ms", elapsed < PowerlineDefaults.SEARCH_TIMEOUT_MS + 150)
    }

    // A provider stuck in blocking code (not cancellable) must not hold the search
    @Test
    fun blockingProviderDoesNotHoldSearch() = runBlocking {
        val blocking = FakeProvider("blocking", ResultSection.APPS, items = {
            Thread.sleep(2_000)
            listOf(it.item("blocking"))
        })
        val fast = FakeProvider("fast", ResultSection.WEB, items = { listOf(it.item("fast")) })
        lateinit var results: List<SectionResults>
        val elapsed = measureTimeMillis { results = pipeline(blocking, fast).search("q") }
        assertEquals(listOf("fast"), results.flatMap { it.items }.map { it.id })
        assertTrue("took $elapsed ms", elapsed < PowerlineDefaults.SEARCH_TIMEOUT_MS + 150)
    }

    @Test
    fun providersAreRunInParallel() = runBlocking {
        val providers = (1..4).map { n ->
            FakeProvider("p$n", ResultSection.WEB, items = {
                delay(100)
                listOf(it.item("p$n"))
            })
        }
        lateinit var results: List<SectionResults>
        val elapsed = measureTimeMillis { results = pipeline(*providers.toTypedArray()).search("q") }
        assertEquals(4, results.single().items.size)
        assertTrue("took $elapsed ms", elapsed < 300)
    }

    @Test
    fun throwingProviderDroppedOthersKept() = runBlocking {
        val broken = FakeProvider("broken", ResultSection.APPS, items = { error("boom") })
        val fast = FakeProvider("fast", ResultSection.WEB, items = { listOf(it.item("fast")) })
        val results = pipeline(broken, fast).search("q")
        assertEquals(listOf("fast"), results.flatMap { it.items }.map { it.id })
    }

    @Test
    fun aliasQueryRunsOnlyWeb() = runBlocking {
        val apps = FakeProvider("apps", ResultSection.APPS, items = { listOf(it.item("a")) })
        val web = FakeProvider("web", ResultSection.WEB, items = { listOf(it.item("w")) })
        val share = FakeProvider("share", ResultSection.SHARE, items = { listOf(it.item("s")) })
        val results = pipeline(apps, web, share).search("yt laneige")
        assertEquals(listOf(ResultSection.WEB), results.map { it.section })
        assertEquals(0, apps.calls)
        assertEquals(0, share.calls)
    }

    private fun pipeline(vararg providers: SearchProvider, signals: RankingSignals = RankingSignals()) =
        SearchPipeline(
            context = context,
            registry = ProviderRegistry(providers.toList()),
            parser = QueryParser(PowerlineDefaults.SEARCH_ENGINES),
            signals = { signals }
        ).also { pipelines += it }

    private class FakeProvider(
        override val id: String,
        override val section: ResultSection,
        override val priority: Int = 0,
        private val items: suspend FakeProvider.(SearchRequest) -> List<SearchItem> = { emptyList() }
    ) : SearchProvider {
        override val displayName = id
        override val icon = Icons.Default.Search

        @Volatile
        var calls = 0

        override suspend fun search(request: SearchRequest, context: Context): List<SearchItem> {
            calls++
            return items(request)
        }

        fun SearchRequest.item(
            itemId: String,
            score: Int = 0,
            action: SearchAction = SearchAction.CopyText(itemId)
        ) = SearchItem(
            id = itemId, providerId = id, title = itemId, subtitle = text, icon = null,
            section = section, score = score, action = action
        )
    }
}
