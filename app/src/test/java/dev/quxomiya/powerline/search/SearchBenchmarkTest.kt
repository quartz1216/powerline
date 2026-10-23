package dev.quxomiya.powerline.search

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.search.providers.AppsProvider
import dev.quxomiya.powerline.search.providers.CalculatorProvider
import dev.quxomiya.powerline.search.providers.ShareProvider
import dev.quxomiya.powerline.search.providers.WebProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// ROADMAP §6 "Search, 1000 apps": the whole pipeline (parse, four providers, ranking with
// history-based signals, dedup) over 1000 indexed apps, for queries typed a key at a time.
// Prints "BENCH <name> <µs per query>"; the numbers are compared across phases, not asserted.
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SearchBenchmarkTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val pipelines = mutableListOf<SearchPipeline>()

    @After
    fun tearDown() {
        pipelines.forEach { it.close() }
    }

    @Test
    fun pipelineOver1000Apps() = runBlocking {
        val apps = apps(1000)
        val userData = UserDataRepository(context)
        // A full history, so frecency is computed over 100 launches
        apps.take(PowerlineDefaults.MAX_HISTORY_ITEMS).forEachIndexed { i, app ->
            userData.recordAppLaunch(app.app.packageName, app.app.className, timestamp = i.toLong())
        }
        userData.setFavoriteApps(apps.take(5).map { it.app.packageName }.toSet())

        val appIndex = MutableStateFlow(apps)
        val engines = MutableStateFlow(PowerlineDefaults.SEARCH_ENGINES.map { AvailableEngine(it) })
        val registry = ProviderRegistry(
            listOf(
                AppsProvider(appIndex),
                CalculatorProvider(),
                WebProvider(engines),
                ShareProvider()
            )
        ) { true }
        val pipeline = SearchPipeline(
            context,
            registry,
            QueryParser(PowerlineDefaults.SEARCH_ENGINES),
            signals = { RankingSignals(userData.getFavoriteApps(), userData.getAppFrecency()) }
        ).also { pipelines += it }

        val queries = TYPED.flatMap { word -> (1..word.length).map { word.take(it) } }
        repeat(WARMUP_ROUNDS) { queries.forEach { pipeline.search(it) } }
        var results = 0
        val start = System.nanoTime()
        repeat(ROUNDS) { queries.forEach { results += pipeline.search(it).size } }
        val perQueryUs = (System.nanoTime() - start) / 1_000.0 / (ROUNDS * queries.size)
        println("BENCH pipeline_1000_apps_us %.1f".format(perQueryUs))
        assertTrue(results > 0)
    }

    // Labels of two to three words from a fixed vocabulary, so prefix, word, initials and fuzzy
    // matches all occur
    private fun apps(count: Int): List<IndexedApp> = (0 until count).map { i ->
        val label = listOf(WORDS[i % WORDS.size], WORDS[(i / WORDS.size + 3) % WORDS.size], if (i % 3 == 0) "$i" else "")
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        val packageName = "com.example.${label.lowercase().replace(' ', '.')}"
        IndexedApp(
            AppInfo(label, packageName, ColorDrawable(), "$packageName.Main"),
            listOf(label, packageName.substringAfter('.')).map(SearchRanker::normalize)
        )
    }

    private companion object {
        const val WARMUP_ROUNDS = 20
        const val ROUNDS = 50
        val WORDS = listOf(
            "Chrome", "Maps", "Photos", "Mail", "Calendar", "Notes", "Music", "Camera", "Files", "Clock",
            "Weather", "News", "Wallet", "Health", "Translate", "Drive", "Docs", "Sheets", "Slides", "Keep",
            "Messages", "Phone", "Contacts", "Settings", "Store", "Video", "Podcasts", "Books", "Fitness", "Home"
        )
        val TYPED = listOf("chrome", "goo ma", "settings", "12*3", "yt lofi", "xyzzy", "cal")
    }
}
