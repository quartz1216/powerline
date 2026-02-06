package com.quartz.powerline.plugins.impl

import android.content.Context
import com.quartz.powerline.domain.models.ResultType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class IntegrationPluginTest {

    @Test
    fun testSearchQueryReturnsShareResult() {
        val plugin = IntegrationPlugin()
        val context = mock(Context::class.java)
        val query = "cat"

        val results = plugin.search(query, context)

        // Only 1 result: Share
        assertEquals(1, results.size)

        val result = results[0]
        assertEquals("integration:share:cat", result.id)
        assertEquals("Share Text", result.title)
        assertEquals("cat", result.payload)
        assertEquals(ResultType.INTEGRATION, result.type)
    }

    @Test
    fun testSearchEmptyReturnsEmpty() {
        val plugin = IntegrationPlugin()
        val context = mock(Context::class.java)
        val query = "   "

        val results = plugin.search(query, context)

        assertTrue(results.isEmpty())
    }
}
