package dev.quxomiya.powerline.search.providers

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.search.AvailableEngine
import dev.quxomiya.powerline.search.IndexedApp
import dev.quxomiya.powerline.search.QueryParser
import dev.quxomiya.powerline.search.SearchAction
import dev.quxomiya.powerline.search.SearchRanker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProvidersTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val parser = QueryParser(PowerlineDefaults.SEARCH_ENGINES)
    private val engines = MutableStateFlow(PowerlineDefaults.SEARCH_ENGINES.map { AvailableEngine(it) })

    // --- Apps ---

    private val apps = MutableStateFlow(
        listOf(
            indexed("Chrome", "com.android.chrome", "Main"),
            indexed("電話", "com.android.dialer", "Dialer", aliases = listOf("phone", "call")),
            indexed("Google Maps", "com.google.android.apps.maps", "Maps")
        )
    )

    private fun appsSearch(query: String) = runBlocking { AppsProvider(apps).search(parser.parse(query), context) }

    @Test
    fun appsMatchLabel() {
        val items = appsSearch("chr")
        assertEquals(listOf("Chrome"), items.map { it.title })
        assertEquals(SearchRanker.PREFIX, items.single().score)
        assertEquals(SearchAction.LaunchApp("com.android.chrome", "com.android.chrome.Main"), items.single().action)
    }

    @Test
    fun appsMatchBuiltInAlias() {
        assertEquals(listOf("電話"), appsSearch("phone").map { it.title })
    }

    @Test
    fun appsMatchPackageWithoutFirstSegment() {
        // "google.android.apps.maps" is a key, "com" is not
        assertEquals(listOf("Google Maps"), appsSearch("android.apps").map { it.title })
        assertTrue(appsSearch("com").none { it.title == "Google Maps" && it.score >= SearchRanker.PREFIX })
    }

    @Test
    fun appsNoMatch() {
        assertEquals(emptyList<String>(), appsSearch("zzz").map { it.title })
    }

    // --- Calculator ---

    private fun calc(query: String) = runBlocking { CalculatorProvider().search(parser.parse(query), context) }

    @Test
    fun calculatorResultCopies() {
        val item = calc("200*10%").single()
        assertEquals("20", item.title)
        assertEquals(SearchAction.CopyText("20"), item.action)
    }

    @Test
    fun calculatorFullWidthInput() {
        assertEquals("3", calc("１＋２").single().title)
    }

    @Test
    fun calculatorSilentOnIncompleteInput() {
        listOf("12+", "(1+2", "1/0", "-5", "chrome").forEach {
            assertEquals("for '$it'", 0, calc(it).size)
        }
    }

    @Test
    fun calculatorSilentInAliasMode() {
        assertEquals(0, calc("yt 1+1").size)
    }

    // --- Web ---

    private fun web(query: String) = runBlocking { WebProvider(engines).search(parser.parse(query), context) }

    @Test
    fun plainTextSearchesDefaultEngine() {
        val item = web("laneige cream").single()
        assertEquals(SearchAction.OpenUrl("https://www.google.com/search?q=laneige%20cream"), item.action)
    }

    @Test
    fun urlOpensDirectlyAboveSearch() {
        val items = web("example.com")
        assertEquals(SearchAction.OpenUrl("https://example.com"), items[0].action)
        assertTrue(items[0].score > items[1].score)
        assertTrue((items[1].action as SearchAction.OpenUrl).url.startsWith("https://www.google.com/"))
    }

    @Test
    fun urlKeepsSchemePortPathAndQuery() {
        assertEquals(SearchAction.OpenUrl("http://a.example.co.jp:8080/p?q=1"), web("http://a.example.co.jp:8080/p?q=1")[0].action)
        assertEquals(SearchAction.OpenUrl("https://sub.example.com/x"), web("sub.example.com/x")[0].action)
    }

    @Test
    fun notUrls() {
        listOf("hello world", "1.5", "a.b", "foo bar.com").forEach { query ->
            assertTrue("for '$query'", web(query).none { it.id.startsWith("url:") })
        }
    }

    @Test
    fun aliasSearchesThatEngineOnly() {
        val item = web("yt laneige").single()
        assertEquals(SearchAction.OpenUrl("https://www.youtube.com/results?search_query=laneige"), item.action)
        assertEquals("Search YouTube", item.subtitle)
    }

    @Test
    fun aliasWithoutTermsShowsNothing() {
        assertEquals(0, web("yt ").size)
    }

    @Test
    fun aliasQueryIsEncoded() {
        val item = web("wiki C++ & Java").single()
        assertEquals(SearchAction.OpenUrl("https://en.wikipedia.org/wiki/Special:Search?search=C%2B%2B%20%26%20Java"), item.action)
    }

    // --- Share ---

    @Test
    fun shareUsesNormalizedText() {
        val item = runBlocking { ShareProvider().search(parser.parse("  Hello   World "), context) }.single()
        assertEquals(SearchAction.ShareText("Hello World"), item.action)
    }

    @Test
    fun shareSilentInAliasMode() {
        assertEquals(0, runBlocking { ShareProvider().search(parser.parse("yt x"), context) }.size)
    }

    private fun indexed(label: String, packageName: String, className: String, aliases: List<String> = emptyList()) =
        IndexedApp(
            AppInfo(label, packageName, ColorDrawable(), "$packageName.$className"),
            (listOf(label, packageName.substringAfter('.')) + aliases).map(SearchRanker::normalize)
        )
}
