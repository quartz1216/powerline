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
import kotlinx.coroutines.test.advanceUntilIdle
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
        // "1+1" should yield Calc(2) and Web("1+1") -> 2 items
        // Wait until results are populated. Since we use runTest and unconfined dispatcher, it should be quick.
        // However, AppRepository is real and does IO. That might be tricky in unit test without mocking repository.
        // But AppRepository calls packageManager which Robolectric handles.

        val results = viewModel.results.first { it.isNotEmpty() }
        assertEquals(2, results.size) // Calc + Web

        // Initial selection should be 0
        assertEquals(0, viewModel.selectedIndex.value)

        // Move Down -> 1
        viewModel.selectNext()
        assertEquals(1, viewModel.selectedIndex.value)

        // Move Down again -> should stay at 1 (last index)
        viewModel.selectNext()
        assertEquals(1, viewModel.selectedIndex.value)

        // Move Up -> 0
        viewModel.selectPrevious()
        assertEquals(0, viewModel.selectedIndex.value)

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

    @Test
    fun `test remove from history`() = runTest {
        // Wait for initial load to finish to avoid uncompleted coroutines from loadApps()
        advanceUntilIdle()

        // 1. Perform a search and execute it to add to history
        viewModel.onQueryChange("test query")
        val results = viewModel.results.first { it.isNotEmpty() }
        val webResult = results.find { it.type == ResultType.WEB }!!

        val application = ApplicationProvider.getApplicationContext<Application>()
        val settingsRepository = viewModel.settingsRepository
        com.quartz.powerline.utils.ActionExecutor.execute(application, webResult, settingsRepository)

        // 2. Clear query and verify it's in history
        viewModel.onQueryChange("")
        val historyResults = viewModel.results.first { it.isNotEmpty() }
        val historyItem = historyResults.find { it.id == webResult.id }
        assertEquals(webResult.title, historyItem?.title)

        // 3. Remove from history
        viewModel.removeFromHistory(historyItem!!)
        advanceUntilIdle()

        // 4. Verify it's gone from results
        val updatedResults = viewModel.results.value
        val removedItem = updatedResults.find { it.id == webResult.id }
        assertEquals(null, removedItem)
    }
}
