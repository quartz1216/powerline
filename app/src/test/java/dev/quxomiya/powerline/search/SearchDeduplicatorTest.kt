package dev.quxomiya.powerline.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchDeduplicatorTest {

    @Test
    fun sameLauncherActivityKeptOnce() {
        val items = listOf(
            item("a", SearchAction.LaunchApp("com.example", "com.example.Main")),
            item("b", SearchAction.LaunchApp("com.example", "com.example.Main"))
        )
        assertEquals(listOf("a"), SearchDeduplicator.deduplicate(items).map { it.id })
    }

    // A package with two launcher icons keeps both
    @Test
    fun differentActivitiesOfOnePackageKept() {
        val items = listOf(
            item("a", SearchAction.LaunchApp("com.example", "com.example.Main")),
            item("b", SearchAction.LaunchApp("com.example", "com.example.Other"))
        )
        assertEquals(2, SearchDeduplicator.deduplicate(items).size)
    }

    @Test
    fun urlsDifferingByTrailingSlashAreOne() {
        val items = listOf(
            item("a", SearchAction.OpenUrl("https://example.com/")),
            item("b", SearchAction.OpenUrl("https://example.com"))
        )
        assertEquals(listOf("a"), SearchDeduplicator.deduplicate(items).map { it.id })
    }

    @Test
    fun sameTextWithDifferentActionsKept() {
        val items = listOf(
            item("a", SearchAction.CopyText("42")),
            item("b", SearchAction.ShareText("42"))
        )
        assertEquals(2, SearchDeduplicator.deduplicate(items).size)
    }

    private fun item(id: String, action: SearchAction) = SearchItem(
        id = id, providerId = "test", title = id, subtitle = null, icon = null,
        section = ResultSection.WEB, score = 0, action = action
    )
}
