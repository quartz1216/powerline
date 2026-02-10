package com.quartz.powerline.plugins.impl

import android.content.Context
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.SearchResult
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
    }

    @Test
    fun testDisabled() = runTest {
        `when`(settingsRepository.getCalculatorEnabled()).thenReturn(false)
        val results = plugin.search("1+1", context)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testCalculation() = runTest {
        val results = plugin.search("1+1", context)
        assertEquals(1, results.size)
        assertEquals("2", results[0].title)
    }
}
