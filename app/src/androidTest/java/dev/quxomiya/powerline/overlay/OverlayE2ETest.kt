package dev.quxomiya.powerline.overlay

import android.app.UiAutomation
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.services.PowerlineAccessibilityService
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
// accessibility key filter. Alt+Space comes from either
//  - a keyboard's evdev node, written with sendevent. The shell can write the nodes on an
//    emulator or rooted device after `adb root && adb shell "chmod 666 /dev/input/event*"`
//    (scripts/device-tests.sh does this), or
//  - a virtual USB keyboard made with the `hid` tool through /dev/uhid, which the shell user can
//    open on production builds where SELinux keeps it from writing evdev nodes (Android 12+).
// Without either, every test here is skipped.
@RunWith(AndroidJUnit4::class)
class OverlayE2ETest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private lateinit var device: UiDevice
    private var keyboard: Keyboard? = null

    private val searchField = By.text(PLACEHOLDER)
    private val panel = By.desc("Powerline status")

    @Before
    fun setup() {
        // The default UiAutomation connection suspends other accessibility services,
        // which would take the overlay down with it
        Configurator.getInstance().uiAutomationFlags = UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
        device = UiDevice.getInstance(instrumentation)

        keyboard = evdevKeyboard() ?: hidKeyboard()
        assumeTrue("No writable keyboard node and no hid tool; see the comment on OverlayE2ETest", keyboard != null)

        // `am instrument` restarts the app process, which unbinds the service; re-enable it
        val component = "${context.packageName}/${PowerlineAccessibilityService::class.java.name}"
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
        val keyboard = keyboard ?: return
        if (device.hasObject(panel)) shell("input keyevent KEYCODE_ESCAPE")
        device.pressHome()
        keyboard.close()
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
        // The app result's title; its package name line depends on a setting
        assertNotNull(device.wait(Until.findObject(By.text("Powerline")), TIMEOUT))
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

    // Shift+Tab from the first grid section reaches the engine chips; Enter types the alias of the
    // first chip (by name: Maps). The overlay must stay open: the Enter release once bubbled up to
    // the background and closed it.
    @Test
    fun shiftTabSelectsEngineChip() {
        shell("input keycombination KEYCODE_SHIFT_LEFT KEYCODE_TAB")
        shell("input keyevent KEYCODE_ENTER")
        type("lofi")
        assertNotNull(device.wait(Until.findObject(By.text("Search Maps")), TIMEOUT))
    }

    // ROADMAP Phase 7: the overlay shows within 200ms of Alt+Space. SearchOverlay logs the time
    // from the key event to its first frame; UiAutomator's own idle waits would add ~1s here.
    @Test
    fun opensWithin200ms() {
        val times = (1..5).map {
            shell("input keyevent KEYCODE_ESCAPE")
            assertTrue(device.wait(Until.gone(panel), TIMEOUT))
            Thread.sleep(TOGGLE_GAP_MS)
            shell("logcat -c")
            pressAltSpace()
            assertTrue(device.wait(Until.hasObject(panel), TIMEOUT))
            val log = shell("logcat -d -s SearchOverlay:I")
            Regex("""Shown (\d+) ms""").findAll(log).lastOrNull()?.groupValues?.get(1)?.toLong()
                ?: throw AssertionError("no open-time log: $log")
        }
        val median = times.sorted()[times.size / 2]
        android.util.Log.i(TAG, "open times $times ms, median $median ms")
        assertTrue("median open time $median ms ($times)", median < 200)
    }

    // ROADMAP Phase 7: UI updates while typing fit in a 16ms frame. Measured as UI-thread work
    // per frame (input handling to sync with the render thread) from gfxinfo framestats; total
    // frame time on the emulator is dominated by injected-input latency and software rendering.
    @Test
    fun typingFramesFitIn16ms() {
        shell("dumpsys gfxinfo ${context.packageName} reset")
        for (c in "powerline chrome") {
            shell(if (c == ' ') "input keyevent KEYCODE_SPACE" else "input text $c")
            Thread.sleep(60)
        }
        Thread.sleep(500)
        val uiMs = uiThreadFrameTimes(shell("dumpsys gfxinfo ${context.packageName} framestats")).sorted()
        assertTrue("no frames", uiMs.isNotEmpty())
        val p50 = uiMs[uiMs.size / 2]
        val p90 = uiMs[(uiMs.size * 9 / 10).coerceAtMost(uiMs.lastIndex)]
        android.util.Log.i(TAG, "typing UI-thread ms over ${uiMs.size} frames: p50=$p50 p90=$p90 max=${uiMs.last()}")
        assertTrue("UI thread p50 $p50 ms, p90 $p90 ms", p50 <= 16.0)
    }

    // TalkBack stand-in: everything clickable in the overlay has something to read out
    @Test
    fun clickableElementsAreLabeled() {
        assertAllClickablesLabeled("standby")
        type("powerline")
        assertNotNull(device.wait(Until.findObject(By.text("Share text")), TIMEOUT))
        assertAllClickablesLabeled("results")
    }

    @Test
    fun switchingAppsCloses() {
        shell("am start -a android.settings.SETTINGS")
        assertTrue("overlay still open", device.wait(Until.gone(panel), TIMEOUT))
    }

    // --- helpers ---

    // SyncQueued - HandleInputStart for each complete frame in `dumpsys gfxinfo <pkg> framestats`
    private fun uiThreadFrameTimes(framestats: String): List<Double> {
        var header: List<String>? = null
        val times = mutableListOf<Double>()
        for (line in framestats.lines()) {
            if (line.startsWith("Flags,")) {
                header = line.trimEnd(',').split(',')
                continue
            }
            val columns = header ?: continue
            if (line.isEmpty() || !line[0].isDigit()) continue
            val row = columns.zip(line.trimEnd(',').split(',').map { it.toLong() }).toMap()
            // Skip frames after a window layout change (1) or skipped frames (8); other bits are
            // vendor flags (HyperOS sets 0x60 on every frame)
            if ((row.getValue("Flags") and FRAME_FLAGS_EXCLUDED) != 0L) continue
            times += (row.getValue("SyncQueued") - row.getValue("HandleInputStart")) / 1_000_000.0
        }
        return times
    }

    private fun assertAllClickablesLabeled(state: String) {
        val clickables = device.findObjects(By.pkg(context.packageName).clickable(true))
        assertTrue("no clickables in $state", clickables.isNotEmpty())
        // The full-screen background (tap to close) is the only unlabeled clickable allowed
        val unlabeled = clickables.filterNot(::hasLabel).filter { it.visibleBounds.width() < device.displayWidth }
        assertTrue("unlabeled clickables in $state: ${unlabeled.map { it.visibleBounds }}", unlabeled.isEmpty())
    }

    private fun hasLabel(node: androidx.test.uiautomator.UiObject2): Boolean =
        !node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank() || node.children.any(::hasLabel)

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

    private fun pressAltSpace() {
        keyboard!!.altSpace()
    }

    private interface Keyboard {
        fun altSpace()
        fun close() {}
    }

    // A keyboard's evdev node; executeShellCommand runs no shell, so one sendevent per call
    private inner class EvdevKeyboard(private val node: String) : Keyboard {
        override fun altSpace() {
            for ((code, value) in listOf(KEY_LEFTALT to 1, KEY_SPACE to 1, KEY_SPACE to 0, KEY_LEFTALT to 0)) {
                shell("sendevent $node $EV_KEY $code $value")
                shell("sendevent $node 0 0 0")
            }
        }
    }

    // A virtual USB keyboard (HID boot protocol) that lives while `hid -` reads our commands.
    // Each report is the modifier byte, a reserved byte and up to six pressed keys.
    private inner class HidKeyboard : Keyboard {
        private val pipes = uiAutomation().executeShellCommandRw("hid -")
        private val commands = ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])

        init {
            send("""{"id":1,"command":"register","name":"Powerline test keyboard","vid":6353,"pid":1,"bus":"usb","descriptor":$HID_KEYBOARD_DESCRIPTOR}""")
            // Until the input system has added the device
            Thread.sleep(HID_REGISTER_MS)
        }

        override fun altSpace() {
            report(HID_LEFT_ALT)
            report(HID_LEFT_ALT, HID_SPACE)
            report(HID_LEFT_ALT)
            report()
        }

        private fun report(modifiers: Int = 0, key: Int = 0) {
            send("""{"id":1,"command":"report","report":[$modifiers,0,$key,0,0,0,0,0]}""")
            Thread.sleep(HID_REPORT_GAP_MS)
        }

        private fun send(command: String) {
            commands.write("$command\n".toByteArray())
            commands.flush()
        }

        // Ends `hid`, which removes the device
        override fun close() {
            commands.close()
            ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use { it.readBytes() }
        }
    }

    // First evdev node that reports KEY_SPACE and that the shell can write to
    private fun evdevKeyboard(): Keyboard? {
        val nodes = shell("getevent -p").split("add device").drop(1)
        return nodes
            .filter { "0039" in it } // KEY_SPACE in the key list
            .mapNotNull { Regex("""\d+: (/dev/input/event\d+)""").find(it)?.groupValues?.get(1) }
            // A lone SYN_REPORT is harmless; sendevent says on stderr when the node is not writable
            .firstOrNull { shellErrors("sendevent $it 0 0 0").isBlank() }
            ?.let(::EvdevKeyboard)
    }

    private fun hidKeyboard(): Keyboard? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && shell("which hid").isNotBlank()) HidKeyboard() else null

    // Through UiDevice so the connection keeps FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES;
    // instrumentation.uiAutomation would connect with default flags and stop the service
    private fun shell(command: String): String = device.executeShellCommand(command)

    // The connection UiDevice uses: asking with the same flags returns it rather than a new one
    private fun uiAutomation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)

    // stderr of a command, which executeShellCommand drops. Before Android 13 there is no way to
    // read it, so the command counts as having succeeded.
    private fun shellErrors(command: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            shell(command)
            return ""
        }
        val (output, input, errors) = uiAutomation().executeShellCommandRwe(command)
        ParcelFileDescriptor.AutoCloseOutputStream(input).close()
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
        return ParcelFileDescriptor.AutoCloseInputStream(errors).use { it.readBytes().decodeToString() }
    }

    private companion object {
        const val PLACEHOLDER = "Type to Search..."
        const val TAG = "OverlayE2ETest"
        const val TIMEOUT = 3_000L
        const val SETTLE_MS = 300L
        // PowerlineAccessibilityService ignores toggles closer than 300ms
        const val TOGGLE_GAP_MS = 400L
        const val FRAME_FLAGS_EXCLUDED = 0x9L
        const val EV_KEY = 1
        const val KEY_LEFTALT = 56
        const val KEY_SPACE = 57

        // HID boot keyboard: modifier bits, a reserved byte, LED output report, six key slots
        const val HID_KEYBOARD_DESCRIPTOR = "[5,1,9,6,161,1,5,7,25,224,41,231,21,0,37,1,117,1,149,8,129,2," +
            "149,1,117,8,129,1,149,5,117,1,5,8,25,1,41,5,145,2,149,1,117,3,145,1,149,6,117,8,21,0,37,101," +
            "5,7,25,0,41,101,129,0,192]"
        const val HID_LEFT_ALT = 0x04
        const val HID_SPACE = 0x2C
        const val HID_REGISTER_MS = 1_500L
        const val HID_REPORT_GAP_MS = 20L
    }
}
