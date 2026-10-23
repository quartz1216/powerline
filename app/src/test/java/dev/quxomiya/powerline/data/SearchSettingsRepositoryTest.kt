package dev.quxomiya.powerline.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.quxomiya.powerline.search.SearchEngine
import dev.quxomiya.powerline.search.ShareTarget
import dev.quxomiya.powerline.search.BuiltIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SearchSettingsRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = context.getSharedPreferences("powerline_settings", Context.MODE_PRIVATE)
    private val gh = SearchEngine("custom_gh", "GitHub", "gh", "https://github.com/search?q=%s")
    private val claude = ShareTarget("claude", "Claude", "cl", "com.anthropic.claude", "com.anthropic.claude.Share")

    @Before
    fun setup() {
        prefs.edit().clear().commit()
    }

    @Test
    fun unsetMeansBuiltInEnginesAndNoShareTargets() {
        val repository = SearchSettingsRepository(context)
        assertEquals(PowerlineDefaults.SEARCH_ENGINES, repository.engines.value)
        assertEquals(emptyList<ShareTarget>(), repository.shareTargets.value)
    }

    @Test
    fun editsRoundTrip() {
        val engines = listOf(gh) + PowerlineDefaults.SEARCH_ENGINES.drop(2)
        SearchSettingsRepository(context).apply {
            setEngines(engines)
            setShareTargets(listOf(claude))
        }
        val reopened = SearchSettingsRepository(context)
        assertEquals(engines, reopened.engines.value)
        assertEquals(listOf(claude), reopened.shareTargets.value)
    }

    // Web search / Share off: their engines and targets are out of effect, and back when switched on
    @Test
    fun activeListsFollowBuiltInSwitches() {
        val repository = SearchSettingsRepository(context)
        repository.setShareTargets(listOf(claude))
        assertEquals(PowerlineDefaults.SEARCH_ENGINES, repository.activeEngines.value)
        assertEquals(listOf(claude), repository.activeShareTargets.value)

        repository.setEnabled(BuiltIn.WEB, false)
        repository.setEnabled(BuiltIn.SHARE, false)
        assertEquals(emptyList<SearchEngine>(), repository.activeEngines.value)
        assertEquals(emptyList<ShareTarget>(), repository.activeShareTargets.value)
        repository.setEngines(listOf(gh))
        assertEquals(emptyList<SearchEngine>(), repository.activeEngines.value)

        repository.setEnabled(BuiltIn.WEB, true)
        assertEquals(listOf(gh), repository.activeEngines.value)
        assertEquals(listOf(gh), SearchSettingsRepository(context).activeEngines.value)
        assertEquals(emptyList<ShareTarget>(), SearchSettingsRepository(context).activeShareTargets.value)
    }

    @Test
    fun resetRestoresBuiltIns() {
        val repository = SearchSettingsRepository(context)
        repository.setEngines(listOf(gh))
        repository.resetEngines()
        assertEquals(PowerlineDefaults.SEARCH_ENGINES, repository.engines.value)
        assertEquals(PowerlineDefaults.SEARCH_ENGINES, SearchSettingsRepository(context).engines.value)
    }

    @Test
    fun showPackageNameDefaultsOnAndRoundTrips() {
        val repository = SearchSettingsRepository(context)
        assertTrue(repository.showPackageName.value)
        repository.setShowPackageName(false)
        assertFalse(repository.showPackageName.value)
        assertFalse(SearchSettingsRepository(context).showPackageName.value)
    }

    @Test
    fun forceAsciiInputDefaultsOnAndRoundTrips() {
        val repository = SearchSettingsRepository(context)
        assertTrue(repository.forceAsciiInput.value)
        repository.setForceAsciiInput(false)
        assertFalse(SearchSettingsRepository(context).forceAsciiInput.value)
    }

    // A write through another instance (settings vs. overlay) reaches this one
    @Test
    fun followsWritesFromAnotherInstance() {
        val overlay = SearchSettingsRepository(context)
        SearchSettingsRepository(context).setShareTargets(listOf(claude))
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(listOf(claude), overlay.shareTargets.value)
    }

    @Test
    fun brokenDataFallsBack() {
        prefs.edit().putString("search_engines", "not json").putString("share_targets", """[{"id":"x"}]""").commit()
        val repository = SearchSettingsRepository(context)
        assertEquals(PowerlineDefaults.SEARCH_ENGINES, repository.engines.value)
        assertEquals(emptyList<ShareTarget>(), repository.shareTargets.value)
    }

    @Test
    fun builtInsOnUntilSwitchedOff() {
        val repository = SearchSettingsRepository(context)
        assertTrue(BuiltIn.entries.all(repository::isEnabled))
        repository.setEnabled(BuiltIn.CALCULATOR, false)
        repository.setEnabled(BuiltIn.SHARE, false)
        repository.setEnabled(BuiltIn.SHARE, true)
        val reopened = SearchSettingsRepository(context)
        assertEquals(setOf(BuiltIn.CALCULATOR), reopened.disabledBuiltIns.value)
        assertFalse(reopened.isEnabled(BuiltIn.CALCULATOR))
    }

    @Test
    fun aliasRules() {
        val used = listOf("yt", null, "CL")
        assertNull(SearchSettingsValidator.alias("", used, required = false))
        assertNotNull(SearchSettingsValidator.alias("", used, required = true))
        assertNotNull(SearchSettingsValidator.alias("a b", used, required = false))
        assertNotNull(SearchSettingsValidator.alias("cl", used, required = true))
        assertNotNull(SearchSettingsValidator.alias(" YT ", used, required = true))
        assertNull(SearchSettingsValidator.alias("gh", used, required = true))
    }

    @Test
    fun urlRules() {
        assertNull(SearchSettingsValidator.url("https://github.com/search?q=%s"))
        assertNotNull(SearchSettingsValidator.url("https://github.com/search"))
        assertNotNull(SearchSettingsValidator.url("github.com/search?q=%s"))
    }
}
