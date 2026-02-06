package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.SharedPreferences
import com.quartz.powerline.domain.models.ResultType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class IntegrationPluginTest {

    @Test
    fun testSearchQueryReturnsCorrectResult() {
        val plugin = IntegrationPlugin()
        val context = mock(Context::class.java)
        val prefs = mock(SharedPreferences::class.java)

        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        // Return null to trigger defaults in SettingsRepository
        `when`(prefs.getStringSet("integration_enabled_apps", null)).thenReturn(null)

        val query = "cat"

        val results = plugin.search(query, context)

        // Expect defaults: Share, Maps, YouTube, Play Store, Amazon, Twitter (6 items)
        assertEquals(6, results.size)

        // Verify one of them, e.g. Maps
        val mapsResult = results.find { it.id.contains(":maps:") }
        assertTrue(mapsResult != null)
        assertEquals("Search in Maps", mapsResult?.title)
        assertEquals("cat", mapsResult?.payload)
        assertEquals(ResultType.INTEGRATION, mapsResult?.type)
    }

    @Test
    fun testSearchEmptyReturnsEmpty() {
        val plugin = IntegrationPlugin()
        val context = mock(Context::class.java)
        // No prefs needed for empty check because it returns early
        val query = "   "

        val results = plugin.search(query, context)

        assertTrue(results.isEmpty())
    }
}
