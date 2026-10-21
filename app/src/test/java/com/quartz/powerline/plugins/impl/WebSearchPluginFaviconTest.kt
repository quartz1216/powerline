package com.quartz.powerline.plugins.impl

import android.content.Context
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.CustomSearchEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
class WebSearchPluginFaviconTest {

    @Mock
    lateinit var settingsRepository: SettingsRepository

    @Mock
    lateinit var context: Context

    private lateinit var plugin: WebSearchPlugin

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        // Default to Advanced mode for these tests to allow custom engines
        `when`(settingsRepository.getIsAdvanced()).thenReturn(true)
        plugin = WebSearchPlugin(settingsRepository)
    }

    @Test
    fun testFaviconUrlGeneration() = runTest {
        // Mock default engines
        val engine = CustomSearchEngine(
            id = "1",
            name = "Google",
            url = "https://www.google.com/search?q=%s",
            alias = null,
            isEnabled = true
        )
        `when`(settingsRepository.getCustomSearchEngines()).thenReturn(listOf(engine))

        val results = plugin.search("test", context)

        assertEquals(1, results.size)
        val result = results[0]
        assertEquals("https://www.google.com/s2/favicons?sz=64&domain_url=www.google.com", result.faviconUrl)
    }

    @Test
    fun testFaviconUrlGenerationWithAlias() = runTest {
        val engine = CustomSearchEngine(
            id = "2",
            name = "GitHub",
            url = "https://github.com/search?q=%s",
            alias = "gh",
            isEnabled = true
        )
        `when`(settingsRepository.getCustomSearchEngines()).thenReturn(listOf(engine))

        val results = plugin.search("gh test", context)

        assertEquals(1, results.size)
        val result = results[0]
        assertEquals("https://www.google.com/s2/favicons?sz=64&domain_url=github.com", result.faviconUrl)
    }
}
