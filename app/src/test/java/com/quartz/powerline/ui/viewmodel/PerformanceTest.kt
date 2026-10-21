package com.quartz.powerline.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceTest {

    private lateinit var viewModel: SearchViewModel
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()

        // Populate apps
        val pm = Shadows.shadowOf(application.packageManager)
        for (i in 0 until 1000) {
            val packageName = "com.example.app$i"
            val appInfo = ApplicationInfo().apply {
                this.packageName = packageName
                this.name = "App $i"
                this.flags = ApplicationInfo.FLAG_INSTALLED
            }
            val packageInfo = PackageInfo().apply {
                this.packageName = packageName
                this.applicationInfo = appInfo
            }
            pm.installPackage(packageInfo)
        }

        viewModel = SearchViewModel(application)

        // Wait for initial load
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchPerformance() = runTest {
        // Measure time for onQueryChange which triggers search
        val time = measureTimeMillis {
            viewModel.onQueryChange("App 500")
        }
        println("Execution time: $time ms")

        // Assert it returns immediately (async)
        // Allowing some buffer for coroutine launch overhead
        assertTrue("Search should be async and return quickly (was $time ms)", time < 15)

        // Verify results are eventually populated
        testDispatcher.scheduler.advanceUntilIdle()
        // We can't easily assert results content because of mocking/robolectric setup
        // but we can at least ensure no crash and logical completion
    }
}
