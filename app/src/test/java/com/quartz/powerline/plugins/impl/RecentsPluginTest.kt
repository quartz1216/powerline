package com.quartz.powerline.plugins.impl

import android.content.Context
import com.quartz.powerline.data.HistoryItem
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.ResultType
import kotlinx.coroutines.flow.MutableStateFlow
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
class RecentsPluginTest {

    @Mock
    lateinit var settingsRepository: SettingsRepository

    @Mock
    lateinit var context: Context

    private val installedAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private lateinit var plugin: RecentsPlugin

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        plugin = RecentsPlugin(settingsRepository, installedAppsFlow)
    }

    @Test
    fun testFaviconUrlRestoration() {
        val historyItem = HistoryItem(
            id = "web_search:1:test",
            type = ResultType.WEB,
            title = "test",
            subtitle = "Google",
            payload = "https://google.com",
            timestamp = System.currentTimeMillis(),
            faviconUrl = "https://favicon.com/icon.png"
        )
        `when`(settingsRepository.getHistory()).thenReturn(listOf(historyItem))
        `when`(settingsRepository.getMaxRecentItems()).thenReturn(5)
        `when`(settingsRepository.getHiddenApps()).thenReturn(emptySet())

        val results = plugin.search("", context)

        assertEquals(1, results.size)
        val result = results[0]
        assertEquals("https://favicon.com/icon.png", result.faviconUrl)
    }

    @Test
    fun testSearchNotEmpty() {
        val results = plugin.search("something", context)
        assertEquals(0, results.size)
    }
}
