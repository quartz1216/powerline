package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class WebSearchPluginTest {

    @Test
    fun testSearchQueryReturnsDirectUrl() {
        val plugin = WebSearchPlugin()
        val context = mock(Context::class.java)
        val prefs = mock(SharedPreferences::class.java)

        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        // Return null for custom engines to trigger defaults (which is fine, we ignore them in this test)
        `when`(prefs.getString("custom_search_engines", null)).thenReturn(null)

        val query = "example.com"

        val results = plugin.search(query, context)

        // We only care about Direct URL here
        val directResult = results.find { it.id == "web_direct" }
        assertTrue(directResult != null)
        assertEquals("Go to example.com", directResult?.title)
        assertEquals("https://example.com", directResult?.payload)
    }

    @Test
    fun testSearchQueryReturnsCustomEngines() {
        val plugin = WebSearchPlugin()
        val context = mock(Context::class.java)
        val prefs = mock(SharedPreferences::class.java)

        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)

        // Mock custom engines
        val customEngine = SearchEngine(id = "test_engine", title = "Test Search", urlTemplate = "https://test.com?q=%s")
        val gson = Gson()
        val json = gson.toJson(listOf(customEngine))

        `when`(prefs.getString("custom_search_engines", null)).thenReturn(json)

        val query = "cat"
        val results = plugin.search(query, context)

        // Should contain our custom engine
        val engineResult = results.find { it.id == "web_engine:test_engine:cat" }
        assertTrue(engineResult != null)
        assertEquals("Test Search", engineResult?.title)
        assertEquals("engine:test_engine:cat", engineResult?.payload)
    }
}
