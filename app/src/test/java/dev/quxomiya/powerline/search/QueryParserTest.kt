package dev.quxomiya.powerline.search

import dev.quxomiya.powerline.data.PowerlineDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueryParserTest {

    private val parser = QueryParser(PowerlineDefaults.SEARCH_ENGINES)

    @Test
    fun collapsesWhitespaceAndLowercases() {
        val request = parser.parse("  Google   Maps ")
        assertEquals("Google Maps", request.text)
        assertEquals("google maps", request.normalizedQuery)
        assertEquals(listOf("google", "maps"), request.tokens)
        assertEquals(SearchMode.TEXT, request.mode)
    }

    @Test
    fun foldsFullWidth() {
        val request = parser.parse("Ｃｈｒｏｍｅ　１２３")
        assertEquals("Chrome 123", request.text)
        assertEquals("chrome 123", request.normalizedQuery)
    }

    @Test
    fun emptyQueryHasNoTokens() {
        val request = parser.parse("   ")
        assertEquals("", request.text)
        assertEquals(emptyList<String>(), request.tokens)
    }

    @Test
    fun aliasPrefixSelectsEngine() {
        val request = parser.parse("yt laneige")
        assertEquals(SearchMode.ALIAS, request.mode)
        assertEquals("youtube", request.engine?.id)
        assertEquals("laneige", request.aliasQuery)
    }

    @Test
    fun aliasIgnoresCaseAndWidth() {
        val request = parser.parse("ＹＴ　Laneige  cream")
        assertEquals("youtube", request.engine?.id)
        assertEquals("Laneige cream", request.aliasQuery)
    }

    // An engine chip puts "yt " in the field before any terms are typed
    @Test
    fun aliasWithTrailingSpaceOnly() {
        val request = parser.parse("yt ")
        assertEquals(SearchMode.ALIAS, request.mode)
        assertEquals("", request.aliasQuery)
    }

    @Test
    fun aliasWithoutSpaceIsText() {
        assertEquals(SearchMode.TEXT, parser.parse("yt").mode)
        assertEquals(SearchMode.TEXT, parser.parse("ytmusic").mode)
    }

    // "ytm" must not be read as "yt" + "m..."
    @Test
    fun longerAliasWins() {
        val request = parser.parse("ytm lofi")
        assertEquals("youtube_music", request.engine?.id)
        assertEquals("lofi", request.aliasQuery)
    }

    @Test
    fun textModeHasNoEngine() {
        assertNull(parser.parse("laneige").engine)
    }
}
