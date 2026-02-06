package com.quartz.powerline.ui.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MainViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = MainViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test selection navigation`() = runTest {
        // Trigger a search to populate results
        viewModel.onQueryChange("1+1")

        // Wait for results
        // "1+1" should yield:
        // 1. Calculator (2)
        // 2. Web Search ("1+1")
        // 3. Memo ("Save '1+1'")
        // 4-9. Integration (Share, Maps, YouTube, Play Store, Amazon, Twitter) -> 6 items
        // Total: 9 items

        val results = viewModel.results.first { it.isNotEmpty() }
        assertEquals(9, results.size)

        // Initial selection should be 0
        assertEquals(0, viewModel.selectedIndex.value)

        // Move Down -> 1
        viewModel.selectNext()
        assertEquals(1, viewModel.selectedIndex.value)

        // Move Down -> 2
        viewModel.selectNext()
        assertEquals(2, viewModel.selectedIndex.value)

        // Move Down -> 3
        viewModel.selectNext()
        assertEquals(3, viewModel.selectedIndex.value)

        // Move to the end (index 8)
        for (i in 4..8) {
            viewModel.selectNext()
            assertEquals(i, viewModel.selectedIndex.value)
        }

        // Move Down again -> should stay at 8 (last index)
        viewModel.selectNext()
        assertEquals(8, viewModel.selectedIndex.value)

        // Move Up -> 7
        viewModel.selectPrevious()
        assertEquals(7, viewModel.selectedIndex.value)

        // Move back to start
        for (i in 6 downTo 0) {
            viewModel.selectPrevious()
            assertEquals(i, viewModel.selectedIndex.value)
        }

        // Move Up again -> should stay at 0
        viewModel.selectPrevious()
        assertEquals(0, viewModel.selectedIndex.value)
    }

    @Test
    fun `test selection reset on query change`() = runTest {
        viewModel.onQueryChange("1+1")
        viewModel.results.first { it.isNotEmpty() }

        viewModel.selectNext()
        assertEquals(1, viewModel.selectedIndex.value)

        // Change query
        viewModel.onQueryChange("2+2")
        assertEquals(0, viewModel.selectedIndex.value)
    }
}
