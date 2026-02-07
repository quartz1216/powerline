package com.quartz.powerline.plugins.impl

import android.content.Context
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class WebSearchPluginTest {

    @Test
    fun testSearchQueryReturnsCorrectResult() {
        val settingsRepository = mock(SettingsRepository::class.java)
        val defaultEngine = SearchEngine(name = "Google", url = "https://www.google.com/search?q=%s", isEnabled = true)
        `when`(settingsRepository.getCustomSearchEngines()).thenReturn(listOf(defaultEngine))

        val plugin = WebSearchPlugin(settingsRepository)
        val context = mock(Context::class.java)
        val query = "cats"

        val results = plugin.search(query, context)

        assertEquals(1, results.size)
        val result = results[0]

        // ID should be unique to the query
        assertEquals("web_search:${defaultEngine.id}:cats", result.id)
        // Title should be the query
        assertEquals("cats", result.title)
        // Subtitle should be Engine Name
        assertEquals("Google", result.subtitle)
        // Type should be WEB
        assertEquals(ResultType.WEB, result.type)
        // Payload should be the URL
        assertEquals("https://www.google.com/search?q=cats", result.payload)
    }

    @Test
    fun testSearchWithAlias() {
        val settingsRepository = mock(SettingsRepository::class.java)
        val engine = SearchEngine(name = "YouTube", url = "https://yt.com/%s", alias = "yt", isEnabled = true)
        `when`(settingsRepository.getCustomSearchEngines()).thenReturn(listOf(engine))

        val plugin = WebSearchPlugin(settingsRepository)
        val context = mock(Context::class.java)
        val query = "yt cats"

        val results = plugin.search(query, context)

        assertEquals(1, results.size)
        val result = results[0]

        assertEquals("yt cats", result.title)
        assertEquals("YouTube", result.subtitle)
        assertEquals("https://yt.com/cats", result.payload)
    }
}
