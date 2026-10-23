package dev.quxomiya.powerline.data

import dev.quxomiya.powerline.search.SearchEngine
import dev.quxomiya.powerline.search.ShareTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// org.json is part of the Android platform, so this runs under Robolectric
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class StoredJsonTest {

    @Test
    fun enginesRoundTripWithAndWithoutAlias() {
        val engines = PowerlineDefaults.SEARCH_ENGINES
        assertEquals(null, engines.first().alias)
        assertEquals(engines, StoredJson.engines(StoredJson.ofEngines(engines)))
    }

    @Test
    fun shareTargetsAndHistoryRoundTrip() {
        val targets = listOf(ShareTarget("claude", "Claude", "cl", "com.anthropic.claude", "com.anthropic.claude.Share"))
        assertEquals(targets, StoredJson.shareTargets(StoredJson.ofShareTargets(targets)))
        val history = listOf(HistoryItem("com.a", "com.a.Main", 2), HistoryItem("com.b", "com.b.Main", 1))
        assertEquals(history, StoredJson.history(StoredJson.ofHistory(history)))
    }

    @Test
    fun entryMissingAFieldIsDropped() {
        val json = """[{"id":"a","name":"A","url":"https://a/%s"},{"id":"b","name":"B"}]"""
        assertEquals(listOf(SearchEngine("a", "A", null, "https://a/%s")), StoredJson.engines(json))
    }

    @Test
    fun notAnArrayIsUnreadable() {
        assertNull(StoredJson.engines("not json"))
        assertNull(StoredJson.history("{}"))
    }
}
