package dev.quxomiya.powerline.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UserDataRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = context.getSharedPreferences("powerline_settings", Context.MODE_PRIVATE)
    private lateinit var repository: UserDataRepository

    @Before
    fun setup() {
        prefs.edit().clear().commit()
        repository = UserDataRepository(context)
    }

    @Test
    fun launchesAreMostRecentFirst() {
        repository.recordAppLaunch("com.a", "com.a.Main", timestamp = 1)
        repository.recordAppLaunch("com.b", null, timestamp = 2)
        assertEquals(
            listOf(HistoryItem("com.b", null, 2), HistoryItem("com.a", "com.a.Main", 1)),
            repository.getHistory()
        )
    }

    @Test
    fun historyIsCapped() {
        repeat(PowerlineDefaults.MAX_HISTORY_ITEMS + 10) { repository.recordAppLaunch("com.app$it", null, timestamp = it.toLong()) }
        val history = repository.getHistory()
        assertEquals(PowerlineDefaults.MAX_HISTORY_ITEMS, history.size)
        assertEquals("com.app${PowerlineDefaults.MAX_HISTORY_ITEMS + 9}", history.first().packageName)
    }

    // V1 stored {id, type, title, ...}; only app launches survive
    @Test
    fun readsV1History() {
        prefs.edit().putString(
            "history",
            """[
                {"id":"com.v1.app","type":"APP","title":"V1 App","timestamp":30},
                {"id":"settings_action","type":"APP","title":"Powerline Status","timestamp":20},
                {"id":"https://google.com","type":"WEB","title":"web","timestamp":10},
                {"id":"1+1","type":"CALCULATOR","title":"2","timestamp":5}
            ]"""
        ).commit()
        assertEquals(listOf(HistoryItem("com.v1.app", null, 30)), repository.getHistory())
    }

    @Test
    fun brokenHistoryReadsAsEmpty() {
        prefs.edit().putString("history", "not json").commit()
        assertEquals(emptyList<HistoryItem>(), repository.getHistory())
    }

    @Test
    fun frecencyDecaysByHalfLife() {
        val now = 1_000_000_000_000L
        repository.recordAppLaunch("com.a", null, timestamp = now - PowerlineDefaults.FRECENCY_HALF_LIFE_MS)
        repository.recordAppLaunch("com.a", null, timestamp = now)
        repository.recordAppLaunch("com.b", null, timestamp = now - 2 * PowerlineDefaults.FRECENCY_HALF_LIFE_MS)
        val frecency = repository.getAppFrecency(now)
        assertEquals(1.5, frecency.getValue("com.a"), 1e-9)
        assertEquals(0.25, frecency.getValue("com.b"), 1e-9)
    }

    @Test
    fun legacyKeysRemovedUserDataKept() {
        prefs.edit()
            .putString("history", "[]")
            .putStringSet("favorite_apps", setOf("com.a"))
            .putBoolean("onboarding_completed", true)
            .putString("app_weights", "{}")
            .putBoolean("is_advanced", true)
            .commit()
        UserDataRepository(context)
        assertEquals(setOf("history", "favorite_apps"), prefs.all.keys)
        assertEquals(setOf("com.a"), UserDataRepository(context).getFavoriteApps())
    }

    @Test
    fun favoritesRoundTrip() {
        repository.setFavoriteApps(setOf("com.a", "com.b"))
        assertEquals(setOf("com.a", "com.b"), UserDataRepository(context).getFavoriteApps())
    }
}
