package com.quartz.powerline.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.ResultType
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
class SearchEngineTest {

    private lateinit var searchEngine: SearchEngine

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        `when`(settingsRepository.getHiddenApps()).thenReturn(emptySet())
        `when`(settingsRepository.getAliases()).thenReturn(emptyMap())
        `when`(settingsRepository.getHistory()).thenReturn(emptyList())

        searchEngine = SearchEngine(context, settingsRepository)
    }

    @Test
    fun `test system command search`() {
        val results = searchEngine.search("wifi", emptyList())

        // Should find Wi-Fi Settings
        val wifiResult = results.find { it.payload == "wifi" }
        assertEquals(ResultType.SYSTEM, wifiResult?.type)
        assertEquals("Wi-Fi Settings", wifiResult?.title)
    }

    @Test
    fun `test settings search`() {
        val results = searchEngine.search("settings", emptyList())

        // Should find app settings
        val settingsResult = results.find { it.payload == "settings_action" }
        assertEquals(ResultType.SYSTEM, settingsResult?.type)
        assertEquals("Settings", settingsResult?.title)
    }

    @Test
    fun `test flashlight search`() {
        val results = searchEngine.search("flash", emptyList())

        val flashlightResult = results.find { it.payload == "flashlight" }
        assertEquals(ResultType.SYSTEM, flashlightResult?.type)
        assertEquals("Flashlight", flashlightResult?.title)
    }
}
