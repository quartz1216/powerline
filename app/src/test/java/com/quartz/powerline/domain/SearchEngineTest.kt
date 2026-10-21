package com.quartz.powerline.domain

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.CustomSearchEngine
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.plugins.PluginManager
import com.quartz.powerline.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import androidx.compose.runtime.Composable

class SearchEngineTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var pluginManager: PluginManager

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var searchEngine: SearchEngine

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        searchEngine = SearchEngine(context, pluginManager, settingsRepository)

        // Mock settings to return empty engines
        `when`(settingsRepository.getCustomSearchEngines()).thenReturn(emptyList())
    }

    class FakePlugin(
        override val id: String,
        private val delayMs: Long,
        private val result: SearchResult
    ) : Plugin {
        override val name = id
        override val description = ""
        override val icon: ImageVector = Mockito.mock(ImageVector::class.java)

        override suspend fun search(query: String, context: Context): List<SearchResult> {
            delay(delayMs)
            return listOf(result)
        }

        override fun execute(context: Context, result: SearchResult): Boolean = true
        @Composable
        override fun SettingsScreen(viewModel: SettingsViewModel) {}
    }

    @Test
    fun benchmarkSearch() = runTest {
        val result1 = SearchResult("1", "p1", "Result 1", "Subtitle 1", null, ResultType.WEB)
        val result2 = SearchResult("2", "p2", "Result 2", "Subtitle 2", null, ResultType.WEB)
        val result3 = SearchResult("3", "p3", "Result 3", "Subtitle 3", null, ResultType.WEB)

        val plugin1 = FakePlugin("p1", 100, result1)
        val plugin2 = FakePlugin("p2", 100, result2)
        val plugin3 = FakePlugin("p3", 100, result3)

        // Mocking the 'plugins' property on the mocked PluginManager
        `when`(pluginManager.plugins).thenReturn(listOf(plugin1, plugin2, plugin3))

        val results = searchEngine.search("test")

        // Assert that results are correct
        assertEquals(3, results.size)
        assertTrue(results.any { it.id == "1" })
        assertTrue(results.any { it.id == "2" })
        assertTrue(results.any { it.id == "3" })
    }
}
