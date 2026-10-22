package dev.quxomiya.powerline.overlay

import android.app.UiAutomation
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.services.StatusBarAccessibilityService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Alt+Space → type → Enter / Esc on the real overlay window.
//
// Powerline only reacts to keys from a physical keyboard: `input keyevent` is not seen by the
// accessibility key filter. The test writes Alt+Space straight to the keyboard's evdev node,
// which needs the node to be writable by the shell:
//
//   adb root && adb shell "chmod 666 /dev/input/event*"
//
// (scripts/device-tests.sh does this.) Without it every test here is skipped.
@RunWith(AndroidJUnit4::class)
class OverlayE2ETest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private lateinit var device: UiDevice
    private lateinit var keyboard: String

    private val searchField = By.text(PLACEHOLDER)
    private val panel = By.desc("Powerline status")

    @Before
    fun setup() {
        // The default UiAutomation connection suspends other accessibility services,
        // which would take the overlay down with it
        Configurator.getInstance().uiAutomationFlags = UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
        device = UiDevice.getInstance(instrumentation)

        keyboard = findKeyboard() ?: ""
        assumeTrue("No writable keyboard under /dev/input; run: adb root && adb shell \"chmod 666 /dev/input/event*\"", keyboard.isNotEmpty())

        // `am instrument` restarts the app process, which unbinds the service; re-enable it
        val component = "${context.packageName}/${StatusBarAccessibilityService::class.java.name}"
        shell("settings --user current delete secure enabled_accessibility_services")
        shell("settings --user current put secure enabled_accessibility_services $component")
        shell("settings --user current put secure accessibility_enabled 1")

        UserDataRepository(context) // touch prefs before the overlay writes history
        device.pressHome()
        device.waitForIdle()
        openOverlay()
    }

    @After
    fun tearDown() {
        if (keyboard.isEmpty()) return
        if (device.hasObject(panel)) shell("input keyevent KEYCODE_ESCAPE")
        device.pressHome()
    }

    @Test
    fun standbyShowsEngineChipsAndApps() {
        assertNotNull(device.findObject(By.text("YouTube")))
        assertNotNull(device.findObject(By.text("Maps")))
        assertNotNull(device.findObject(searchField))
    }

    @Test
    fun altSpaceTogglesClosed() {
        Thread.sleep(TOGGLE_GAP_MS)
        pressAltSpace()
        assertTrue("overlay still open", device.wait(Until.gone(panel), TIMEOUT))
    }

    @Test
    fun escCloses() {
        shell("input keyevent KEYCODE_ESCAPE")
        assertTrue("overlay still open", device.wait(Until.gone(panel), TIMEOUT))
    }

    @Test
    fun reopenStartsEmpty() {
        type("chrome")
        assertNotNull(device.wait(Until.findObject(By.text("Search Google")), TIMEOUT))
        shell("input keyevent KEYCODE_ESCAPE")
        device.wait(Until.gone(panel), TIMEOUT)
        Thread.sleep(TOGGLE_GAP_MS)
        openOverlay()
        assertNotNull("query not reset", device.wait(Until.findObject(searchField), TIMEOUT))
    }

    @Test
    fun aliasShowsOnlyThatEngine() {
        type("yt laneige")
        assertNotNull(device.wait(Until.findObject(By.text("Search YouTube")), TIMEOUT))
        assertFalse(device.hasObject(By.text("Search Google")))
        assertFalse(device.hasObject(By.text("Share text")))
    }

    @Test
    fun calculatorResultShown() {
        type("12*3")
        assertNotNull(device.wait(Until.findObject(By.text("36")), TIMEOUT))
        assertNotNull(device.findObject(By.text("12*3 · Copy result")))
    }

    @Test
    fun enterLaunchesTopAppAndRecordsHistory() {
        type("powerline")
        assertNotNull(device.wait(Until.findObject(By.text(context.packageName)), TIMEOUT))
        shell("input keyevent KEYCODE_ENTER")
        assertTrue("overlay still open", device.wait(Until.gone(panel), TIMEOUT))
        // Powerline's own launcher activity is the status screen
        assertTrue(device.wait(Until.hasObject(By.pkg(context.packageName)), TIMEOUT))
        val history = UserDataRepository(context).getHistory()
        assertEquals(context.packageName, history.firstOrNull()?.packageName)
    }

    @Test
    fun arrowKeysMoveSelection() {
        type("powerline")
        assertNotNull(device.wait(Until.findObject(By.text("Search Google")), TIMEOUT))
        // Apps, then web search: down once leaves the app for the next result
        shell("input keyevent KEYCODE_DPAD_DOWN")
        shell("input keyevent KEYCODE_DPAD_UP")
        shell("input keyevent KEYCODE_ENTER")
        assertTrue(device.wait(Until.gone(panel), TIMEOUT))
        assertEquals(context.packageName, UserDataRepository(context).getHistory().firstOrNull()?.packageName)
    }

    // apps → web → share → (wrap) apps; Enter then launches the top app
    @Test
    fun tabCyclesResultSections() {
        type("powerline")
        assertNotNull(device.wait(Until.findObject(By.text("Share text")), TIMEOUT))
        repeat(3) { shell("input keyevent KEYCODE_TAB") }
        shell("input keyevent KEYCODE_ENTER")
        assertTrue(device.wait(Until.gone(panel), TIMEOUT))
        assertEquals(context.packageName, UserDataRepository(context).getHistory().firstOrNull()?.packageName)
    }

    // Shift+Tab from the first grid section reaches the engine chips; Enter types the alias.
    // The overlay must stay open: the Enter release once bubbled up to the background and closed it.
    @Test
    fun shiftTabSelectsEngineChip() {
        shell("input keycombination KEYCODE_SHIFT_LEFT KEYCODE_TAB")
        shell("input keyevent KEYCODE_ENTER")
        type("lofi")
        assertNotNull(device.wait(Until.findObject(By.text("Search YouTube Music")), TIMEOUT))
    }

    @Test
    fun switchingAppsCloses() {
        shell("am start -a android.settings.SETTINGS")
        assertTrue("overlay still open", device.wait(Until.gone(panel), TIMEOUT))
    }

    // --- helpers ---

    private fun openOverlay() {
        // The service may still be binding right after setup; retry a few times
        repeat(5) {
            pressAltSpace()
            if (device.wait(Until.hasObject(panel), 1_500)) {
                // Let the first frame settle and focus land in the search field before typing
                device.waitForIdle()
                Thread.sleep(SETTLE_MS)
                return
            }
            Thread.sleep(TOGGLE_GAP_MS)
        }
        throw AssertionError("Alt+Space did not open the overlay")
    }

    private fun type(text: String) {
        shell("input text ${text.replace(" ", "%s")}")
    }

    // executeShellCommand runs no shell, so one sendevent per call
    private fun pressAltSpace() {
        for ((code, value) in listOf(KEY_LEFTALT to 1, KEY_SPACE to 1, KEY_SPACE to 0, KEY_LEFTALT to 0)) {
            shell("sendevent $keyboard $EV_KEY $code $value")
            shell("sendevent $keyboard 0 0 0")
        }
    }

    // First evdev node that reports KEY_SPACE and that the shell can write to
    private fun findKeyboard(): String? {
        val nodes = shell("getevent -p").split("add device").drop(1)
        return nodes
            .filter { "0039" in it } // KEY_SPACE in the key list
            .mapNotNull { Regex("""\d+: (/dev/input/event\d+)""").find(it)?.groupValues?.get(1) }
            // A lone SYN_REPORT is harmless; it fails when the node is not writable
            .firstOrNull { shell("sendevent $it 0 0 0").isBlank() }
    }

    // Through UiDevice so the connection keeps FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES;
    // instrumentation.uiAutomation would connect with default flags and stop the service
    private fun shell(command: String): String = device.executeShellCommand(command)

    private companion object {
        const val PLACEHOLDER = "Search apps, web, math..."
        const val TIMEOUT = 3_000L
        const val SETTLE_MS = 300L
        // StatusBarAccessibilityService ignores toggles closer than 300ms
        const val TOGGLE_GAP_MS = 400L
        const val EV_KEY = 1
        const val KEY_LEFTALT = 56
        const val KEY_SPACE = 57
    }
}
