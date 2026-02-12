package com.quartz.powerline.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*

class SettingsRepositoryBenchmarkTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        context = mock(Context::class.java)
        sharedPreferences = mock(SharedPreferences::class.java)
        editor = mock(SharedPreferences.Editor::class.java)

        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        doNothing().`when`(editor).apply()

        settingsRepository = SettingsRepository(context)
    }

    @Test
    fun benchmarkSetAliases() = runTest {
        // Create a large map of aliases
        val largeAliases = (1..5000).associate { "alias$it" to "com.package.$it" }

        val startTime = System.nanoTime()
        settingsRepository.setAliases(largeAliases)
        val endTime = System.nanoTime()

        val durationMs = (endTime - startTime) / 1_000_000.0
        println("Execution time for setAliases (5000 items): $durationMs ms")

        // Assert that it takes some measurable time (e.g., > 1ms) to ensure we're measuring something.
        // This is a loose assertion just to confirm the test ran.
        Assert.assertTrue("Execution time should be measurable", durationMs > 0)
    }
}
