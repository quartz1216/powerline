#!/usr/bin/env sh
# Runs the instrumented tests on a connected emulator or device.
#
# Powerline only reacts to Alt+Space from a physical keyboard, so OverlayE2ETest writes the
# keys straight to a keyboard's node under /dev/input. On an emulator or rooted device the
# script makes the nodes writable as root. On a production device the shell user can usually
# write them already (group "input"); without a keyboard node, connect one (Bluetooth, USB,
# or scrcpy with a HID keyboard). Without a writable node the E2E tests are skipped.
#
# The app stays installed after the run, so a phone keeps working Powerline.
set -e
cd "$(dirname "$0")/.."

adb wait-for-device
if adb root 2>/dev/null | grep -qv "cannot run as root"; then
    adb wait-for-device
    # Git Bash on Windows rewrites /dev/... paths unless told not to
    MSYS_NO_PATHCONV=1 adb shell "chmod 666 /dev/input/event*"
fi

./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true "$@"
