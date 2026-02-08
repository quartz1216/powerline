package com.quartz.powerline.plugins.impl

import android.content.Context
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.SearchResult
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
class CalculatorPluginTest {

    @Mock
    lateinit var settingsRepository: SettingsRepository

    @Mock
    lateinit var context: Context

    private lateinit var plugin: CalculatorPlugin

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        plugin = CalculatorPlugin(settingsRepository)
        `when`(settingsRepository.getCalculatorEnabled()).thenReturn(true)
        `when`(settingsRepository.getCalculatorAlias()).thenReturn("")
    }

    @Test
    fun testDisabled() {
        `when`(settingsRepository.getCalculatorEnabled()).thenReturn(false)
        val results = plugin.search("1+1", context)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testNoAlias() {
        `when`(settingsRepository.getCalculatorAlias()).thenReturn("")

        val results = plugin.search("1+1", context)
        assertEquals(1, results.size)
        assertEquals("2", results[0].title)
    }

    @Test
    fun testWithAliasExclusive() {
        `when`(settingsRepository.getCalculatorAlias()).thenReturn("calc")

        // Direct query fails
        var results = plugin.search("1+1", context)
        assertTrue(results.isEmpty())

        // Alias query succeeds
        results = plugin.search("calc 1+1", context)
        assertEquals(1, results.size)
        assertEquals("2", results[0].title)
    }

    @Test
    fun testWithAliasBadFormat() {
        `when`(settingsRepository.getCalculatorAlias()).thenReturn("calc")

        // "calculate 1+1" should not match "calc " prefix logic strictly speaking
        val results = plugin.search("calculate 1+1", context)
        assertTrue(results.isEmpty())
    }
}
