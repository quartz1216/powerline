package com.quartz.powerline.plugins.impl

import android.content.Context
import com.quartz.powerline.domain.models.ResultType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class WebSearchPluginTest {

    @Test
    fun testSearchQueryReturnsCorrectResult() {
        val plugin = WebSearchPlugin()
        val context = mock(Context::class.java)
        val query = "cats"

        val results = plugin.search(query, context)

        assertEquals(1, results.size)
        val result = results[0]

        // Desired behavior:
        // ID should be unique to the query
        assertEquals("web_search:cats", result.id)
        // Title should be the query
        assertEquals("cats", result.title)
        // Subtitle should be "Search Web"
        assertEquals("Search Web", result.subtitle)
        // Type should be WEB
        assertEquals(ResultType.WEB, result.type)
        // Payload should be the query
        assertEquals("cats", result.payload)
    }
}
