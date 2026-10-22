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

    @Test
    fun resetRestoresBuiltIns() {
        val repository = SearchSettingsRepository(context)
        repository.setEngines(listOf(gh))
        repository.resetEngines()
        assertEquals(PowerlineDefaults.SEARCH_ENGINES, repository.engines.value)
        assertEquals(PowerlineDefaults.SEARCH_ENGINES, SearchSettingsRepository(context).engines.value)
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
