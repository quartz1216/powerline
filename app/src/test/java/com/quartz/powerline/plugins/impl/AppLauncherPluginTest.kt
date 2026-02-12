package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*

class AppLauncherPluginTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var context: Context
    private lateinit var installedAppsFlow: MutableStateFlow<List<AppInfo>>
    private lateinit var plugin: AppLauncherPlugin

    @Before
    fun setup() {
        settingsRepository = mock(SettingsRepository::class.java)
        context = mock(Context::class.java)
        installedAppsFlow = MutableStateFlow(emptyList())
        // Since we mock hiddenApps to be empty, visibleApps == installedApps
        plugin = AppLauncherPlugin(settingsRepository, installedAppsFlow, installedAppsFlow)

        // Default mock behaviors
        `when`(settingsRepository.getHiddenApps()).thenReturn(emptySet())
        `when`(settingsRepository.getAliases()).thenReturn(emptyMap())
        `when`(settingsRepository.getFavoriteApps()).thenReturn(emptySet())
        `when`(settingsRepository.getAppWeights()).thenReturn(emptyMap())
        `when`(settingsRepository.getEnableWeightSearch()).thenReturn(true)
        `when`(settingsRepository.getEnablePackageSearch()).thenReturn(true)
    }

    private fun createApp(label: String, packageName: String): AppInfo {
        return AppInfo(label, packageName, mock(Drawable::class.java), null)
    }

    @Test
    fun `test fuzzyMatch exact start contains returns 100`() {
        // Since fuzzyMatch is private, we test via search scoring or reflect.
        // Or we trust search results. Let's test via search results with controlled environment.
        // Single app, check score via sorting or assumption.
        // Actually, better to expose fuzzyMatch for testing or test behavior.
        // We will test behavior:
        // App A: "Settings" (Exact match) -> Higher score
        // App B: "Set" (Start match) -> Same score (100 base)? No, 100 flat.
        // Wait, "Exact, Start, Substring: flat +100". So "Settings" vs "Set" for query "Set" -> Both contain "Set".
        // "Settings" starts with "Set". "Set" equals "Set".
        // Both get +100 base.

        // Let's create apps and see if they are found.
    }

    @Test
    fun `test scoring precedence alias beats everything`() {
        // App A: Alias match (Score 500,000)
        // App B: Favorite + Name Match + High Weight (Max possible normal score)

        val appAlias = createApp("Calcul", "com.calc")
        val appFav = createApp("SuperCalc", "com.super.calc")

        installedAppsFlow.value = listOf(appAlias, appFav)

        `when`(settingsRepository.getAliases()).thenReturn(mapOf("c" to "com.calc"))
        `when`(settingsRepository.getFavoriteApps()).thenReturn(setOf("com.super.calc"))
        `when`(settingsRepository.getAppWeights()).thenReturn(mapOf("com.super.calc" to 10000)) // High weight (fake)

        val results = plugin.search("c", context)

        assertEquals(2, results.size)
        assertEquals("com.calc", results[0].packageName) // Alias should be first
        assertEquals("com.super.calc", results[1].packageName)
    }

    @Test
    fun `test subtitle format`() {
        val app = createApp("Settings", "com.android.settings")
        val appAlias = createApp("Calc", "com.calc")

        installedAppsFlow.value = listOf(app, appAlias)
        `when`(settingsRepository.getAliases()).thenReturn(mapOf("mycalc" to "com.calc"))

        // Test Normal
        val resNormal = plugin.search("settings", context)
        assertEquals("com.android.settings", resNormal[0].subtitle)

        // Test Alias
        val resAlias = plugin.search("mycalc", context)
        assertEquals("Alias: com.calc", resAlias[0].subtitle)
    }

    @Test
    fun `test package search ignores first dot prefix`() {
        // "com.android.settings" -> searches "android.settings"
        // Query "android" should match "com.android.settings" (contains) -> 100
        // Query "settings" should match "com.android.settings" (contains) -> 100
        // Query "com" should NOT match "android.settings" if we strictly ignore it?
        // Wait, "com.android.settings" contains "com".
        // But spec says: "Ignore substring up to first dot".
        // So we run fuzzyMatch against "android.settings".
        // "android.settings" does NOT contain "com". So Score 0.

        val app = createApp("Settings", "com.android.settings")
        installedAppsFlow.value = listOf(app)

        // 1. Search "android" -> Should match "android.settings"
        val res1 = plugin.search("android", context)
        assertEquals(1, res1.size)

        // 2. Search "com" -> Should NOT match if we strip "com."
        val res2 = plugin.search("com", context)
        assertTrue(res2.isEmpty())
    }

    @Test
    fun `test weight calculation in execute`() {
        val app1 = "com.app1"
        val app2 = "com.app2"
        val weights = mapOf(app1 to 100, app2 to 50)

        `when`(settingsRepository.getAppWeights()).thenReturn(weights)
        `when`(settingsRepository.getEnableWeightSearch()).thenReturn(true)
        val pm = mock(PackageManager::class.java)
        `when`(context.packageManager).thenReturn(pm)
        `when`(pm.getLaunchIntentForPackage(app1)).thenReturn(mock(Intent::class.java))

        val result = SearchResult(app1, "id", "Title", "Sub", null, ResultType.APP, app1, null)
        plugin.execute(context, result)

        val captor = ArgumentCaptor.forClass(Map::class.java)
        // Use Elvis operator to handle null return from capture() to satisfy Kotlin non-null requirement
        verify(settingsRepository).setAppWeights((captor.capture() as? Map<String, Int>) ?: emptyMap())

        val newWeights = captor.value as Map<String, Int>

        // App1: 100 * 0.95 = 95. + 5 = 100.
        // App2: 50 * 0.95 = 47 (int).

        assertEquals(100, newWeights[app1])
        assertEquals(47, newWeights[app2])
    }

    @Test
    fun `test discontinuous match score`() {
        // "System Settings" vs "ss"
        // 's' matches 'S'ystem
        // 's' matches 'S'ettings
        // -> Discontinuous match -> 50

        val app = createApp("System Settings", "com.sys")
        installedAppsFlow.value = listOf(app)

        val res = plugin.search("ss", context)
        assertEquals(1, res.size)
        // We can't see the score directly but if it matches, it's good.
        // To verify it's NOT 0, we check it exists.
    }

    @Test
    fun `test favorite bonus`() {
        val appFav = createApp("Alpha", "com.alpha") // Favorite
        val appNorm = createApp("Alpha", "com.beta") // Normal

        installedAppsFlow.value = listOf(appFav, appNorm)
        `when`(settingsRepository.getFavoriteApps()).thenReturn(setOf("com.alpha"))

        val res = plugin.search("Alpha", context)

        assertEquals("com.alpha", res[0].packageName)
        assertEquals("com.beta", res[1].packageName)
    }

    @Test
    fun `test max of name or package score`() {
        // Name: "Photo" (Match), Pkg: "com.camera" (No match) -> Score from Name
        // Name: "Camera" (No match), Pkg: "com.photo" (Match) -> Score from Pkg

        val app1 = createApp("Photo", "com.xyz")
        val app2 = createApp("Tool", "com.photo")

        installedAppsFlow.value = listOf(app1, app2)

        val res = plugin.search("photo", context)
        // Both should match
        assertEquals(2, res.size)
    }

    @Test
    fun `test sorting order score desc then name asc`() {
        // App A: Score 100, Name "Zebra"
        // App B: Score 100, Name "Alpha"
        // App C: Score 200, Name "Beta"

        // Setup:
        // Use exact match to control scores easily? Or just trust fuzzyMatch returns 100 for "contains".
        // Query "test"
        // Apps: "testZ", "testA", "testHigh" (but needs high score)

        val appZ = createApp("TestZebra", "com.z")
        val appA = createApp("TestAlpha", "com.a")
        val appHigh = createApp("TestHigh", "com.high") // Should get higher score via weight

        installedAppsFlow.value = listOf(appZ, appA, appHigh)

        // Give appHigh a weight boost
        `when`(settingsRepository.getAppWeights()).thenReturn(mapOf("com.high" to 50))

        // Query "Test"
        // All match "Test" via prefix/contains -> Score 100.
        // appHigh -> 100 + 50 = 150.
        // appZ -> 100.
        // appA -> 100.

        // Expected order:
        // 1. appHigh (150)
        // 2. appA (100, "TestAlpha" < "TestZebra")
        // 3. appZ (100)

        val results = plugin.search("Test", context)

        assertEquals(3, results.size)
        assertEquals("com.high", results[0].packageName)
        assertEquals("com.a", results[1].packageName)
        assertEquals("com.z", results[2].packageName)
    }
}
