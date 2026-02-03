package com.quartz.powerline.domain

import android.content.Context
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.CustomPlugin
import com.quartz.powerline.domain.models.PluginAction
import com.quartz.powerline.domain.models.ResultType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class SearchEngineTest {

    private lateinit var searchEngine: SearchEngine
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = mock(Context::class.java)
        settingsRepository = mock(SettingsRepository::class.java)
        searchEngine = SearchEngine(context, settingsRepository)

        `when`(settingsRepository.getHiddenApps()).thenReturn(emptySet())
        `when`(settingsRepository.getAliases()).thenReturn(emptyMap())
        `when`(settingsRepository.getHistory()).thenReturn(emptyList())
    }

    @Test
    fun `search should return plugin result when keyword matches`() {
        val plugin = CustomPlugin(
            id = "1",
            name = "Test Plugin",
            keyword = "tp",
            action = PluginAction.OPEN_URL,
            actionData = "https://example.com/{q}"
        )
        `when`(settingsRepository.getPlugins()).thenReturn(listOf(plugin))

        val results = searchEngine.search("tp hello", emptyList())

        val pluginResult = results.find { it.type == ResultType.PLUGIN }
        assert(pluginResult != null)
        assertEquals("Test Plugin: hello", pluginResult?.title)
        assertEquals("hello", pluginResult?.payload)
    }

    @Test
    fun `search should return plugin result without argument when keyword matches exactly`() {
        val plugin = CustomPlugin(
            id = "1",
            name = "Test Plugin",
            keyword = "tp",
            action = PluginAction.OPEN_URL,
            actionData = "https://example.com/{q}"
        )
        `when`(settingsRepository.getPlugins()).thenReturn(listOf(plugin))

        val results = searchEngine.search("tp", emptyList())

        val pluginResult = results.find { it.type == ResultType.PLUGIN }
        assert(pluginResult != null)
        assertEquals("Test Plugin", pluginResult?.title)
        assertEquals("", pluginResult?.payload)
    }
}
