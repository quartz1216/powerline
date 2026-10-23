package dev.quxomiya.powerline.search

import android.graphics.drawable.ColorDrawable
import dev.quxomiya.powerline.data.PowerlineDefaults
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SearchRankerTest {

    private val parser = QueryParser(PowerlineDefaults.SEARCH_ENGINES)

    private fun match(query: String, target: String) =
        SearchRanker.match(parser.parse(query), SearchRanker.normalize(target))

    @Test
    fun matchTiers() {
        assertEquals(SearchRanker.EXACT, match("chrome", "Chrome"))
        assertEquals(SearchRanker.PREFIX, match("chr", "Chrome"))
        assertEquals(SearchRanker.WORD, match("maps", "Google Maps"))
        assertEquals(SearchRanker.SUBSTRING, match("oogle", "Google"))
        assertEquals(SearchRanker.FUZZY, match("gml", "Gmail"))
        assertEquals(0, match("xyz", "Gmail"))
    }

    @Test
    fun initialsMatchAsWord() {
        assertEquals(SearchRanker.WORD, match("gm", "Google Maps"))
        // A single letter is a prefix match, not an initials match
        assertEquals(SearchRanker.PREFIX, match("g", "Google Maps"))
    }

    @Test
    fun everyTokenStartingAWordMatchesAsWord() {
        assertEquals(SearchRanker.WORD, match("maps goo", "Google Maps"))
    }

    @Test
    fun matchIgnoresCaseAndWidth() {
        assertEquals(SearchRanker.EXACT, match("ＣＨＲＯＭＥ", "chrome"))
    }

    @Test
    fun emptyQueryMatchesNothing() {
        assertEquals(0, match("", "Chrome"))
    }

    @Test
    fun favoriteAddsBonus() {
        val item = appItem("com.example.a", score = SearchRanker.PREFIX)
        val signals = RankingSignals(favoriteApps = setOf("com.example.a"))
        assertEquals(SearchRanker.PREFIX + SearchRanker.FAVORITE_BONUS, SearchRanker.score(item, signals))
    }

    @Test
    fun frecencyBonusIsCapped() {
        val item = appItem("com.example.a", score = 0)
        assertEquals(40, SearchRanker.score(item, RankingSignals(appFrecency = mapOf("com.example.a" to 1.0))))
        assertEquals(
            SearchRanker.MAX_FRECENCY_BONUS,
            SearchRanker.score(item, RankingSignals(appFrecency = mapOf("com.example.a" to 50.0)))
        )
    }

    @Test
    fun nonAppItemsGetNoBonus() {
        val item = SearchItem(
            id = "web", providerId = "web", title = "x", subtitle = null, icon = null,
            section = ResultSection.WEB, score = 1, action = SearchAction.OpenUrl("https://x.com")
        )
        val signals = RankingSignals(favoriteApps = setOf("https://x.com"), appFrecency = mapOf("https://x.com" to 5.0))
        assertEquals(1, SearchRanker.score(item, signals))
    }

    private fun appItem(packageName: String, score: Int) = SearchItem(
        id = packageName, providerId = "apps", title = packageName, subtitle = null, icon = ColorDrawable(),
        section = ResultSection.APPS, score = score, action = SearchAction.LaunchApp(packageName, "Main")
    )
}
