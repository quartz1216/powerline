package com.quartz.powerline.plugins.impl

import android.content.Context
import com.quartz.powerline.domain.models.ResultType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class IntegrationPluginTest {

    @Test
    fun testSearchQueryReturnsCorrectResult() {
        val plugin = IntegrationPlugin()
        val context = mock(Context::class.java)
        val query = "Hello Gemini"

        val results = plugin.search(query, context)

        assertEquals(1, results.size)
        val result = results[0]

        assertEquals("integration_gemini:Hello Gemini", result.id)
        assertEquals("Ask Gemini", result.title)
        assertEquals("Hello Gemini", result.subtitle)
        assertEquals(ResultType.INTEGRATION, result.type)
        assertEquals("Hello Gemini", result.payload)
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
