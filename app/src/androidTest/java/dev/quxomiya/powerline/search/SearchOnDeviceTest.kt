package dev.quxomiya.powerline.search

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.search.providers.AppsProvider
import dev.quxomiya.powerline.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import kotlin.system.measureNanoTime

// Search against the apps actually installed on the device (Phase 4–5 acceptance)
@RunWith(AndroidJUnit4::class)
class SearchOnDeviceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.targetContext.applicationContext as Application
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var appIndex: AppIndex
    private lateinit var pipeline: SearchPipeline

    @Before
    fun setup() = runBlocking {
        appIndex = AppIndex(application, scope)
        withTimeout(10_000) { appIndex.apps.first { it.isNotEmpty() } }
        pipeline = SearchPipeline(
            context = application,
            registry = ProviderRegistry(appIndex),
            parser = QueryParser(PowerlineDefaults.SEARCH_ENGINES),
            signals = { RankingSignals() }
        )
    }

    @After
    fun tearDown() {
        pipeline.close()
        appIndex.close()
        scope.cancel()
    }

    private fun search(query: String) = runBlocking { pipeline.search(query) }

    private fun installed(packageName: String) = appIndex.apps.value.any { it.app.packageName == packageName }

    @Test
    fun appIndexHasLaunchableApps() {
        val packages = appIndex.apps.value.map { it.app.packageName }
        assertTrue(packages.contains(application.packageName))
        assertEquals("sorted by label", appIndex.apps.value.map { it.app.label.lowercase() }.sorted(), appIndex.apps.value.map { it.app.label.lowercase() })
    }

    @Test
    fun appNameIsTopResult() {
        val first = search("powerline").first().items.first()
        assertEquals(ResultSection.APPS, first.section)
        assertEquals(application.packageName, (first.action as SearchAction.LaunchApp).packageName)
    }

    @Test
    fun builtInAliasFindsDialer() {
        val dialers = PowerlineDefaults.APP_ALIASES.filterValues { "phone" in it }.keys.filter(::installed)
        org.junit.Assume.assumeTrue("no dialer installed", dialers.isNotEmpty())
        val apps = search("phone").first { it.section == ResultSection.APPS }.items
        assertTrue(apps.any { (it.action as SearchAction.LaunchApp).packageName in dialers })
    }

    @Test
    fun aliasQueryIsWebOnly() {
        val results = search("yt laneige")
        assertEquals(listOf(ResultSection.WEB), results.map { it.section })
        assertEquals(
            SearchAction.OpenUrl("https://www.youtube.com/results?search_query=laneige"),
            results.single().items.single().action
        )
    }

    @Test
    fun calculatorSection() {
        val calc = search("12*3").first { it.section == ResultSection.CALCULATOR }.items.single()
        assertEquals("36", calc.title)
    }

    @Test
    fun urlBeforeSearch() {
        val web = search("example.com").first { it.section == ResultSection.WEB }.items
        assertEquals(SearchAction.OpenUrl("https://example.com"), web.first().action)
    }

    @Test
    fun noDuplicatesAndLimitsHold() {
        for (query in listOf("a", "e", "go", "s", "c", "android", "com")) {
            val results = search(query)
            val items = results.flatMap { it.items }
            assertEquals("duplicates for '$query'", items.size, items.map { it.action }.distinct().size)
            assertTrue(results.all { it.items.size <= PowerlineDefaults.MAX_RESULTS_PER_SECTION })
            assertTrue(items.size <= PowerlineDefaults.MAX_TOTAL_RESULTS)
        }
    }

    // ROADMAP Phase 4: first result within 100ms for a 1–2 letter app search
    @Test
    fun shortQueriesAreFast() {
        search("x") // warm up
        for (query in listOf("c", "s", "go", "pl", "ch", "a")) {
            var results: List<SectionResults> = emptyList()
            val ms = measureNanoTime { results = search(query) } / 1_000_000
            assertTrue("'$query' took $ms ms", ms < 100)
            assertTrue("'$query' returned nothing", results.isNotEmpty())
        }
    }

    @Test
    fun appsProviderAloneIsFast() = runBlocking {
        val provider = AppsProvider(appIndex.apps)
        val request = QueryParser(PowerlineDefaults.SEARCH_ENGINES).parse("c")
        provider.search(request, application)
        val ms = measureNanoTime { provider.search(request, application) } / 1_000_000
        assertTrue("took $ms ms for ${appIndex.apps.value.size} apps", ms < PowerlineDefaults.PROVIDER_TIMEOUT_MS)
    }

    // Typing fast: the results shown must never be those of an older query
    @Test
    fun typingNeverShowsStaleResults() = runBlocking {
        lateinit var viewModel: SearchViewModel
        instrumentation.runOnMainSync { viewModel = SearchViewModel(application) }
        withTimeout(10_000) { viewModel.visibleApps.first { it.isNotEmpty() } }

        val shown = Collections.synchronizedList(mutableListOf<SearchUiState>())
        val collector = scope.launch { viewModel.uiState.collect { shown += it } }

        val typed = "chrome"
        for (end in 1..typed.length) {
            instrumentation.runOnMainSync { viewModel.onQueryChange(typed.substring(0, end)) }
            Thread.sleep(15) // faster than the 40ms debounce
        }
        withTimeout(2_000) { viewModel.uiState.first { it.items.isNotEmpty() } }
        Thread.sleep(300)
        collector.cancel()

        val withResults = shown.filter { it.items.isNotEmpty() }
        assertTrue(withResults.isNotEmpty())
        // The share item echoes the query it was built from
        withResults.forEach { state ->
            val shareText = state.items.single { it.section == ResultSection.SHARE }.title
            assertEquals(typed, shareText)
        }

        // Back to empty: results are dropped at once
        instrumentation.runOnMainSync { viewModel.onQueryChange("") }
        assertEquals(SearchUiState(), viewModel.uiState.value)
    }
}
