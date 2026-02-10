package com.quartz.powerline.plugins.impl

import android.content.Context
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.CustomSearchEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebSearchPluginSchemeTest {

    @Mock
    lateinit var settingsRepository: SettingsRepository

    @Mock
    lateinit var context: Context

    private lateinit var plugin: WebSearchPlugin

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        plugin = WebSearchPlugin(settingsRepository)
    }

    @Test
    fun testEngineWithoutSchemeGeneratesCorrectFaviconAndPayload() = runTest {
        // User enters "duckduckgo.com/?q=%s" without https
        val engine = CustomSearchEngine(
            id = "ddg",
            name = "DuckDuckGo",
            url = "duckduckgo.com/?q=%s",
            alias = "d",
            isEnabled = true
        )
        `when`(settingsRepository.getCustomSearchEngines()).thenReturn(listOf(engine))

        val results = plugin.search("d cats", context)

        assertEquals(1, results.size)
        val result = results[0]

        // Check Favicon URL (should come from extracted domain)
        assertEquals("https://www.google.com/s2/favicons?sz=64&domain_url=duckduckgo.com", result.faviconUrl)

        // Check Payload (should start with https://)
        assertTrue("Payload should start with https://", result.payload!!.startsWith("https://"))
        assertEquals("https://duckduckgo.com/?q=cats", result.payload)
    }

    @Test
    fun testEngineWithHttpScheme() = runTest {
        val engine = CustomSearchEngine(
            id = "http_test",
            name = "Http Site",
            url = "http://example.com/search?q=%s",
            alias = null,
            isEnabled = true
        )
        `when`(settingsRepository.getCustomSearchEngines()).thenReturn(listOf(engine))

        val results = plugin.search("test", context)
        val result = results[0]

        assertEquals("https://www.google.com/s2/favicons?sz=64&domain_url=example.com", result.faviconUrl)
        assertEquals("http://example.com/search?q=test", result.payload)
    }
}
