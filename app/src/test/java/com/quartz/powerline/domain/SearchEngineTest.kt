package com.quartz.powerline.domain

import android.app.Application
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.ResultType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchEngineTest {

    private lateinit var searchEngine: SearchEngine
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepository(context)
        // Clear history and aliases for clean state
        settingsRepository.setAliases(emptyMap())
        context.getSharedPreferences("powerline_settings", android.content.Context.MODE_PRIVATE).edit().clear().commit()

        searchEngine = SearchEngine(context, settingsRepository)
    }

    @Test
    fun `test keyword search with alias`() {
        val youtubePkg = "com.google.android.youtube"
        settingsRepository.setAliases(mapOf("y" to youtubePkg))

        val apps = listOf(
            AppInfo("YouTube", youtubePkg, ColorDrawable(0))
        )

        val results = searchEngine.search("y cat", apps)

        val appSearch = results.find { it.type == ResultType.APP_SEARCH && it.packageName == youtubePkg }
        assertTrue("Should find app search result", appSearch != null)
        assertEquals("Search YouTube", appSearch?.title)
        assertEquals("cat", appSearch?.subtitle)
        assertEquals("cat", appSearch?.payload)
    }

    @Test
    fun `test keyword search with app name`() {
        val youtubePkg = "com.google.android.youtube"
        val apps = listOf(
            AppInfo("YouTube", youtubePkg, ColorDrawable(0))
        )

        val results = searchEngine.search("youtube cat", apps)

        val appSearch = results.find { it.type == ResultType.APP_SEARCH && it.packageName == youtubePkg }
        assertTrue("Should find app search result", appSearch != null)
        assertEquals("Search YouTube", appSearch?.title)
        assertEquals("cat", appSearch?.subtitle)
    }

    @Test
    fun `test default search providers`() {
        val youtubePkg = "com.google.android.youtube"
        val apps = listOf(
            AppInfo("YouTube", youtubePkg, ColorDrawable(0))
        )

        val results = searchEngine.search("cat", apps)

        val appSearch = results.find { it.type == ResultType.APP_SEARCH && it.packageName == youtubePkg }
        assertTrue("Should find YouTube search result", appSearch != null)
        assertEquals("Search YouTube", appSearch?.title)
        assertEquals("cat", appSearch?.subtitle)
    }
}
